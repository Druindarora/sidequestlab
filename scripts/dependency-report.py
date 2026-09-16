#!/usr/bin/env python3
"""Collect and normalize the weekly dependency report for GitHub Actions."""

from __future__ import annotations

import argparse
import json
import os
import re
import signal
import subprocess
import xml.etree.ElementTree as ET
from datetime import datetime, timezone
from pathlib import Path
from typing import Any


SCHEMA_VERSION = "1.0.0"
UPDATE_TYPES = ("patch", "minor", "major", "unknown")
SEVERITIES = ("info", "low", "moderate", "high", "critical")
MAVEN_UPDATE_LINE = re.compile(
    r"^\s*\[INFO\]\s+(.+?)\s+\.{2,}\s+(.+?)\s+->\s+(.+?)\s*$"
)
ANSI_ESCAPE = re.compile(r"\x1b\[[0-?]*[ -/]*[@-~]")
PRERELEASE_MARKER = re.compile(
    r"(?:^|[._+-])(?:snapshot|alpha|beta|milestone|rc|cr|preview|pre|dev|nightly|ea|m)"
    r"(?:[._-]?\d+)?(?:$|[._+-])",
    re.IGNORECASE,
)
COMPACT_PRERELEASE_MARKER = re.compile(r"^\d+(?:\.\d+)*(?:a|b|rc)\d+$", re.IGNORECASE)
STABLE_VERSION = re.compile(
    r"^v?\d+(?:\.\d+)*(?:[.-]?(?:final|release|ga))?(?:\+[0-9A-Za-z.-]+)?$",
    re.IGNORECASE,
)


def generated_at() -> str:
    return datetime.now(timezone.utc).isoformat(timespec="milliseconds").replace("+00:00", "Z")


def write_json(path: Path, value: Any) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


def parse_json(text: str) -> tuple[Any | None, str | None]:
    try:
        return json.loads(text), None
    except (TypeError, json.JSONDecodeError) as error:
        return None, str(error)


def safe_int(value: str | None) -> int | None:
    try:
        return int(value) if value is not None else None
    except ValueError:
        return None


class CommandRunner:
    """Run collectors while retaining their complete output and execution metadata."""

    def __init__(self, raw_root: Path) -> None:
        self.raw_root = raw_root
        self.raw_root.mkdir(parents=True, exist_ok=True)

    def run(
        self,
        collector_id: str,
        command: str,
        arguments: list[str],
        *,
        cwd: str = ".",
        expected_exit_codes: tuple[int, ...] = (0,),
    ) -> dict[str, Any]:
        raw_directory = self.raw_root / collector_id
        raw_directory.mkdir(parents=True, exist_ok=True)
        stdout = ""
        stderr = ""
        return_code: int | None = None
        spawn_error: str | None = None

        try:
            completed = subprocess.run(
                [command, *arguments],
                cwd=cwd,
                env=os.environ.copy(),
                text=True,
                encoding="utf-8",
                errors="replace",
                capture_output=True,
                check=False,
            )
            stdout = completed.stdout
            stderr = completed.stderr
            return_code = completed.returncode
        except OSError as error:
            spawn_error = str(error)

        exit_code = return_code if return_code is not None and return_code >= 0 else None
        signal_name: str | None = None
        if return_code is not None and return_code < 0:
            try:
                signal_name = signal.Signals(-return_code).name
            except ValueError:
                signal_name = str(-return_code)

        metadata = {
            "command": command,
            "arguments": arguments,
            "working_directory": cwd,
            "exit_code": exit_code,
            "signal": signal_name,
            "spawn_error": spawn_error,
            "expected_exit_code": exit_code in expected_exit_codes if exit_code is not None else False,
        }
        (raw_directory / "stdout.txt").write_text(stdout, encoding="utf-8")
        (raw_directory / "stderr.txt").write_text(stderr, encoding="utf-8")
        write_json(raw_directory / "command.json", metadata)
        return {
            "stdout": stdout,
            "stderr": stderr,
            **metadata,
            "raw_directory": raw_directory.as_posix(),
        }


