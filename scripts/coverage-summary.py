#!/usr/bin/env python3
"""Build deterministic coverage JSON and compact Markdown summaries."""

from __future__ import annotations

import argparse
import json
import os
import sys
import xml.etree.ElementTree as ET
from collections import defaultdict

HIGH_BRANCH_THRESHOLD = 50.0
MEDIUM_BRANCH_THRESHOLD = 70.0
HIGH_LINE_THRESHOLD = 60.0
MEDIUM_LINE_THRESHOLD = 80.0


def to_int(value: str | None, default: int = 0) -> int:
    if value is None:
        return default
    try:
        return int(value)
    except ValueError:
        return default


def percent(covered: int, total: int) -> float:
    if total <= 0:
        return 100.0
    return round((covered / total) * 100.0, 2)


def classify_priority(lines_pct: float, branches_pct: float) -> tuple[str, bool]:
    if branches_pct < HIGH_BRANCH_THRESHOLD or lines_pct < HIGH_LINE_THRESHOLD:
        return "high", True
    if branches_pct < MEDIUM_BRANCH_THRESHOLD or lines_pct < MEDIUM_LINE_THRESHOLD:
        return "medium", True
    return "low", False


def coverage_sort_key(branches: float, lines: float, *ties: str) -> tuple[float, float, tuple[str, ...]]:
    return (branches, lines, tuple(ties))


def normalize_path(path: str) -> str:
    return os.path.normpath(path).replace("\\", "/")


def repo_relative(path: str, root_dir: str) -> str:
    root_norm = normalize_path(os.path.abspath(root_dir))
    path_norm = normalize_path(os.path.abspath(path))
    if path_norm == root_norm:
        return "."
    prefix = root_norm + "/"
    if path_norm.startswith(prefix):
        return path_norm[len(prefix) :]
    return path_norm


def resolve_lcov_path(source_path: str, root_dir: str, lcov_path: str) -> str:
    if os.path.isabs(source_path):
        return normalize_path(source_path)

    source_norm = normalize_path(source_path)
    candidates = []
    if source_norm.startswith("frontend/") or source_norm.startswith("backend/"):
        candidates.append(os.path.join(root_dir, source_norm))
    elif source_norm.startswith("src/"):
        candidates.append(os.path.join(root_dir, "frontend", source_norm))
        candidates.append(os.path.join(root_dir, source_norm))
    else:
        candidates.append(os.path.join(root_dir, source_norm))

    candidates.append(os.path.join(os.path.dirname(os.path.abspath(lcov_path)), source_norm))

    for candidate in candidates:
        if os.path.exists(candidate):
            return normalize_path(candidate)

    return normalize_path(candidates[0])


def kind_from_path(rel_path: str) -> str:
    if rel_path.endswith(".component.ts"):
        return "component"
    if rel_path.endswith(".service.ts"):
        return "service"
    if rel_path.endswith(".guard.ts"):
        return "guard"
    if rel_path.endswith(".interceptor.ts"):
        return "interceptor"
    if rel_path.endswith(".ts") and (
        "/pages/" in rel_path or "/ui/" in rel_path or "/layout/" in rel_path or rel_path.endswith("/app.ts")
    ):
        return "component"
    return "other"


def is_frontend_app_source(rel_path: str) -> bool:
    if not rel_path.startswith("frontend/src/app/"):
        return False
    if rel_path.startswith("frontend/src/app/api/"):
        return False
    if not rel_path.endswith(".ts"):
        return False
    if rel_path.endswith(".spec.ts") or rel_path.endswith(".test.ts"):
        return False
    return True


