import re
import os
import zipfile
import shutil

def normalize_digits(text):
    fa_digits = "۰۱۲۳۴۵۶۷۸۹"
    ar_digits = "٠١٢٣٤٥٦٧٨٩"
    en_digits = "0123456789"
    for i in range(10):
        text = text.replace(fa_digits[i], en_digits[i]).replace(ar_digits[i], en_digits[i])
    return text

def normalize_fa(text):
    text = text.replace("ي", "ی")
    text = text.replace("ك", "ک")
    text = text.replace("أ", "ا").replace("إ", "ا").replace("آ", "ا")
    text = text.replace("‌", "") # remove half-space
    text = text.replace("ـ", "") # remove kashida
    text = "".join(text.split()) # remove all whitespace
    diacritics = ["َ", "ُ", "ِ", "ّ", "ْ", "ً", "ٌ", "ٍ"]
    for d in diacritics:
        text = text.replace(d, "")
    return text

def parse_markdown_file(filepath):
    print(f"Reading {filepath}...")
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    
    content = normalize_digits(content)
    puzzle_blocks = re.split(r'#(?=\s+جدول\s+کلمات\s+متقاطع)', content)
    puzzles = []
    
    for block in puzzle_blocks:
        if not block.strip():
            continue
            
        lines = [line.strip() for line in block.split('\n')]
        title_line = lines[0]
        
        num_match = re.search(r'شماره\s+(\d+)', title_line)
        if not num_match:
            continue
        p_num = int(num_match.group(1))
        
        rows, cols = 10, 10
        difficulty = "بسیار آسان"
        id_prefix = "very_easy_new"
            
        p_id = f"{id_prefix}_{p_num}"
        title_text = f"جدول شماره {p_num} ({difficulty})"
        
        print(f"  Found puzzle: {title_text} ({rows}x{cols})")
        
        # Extract Answer Grid Block
        answer_grid_lines = []
        in_answer_block = False
        for line in lines:
            if '## شکل جدول پاسخ‌دار' in line:
                in_answer_block = True
                continue
            if in_answer_block:
                if line.startswith('```'):
                    if answer_grid_lines:
                        break
                    continue
                if line:
                    answer_grid_lines.append(line)
        
        # Extract Raw Grid Block
        raw_grid_lines = []
        in_raw_block = False
        for line in lines:
            if '## شکل جدول خام' in line:
                in_raw_block = True
                continue
            if in_raw_block:
                if line.startswith('```'):
                    if raw_grid_lines:
                        break
                    continue
                if line:
                    raw_grid_lines.append(line)
                    
        # Parse Answer Grid Cells
        grid = []
        for r_line in answer_grid_lines[:rows]:
            row_cells = []
            tokens = r_line.split()
            for token in tokens:
                if '#' in token:
                    count = len(token) // 3
                    row_cells.extend(["■"] * count)
                else:
                    char = token.strip()
                    if char:
                        if char in ['آ', 'أ', 'إ']:
                            char = 'ا'
                        elif char == 'ي':
                            char = 'ی'
                        elif char == 'ك':
                            char = 'ک'
                        row_cells.append(char)
            row_cells = row_cells[:cols] + ["■"] * max(0, cols - len(row_cells))
            grid.append(row_cells)
            
        # Parse Number Grid Cells
        numbers = []
        for r_line in raw_grid_lines[:rows]:
            row_nums = []
            tokens = re.findall(r'(#+|\d+-|\.)', r_line)
            for token in tokens:
                if '#' in token:
                    count = len(token) // 3
                    row_nums.extend([0] * count)
                elif token == '.':
                    row_nums.append(0)
                elif token.endswith('-'):
                    row_nums.append(int(token[:-1]))
            row_nums = row_nums[:cols] + [0] * max(0, cols - len(row_nums))
            numbers.append(row_nums)
            
        if len(grid) != rows or any(len(row) != cols for row in grid):
            while len(grid) < rows:
                grid.append(["■"] * cols)
            for i in range(rows):
                grid[i] = grid[i][:cols] + ["■"] * max(0, cols - len(grid[i]))
                
        if len(numbers) != rows or any(len(row) != cols for row in numbers):
            while len(numbers) < rows:
                numbers.append([0] * cols)
            for i in range(rows):
                numbers[i] = numbers[i][:cols] + [0] * max(0, cols - len(numbers[i]))
                
        for r in range(rows):
            for c in range(cols):
                if grid[r][c] == "■":
                    numbers[r][c] = 0

        across_clues_text = []
        down_clues_text = []
        current_section = None
        for line in lines:
            if '## سوالات افقی' in line:
                current_section = "across"
                continue
            elif '## سوالات عمودی' in line:
                current_section = "down"
                continue
            elif '## ' in line:
                current_section = None
                
            if current_section == "across":
                across_clues_text.append(line)
            elif current_section == "down":
                down_clues_text.append(line)
                
        def parse_table_rows(section_lines, direction):
            clues_list = []
            for line in section_lines:
                if line.startswith('|') and not line.startswith('| شماره') and not line.strip().startswith('|---'):
                    parts = [p.strip() for p in line.split('|')[1:-1]]
                    if len(parts) >= 5:
                        try:
                            num = int(parts[0])
                            clue_text = parts[2]
                            ans = parts[3].replace('**', '').strip()
                            cat = parts[4]
                            
                            ans_norm = ""
                            for ch in ans:
                                if ch in ['آ', 'أ', 'إ']:
                                    ans_norm += 'ا'
                                elif ch == 'ي':
                                    ans_norm += 'ی'
                                elif ch == 'ك':
                                    ans_norm += 'ک'
                                else:
                                    ans_norm += ch
                            
                            clues_list.append({
                                "number": num,
                                "answer": ans_norm,
                                "clueText": clue_text,
                                "category": cat,
                                "direction": direction
                            })
                        except ValueError:
                            continue
            return clues_list
            
        across_clues_parsed = parse_table_rows(across_clues_text, "across")
        down_clues_parsed = parse_table_rows(down_clues_text, "down")
        
        across_connected = []
        down_connected = []
        
        # Trace words
        for r in range(rows):
            c = cols - 1
            while c >= 0:
                if grid[r][c] == "■":
                    c -= 1
                    continue
                
                start_col = c
                word_chars = []
                while c >= 0 and grid[r][c] != "■":
                    word_chars.append(grid[r][c])
                    c -= 1
                
                length = len(word_chars)
                if length >= 2:
                    grid_ans = "".join(word_chars)
                    num = numbers[r][start_col]
                    
                    matched_clue = next((cl for cl in across_clues_parsed if cl['number'] == num), None)
                    if matched_clue:
                        across_connected.append({
                            "number": num,
                            "answer": matched_clue['answer'],
                            "clueText": matched_clue['clueText'],
                            "category": matched_clue['category'],
                            "direction": "across",
                            "startRow": r,
                            "startCol": start_col,
                            "length": length
                        })
                    else:
                        matched_by_ans = next((cl for cl in across_clues_parsed if normalize_fa(cl['answer']) == normalize_fa(grid_ans)), None)
                        if matched_by_ans:
                            across_connected.append({
                                "number": matched_by_ans['number'],
                                "answer": matched_by_ans['answer'],
                                "clueText": matched_by_ans['clueText'],
                                "category": matched_by_ans['category'],
                                "direction": "across",
                                "startRow": r,
                                "startCol": start_col,
                                "length": length
                            })
                            numbers[r][start_col] = matched_by_ans['number']
                            
        for c in range(cols):
            r = 0
            while r < rows:
                if grid[r][c] == "■":
                    r += 1
                    continue
                
                start_row = r
                word_chars = []
                while r < rows and grid[r][c] != "■":
                    word_chars.append(grid[r][c])
                    r += 1
                
                length = len(word_chars)
                if length >= 2:
                    grid_ans = "".join(word_chars)
                    num = numbers[start_row][c]
                    
                    matched_clue = next((cl for cl in down_clues_parsed if cl['number'] == num), None)
                    if matched_clue:
                        down_connected.append({
                            "number": num,
                            "answer": matched_clue['answer'],
                            "clueText": matched_clue['clueText'],
                            "category": matched_clue['category'],
                            "direction": "down",
                            "startRow": start_row,
                            "startCol": c,
                            "length": length
                        })
                    else:
                        matched_by_ans = next((cl for cl in down_clues_parsed if normalize_fa(cl['answer']) == normalize_fa(grid_ans)), None)
                        if matched_by_ans:
                            down_connected.append({
                                "number": matched_by_ans['number'],
                                "answer": matched_by_ans['answer'],
                                "clueText": matched_by_ans['clueText'],
                                "category": matched_by_ans['category'],
                                "direction": "down",
                                "startRow": start_row,
                                "startCol": c,
                                "length": length
                            })
                            numbers[start_row][c] = matched_by_ans['number']

        flat_solutions = []
        for r in grid:
            flat_solutions.extend(r)
            
        puzzles.append({
            "id": p_id,
            "title": title_text,
            "difficulty": difficulty,
            "rows": rows,
            "cols": cols,
            "gridSolutions": flat_solutions,
            "acrossClues": across_connected,
            "downClues": down_connected
        })
        
    return puzzles

