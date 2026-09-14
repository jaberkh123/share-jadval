#!/usr/bin/env python3
"""Convert JSON puzzles from جدول مرحله ای/ to Kotlin for PuzzleDataBigGrid."""
import json, os, re

BASE_DIR = "/home/jaber/Downloads/tools/jadval-saz/crossword_scripts/download/جدول مرحله ای"
OUTPUT_DIR = "app/src/main/java/com/jadval/shahr/data"

# (batch_dir_name, grid_size, output_var_name)
BATCHES = [
    ("batch_16x16_popular-done", 16, "biggrid_16x16"),
    ("batch_18x18-done",         18, "biggrid_18x18"),
    ("batch_20x20-done",         20, "biggrid_20x20"),
    ("batch_22x22-done",         22, "biggrid_22x22"),
    ("batch_24x24-done",         24, "biggrid_24x24"),
    ("batch_26x26-done",         26, "biggrid_26x26"),
]

RANK_MAP = {
    16: "ستوان شهر جدول",
    18: "سروان شهر جدول",
    20: "سرگرد شهر جدول",
    22: "سپهبد شهر جدول",
    24: "ارتشبد شهر جدول",
    26: "فیلد مارشال شهر جدول",
}

def norm(ch):
    if ch in ('آ', 'أ', 'إ'):
        return 'ا'
    if ch == 'ي':
        return 'ی'
    if ch == 'ك':
        return 'ک'
    return ch

def esc(s):
    return s.replace('\\', '\\\\').replace('"', '\\"')

def load_puzzle(path):
    with open(path, 'r', encoding='utf-8') as f:
        data = json.load(f)
    sz = data["size"]
    gl, gb = data["grid_letters"], data["grid_black"]
    # Raw grid exactly as stored in JSON.
    # NOTE: JSON stores across words LTR (first letter at leftmost cell),
    # i.e. the JSON grid is the horizontal MIRROR of the real RTL crossword.
    raw = []
    for r in range(sz):
        for c in range(sz):
            if gb[r][c] or gl[r][c] is None:
                raw.append("■")
            else:
                raw.append(norm(gl[r][c]))
    # FIX: mirror the ENTIRE grid horizontally (reverse every row). This:
    #   - makes across words read RTL (first letter at rightmost cell of the span)
    #   - preserves every intersection (unlike the old per-word in-place letter
    #     reversal, which scrambled crossing cells and corrupted down answers)
    #   - reproduces the original published puzzle's black-square pattern
    flat = []
    mgb = []
    for r in range(sz):
        flat.extend(reversed(raw[r * sz:(r + 1) * sz]))
        mgb.append(list(reversed(gb[r])))
    across, down = [], []
    for s in data["slots"]:
        # After mirroring, JSON column c maps to column (sz - 1 - c).
        # Game engine convention: across startCol = RIGHTMOST column of the word,
        # which is exactly where the first letter lands after the mirror.
        start_col = sz - 1 - s["col"]
        cl = {
            "number": 0,
            "answer": "".join(norm(ch) for ch in s["word"]),
            "clueText": s["clue"],
            "category": s.get("category", "عمومی"),
            "direction": s["dir"],
            "startRow": s["row"],
            "startCol": start_col,
            "length": s["length"],
        }
        (across if s["dir"] == "across" else down).append(cl)
    # Assign numbers (RTL-aware: across words go right-to-left)
    ng = [[0] * sz for _ in range(sz)]
    num = 1
    for r in range(sz):
        for c in range(sz):
            if mgb[r][c]:
                continue
            sa, sd = False, False
            # RTL Across: word starts here if RIGHT is blocked/edge AND run to left >= 2
            if (c == sz - 1 or mgb[r][c + 1]):
                l, cc = 0, c
                while cc >= 0 and not mgb[r][cc]:
                    l += 1
                    cc -= 1
                if l >= 2:
                    sa = True
            # Down: word starts here if TOP is blocked/edge AND run down >= 2
            if (r == 0 or mgb[r - 1][c]):
                l, rr = 0, r
                while rr < sz and not mgb[rr][c]:
                    l += 1
                    rr += 1
                if l >= 2:
                    sd = True
            if sa or sd:
                ng[r][c] = num
                num += 1
    for cl in across:
        cl["number"] = ng[cl["startRow"]][cl["startCol"]]
    for cl in down:
        cl["number"] = ng[cl["startRow"]][cl["startCol"]]
    across.sort(key=lambda x: x["number"])
    down.sort(key=lambda x: x["number"])
    # No letter reversal and no down-answer recomputation:
    # answers come directly from the slot words and the mirrored grid
    # matches them exactly, because the mirror preserves all intersections.
    return {"size": sz, "flat": flat, "across": across, "down": down}


