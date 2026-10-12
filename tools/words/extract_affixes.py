#!/usr/bin/env python3
"""활성 단어와 실제로 존재하는 어간만 연결해 1단계 검수 자료를 만든다."""

from __future__ import annotations

import argparse
import csv
import json
import re
from collections import Counter
from dataclasses import dataclass
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
WORD_FILE = ROOT / "exports/words.json"
AFFIX_FILE = ROOT / "exports/source/affixes/affixes.tsv"
OUTPUT_DIR = ROOT / "docs/tasks/46-affix-breakdown"
ALPHABETIC = re.compile(r"[a-z]+\Z")


@dataclass(frozen=True)
class Affix:
    form: str
    kind: str
    meaning: str
    example: str

    @property
    def letters(self) -> str:
        return self.form.strip("-")


# 이 목록에는 사람이 뜻을 대조한 뒤 확정한 (단어, 어간, 접사)만 넣는다.
# 형태가 맞는다는 이유만으로 true로 올리지 않는다.
_CLEAR_PAIRS = [
    ("abandonment", "abandon", "-ment"),
    ("abolitionist", "abolition", "-ist"),
    ("abundantly", "abundant", "-ly"),
    ("acceleration", "accelerate", "-ion"),
    ("acidic", "acid", "-ic"),
    ("acidity", "acid", "-ity"),
    ("accreditation", "accredit", "-ation"),
    ("adornment", "adorn", "-ment"),
    ("aggressiveness", "aggressive", "-ness"),
    ("alignment", "align", "-ment"),
    ("amplifier", "amplify", "-er"),
    ("antivirus", "virus", "anti-"),
    ("assertiveness", "assertive", "-ness"),
    ("atomic", "atom", "-ic"),
    ("attainment", "attain", "-ment"),
    ("beholder", "behold", "-er"),
    ("blissful", "bliss", "-ful"),
    ("bulky", "bulk", "-y"),
    ("carver", "carve", "-er"),
    ("circulation", "circulate", "-ion"),
    ("citation", "cite", "-ation"),
    ("coercion", "coerce", "-ion"),
    ("collaborative", "collaborate", "-ive"),
    ("compiler", "compile", "-er"),
    ("composer", "compose", "-er"),
    ("computation", "compute", "-ation"),
    ("computational", "computation", "-al"),
    ("conqueror", "conquer", "-or"),
    ("contamination", "contaminate", "-ion"),
    ("contradiction", "contradict", "-ion"),
    ("correlation", "correlate", "-ion"),
    ("customarily", "customary", "-ly"),
    ("defiance", "defy", "-ance"),
    ("defiant", "defy", "-ant"),
    ("dehydration", "dehydrate", "-ion"),
    ("depletion", "deplete", "-ion"),
    ("deportation", "deport", "-ation"),
    ("deprivation", "deprive", "-ation"),
    ("detonation", "detonate", "-ion"),
    ("devastation", "devastate", "-ion"),
    ("deterioration", "deteriorate", "-ion"),
]
TRANSPARENT: dict[tuple[str, str, str], str] = {
    pair: "단어와 어간의 현재 뜻이 접사의 뜻을 더한 구성과 직접 이어짐 (1차 수동 검토)"
    for pair in _CLEAR_PAIRS
}

OPAQUE: dict[str, str] = {
    "discover": "현대적 뜻인 '발견하다'는 dis(반대) + cover(덮다)로 설명되지 않는다.",
    "resign": "'사임하다'는 re(다시) + sign(서명하다)로 설명되지 않는다.",
    "dispatch": "'파견하다'는 dis(반대) + patch(헝겊 조각)로 설명되지 않는다.",
}


def read_affixes(path: Path) -> list[Affix]:
    with path.open(encoding="utf-8", newline="") as stream:
        reader = csv.DictReader(stream, delimiter="\t")
        assert reader.fieldnames == ["form", "type", "meaning_ko", "example"]
        affixes = [Affix(row["form"], row["type"], row["meaning_ko"], row["example"]) for row in reader]
    assert 40 <= len(affixes) <= 60
    for affix in affixes:
        assert affix.kind in {"prefix", "suffix"}
        assert affix.letters.isalpha() and affix.meaning and affix.example
        assert (affix.form.endswith("-") if affix.kind == "prefix" else affix.form.startswith("-"))
    assert len({(item.form, item.kind) for item in affixes}) == len(affixes)
    return affixes