def parse_version(version: Any) -> tuple[int, int, int] | None:
    if not isinstance(version, str):
        return None
    match = re.match(r"^(\d+)(?:\.(\d+))?(?:\.(\d+))?", version.strip().lstrip("vV"))
    if not match:
        return None
    major, minor, patch = match.groups()
    return int(major), int(minor or 0), int(patch or 0)


def classify_update(current: Any, available: Any) -> str:
    previous = parse_version(current)
    candidate = parse_version(available)
    if previous is None or candidate is None:
        return "unknown"
    if candidate[0] != previous[0]:
        return "major"
    if candidate[1] != previous[1]:
        return "minor"
    if candidate[2] != previous[2]:
        return "patch"
    return "unknown"


def version_stability(version: Any) -> str:
    """Classify only recognized version shapes; ambiguous qualifiers remain unknown."""

    if not isinstance(version, str) or not version.strip():
        return "unknown"
    normalized = version.strip()
    if "${" in normalized:
        return "unknown"
    if PRERELEASE_MARKER.search(normalized) or COMPACT_PRERELEASE_MARKER.match(normalized):
        return "prerelease"
    if STABLE_VERSION.match(normalized):
        return "stable"
    return "unknown"


def aggregate_status(statuses: list[str]) -> str:
    if not statuses:
        return "failed"
    if all(status == "success" for status in statuses):
        return "success"
    if all(status == "failed" for status in statuses):
        return "failed"
    return "degraded"


def npm_role(package_json: dict[str, Any], name: str, reported_type: Any) -> str:
    roles = (
        ("dependencies", "runtime/direct"),
        ("devDependencies", "dev"),
        ("optionalDependencies", "optional"),
        ("peerDependencies", "peer"),
        ("bundledDependencies", "bundled"),
        ("bundleDependencies", "bundled"),
    )
    for field, role in roles:
        collection = package_json.get(field)
        if isinstance(collection, list) and name in collection:
            return role
        if isinstance(collection, dict) and name in collection:
            return role
    reported_roles = {
        "dependencies": "runtime/direct",
        "devDependencies": "dev",
        "optionalDependencies": "optional",
        "peerDependencies": "peer",
    }
    return reported_roles.get(reported_type, "transitive/other")


def normalize_audit(audit: dict[str, Any]) -> dict[str, Any]:
    counts = {severity: 0 for severity in SEVERITIES}
    raw_metadata = audit.get("metadata")
    metadata: dict[str, Any] = raw_metadata if isinstance(raw_metadata, dict) else {}
    raw_metadata_counts = metadata.get("vulnerabilities")
    metadata_counts: dict[str, Any] = (
        raw_metadata_counts if isinstance(raw_metadata_counts, dict) else {}
    )
    for severity in SEVERITIES:
        counts[severity] = int(metadata_counts.get(severity) or 0)
    counts["total"] = int(metadata_counts.get("total") or sum(counts.values()))

    raw_vulnerabilities = audit.get("vulnerabilities")
    vulnerabilities = []
    if isinstance(raw_vulnerabilities, dict):
        for name, raw_item in raw_vulnerabilities.items():
            item = raw_item if isinstance(raw_item, dict) else {}
            normalized_via = []
            raw_via_items = item.get("via")
            via_items = raw_via_items if isinstance(raw_via_items, list) else []
            for via in via_items:
                if isinstance(via, str):
                    normalized_via.append(via)
                elif isinstance(via, dict):
                    normalized_via.append(
                        {
                            "source": via.get("source"),
                            "name": via.get("name"),
                            "title": via.get("title"),
                            "url": via.get("url"),
                            "severity": via.get("severity"),
                            "range": via.get("range"),
                        }
                    )
            vulnerabilities.append(
                {
                    "name": name,
                    "severity": item.get("severity") or "unknown",
                    "direct": item.get("isDirect") if isinstance(item.get("isDirect"), bool) else None,
                    "via": normalized_via,
                    "effects": item.get("effects") if isinstance(item.get("effects"), list) else [],
                    "range": item.get("range"),
                    "nodes": item.get("nodes") if isinstance(item.get("nodes"), list) else [],
                    "fix_available": item.get("fixAvailable"),
                }
            )
    vulnerabilities.sort(key=lambda item: str(item["name"]))
    return {"counts": counts, "vulnerabilities": vulnerabilities}


