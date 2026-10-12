#!/usr/bin/env python3
"""2022 개정 영어과 기본 어휘를 기존 영어 단어와 대조한다."""

import csv
import json
import re
from collections import Counter
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
SOURCE = ROOT / "exports/source/curriculum/moe-2022-33-annex14-extracted.txt"
WORDS = ROOT / "exports/words.json"
OUTPUT = ROOT / "docs/tasks/45-suneung-words/candidates.tsv"
# wicked는 고시의 독립 표제어(형용사)다. 기존 wick의 과거형으로 처리하지 않는다.
INDEPENDENT_HEADWORDS = {"wicked"}


def normalized(word: str) -> str:
    return word.strip().lower()


def source_entries(text: str):
    """PDF 264~296쪽의 대표형을 읽는다. 괄호 안 파생어는 별도 대표형이 아니다."""
    section = text.split("=== PDF PAGE 264 ===", 1)[1].split("=== PDF PAGE 297 ===", 1)[0]
    entries = []
    pending = ""
    page = 264
    for raw in section.splitlines():
        line = raw.strip()
        marker = re.fullmatch(r"=== PDF PAGE (\d+) ===", line)
        if marker:
            page = int(marker.group(1))
            continue
        if not line or line in {"영어과 교육과정", "[별표] 영역", "기본 어휘 목록"}:
            continue
        if re.fullmatch(r"[A-Z]|2\d\d", line):
            continue
        if pending:
            pending += " " + line
            if pending.count("(") > pending.count(")"):
                continue
            entries.append((pending, page))
            pending = ""
        elif line.startswith("("):
            # PDF 열 바꿈으로 떨어진 괄호 속 파생어.
            continue
        elif re.match(r"^[a-z]", line) or line == "I*":
            if line.count("(") > line.count(")"):
                pending = line
            else:
                entries.append((line, page))
        else:
            raise ValueError(f"알 수 없는 원문 행: {line!r}")
    if pending:
        raise ValueError(f"닫히지 않은 원문 행: {pending!r}")
    return entries


def headwords(entry: str):
    heading = entry.split("(", 1)[0].strip()
    return [normalized(x.rstrip("*")) for x in heading.split(" / ")]


def inflection_bases(word: str):
    """복수·과거·진행형의 명확한 규칙형 후보만 돌려준다."""
    bases = set()
    if word.endswith("ies") and len(word) > 4:
        bases.add(word[:-3] + "y")
    if word.endswith("es") and len(word) > 3:
        bases.update((word[:-2], word[:-1]))
    if word.endswith("s") and len(word) > 2:
        bases.add(word[:-1])
    if word.endswith("ied") and len(word) > 4:
        bases.add(word[:-3] + "y")
    if word.endswith("ed") and len(word) > 3:
        bases.update((word[:-2], word[:-1]))
        if len(word) > 4 and word[-3] == word[-4]:
            bases.add(word[:-3])
    if word.endswith("ing") and len(word) > 5:
        stem = word[:-3]
        bases.update((stem, stem + "e"))
        if len(stem) > 2 and stem[-1] == stem[-2]:
            bases.add(stem[:-1])
    return bases


def main():
    entries = source_entries(SOURCE.read_text(encoding="utf-8"))
    if len(entries) != 3001:
        raise ValueError(f"원문에서 추출한 대표형 3,001개 예상, 실제 {len(entries)}개")
    existing = {
        normalized(row["word"])
        for row in json.loads(WORDS.read_text(encoding="utf-8"))["words"]
        if row["language"] == "en"
    }
    counts = Counter()
    candidates = []
    special = []
    for entry, page in entries:
        heading = entry.split("(", 1)[0]
        level = "중·공통" if "**" in heading else "초등" if "*" in heading else "그 외"
        counts[level] += 1
        if level != "그 외":
            continue
        variants = headwords(entry)
        if any(" " in word for word in variants):
            counts["구·숙어"] += 1
            special.append((entry, "구·숙어"))
        elif any(word in existing for word in variants):
            counts["기존 중복"] += 1
        elif any(word not in INDEPENDENT_HEADWORDS and inflection_bases(word) & existing for word in variants):
            counts["변화형"] += 1
            special.append((entry, "변화형"))
        else:
            counts["신규 후보"] += 1
            candidates.append((variants[0], entry, f"고시 별책14 PDF {page}쪽"))
    if sum(counts[k] for k in ("구·숙어", "기존 중복", "변화형", "신규 후보")) != 1000:
        raise ValueError("제외 후 분류 합계가 1,000개가 아님")
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    with OUTPUT.open("w", encoding="utf-8", newline="") as f:
        writer = csv.writer(f, delimiter="\t", lineterminator="\n")
        writer.writerow(("단어", "출처 표기", "비고"))
        writer.writerows(candidates)
    print(json.dumps(dict(counts), ensure_ascii=False, indent=2))
    print("별도 집계:", special)


if __name__ == "__main__":
    main()