def active_words(path: Path) -> tuple[int, dict[tuple[str, str], dict]]:
    data = json.loads(path.read_text(encoding="utf-8"))
    words = data["words"]
    active = {
        (row["language"], row["word"].strip().lower()): row
        for row in words
        if not row.get("deleted", False) and ALPHABETIC.fullmatch(row["word"].strip().lower())
    }
    assert len(active) == sum(not row.get("deleted", False) and bool(ALPHABETIC.fullmatch(row["word"].strip().lower())) for row in words)
    return len(words), active


def stem_spellings(word: str, affix: Affix) -> list[tuple[str, str]]:
    """표면 철자를 원래 어간 철자로 되돌린다. 존재 여부는 호출자가 검사한다."""
    letters = affix.letters
    if affix.kind == "prefix":
        return [(word[len(letters):], "그대로")] if word.startswith(letters) and len(word) > len(letters) + 1 else []
    if not word.endswith(letters) or len(word) <= len(letters) + 1:
        return []
    part = word[:-len(letters)]
    possible = [(part, "그대로")]
    if part.endswith("i"):
        possible.append((part[:-1] + "y", "y→i"))
    if not part.endswith("e"):
        possible.append((part + "e", "-e 탈락"))
    if len(part) >= 2 and part[-1] == part[-2]:
        possible.append((part[:-1], "자음 겹침"))
    if letters == "ly" and not part.endswith("le"):
        possible.append((part + "le", "-le→-ly"))
    return possible


def extract(active: dict[tuple[str, str], dict], affixes: list[Affix]) -> list[dict[str, str]]:
    found: dict[tuple[str, str, str, str], dict[str, str]] = {}
    for (language, word), row in active.items():
        for affix in affixes:
            for stem, rule in stem_spellings(word, affix):
                stem_row = active.get((language, stem))
                if stem_row is None or stem == word:
                    continue
                key = (language, word, stem, affix.form)
                if key in found:
                    continue
                meaning = row["meaningKo"].removeprefix("*")
                stem_meaning = stem_row["meaningKo"].removeprefix("*")
                gloss = f"{affix.meaning} + {stem_meaning} → {meaning}"
                reviewed = TRANSPARENT.get((word, stem, affix.form))
                reason = reviewed or OPAQUE.get(word) or "철자와 어간은 확인했으나 뜻의 연결은 사람 검수 전"
                found[key] = {
                    "language": language,
                    "word": word,
                    "분해": f"{affix.form} + {stem}" if affix.kind == "prefix" else f"{stem} + {affix.form}",
                    "어간": stem,
                    "접사": affix.form,
                    "종류": affix.kind,
                    "철자 규칙": rule,
                    "제안 뜻풀이": gloss,
                    "transparent": "true" if reviewed else "false",
                    "판단 이유": reason,
                }
    return [found[key] for key in sorted(found)]


def self_test(affixes: list[Affix]) -> None:
    index = {(item.form, item.kind): item for item in affixes}
    examples = [
        ("happiness", "happy", "-ness", "suffix", "y→i"),
        ("usable", "use", "-able", "suffix", "-e 탈락"),
        ("impossible", "possible", "im-", "prefix", "그대로"),
        ("beginner", "begin", "-er", "suffix", "자음 겹침"),
        ("simply", "simple", "-ly", "suffix", "-le→-ly"),
    ]
    for word, stem, form, kind, rule in examples:
        assert (stem, rule) in stem_spellings(word, index[(form, kind)]), (word, stem)
    sample = {
        ("en", word): {"meaningKo": word}
        for word in {"unhappy", "happy", "impossible", "possible", "discover", "cover", "uncle"}
    }
    sample_rows = extract(sample, affixes)
    assert any(row["word"] == "unhappy" and row["어간"] == "happy" for row in sample_rows)
    assert any(row["word"] == "impossible" and row["어간"] == "possible" for row in sample_rows)
    assert not any(row["word"] == "uncle" for row in sample_rows)
    del sample[("en", "cover")]
    assert not any(row["word"] == "discover" for row in extract(sample, affixes))


def write_review(path: Path, rows: list[dict[str, str]]) -> None:
    columns = ["language", "word", "분해", "어간", "접사", "종류", "철자 규칙", "제안 뜻풀이", "transparent", "판단 이유"]
    with path.open("w", encoding="utf-8", newline="") as stream:
        writer = csv.DictWriter(stream, fieldnames=columns, delimiter="\t", lineterminator="\n")
        writer.writeheader()
        writer.writerows(rows)