def version_fields(current: Any, wanted: Any, available: Any) -> dict[str, str]:
    return {
        "current_version_stability": version_stability(current),
        "wanted_version_stability": version_stability(wanted),
        "available_version_stability": version_stability(available),
    }


def read_manifest(directory: str) -> tuple[dict[str, Any], str | None]:
    manifest = Path(directory) / "package.json"
    try:
        value = json.loads(manifest.read_text(encoding="utf-8"))
        if not isinstance(value, dict):
            return {}, "package.json does not contain a JSON object"
        return value, None
    except (OSError, json.JSONDecodeError) as error:
        return {}, str(error)


def collect_npm(
    runner: CommandRunner, raw_root: Path, component: str, directory: str
) -> dict[str, Any]:
    package_json, manifest_error = read_manifest(directory)
    manifest_status = "failed" if manifest_error else "success"

    install_command = runner.run(
        f"{component}/npm-ci",
        "npm",
        ["ci", "--ignore-scripts"],
        cwd=directory,
    )
    install_status = "success" if install_command["expected_exit_code"] else "failed"

    outdated_command = runner.run(
        f"{component}/npm-outdated",
        "npm",
        ["outdated", "--json", "--long"],
        cwd=directory,
        expected_exit_codes=(0, 1),
    )
    outdated_output = outdated_command["stdout"].strip()
    if not outdated_output and outdated_command["exit_code"] == 0:
        outdated_value, outdated_error = {}, None
    elif not outdated_output:
        outdated_value, outdated_error = None, "npm outdated returned no JSON for a non-zero exit code"
    else:
        outdated_value, outdated_error = parse_json(outdated_output)
    outdated_command_error = (
        outdated_value.get("error")
        if isinstance(outdated_value, dict) and outdated_value.get("error")
        else None
    )
    if outdated_error:
        outdated_status = "failed"
    elif outdated_command["expected_exit_code"] and not outdated_command_error:
        outdated_status = "success"
    else:
        outdated_status = "degraded"

    updates = []
    if isinstance(outdated_value, dict) and not outdated_command_error:
        for name, raw_item in outdated_value.items():
            item = raw_item if isinstance(raw_item, dict) else {}
            role = npm_role(package_json, name, item.get("type"))
            current = item.get("current")
            wanted = item.get("wanted")
            available = item.get("latest")
            updates.append(
                {
                    "id": f"npm:{component}:{name}",
                    "component": component,
                    "ecosystem": "npm",
                    "name": name,
                    "category": role,
                    "direct": role != "transitive/other",
                    "current_version": current,
                    "wanted_version": wanted,
                    "available_version": available,
                    **version_fields(current, wanted, available),
                    "update_type": classify_update(current, available),
                    "wanted_update_type": classify_update(current, wanted),
                    "location": item.get("location"),
                    "dependent": item.get("dependent"),
                    "source": f"{raw_root.as_posix()}/{component}/npm-outdated/stdout.txt",
                }
            )
    updates.sort(key=lambda item: str(item["name"]))

    audit_command = runner.run(
        f"{component}/npm-audit",
        "npm",
        ["audit", "--json"],
        cwd=directory,
        expected_exit_codes=(0, 1),
    )
    audit_value, audit_error = parse_json(audit_command["stdout"].strip())
    audit_command_error = (
        audit_value.get("error")
        if isinstance(audit_value, dict) and audit_value.get("error")
        else None
    )
    if audit_error:
        audit_status = "failed"
    elif audit_command["expected_exit_code"] and not audit_command_error:
        audit_status = "success"
    else:
        audit_status = "degraded"
    audit = (
        {"counts": None, "vulnerabilities": []}
        if audit_error or audit_command_error or not isinstance(audit_value, dict)
        else normalize_audit(audit_value)
    )

    statuses = [manifest_status, install_status, outdated_status, audit_status]
    return {
        "component": {
            "id": component,
            "path": directory,
            "ecosystem": "npm",
            "collection": {
                "status": aggregate_status(statuses),
                "manifest": {
                    "status": manifest_status,
                    "parse_error": manifest_error,
                    "source": f"{directory}/package.json",
                },
                "install": {
                    "status": install_status,
                    "exit_code": install_command["exit_code"],
                    "command_error": install_command["spawn_error"],
                    "source": f"{raw_root.as_posix()}/{component}/npm-ci",
                },
                "updates": {
                    "status": outdated_status,
                    "exit_code": outdated_command["exit_code"],
                    "parse_error": outdated_error,
                    "command_error": outdated_command_error or outdated_command["spawn_error"],
                    "source": f"{raw_root.as_posix()}/{component}/npm-outdated",
                },
                "vulnerabilities": {
                    "status": audit_status,
                    "exit_code": audit_command["exit_code"],
                    "parse_error": audit_error,
                    "command_error": audit_command_error or audit_command["spawn_error"],
                    "source": f"{raw_root.as_posix()}/{component}/npm-audit",
                },
            },
            "update_count": len(updates),
            "updates": updates,
            "vulnerabilities": {
                "available": audit_status != "failed",
                **audit,
                "source": f"{raw_root.as_posix()}/{component}/npm-audit/stdout.txt",
            },
        },
        "updates": updates,
        "statuses": statuses,
    }