def parse_lcov(lcov_path: str, root_dir: str) -> tuple[dict[str, dict[str, int]], dict[str, int]]:
    per_file: dict[str, dict[str, int]] = {}
    totals = {"LF": 0, "LH": 0, "FNF": 0, "FNH": 0, "BRF": 0, "BRH": 0}

    current: dict[str, int | str] | None = None
    with open(lcov_path, encoding="utf-8") as handle:
        for raw_line in handle:
            line = raw_line.strip()
            if line.startswith("SF:"):
                if current is not None:
                    sf = str(current["SF"])
                    file_bucket = per_file.setdefault(sf, {"LF": 0, "LH": 0, "FNF": 0, "FNH": 0, "BRF": 0, "BRH": 0})
                    for key in file_bucket.keys():
                        file_bucket[key] += int(current.get(key, 0))

                resolved = resolve_lcov_path(line[3:], root_dir, lcov_path)
                current = {"SF": resolved, "LF": 0, "LH": 0, "FNF": 0, "FNH": 0, "BRF": 0, "BRH": 0}
                continue

            if current is None:
                continue

            if line == "end_of_record":
                sf = str(current["SF"])
                file_bucket = per_file.setdefault(sf, {"LF": 0, "LH": 0, "FNF": 0, "FNH": 0, "BRF": 0, "BRH": 0})
                for key in file_bucket.keys():
                    file_bucket[key] += int(current.get(key, 0))
                current = None
                continue

            if ":" not in line:
                continue
            key, value = line.split(":", 1)
            if key in ("LF", "LH", "FNF", "FNH", "BRF", "BRH"):
                current[key] = to_int(value)

    for metrics in per_file.values():
        for key in totals.keys():
            totals[key] += metrics.get(key, 0)

    return per_file, totals


def find_counter(element: ET.Element, counter_type: str) -> tuple[int, int]:
    for counter in element.findall("counter"):
        if counter.get("type") == counter_type:
            missed = to_int(counter.get("missed"), 0)
            covered = to_int(counter.get("covered"), 0)
            return missed, covered
    return 0, 0


def parse_backend(jacoco_xml_path: str) -> tuple[dict[str, dict[str, int]], list[dict[str, object]]]:
    root = ET.parse(jacoco_xml_path).getroot()

    totals = {}
    for counter_type in ("LINE", "BRANCH", "INSTRUCTION"):
        missed, covered = find_counter(root, counter_type)
        totals[counter_type] = {"missed": missed, "covered": covered, "total": missed + covered}

    classes: list[dict[str, object]] = []
    for package in root.findall("package"):
        package_name = (package.get("name") or "").replace("/", ".")
        for cls in package.findall("class"):
            class_fqn = (cls.get("name") or "").replace("/", ".")
            class_name = class_fqn.split(".")[-1] if class_fqn else "UNKNOWN_CLASS"

            line_missed, line_covered = find_counter(cls, "LINE")
            branch_missed, branch_covered = find_counter(cls, "BRANCH")
            line_total = line_missed + line_covered
            branch_total = branch_missed + branch_covered

            lines_pct = percent(line_covered, line_total)
            branches_pct = percent(branch_covered, branch_total)
            priority, below_threshold = classify_priority(lines_pct, branches_pct)

            classes.append(
                {
                    "name": class_name,
                    "package": package_name,
                    "lines": lines_pct,
                    "branches": branches_pct,
                    "priority": priority,
                    "belowThreshold": below_threshold,
                    "lineCovered": line_covered,
                    "lineTotal": line_total,
                    "branchCovered": branch_covered,
                    "branchTotal": branch_total,
                }
            )

    return totals, classes


def frontend_units(per_file: dict[str, dict[str, int]], root_dir: str) -> list[dict[str, object]]:
    units: list[dict[str, object]] = []
    for abs_path, metrics in per_file.items():
        rel_path = repo_relative(abs_path, root_dir)
        if not is_frontend_app_source(rel_path):
            continue

        lines_pct = percent(metrics["LH"], metrics["LF"])
        branches_pct = percent(metrics["BRH"], metrics["BRF"])
        priority, below_threshold = classify_priority(lines_pct, branches_pct)
        file_name = os.path.basename(rel_path)
        if file_name.endswith(".ts"):
            file_name = file_name[:-3]

        units.append(
            {
                "name": file_name,
                "path": rel_path,
                "kind": kind_from_path(rel_path),
                "lines": lines_pct,
                "branches": branches_pct,
                "belowThreshold": below_threshold,
                "priority": priority,
            }
        )

    units.sort(key=lambda item: coverage_sort_key(item["branches"], item["lines"], item["path"], item["name"]))
    return units