def generate_kotlin_code(puzzles, outfile):
    with open(outfile, 'w', encoding='utf-8') as f:
        f.write("package com.example.data\n\n")
        
        for p in puzzles:
            p_id = p['id']
            p_num = int(re.search(r'(\d+)', p_id).group(1))
            new_id = f"very_easy_new_{p_num}"
            title = f"جدول شماره {p_num} (بسیار آسان)"
            
            f.write(f"fun create_{new_id}(): Puzzle = Puzzle(\n")
            f.write(f"    id = \"{new_id}\",\n")
            f.write(f"    title = \"{title}\",\n")
            f.write(f"    difficulty = \"بسیار آسان\",\n")
            f.write(f"    rows = {p['rows']},\n")
            f.write(f"    cols = {p['cols']},\n")
            
            f.write("    gridSolutions = listOf(\n")
            chunks = [p['gridSolutions'][i:i+p['cols']] for i in range(0, len(p['gridSolutions']), p['cols'])]
            for chunk in chunks:
                chunk_str = ", ".join([f'"{char}"' for char in chunk])
                f.write(f"        {chunk_str},\n")
            f.write("    ),\n")
            
            f.write("    acrossClues = listOf(\n")
            for cl in p['acrossClues']:
                esc_clue = cl['clueText'].replace('"', '\\"')
                f.write(f"        Clue({cl['number']}, \"{cl['answer']}\", \"{esc_clue}\", \"{cl['category']}\", \"across\", {cl['startRow']}, {cl['startCol']}, {cl['length']}),\n")
            f.write("    ),\n")
            
            f.write("    downClues = listOf(\n")
            for cl in p['downClues']:
                esc_clue = cl['clueText'].replace('"', '\\"')
                f.write(f"        Clue({cl['number']}, \"{cl['answer']}\", \"{esc_clue}\", \"{cl['category']}\", \"down\", {cl['startRow']}, {cl['startCol']}, {cl['length']}),\n")
            f.write("    )\n")
            f.write(")\n\n")