def child_text(element: ET.Element, name: str, namespace: dict[str, str]) -> str | None:
    child = element.find(f"m:{name}", namespace)
    if child is None or child.text is None:
        return None
    return child.text.strip() or None


def parse_maven_roles(pom_path: Path) -> tuple[dict[str, dict[str, Any]], str | None]:
    """Read direct and managed dependency roles without resolving the effective POM."""

    try:
        root = ET.parse(pom_path).getroot()
    except (OSError, ET.ParseError) as error:
        return {}, str(error)

    namespace_uri = root.tag.split("}", 1)[0].lstrip("{") if "}" in root.tag else ""
    namespace = {"m": namespace_uri}
    prefix = "m:" if namespace_uri else ""
    roles: dict[str, dict[str, Any]] = {}
    locations = (
        (f"{prefix}dependencyManagement/{prefix}dependencies/{prefix}dependency", "dependency_management"),
        (f"{prefix}dependencies/{prefix}dependency", "dependency"),
    )
    for xpath, declaration in locations:
        for dependency in root.findall(xpath, namespace):
            group_id = child_text(dependency, "groupId", namespace)
            artifact_id = child_text(dependency, "artifactId", namespace)
            if not group_id or not artifact_id:
                continue
            scope = child_text(dependency, "scope", namespace) or "compile"
            optional = (child_text(dependency, "optional", namespace) or "false").lower() == "true"
            roles[f"{group_id}:{artifact_id}"] = {
                "scope": scope,
                "optional": optional,
                "declaration": declaration,
            }
    return roles, None


def maven_coordinates(identifier: str) -> str:
    parts = [part.strip() for part in identifier.split(":")]
    return ":".join(parts[:2]) if len(parts) >= 2 else identifier