def backend_priority_packages(classes: list[dict[str, object]]) -> list[dict[str, object]]:
    per_package: dict[str, dict[str, object]] = defaultdict(
        lambda: {
            "lineCovered": 0,
            "lineTotal": 0,
            "branchCovered": 0,
            "branchTotal": 0,
            "weakClassCount": 0,
        }
    )

    for cls in classes:
        package_name = str(cls["package"])
        bucket = per_package[package_name]
        bucket["lineCovered"] = int(bucket["lineCovered"]) + int(cls["lineCovered"])
        bucket["lineTotal"] = int(bucket["lineTotal"]) + int(cls["lineTotal"])
        bucket["branchCovered"] = int(bucket["branchCovered"]) + int(cls["branchCovered"])
        bucket["branchTotal"] = int(bucket["branchTotal"]) + int(cls["branchTotal"])
        if bool(cls["belowThreshold"]):
            bucket["weakClassCount"] = int(bucket["weakClassCount"]) + 1

    packages: list[dict[str, object]] = []
    for package_name, bucket in per_package.items():
        lines_pct = percent(int(bucket["lineCovered"]), int(bucket["lineTotal"]))
        branches_pct = percent(int(bucket["branchCovered"]), int(bucket["branchTotal"]))
        priority, below_threshold = classify_priority(lines_pct, branches_pct)
        packages.append(
            {
                "name": package_name,
                "lines": lines_pct,
                "branches": branches_pct,
                "weakClassCount": int(bucket["weakClassCount"]),
                "belowThreshold": below_threshold,
                "priority": priority,
            }
        )

    packages.sort(key=lambda item: coverage_sort_key(item["branches"], item["lines"], item["name"]))
    return packages


def format_metric(metric: dict[str, object]) -> str:
    return f"{metric['pct']:.2f}% ({metric['covered']}/{metric['total']})"


def render_markdown(summary: dict[str, object], top_limit: int) -> str:
    frontend = summary["frontend"]
    backend = summary["backend"]

    lines: list[str] = []
    lines.append("### Frontend snapshot totals")
    lines.append(f"- Lines: {format_metric(frontend['totals']['lines'])}")
    lines.append(f"- Functions: {format_metric(frontend['totals']['functions'])}")
    lines.append(f"- Branches: {format_metric(frontend['totals']['branches'])}")
    lines.append("")

    lines.append("### Backend snapshot totals")
    lines.append(f"- Lines: {format_metric(backend['totals']['lines'])}")
    lines.append(f"- Branches: {format_metric(backend['totals']['branches'])}")
    lines.append(f"- Instructions: {format_metric(backend['totals']['instructions'])}")
    lines.append("")

    lines.append("### Backend priority packages")
    lines.append("- Decision focus: backend packages are the first remediation target.")
    backend_packages = backend["priorityPackages"][:top_limit]
    if backend_packages:
        for idx, item in enumerate(backend_packages, start=1):
            lines.append(
                f"{idx}. `{item['name']}` | branches {item['branches']:.2f}% | lines {item['lines']:.2f}% "
                f"| weak classes {item['weakClassCount']} | priority {item['priority']}"
            )
    else:
        lines.append("- No backend package data available.")
    lines.append("")

    lines.append("### Backend top weak classes")
    weak_classes = backend["weakClasses"][:top_limit]
    if weak_classes:
        for idx, item in enumerate(weak_classes, start=1):
            lines.append(
                f"{idx}. `{item['package']}.{item['name']}` | branches {item['branches']:.2f}% | "
                f"lines {item['lines']:.2f}% | priority {item['priority']}"
            )
    else:
        lines.append("- No weak backend classes below the configured thresholds.")
    lines.append("")

    lines.append("### Frontend priority components")
    frontend_priority = frontend["priorityComponents"][:top_limit]
    if frontend_priority:
        for idx, item in enumerate(frontend_priority, start=1):
            lines.append(
                f"{idx}. `{item['name']}` ({item['kind']}) | branches {item['branches']:.2f}% | "
                f"lines {item['lines']:.2f}% | priority {item['priority']}"
            )
    else:
        lines.append("- No frontend component/service coverage entries found under `src/app`.")
    lines.append("")

    lines.append("### Frontend top weak components")
    weak_components = frontend["weakComponents"][:top_limit]
    if weak_components:
        for idx, item in enumerate(weak_components, start=1):
            lines.append(
                f"{idx}. `{item['name']}` ({item['kind']}) | branches {item['branches']:.2f}% | "
                f"lines {item['lines']:.2f}% | priority {item['priority']} | path `{item['path']}`"
            )
    else:
        lines.append("- No weak frontend components below the configured thresholds.")
    lines.append("")
    return "\n".join(lines)


