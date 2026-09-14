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
    
    # Normalize digits
    content = normalize_digits(content)
    
    # Split by puzzle sections
    puzzle_blocks = re.split(r'#(?=\s+جدول\s+کلمات\s+متقاطع)', content)
    
    puzzles = []
    
    for block in puzzle_blocks:
        if not block.strip():
            continue
            
        lines = [line.strip() for line in block.split('\n')]
        title_line = lines[0]
        
        # Determine number
        num_match = re.search(r'شماره\s+(\d+)', title_line)
        if not num_match:
            continue
        p_num = int(num_match.group(1))
        
        # Determine dimensions (with primary folder path check)
        normalized_path = normalize_digits(filepath)
        path_dim = None
        if '25' in normalized_path:
            path_dim = 25
        elif '17' in normalized_path:
            path_dim = 17
        elif '10' in normalized_path:
            path_dim = 10

        if path_dim is not None:
            rows, cols = path_dim, path_dim
        else:
            dim_match = re.search(r'ابعاد\s*جدول:\s*\*\*(\d+)\s*[×xX]\s*(\d+)\*\*', block)
            if dim_match:
                rows = int(dim_match.group(1))
                cols = int(dim_match.group(2))
            else:
                if '۲۵×۲۵' in title_line or '25×25' in title_line or '25x25' in title_line or '۲۵ × ۲۵' in block:
                    rows, cols = 25, 25
                elif '۱۷×۱۷' in title_line or '17×17' in title_line or '17x17' in title_line or '۱۷ × ۱۷' in block:
                    rows, cols = 17, 17
                elif '۱۰×۱۰' in title_line or '10×10' in title_line or '10x10' in title_line or '۱۰ × ۱۰' in block:
                    rows, cols = 10, 10
                else:
                    rows, cols = 10, 10 # default fallback
        
        # Determine difficulty
        normalized_path_lower = normalized_path.lower()
        if 'easy' in normalized_path_lower or 'بسیار آسان' in filepath:
            difficulty = "بسیار آسان"
            id_prefix = "very_easy"
        elif rows == 25:
            difficulty = "سخت"
            id_prefix = "hard"
        elif rows == 17:
            difficulty = "متوسط"
            id_prefix = "medium"
        else:
            difficulty = "آسان"
            id_prefix = "easy"
            
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
            
        # Size sanity checks
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
                
        # Align black cells
        for r in range(rows):
            for c in range(cols):
                if grid[r][c] == "■":
                    numbers[r][c] = 0

        # Extract clue tables
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
        # Across: RTL
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
                            
        # Down: Top to Bottom
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
        f.write("data class Clue(\n")
        f.write("    val number: Int,\n")
        f.write("    val answer: String,\n")
        f.write("    val clueText: String,\n")
        f.write("    val category: String,\n")
        f.write("    val direction: String, // \"across\" or \"down\"\n")
        f.write("    val startRow: Int,\n")
        f.write("    val startCol: Int,\n")
        f.write("    val length: Int\n")
        f.write(")\n\n")
        
        f.write("data class Puzzle(\n")
        f.write("    val id: String,\n")
        f.write("    val title: String,\n")
        f.write("    val difficulty: String, // \"آسان\" | \"متوسط\" | \"سخت\"\n")
        f.write("    val rows: Int = 10,\n")
        f.write("    val cols: Int = 10,\n")
        f.write("    val gridSolutions: List<String>,\n")
        f.write("    val acrossClues: List<Clue>,\n")
        f.write("    val downClues: List<Clue>\n")
        f.write(") {\n")
        f.write("    fun getCellNumbers(): List<Int> {\n")
        f.write("        val numbers = MutableList(rows * cols) { 0 }\n")
        f.write("        for (clue in acrossClues) {\n")
        f.write("            val index = clue.startRow * cols + clue.startCol\n")
        f.write("            if (index in numbers.indices) {\n")
        f.write("                numbers[index] = clue.number\n")
        f.write("            }\n")
        f.write("        }\n")
        f.write("        for (clue in downClues) {\n")
        f.write("            val index = clue.startRow * cols + clue.startCol\n")
        f.write("            if (index in numbers.indices) {\n")
        f.write("                numbers[index] = clue.number\n")
        f.write("            }\n")
        f.write("        }\n")
        f.write("        return numbers\n")
        f.write("    }\n")
        f.write("}\n\n")
        
        f.write("object PuzzleData {\n")
        
        object_names = []
        for p in puzzles:
            p_var = p['id']
            object_names.append(p_var)
            
            # Generate getter-based structure using a builder function to avoid JVM static block size limits
            f.write(f"    private fun create_{p_var}(): Puzzle = Puzzle(\n")
            f.write(f"        id = \"{p['id']}\",\n")
            f.write(f"        title = \"{p['title']}\",\n")
            f.write(f"        difficulty = \"{p['difficulty']}\",\n")
            f.write(f"        rows = {p['rows']},\n")
            f.write(f"        cols = {p['cols']},\n")
            
            f.write("        gridSolutions = listOf(\n")
            chunks = [p['gridSolutions'][i:i+p['cols']] for i in range(0, len(p['gridSolutions']), p['cols'])]
            for chunk in chunks:
                chunk_str = ", ".join([f'"{char}"' for char in chunk])
                f.write(f"            {chunk_str},\n")
            f.write("        ),\n")
            
            f.write("        acrossClues = listOf(\n")
            for cl in p['acrossClues']:
                esc_clue = cl['clueText'].replace('"', '\\"')
                f.write(f"            Clue({cl['number']}, \"{cl['answer']}\", \"{esc_clue}\", \"{cl['category']}\", \"across\", {cl['startRow']}, {cl['startCol']}, {cl['length']}),\n")
            f.write("        ),\n")
            
            f.write("        downClues = listOf(\n")
            for cl in p['downClues']:
                esc_clue = cl['clueText'].replace('"', '\\"')
                f.write(f"            Clue({cl['number']}, \"{cl['answer']}\", \"{esc_clue}\", \"{cl['category']}\", \"down\", {cl['startRow']}, {cl['startCol']}, {cl['length']}),\n")
            f.write("        )\n")
            f.write("    )\n\n")
            
            f.write(f"    val {p_var}: Puzzle get() = create_{p_var}()\n\n")
            
        f.write("    val puzzlesList: List<Puzzle> by lazy {\n")
        f.write("        listOf(\n")
        for chunk in [object_names[i:i+4] for i in range(0, len(object_names), 4)]:
            calls = [f"create_{name}()" for name in chunk]
            f.write(f"            {', '.join(calls)},\n")
        f.write("        )\n")
        f.write("    }\n\n")
        
        f.write("    fun getPuzzlesByDifficulty(difficulty: String): List<Puzzle> {\n")
        f.write("        return puzzlesList.filter { it.difficulty == difficulty }\n")
        f.write("    }\n\n")
        
        f.write("    fun getPuzzleById(id: String): Puzzle? {\n")
        f.write("        return puzzlesList.find { it.id == id }\n")
        f.write("    }\n")
        f.write("}\n")