def collect_maven_category(
    runner: CommandRunner,
    raw_root: Path,
    category: str,
    goal: str,
    dependency_roles: dict[str, dict[str, Any]],
) -> dict[str, Any]:
    command = runner.run(
        f"backend/maven-{category}",
        "bash",
        ["./mvnw", "-B", "-DskipTests", "-DallowSnapshots=true", "-Dstyle.color=never", goal],
        cwd="backend",
    )
    output_lines = command["stdout"].splitlines()
    candidate_lines = []
    entries = []
    for raw_line in output_lines:
        line = ANSI_ESCAPE.sub("", raw_line)
        if re.match(r"^\s*\[INFO\].+\s->\s", line):
            candidate_lines.append(line)
        match = MAVEN_UPDATE_LINE.match(line)
        if not match:
            continue
        identifier, current, available = (part.strip() for part in match.groups())
        role = dependency_roles.get(maven_coordinates(identifier)) if category == "dependency" else None
        dependency_category = "optional" if role and role["optional"] else (role or {}).get("scope", "unknown")
        normalized_category = dependency_category if category == "dependency" else category
        entries.append(
            {
                "id": f"maven:backend:{category}:{identifier}",
                "component": "backend",
                "ecosystem": "maven",
                "name": identifier,
                "category": normalized_category,
                "maven_category": category,
                "maven_scope": role.get("scope") if role else None,
                "optional": role.get("optional") if role else None,
                "declaration": role.get("declaration") if role else None,
                "direct": True,
                "current_version": current or None,
                "wanted_version": None,
                "available_version": available or None,
                **version_fields(current, None, available),
                "update_type": classify_update(current, available),
                "wanted_update_type": None,
                "location": "backend/pom.xml",
                "dependent": None,
                "source": f"{raw_root.as_posix()}/backend/maven-{category}/stdout.txt",
            }
        )

    unparsed_lines = [line for line in candidate_lines if not MAVEN_UPDATE_LINE.match(line)]
    if command["expected_exit_code"] and not unparsed_lines:
        status = "success"
    elif entries or unparsed_lines:
        status = "degraded"
    else:
        status = "failed"
    return {
        "category": category,
        "status": status,
        "exit_code": command["exit_code"],
        "command_error": command["spawn_error"],
        "parse_error": (
            f"{len(unparsed_lines)} update line(s) did not match the Maven parser"
            if unparsed_lines
            else None
        ),
        "unparsed_update_lines": unparsed_lines,
        "source": f"{raw_root.as_posix()}/backend/maven-{category}",
        "entries": entries,
    }


def collect_backend(runner: CommandRunner, raw_root: Path) -> dict[str, Any]:
    dependency_roles, pom_error = parse_maven_roles(Path("backend/pom.xml"))
    pom_status = "failed" if pom_error else "success"
    collections = [
        collect_maven_category(
            runner, raw_root, "dependency", "versions:display-dependency-updates", dependency_roles
        ),
        collect_maven_category(runner, raw_root, "plugin", "versions:display-plugin-updates", dependency_roles),
        collect_maven_category(
            runner, raw_root, "property", "versions:display-property-updates", dependency_roles
        ),
    ]
    updates = sorted(
        [entry for collection in collections for entry in collection["entries"]],
        key=lambda item: str(item["id"]),
    )
    statuses = [pom_status, *(collection["status"] for collection in collections)]
    update_metadata = {}
    for collection in collections:
        metadata = {key: value for key, value in collection.items() if key not in ("category", "entries")}
        metadata["update_count"] = len(collection["entries"])
        update_metadata[collection["category"]] = metadata
    return {
        "component": {
            "id": "backend",
            "path": "backend",
            "ecosystem": "maven",
            "collection": {
                "status": aggregate_status(statuses),
                "pom": {
                    "status": pom_status,
                    "parse_error": pom_error,
                    "source": "backend/pom.xml",
                },
                "updates": update_metadata,
            },
            "update_count": len(updates),
            "updates": updates,
            "vulnerabilities": {
                "available": False,
                "reason": "not_collected_by_design",
            },
        },
        "updates": updates,
        "statuses": statuses,
    }


def flatten_pages(value: Any) -> list[Any]:
    if not isinstance(value, list):
        return []
    flattened = []
    for page in value:
        flattened.extend(page if isinstance(page, list) else [page])
    return flattened


def github_api(runner: CommandRunner, collector_id: str, endpoint: str) -> dict[str, Any]:
    command = runner.run(
        f"github/{collector_id}",
        "gh",
        ["api", "--paginate", "--slurp", "-H", "Accept: application/vnd.github+json", endpoint],
    )
    parsed, parse_error = parse_json(command["stdout"].strip())
    if parse_error:
        status = "failed"
    elif command["expected_exit_code"]:
        status = "success"
    else:
        status = "degraded"
    return {
        "command": command,
        "parsed": parsed,
        "parse_error": parse_error,
        "status": status,
        "values": [] if parse_error else flatten_pages(parsed),
    }


