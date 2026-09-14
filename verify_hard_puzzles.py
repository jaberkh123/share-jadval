#!/usr/bin/env python3
"""Verify all hard puzzle clues match the grid after flip + startCol fix."""
import re, os

DATA_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)),
    "app/src/main/java/com/jadval/shahr/data")

FILES = [
    ("PuzzleData_batch_hard_8x8.kt", 8),
    ("PuzzleData_batch_hard_10x10.kt", 10),
    ("PuzzleData_batch_hard_12x12.kt", 12),
    ("PuzzleData_batch_hard_14x14.kt", 14),
    ("PuzzleData_batch_hard_16x16.kt", 16),
    ("PuzzleData_batch_hard_18x18.kt", 18),
    ("PuzzleData_batch_hard_20x20.kt", 20),
]

CLUE_RE = re.compile(
    r'Clue\((\d+),\s*"((?:[^"\\]|\\.)*)",\s*"[^"]*",\s*"[^"]*",\s*"(across|down)",\s*(\d+),\s*(\d+),\s*(\d+)\)'
)
GRID_RE = re.compile(r'gridSolutions\s*=\s*listOf\((.*?)\)', re.DOTALL)
CELL_RE = re.compile(r'"([^"]+)"')


def verify_file(filepath, sz):
    with open(filepath, "r", encoding="utf-8") as f:
        content = f.read()

    grids = GRID_RE.findall(content)
    # Split by puzzle function boundaries
    puzzle_sections = re.split(r'(?=fun create_\w)', content)

    errors = 0
    total = 0
    puzzles = 0

    for section in puzzle_sections:
        grid_m = GRID_RE.search(section)
        if not grid_m:
            continue
        puzzles += 1
        cells = CELL_RE.findall(grid_m.group(1))
        if len(cells) != sz * sz:
            print(f"  Puzzle {puzzles}: grid has {len(cells)} cells, expected {sz*sz}")
            errors += 1
            continue

        for m in CLUE_RE.finditer(section):
            answer = m.group(2)
            direction = m.group(3)
            row = int(m.group(4))
            col = int(m.group(5))
            length = int(m.group(6))
            total += 1

            word = ""
            if direction == "across":
                for j in range(length):
                    c = col - j
                    idx = row * sz + c
                    if 0 <= idx < len(cells):
                        word += cells[idx]
                    else:
                        word += "??"
            else:
                for j in range(length):
                    r = row + j
                    idx = r * sz + col
                    if 0 <= idx < len(cells):
                        word += cells[idx]
                    else:
                        word += "??"

            if word != answer:
                print(f"  {direction.upper()} FAIL puzzle#{puzzles}: expected \"{answer}\" row={row} col={col} len={length} got \"{word}\"")
                errors += 1

    return puzzles, total, errors


def main():
    grand_puzzles = 0
    grand_total = 0
    grand_errors = 0
    for fname, sz in FILES:
        fpath = os.path.join(DATA_DIR, fname)
        if not os.path.exists(fpath):
            print(f"  SKIP {fname}")
            continue
        puzzles, total, errors = verify_file(fpath, sz)
        grand_puzzles += puzzles
        grand_total += total
        grand_errors += errors
        status = "OK" if errors == 0 else "FAIL"
        print(f"  {status} {fname}: {puzzles} puzzles, {total} clues, {errors} errors")

    print(f"\n{'='*50}")
    print(f"Total: {grand_puzzles} puzzles, {grand_total} clues, {grand_errors} errors")
    if grand_errors == 0:
        print("ALL CLUES VERIFIED CORRECT!")
    else:
        print(f"{grand_errors} ERRORS FOUND!")


if __name__ == "__main__":
    main()