def write_report(path: Path, total: int, active: dict[tuple[str, str], dict], affixes: list[Affix], rows: list[dict[str, str]]) -> None:
    by_affix = Counter(row["접사"] for row in rows)
    transparent = sum(row["transparent"] == "true" for row in rows)
    count_words = len({(row["language"], row["word"]) for row in rows})
    multi = Counter((row["language"], row["word"]) for row in rows)
    lines = [
        "# TASK 46 1단계: 접사 후보 추출 결과", "",
        "## 입력과 기준", "",
        f"- 단어 입력: `exports/words.json` 전체 {total:,}행, 활성 알파벳 단어 {len(active):,}개. `deleted: true` 9개는 어간과 후보 양쪽에서 제외.",
        f"- 접사 입력: `exports/source/affixes/affixes.tsv` {len(affixes)}개. 같은 언어의 활성 단어를 어간으로 찾은 경우만 후보로 기록.",
        "- 철자 되돌리기: 직접 결합, y→i, -e 탈락, 자음 겹침, -le→-ly. im-/ir-/il-은 각각 별도 접두사로 검사.",
        "- 어원은 주장하지 않는다. 철자만 맞는 경우는 뜻이 연결된다고 단정하지 않는다.", "",
        "## 숫자 요약", "",
        "| 지표 | 개수 |", "|------|-----:|",
        f"| 전체 후보 분해 | {len(rows):,} |",
        f"| 후보 단어(중복 제거) | {count_words:,} |",
        f"| 여러 분해가 잡힌 단어 | {sum(count > 1 for count in multi.values()):,} |",
        f"| 투명한 분해로 1차 확인(`true`) | {transparent:,} |",
        f"| 의미 미확인·애매함(`false`) | {len(rows) - transparent:,} |", "",
        "`false`에는 뜻이 불투명한 분석과 아직 사람이 검수하지 않은 분석이 함께 있다. `review.tsv`의 판단 이유를 보고 확정해야 한다.", "",
        "## 접사별 후보 분포", "", "| 접사 | 종류 | 후보 분해 |", "|------|------|----------:|",
    ]
    for affix in affixes:
        lines.append(f"| `{affix.form}` | {affix.kind} | {by_affix[affix.form]:,} |")
    lines += [
        "", "## 검수용 목록", "",
        "- [review.tsv](review.tsv): 단어·분해·어간·접사·철자 규칙·제안 뜻풀이·투명성·판단 이유를 한 줄씩 기록.",
        "- `transparent=true`도 최종 승인 전에는 앱 데이터로 쓰지 않는다. 한 단어에 후보가 여러 개면 뜻이 가장 정확한 분석 하나를 선택하거나 전부 제외한다.",
        "- 권장 검수: `false` 중 빈도 높은 접사와 친숙한 단어부터 뜻을 대조하고, 각 행에 true/false와 판단 이유를 확정한다. 애매하면 false를 유지한다.",
        "", "## 자체 검증", "",
        "- 합성 예시로 happiness→happy, usable→use, impossible→possible, beginner→begin, simply→simple을 확인했다.",
        "- 합성 자료에서 어간이 빠진 discover는 후보가 되지 않고, `cle`이 없는 uncle도 후보가 되지 않는다.",
        "- 실제 결과의 모든 어간·후보 단어는 같은 언어의 활성 단어 목록에 존재한다.", "",
        "## 다음 단계", "",
        "- 사용자 검수 방식 결정 전까지 2단계 씨드, DB, 앱 코드는 만들거나 고치지 않는다.", "",
    ]
    path.write_text("\n".join(lines), encoding="utf-8")


def main() -> None:
    parser = argparse.ArgumentParser(description="접사 후보와 1단계 검수 파일 생성")
    parser.add_argument("--words", type=Path, default=WORD_FILE)
    parser.add_argument("--affixes", type=Path, default=AFFIX_FILE)
    parser.add_argument("--output", type=Path, default=OUTPUT_DIR)
    args = parser.parse_args()
    affixes = read_affixes(args.affixes)
    self_test(affixes)
    total, active = active_words(args.words)
    rows = extract(active, affixes)
    assert all((row["language"], row["어간"]) in active and (row["language"], row["word"]) in active for row in rows)
    assert set(TRANSPARENT) <= {(row["word"], row["어간"], row["접사"]) for row in rows}
    args.output.mkdir(parents=True, exist_ok=True)
    write_review(args.output / "review.tsv", rows)
    write_report(args.output / "REPORT-1.md", total, active, affixes, rows)
    print(f"후보 {len(rows)}개, 투명 {sum(row['transparent'] == 'true' for row in rows)}개, 애매·미검수 {sum(row['transparent'] == 'false' for row in rows)}개")


if __name__ == "__main__":
    main()
