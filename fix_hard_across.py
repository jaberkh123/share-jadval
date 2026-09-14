#!/usr/bin/env python3
"""
Fix hard puzzle data: flip grid rows + fix startCol for ALL clues.

The root issue: JSON source has across words LTR (left-to-right in grid),
but the code reads across words RTL (right-to-left, decreasing col).
Easy puzzles use RTL grid (from markdown), hard puzzles use LTR (from JSON).

Fix:
1. Reverse each row in gridSolutions → across words become RTL
2. For ALL clues (across AND down): new_startCol = (cols-1) - original_startCol

We must first undo the previous wrong fix:
  previous_fix: current_startCol = original_startCol + length - 1 (across only)
  undo: original_startCol = current_startCol - length + 1
Then apply: correct_startCol = (cols-1) - original_startCol
"""
import re
import os

DATA_DIR = os.path.join(os.path.dirname(__file__),
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

# Match Clue(...) with direction "across" or "down"
CLUE_RE = re.compile(
    r'Clue\('
    r'(\d+),\s*'                          # 1: number
    r'"((?:[^"\\]|\\.)*)",\s*'            # 2: answer
    r'"((?:[^"\\]|\\.)*)",\s*'            # 3: clueText
    r'"([^"]*)",\s*'                       # 4: category
    r'"(across|down)",\s*'                 # 5: direction
    r'(\d+),\s*'                           # 6: startRow
    r'(\d+),\s*'                           # 7: startCol
    r'(\d+)'                               # 8: length
    r'\)'
)

# Match gridSolutions = listOf(...) — handles multi-line
GRID_RE = re.compile(
    r'(gridSolutions\s*=\s*listOf\(\s*\n)((?:\s*".*?"[,]\s*\n)+?)(\s*\))',
    re.DOTALL
)

# Match rows = N
ROWS_RE = re.compile(r'rows\s*=\s*(\d+)')

# Match a single row of grid solutions: "x", "y", ...
ROW_LINE_RE = re.compile(r'"(.*?)"')


def fix_file(filepath):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()

    # Find grid size
    rows_match = ROWS_RE.search(content)
    if not rows_match:
        print(f"  SKIP: could not find rows in {os.path.basename(filepath)}")
        return
    sz = int(rows_match.group(1))

    # Fix gridSolutions: flip each row
    def flip_grid(m):
        prefix = m.group(1)
        grid_block = m.group(2)
        suffix = m.group(3)

        row_lines = []
        for line in grid_block.strip().split('\n'):
            cells = ROW_LINE_RE.findall(line)
            if cells:
                cells.reverse()
                new_line = '        ' + ', '.join(f'"{c}"' for c in cells) + ','
                row_lines.append(new_line)

        return prefix + '\n'.join(row_lines) + '\n' + suffix

    new_content = GRID_RE.sub(flip_grid, content)

    # Fix Clue startCol
    across_fixed = 0
    down_fixed = 0

    def fix_clue(m):
        nonlocal across_fixed, down_fixed
        number = m.group(1)
        answer = m.group(2)
        clue_text = m.group(3)
        category = m.group(4)
        direction = m.group(5)
        start_row = int(m.group(6))
        start_col = int(m.group(7))
        length = int(m.group(8))

        if direction == "across":
            # Undo previous fix: original_startCol = current_startCol - length + 1
            original_start_col = start_col - length + 1
            across_fixed += 1
        else:
            # Down clues were not changed by previous fix
            original_start_col = start_col
            down_fixed += 1

        # Apply correct fix: flip column position
        correct_start_col = (sz - 1) - original_start_col

        return (f'Clue({number}, "{answer}", "{clue_text}", "{category}", '
                f'"{direction}", {start_row}, {correct_start_col}, {length})')

    new_content = CLUE_RE.sub(fix_clue, new_content)

    if across_fixed > 0 or down_fixed > 0:
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(new_content)
        print(f"  Flipped grid + fixed {across_fixed} across + {down_fixed} down clues in {os.path.basename(filepath)}")
    else:
        print(f"  No clues to fix in {os.path.basename(filepath)}")


def main():
    for fname, sz in FILES:
        fpath = os.path.join(DATA_DIR, fname)
        if os.path.exists(fpath):
            print(f"Processing {fname} (size={sz})...")
            fix_file(fpath)
        else:
            print(f"  SKIP: {fname} not found")
    print("\nDone!")


if __name__ == "__main__":
    main()