def main():
    # Find any uploaded zip files
    zip_files = [f for f in os.listdir('.') if f.endswith('.zip')]
    temp_dir = 'temp_extracted_puzzles'
    
    puzzles = []
    processed_zip = None
    
    if zip_files:
        target_zip = zip_files[0]
        processed_zip = target_zip
        print(f"Detected zip file: {target_zip}. Extracting...")
        if os.path.exists(temp_dir):
            shutil.rmtree(temp_dir)
        os.makedirs(temp_dir, exist_ok=True)
        
        with zipfile.ZipFile(target_zip, 'r') as zip_ref:
            zip_ref.extractall(temp_dir)
            
        # Find all .md files in the temp_dir
        md_files = []
        for root, dirs, files in os.walk(temp_dir):
            for file in files:
                if file.endswith('.md'):
                    md_files.append(os.path.join(root, file))
                    
        md_files.sort()
        for filepath in md_files:
            puzzles.extend(parse_markdown_file(filepath))
            
        # Clean up extraction directory
        shutil.rmtree(temp_dir)
    else:
        # No zip found, check for local .md files in workspace
        md_files = [f for f in os.listdir('.') if f.endswith('.md') and f != 'path.md']
        md_files.sort()
        if md_files:
            print("No zip file found, but detected local markdown files in workspace. Parsing...")
            for filepath in md_files:
                puzzles.extend(parse_markdown_file(filepath))
        else:
            print("No crossword puzzles found to parse! Applet will remain with empty puzzlesList.")
            return

    if puzzles:
        # Sort puzzles by difficulty and number for cleaner order
        # Difficulties: بسیار آسان, آسان, متوسط, سخت
        diff_order = {"بسیار آسان": 0, "آسان": 1, "متوسط": 2, "سخت": 3}
        def puzzle_sort_key(p):
            p_num = 999
            num_match = re.search(r'(\d+)', p['id'])
            if num_match:
                p_num = int(num_match.group(1))
            return (diff_order.get(p['difficulty'], 99), p_num)
            
        puzzles.sort(key=puzzle_sort_key)
        
        generate_kotlin_code(puzzles, 'app/src/main/java/com/example/data/PuzzleModels.kt')
        print(f"Successfully processed {len(puzzles)} puzzles and generated PuzzleModels.kt!")
        
        # Clean up the original zip file if processed
        if processed_zip and os.path.exists(processed_zip):
            print(f"Deleting the processed zip file: {processed_zip}")
            os.remove(processed_zip)
    else:
        print("No valid puzzles parsed.")

if __name__ == '__main__':
    main()
