#!/usr/bin/env python3
"""Create deterministic human/machine-readable summaries from SpotBugs XML."""

from __future__ import annotations

import argparse
import json
import os
import sys
import xml.etree.ElementTree as ET
from collections import Counter
from datetime import datetime, timezone


def to_int(value: str | None, default: int) -> int:
    if value is None:
        return default
    try:
        return int(value)
    except ValueError:
        return default


def first_attr(elements: list[ET.Element], attr: str) -> str | None:
    for element in elements:
        value = element.get(attr)
        if value:
            return value
    return None


def extract_primary_class(bug: ET.Element) -> str:
    class_name = first_attr(list(bug.findall("Class")), "classname")
    if class_name:
        return class_name

    source_class = first_attr(list(bug.findall(".//SourceLine")), "classname")
    return source_class or "UNKNOWN_CLASS"


def extract_primary_file(bug: ET.Element, class_name: str) -> str:
    source_path = first_attr(list(bug.findall(".//SourceLine")), "sourcepath")
    return source_path or class_name


def counter_to_top_list(counter: Counter[str], limit: int) -> list[dict[str, object]]:
    ranked = sorted(counter.items(), key=lambda item: (-item[1], item[0]))[:limit]
    return [{"name": name, "count": count} for name, count in ranked]


def build_summary(xml_file: str, top_limit: int) -> dict[str, object]:
    root = ET.parse(xml_file).getroot()
    bugs = list(root.findall("BugInstance"))

    total_findings = 0
    categories: Counter[str] = Counter()
    bug_types: Counter[str] = Counter()
    classes: Counter[str] = Counter()
    files: Counter[str] = Counter()
    priorities: Counter[str] = Counter()
    review_groups: Counter[tuple[int, str, str]] = Counter()

    for bug in bugs:
        total_findings += 1

        priority_value = to_int(bug.get("priority"), 99)
        priority_label = f"P{priority_value}" if priority_value != 99 else "P?"
        category = bug.get("category") or "UNKNOWN_CATEGORY"
        bug_type = bug.get("type") or "UNKNOWN_TYPE"
        pattern = bug.get("abbrev") or ""
        bug_label = f"{bug_type} [{pattern}]" if pattern else bug_type
        class_name = extract_primary_class(bug)
        file_name = extract_primary_file(bug, class_name)

        priorities[priority_label] += 1
        categories[category] += 1
        bug_types[bug_label] += 1
        classes[class_name] += 1
        files[file_name] += 1
        review_groups[(priority_value, category, bug_label)] += 1

    ranked_review = sorted(
        review_groups.items(),
        key=lambda item: (item[0][0], -item[1], item[0][1], item[0][2]),
    )[:top_limit]

    review_first = [
        {
            "priority": f"P{priority}" if priority != 99 else "P?",
            "category": category,
            "bug_type": bug_type,
            "count": count,
        }
        for (priority, category, bug_type), count in ranked_review
    ]

    return {
        "generated_at_utc": datetime.now(timezone.utc).strftime("%Y-%m-%d %H:%M:%S UTC"),
        "input_xml": os.path.abspath(xml_file),
        "total_findings": total_findings,
        "top_categories": counter_to_top_list(categories, min(top_limit, 5)),
        "top_bug_types": counter_to_top_list(bug_types, top_limit),
        "top_classes": counter_to_top_list(classes, top_limit),
        "top_files": counter_to_top_list(files, top_limit),
        "priority_breakdown": counter_to_top_list(priorities, 10),
        "review_first_rule": (
            "Sort groups by priority ascending (P1 highest), then finding count "
            "descending, then category and bug type alphabetically."
        ),
        "review_first": review_first,
    }


def render_text(summary: dict[str, object]) -> str:
    def format_list(items: list[dict[str, object]], empty_text: str) -> list[str]:
        if not items:
            return [f"  - {empty_text}"]
        return [f"  {idx}. {entry['name']}: {entry['count']}" for idx, entry in enumerate(items, start=1)]

    lines: list[str] = []
    lines.append("SpotBugs Readable Summary")
    lines.append("=========================")
    lines.append(f"Generated at: {summary['generated_at_utc']}")
    lines.append(f"Source XML: {summary['input_xml']}")
    lines.append(f"Total findings: {summary['total_findings']}")
    lines.append("")

    lines.append("Priority breakdown")
    lines.append("------------------")
    lines.extend(format_list(summary["priority_breakdown"], "No priority data"))
    lines.append("")

    lines.append("Top categories")
    lines.append("--------------")
    lines.extend(format_list(summary["top_categories"], "No findings"))
    lines.append("")

    lines.append("Top bug types / patterns")
    lines.append("------------------------")
    lines.extend(format_list(summary["top_bug_types"], "No findings"))
    lines.append("")

    lines.append("Top classes affected")
    lines.append("--------------------")
    lines.extend(format_list(summary["top_classes"], "No findings"))
    lines.append("")

    lines.append("Top files affected")
    lines.append("------------------")
    lines.extend(format_list(summary["top_files"], "No findings"))
    lines.append("")

    lines.append("Review first")
    lines.append("------------")
    lines.append(f"Rule: {summary['review_first_rule']}")

    review_first = summary["review_first"]
    if not review_first:
        lines.append("  - No findings")
    else:
        for idx, entry in enumerate(review_first, start=1):
            lines.append(
                f"  {idx}. {entry['priority']} | {entry['category']} | "
                f"{entry['bug_type']} -> {entry['count']}"
            )

    lines.append("")
    return "\n".join(lines)


def write_text(path: str, content: str) -> None:
    with open(path, "w", encoding="utf-8") as f:
        f.write(content)


def write_json(path: str, payload: dict[str, object]) -> None:
    with open(path, "w", encoding="utf-8") as f:
        json.dump(payload, f, indent=2, sort_keys=True)
        f.write("\n")


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Summarize SpotBugs XML reports.")
    parser.add_argument("--input", required=True, help="Input SpotBugs XML file")
    parser.add_argument("--text-output", required=True, help="Readable summary output path")
    parser.add_argument("--json-output", help="Optional JSON summary output path")
    parser.add_argument("--top-limit", type=int, default=10, help="Max rows per top list (default: 10)")
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    try:
        summary = build_summary(args.input, max(1, args.top_limit))
    except ET.ParseError as err:
        print(f"Failed to parse SpotBugs XML ({args.input}): {err}", file=sys.stderr)
        return 1
    except OSError as err:
        print(f"Failed to read SpotBugs XML ({args.input}): {err}", file=sys.stderr)
        return 1

    try:
        write_text(args.text_output, render_text(summary))
        if args.json_output:
            write_json(args.json_output, summary)
    except OSError as err:
        print(f"Failed to write summary output: {err}", file=sys.stderr)
        return 1

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