def component_for_file(filename: str) -> str | None:
    if filename.startswith("frontend/"):
        return "frontend"
    if filename.startswith("backend/"):
        return "backend"
    if filename.startswith("apps/public-site/"):
        return "public-site"
    return None


def collect_dependabot_pull_requests(
    runner: CommandRunner, raw_root: Path, repository: str, analyzed_branch: str
) -> dict[str, Any]:
    pulls = github_api(
        runner,
        "dependabot-pulls",
        f"/repos/{repository}/pulls?state=open&base={analyzed_branch}&per_page=100",
    )
    if pulls["status"] == "failed":
        return {
            "status": "failed",
            "exit_code": pulls["command"]["exit_code"],
            "parse_error": pulls["parse_error"],
            "source": f"{raw_root.as_posix()}/github/dependabot-pulls",
            "pull_requests": [],
        }

    dependabot_pulls = [
        pull
        for pull in pulls["values"]
        if isinstance(pull, dict)
        and isinstance(pull.get("user"), dict)
        and pull["user"].get("login") in ("dependabot[bot]", "app/dependabot")
    ]
    detail_statuses = []
    pull_requests = []
    for pull in dependabot_pulls:
        number = pull.get("number")
        raw_head = pull.get("head")
        raw_base = pull.get("base")
        raw_user = pull.get("user")
        head: dict[str, Any] = raw_head if isinstance(raw_head, dict) else {}
        base: dict[str, Any] = raw_base if isinstance(raw_base, dict) else {}
        user: dict[str, Any] = raw_user if isinstance(raw_user, dict) else {}
        files_result = github_api(
            runner, f"pr-{number}-files", f"/repos/{repository}/pulls/{number}/files?per_page=100"
        )
        runs_result = github_api(
            runner,
            f"pr-{number}-ci-runs",
            f"/repos/{repository}/actions/workflows/ci.yml/runs?event=pull_request&head_sha={head.get('sha')}&per_page=100",
        )
        detail_statuses.extend((files_result["status"], runs_result["status"]))

        files = []
        for raw_file in files_result["values"]:
            if not isinstance(raw_file, dict):
                continue
            files.append(
                {
                    "filename": raw_file.get("filename"),
                    "status": raw_file.get("status"),
                    "additions": raw_file.get("additions"),
                    "deletions": raw_file.get("deletions"),
                    "changes": raw_file.get("changes"),
                }
            )
        files.sort(key=lambda item: str(item["filename"]))
        components = sorted(
            {
                component
                for item in files
                if isinstance(item["filename"], str)
                for component in [component_for_file(item["filename"])]
                if component
            }
        )

        workflow_runs = []
        for page in runs_result["values"]:
            if isinstance(page, dict) and isinstance(page.get("workflow_runs"), list):
                workflow_runs.extend(page["workflow_runs"])
        workflow_runs = [
            run
            for run in workflow_runs
            if isinstance(run, dict) and run.get("head_sha") == head.get("sha")
        ]
        workflow_runs.sort(key=lambda run: str(run.get("created_at") or ""), reverse=True)
        latest_run = workflow_runs[0] if workflow_runs else None

        pull_requests.append(
            {
                "number": number,
                "title": pull.get("title"),
                "url": pull.get("html_url"),
                "state": pull.get("state"),
                "target_branch": base.get("ref"),
                "head_ref": head.get("ref"),
                "head_sha": head.get("sha"),
                "author": user.get("login"),
                "draft": bool(pull.get("draft")),
                "created_at": pull.get("created_at"),
                "updated_at": pull.get("updated_at"),
                "components": components,
                "files_collection": {
                    "status": files_result["status"],
                    "exit_code": files_result["command"]["exit_code"],
                    "parse_error": files_result["parse_error"],
                    "source": f"{raw_root.as_posix()}/github/pr-{number}-files",
                },
                "files": files,
                "ci": {
                    "workflow": "CI",
                    "collection": {
                        "status": runs_result["status"],
                        "exit_code": runs_result["command"]["exit_code"],
                        "parse_error": runs_result["parse_error"],
                        "source": f"{raw_root.as_posix()}/github/pr-{number}-ci-runs",
                    },
                    "available": latest_run is not None,
                    "status": latest_run.get("status") if latest_run else None,
                    "conclusion": latest_run.get("conclusion") if latest_run else None,
                    "run_id": latest_run.get("id") if latest_run else None,
                    "url": latest_run.get("html_url") if latest_run else None,
                    "created_at": latest_run.get("created_at") if latest_run else None,
                    "updated_at": latest_run.get("updated_at") if latest_run else None,
                },
            }
        )
    pull_requests.sort(key=lambda pull: int(pull["number"] or 0))
    return {
        "status": aggregate_status([pulls["status"], *detail_statuses]),
        "exit_code": pulls["command"]["exit_code"],
        "parse_error": pulls["parse_error"],
        "source": f"{raw_root.as_posix()}/github/dependabot-pulls",
        "pull_requests": pull_requests,
    }