def gen_fn(vn, pz, idx):
    sz = pz["size"]
    flat = pz["flat"]
    L = []
    L.append(f"fun create_{vn}(): Puzzle = Puzzle(")
    L.append(f'    id = "{vn}",')
    L.append(f'    title = "جدول شماره {idx} ({sz}×{sz})",')
    L.append(f'    difficulty = "سخت",')
    L.append(f'    rows = {sz},')
    L.append(f'    cols = {sz},')
    L.append("    gridSolutions = listOf(")
    for r in range(sz):
        row = flat[r * sz:(r + 1) * sz]
        L.append("        " + ", ".join(f'"{c}"' for c in row) + ",")
    L.append("    ),")
    L.append("    acrossClues = listOf(")
    for cl in pz["across"]:
        L.append(
            f'        Clue({cl["number"]}, "{cl["answer"]}", "{esc(cl["clueText"])}", "{cl["category"]}", "across", {cl["startRow"]}, {cl["startCol"]}, {cl["length"]}),'
        )
    L.append("    ),")
    L.append("    downClues = listOf(")
    for cl in pz["down"]:
        L.append(
            f'        Clue({cl["number"]}, "{cl["answer"]}", "{esc(cl["clueText"])}", "{cl["category"]}", "down", {cl["startRow"]}, {cl["startCol"]}, {cl["length"]}),'
        )
    L.append("    )")
    L.append(")")
    return "\n".join(L)


def gen_batch(bdir, gsz, vn):
    bp = os.path.join(BASE_DIR, bdir)
    if not os.path.isdir(bp):
        print(f"  skip {bp}")
        return []
    jf = sorted([f for f in os.listdir(bp) if f.endswith(".json") and "batch_summary" not in f])
    if not jf:
        print(f"  skip {bp}")
        return []
    print(f"  {len(jf)} puzzles from {bdir}...")
    O = ["package com.jadval.shahr.data\n"]
    vns = []
    for i, j in enumerate(jf, 1):
        try:
            pz = load_puzzle(os.path.join(bp, j))
            vn2 = f"{vn}_{i:02d}"
            vns.append(vn2)
            O.append(gen_fn(vn2, pz, i))
            O.append("")
        except Exception as e:
            print(f"    err {j}: {e}")
    # Build the list variable
    O.append(f"val {vn}: List<Puzzle> = listOf(")
    for cs in range(0, len(vns), 4):
        ch = vns[cs:cs + 4]
        O.append("    " + ", ".join(f"create_{v}()" for v in ch) + ",")
    O.append(")")
    op = os.path.join(OUTPUT_DIR, f"PuzzleData_batch_{vn}.kt")
    with open(op, "w", encoding="utf-8") as f:
        f.write("\n".join(O) + "\n")
    print(f"  done {op} ({len(vns)} puzzles)")
    return vns


def main():
    os.makedirs(OUTPUT_DIR, exist_ok=True)
    all_v = []
    for bd, gs, vn in BATCHES:
        r = gen_batch(bd, gs, vn)
        if r:
            all_v.append(vn)
    print(f"\nAll batches: {all_v}")

    # Now generate PuzzleModels addition snippet
    print("\n--- Add to PuzzleModels.kt PuzzleDataBigGrid ---")
    print(f"val biggrid_puzzles = {' + '.join(all_v)}")
    print(f"Sections: sizes = {[16,18,20,22,24,26]}")


if __name__ == "__main__":
    main()
