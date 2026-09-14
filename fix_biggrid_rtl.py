#!/usr/bin/env python3
"""
Fix BigGrid puzzle data: reverse across-word letters in gridSolutions
so they match the RTL convention (first letter at startCol = rightmost).
"""
import re, os, glob

BASE = "/home/jaber/Downloads/games/حدول-مرحله-ای (2)/app/src/main/java/com/jadval/shahr/data"

def process_file(filepath):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()

    # Split into puzzle functions
    parts = re.split(r'(?=(?:fun |$))', content)
    new_content = parts[0]

    for part in parts[1:]:
        if 'gridSolutions' not in part:
            new_content += part
            continue

        rows_m = re.search(r'rows\s*=\s*(\d+)', part)
        cols_m = re.search(r'cols\s*=\s*(\d+)', part)
        if not rows_m or not cols_m:
            new_content += part
            continue

        rows = int(rows_m.group(1))
        cols = int(cols_m.group(1))

        # Parse gridSolutions - find the full listOf block
        grid_start = part.index('gridSolutions = listOf(')
        # Find matching closing paren
        depth = 0
        grid_end = grid_start
        for i in range(grid_start, len(part)):
            if part[i] == '(':
                depth += 1
            elif part[i] == ')':
                depth -= 1
                if depth == 0:
                    grid_end = i + 1
                    break

        grid_block = part[grid_start:grid_end]
        cells = re.findall(r'"([^"]*)"', grid_block)

        # Build 2D grid
        grid = []
        for r in range(rows):
            grid.append(list(cells[r * cols:(r + 1) * cols]))

        # Parse across clues
        across_m = re.search(r'acrossClues\s*=\s*listOf\((.*?)\)\s*\)', part, re.DOTALL)
        if across_m:
            for cm in re.finditer(
                r'Clue\((\d+),\s*"([^"]*)",\s*"(.*?)",\s*"([^"]*)",\s*"across",\s*(\d+),\s*(\d+),\s*(\d+)\)',
                across_m.group(0)
            ):
                sr = int(cm.group(5))
                sc = int(cm.group(6))
                ln = int(cm.group(7))
                # Cells: sc, sc-1, ..., sc-ln+1
                indices = [sc - i for i in range(ln)]
                letters = [grid[r][c] for r, c in [(sr, idx) for idx in indices]]
                letters.reverse()
                for ci, idx in enumerate(indices):
                    grid[sr][idx] = letters[ci]

        # Parse down clues and update answers
        down_m = re.search(r'downClues\s*=\s*listOf\((.*?)\)\s*\)', part, re.DOTALL)
        down_updates = {}
        if down_m:
            for cm in re.finditer(
                r'Clue\((\d+),\s*"([^"]*)",\s*"(.*?)",\s*"([^"]*)",\s*"down",\s*(\d+),\s*(\d+),\s*(\d+)\)',
                down_m.group(0)
            ):
                num = int(cm.group(1))
                old_answer = cm.group(2)
                sr = int(cm.group(5))
                sc = int(cm.group(6))
                ln = int(cm.group(7))
                new_answer = ''.join(grid[sr + i][sc] for i in range(ln))
                if new_answer != old_answer:
                    down_updates[(num, old_answer)] = new_answer

        # Rebuild gridSolutions block
        new_lines = []
        for r in range(rows):
            row_cells = ', '.join(f'"{grid[r][c]}"' for c in range(cols))
            new_lines.append('        ' + row_cells + ',')
        new_grid_block = 'gridSolutions = listOf(\n' + '\n'.join(new_lines) + '\n    )'

        # Replace in text
        new_part = part[:grid_start] + new_grid_block + part[grid_end:]

        # Update down clue answers
        for (num, old_ans), new_ans in down_updates.items():
            old_str = f'Clue({num}, "{old_ans}",'
            new_str = f'Clue({num}, "{new_ans}",'
            new_part = new_part.replace(old_str, new_str, 1)

        new_content += new_part

    with open(filepath, 'w', encoding='utf-8') as f:
        f.write(new_content)
    print(f"Fixed: {os.path.basename(filepath)}")

for f in sorted(glob.glob(os.path.join(BASE, 'PuzzleData_batch_biggrid_*.kt'))):
    process_file(f)

print("Done!")