def analyzed_sha() -> str | None:
    try:
        result = subprocess.run(
            ["git", "rev-parse", "HEAD"], text=True, encoding="utf-8", capture_output=True, check=False
        )
    except OSError:
        return None
    return result.stdout.strip() if result.returncode == 0 else None


def build_report(
    components: list[dict[str, Any]],
    updates: list[dict[str, Any]],
    dependabot: dict[str, Any],
    primary_statuses: list[str],
    analyzed_branch: str,
    raw_root: Path,
) -> dict[str, Any]:
    repository = os.environ.get("GITHUB_REPOSITORY", "")
    server_url = os.environ.get("GITHUB_SERVER_URL", "https://github.com")
    run_id = os.environ.get("GITHUB_RUN_ID")
    return {
        "schema_version": SCHEMA_VERSION,
        "repository": {
            "full_name": repository or None,
            "server_url": server_url,
            "api_url": os.environ.get("GITHUB_API_URL", "https://api.github.com"),
        },
        "workflow": {
            "name": os.environ.get("GITHUB_WORKFLOW"),
            "ref": os.environ.get("GITHUB_REF"),
            "event": os.environ.get("GITHUB_EVENT_NAME"),
            "run_id": run_id,
            "run_number": safe_int(os.environ.get("GITHUB_RUN_NUMBER")),
            "run_attempt": safe_int(os.environ.get("GITHUB_RUN_ATTEMPT")),
            "triggering_sha": os.environ.get("GITHUB_SHA"),
            "run_url": f"{server_url}/{repository}/actions/runs/{run_id}" if repository and run_id else None,
        },
        "analyzed_branch": analyzed_branch,
        "analyzed_sha": analyzed_sha(),
        "generated_at": generated_at(),
        "collection": {
            "status": aggregate_status(primary_statuses),
            "component_statuses": {
                component["id"]: component["collection"]["status"] for component in components
            },
            "dependabot_pull_requests_status": dependabot["status"],
        },
        "components": components,
        "updates": updates,
        "dependabot_pull_requests": {
            "collection": {
                "status": dependabot["status"],
                "exit_code": dependabot["exit_code"],
                "parse_error": dependabot["parse_error"],
                "source": dependabot["source"],
            },
            "count": len(dependabot["pull_requests"]),
            "items": dependabot["pull_requests"],
        },
        "artifacts": [
            {
                "name": "dependency-report",
                "kind": "normalized_report",
                "paths": ["dependency-report.json"],
            },
            {
                "name": "dependency-report-raw",
                "kind": "raw_collection_data",
                "paths": [f"{raw_root.as_posix()}/"],
            },
        ],
    }


def markdown_cell(value: Any) -> str:
    return str(value).replace("|", "\\|").replace("\n", " ")