def main():
    target_zip = '10-10-easy.zip'
    temp_dir = 'temp_extracted_puzzles_new'
    
    puzzles = []
    
    if os.path.exists(target_zip):
        print(f"Extracting {target_zip}...")
        if os.path.exists(temp_dir):
            shutil.rmtree(temp_dir)
        os.makedirs(temp_dir, exist_ok=True)
        
        with zipfile.ZipFile(target_zip, 'r') as zip_ref:
            zip_ref.extractall(temp_dir)
            
        md_files = []
        for root, dirs, files in os.walk(temp_dir):
            for file in files:
                if file.endswith('.md'):
                    md_files.append(os.path.join(root, file))
                    
        md_files.sort()
        for filepath in md_files:
            puzzles.extend(parse_markdown_file(filepath))
            
        shutil.rmtree(temp_dir)
        
    if puzzles:
        # Sort puzzles by ID number
        def get_p_num(p):
            match = re.search(r'(\d+)', p['id'])
            return int(match.group(1)) if match else 999
        puzzles.sort(key=get_p_num)
        
        out_path = 'app/src/main/java/com/example/data/PuzzleDataVeryEasyNew.kt'
        generate_kotlin_code(puzzles, out_path)
        print(f"Successfully processed {len(puzzles)} puzzles and generated {out_path}!")
    else:
        print("No valid puzzles parsed.")

if __name__ == '__main__':
    main()