def build_summary(frontend_lcov: str, backend_jacoco_xml: str, root_dir: str, top_limit: int) -> dict[str, object]:
    per_file, lcov_totals = parse_lcov(frontend_lcov, root_dir)
    frontend_all_units = frontend_units(per_file, root_dir)
    frontend_weak_units = [item for item in frontend_all_units if item["belowThreshold"]]

    backend_totals_raw, backend_classes_full = parse_backend(backend_jacoco_xml)
    backend_packages = backend_priority_packages(backend_classes_full)

    backend_classes_full.sort(
        key=lambda item: coverage_sort_key(item["branches"], item["lines"], item["package"], item["name"])
    )
    backend_weak_classes = [
        {
            "name": item["name"],
            "package": item["package"],
            "lines": item["lines"],
            "branches": item["branches"],
            "priority": item["priority"],
        }
        for item in backend_classes_full
        if item["belowThreshold"]
    ]

    if not backend_weak_classes:
        backend_weak_classes = [
            {
                "name": item["name"],
                "package": item["package"],
                "lines": item["lines"],
                "branches": item["branches"],
                "priority": item["priority"],
            }
            for item in backend_classes_full[:top_limit]
        ]

    frontend_priority_components = frontend_all_units[: max(top_limit * 2, top_limit)]

    return {
        "frontend": {
            "totals": {
                "lines": {
                    "covered": lcov_totals["LH"],
                    "total": lcov_totals["LF"],
                    "pct": percent(lcov_totals["LH"], lcov_totals["LF"]),
                },
                "functions": {
                    "covered": lcov_totals["FNH"],
                    "total": lcov_totals["FNF"],
                    "pct": percent(lcov_totals["FNH"], lcov_totals["FNF"]),
                },
                "branches": {
                    "covered": lcov_totals["BRH"],
                    "total": lcov_totals["BRF"],
                    "pct": percent(lcov_totals["BRH"], lcov_totals["BRF"]),
                },
            },
            "priorityComponents": frontend_priority_components,
            "weakComponents": frontend_weak_units,
        },
        "backend": {
            "totals": {
                "lines": {
                    "covered": backend_totals_raw["LINE"]["covered"],
                    "total": backend_totals_raw["LINE"]["total"],
                    "pct": percent(backend_totals_raw["LINE"]["covered"], backend_totals_raw["LINE"]["total"]),
                },
                "branches": {
                    "covered": backend_totals_raw["BRANCH"]["covered"],
                    "total": backend_totals_raw["BRANCH"]["total"],
                    "pct": percent(backend_totals_raw["BRANCH"]["covered"], backend_totals_raw["BRANCH"]["total"]),
                },
                "instructions": {
                    "covered": backend_totals_raw["INSTRUCTION"]["covered"],
                    "total": backend_totals_raw["INSTRUCTION"]["total"],
                    "pct": percent(
                        backend_totals_raw["INSTRUCTION"]["covered"], backend_totals_raw["INSTRUCTION"]["total"]
                    ),
                },
            },
            "priorityPackages": backend_packages,
            "weakClasses": backend_weak_classes,
        },
    }


def write_json(path: str, payload: dict[str, object]) -> None:
    with open(path, "w", encoding="utf-8") as handle:
        json.dump(payload, handle, indent=2, sort_keys=False)
        handle.write("\n")


def write_text(path: str, text: str) -> None:
    with open(path, "w", encoding="utf-8") as handle:
        handle.write(text)


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Generate coverage summary JSON + markdown.")
    parser.add_argument("--frontend-lcov", required=True, help="Path to frontend lcov.info")
    parser.add_argument("--backend-jacoco", required=True, help="Path to backend jacoco.xml")
    parser.add_argument("--root-dir", required=True, help="Repo root dir used for stable relative paths")
    parser.add_argument("--json-output", required=True, help="Output path for coverage-summary.json")
    parser.add_argument("--markdown-output", required=True, help="Output path for compact markdown summary")
    parser.add_argument("--top-limit", type=int, default=5, help="Top N entries in markdown lists")
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    try:
        summary = build_summary(
            frontend_lcov=os.path.abspath(args.frontend_lcov),
            backend_jacoco_xml=os.path.abspath(args.backend_jacoco),
            root_dir=os.path.abspath(args.root_dir),
            top_limit=max(1, args.top_limit),
        )
        markdown = render_markdown(summary, top_limit=max(1, args.top_limit))
        write_json(args.json_output, summary)
        write_text(args.markdown_output, markdown)
    except ET.ParseError as err:
        print(f"Failed to parse JaCoCo XML ({args.backend_jacoco}): {err}", file=sys.stderr)
        return 1
    except OSError as err:
        print(f"Coverage summary generation failed: {err}", file=sys.stderr)
        return 1

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