def build_summary(report: dict[str, Any]) -> str:
    components = report["components"]
    pulls = report["dependabot_pull_requests"]["items"]
    lines = [
        "# Weekly Dependency Report",
        "",
        f"- Analyzed branch: `{report['analyzed_branch']}`",
        f"- Collection status: **{report['collection']['status']}**",
        "",
        "## Collections and updates",
        "",
        "| Component | Collection | Updates | Patch | Minor | Major | Unknown |",
        "| --- | --- | ---: | ---: | ---: | ---: | ---: |",
    ]
    for component in components:
        counts = {
            update_type: sum(
                update.get("update_type") == update_type for update in component["updates"]
            )
            for update_type in UPDATE_TYPES
        }
        lines.append(
            f"| {component['id']} | {component['collection']['status']} | {component['update_count']} "
            f"| {counts['patch']} | {counts['minor']} | {counts['major']} | {counts['unknown']} |"
        )

    lines.extend(
        [
            "",
            "## npm vulnerabilities",
            "",
            "| Component | Collection | Total | Critical | High | Moderate | Low |",
            "| --- | --- | ---: | ---: | ---: | ---: | ---: |",
        ]
    )
    for component in (item for item in components if item["ecosystem"] == "npm"):
        counts = component["vulnerabilities"]["counts"]
        values = {
            key: counts.get(key, "n/a") if isinstance(counts, dict) else "n/a"
            for key in ("total", "critical", "high", "moderate", "low")
        }
        lines.append(
            f"| {component['id']} | {component['collection']['vulnerabilities']['status']} "
            f"| {values['total']} | {values['critical']} | {values['high']} "
            f"| {values['moderate']} | {values['low']} |"
        )

    lines.extend(
        [
            "",
            "## Open Dependabot pull requests targeting dev",
            "",
            f"Collection: **{report['dependabot_pull_requests']['collection']['status']}** "
            f"— detected: **{len(pulls)}**",
            "",
            "| PR | Components | CI status | CI conclusion |",
            "| --- | --- | --- | --- |",
        ]
    )
    if pulls:
        for pull in pulls:
            lines.append(
                f"| [#{pull['number']}]({pull['url']}) {markdown_cell(pull['title'])} "
                f"| {', '.join(pull['components']) or 'unmapped'} "
                f"| {pull['ci']['status'] or 'unavailable'} "
                f"| {pull['ci']['conclusion'] or 'unavailable'} |"
            )
    else:
        lines.append("| — | — | — | — |")
    return "\n".join(lines) + "\n"


def parse_arguments() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output", default="dependency-report.json", type=Path)
    parser.add_argument("--raw-directory", default="dependency-report-raw", type=Path)
    parser.add_argument("--summary", default="dependency-report-summary.md", type=Path)
    parser.add_argument("--analyzed-branch", default=os.environ.get("ANALYZED_BRANCH", "dev"))
    return parser.parse_args()


def main() -> int:
    arguments = parse_arguments()
    runner = CommandRunner(arguments.raw_directory)
    frontend = collect_npm(runner, arguments.raw_directory, "frontend", "frontend")
    backend = collect_backend(runner, arguments.raw_directory)
    public_site = collect_npm(runner, arguments.raw_directory, "public-site", "apps/public-site")
    dependabot = collect_dependabot_pull_requests(
        runner,
        arguments.raw_directory,
        os.environ.get("GITHUB_REPOSITORY", ""),
        arguments.analyzed_branch,
    )

    components = [frontend["component"], backend["component"], public_site["component"]]
    updates = sorted(
        [*frontend["updates"], *backend["updates"], *public_site["updates"]],
        key=lambda item: str(item["id"]),
    )
    primary_statuses = [
        *frontend["statuses"],
        *backend["statuses"],
        *public_site["statuses"],
        dependabot["status"],
    ]
    report = build_report(
        components,
        updates,
        dependabot,
        primary_statuses,
        arguments.analyzed_branch,
        arguments.raw_directory,
    )
    write_json(arguments.output, report)
    arguments.summary.parent.mkdir(parents=True, exist_ok=True)
    arguments.summary.write_text(build_summary(report), encoding="utf-8")
    print(f"Dependency report collection status: {report['collection']['status']}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
