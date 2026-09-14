#!/usr/bin/env python3
"""Convert JSON puzzles from javal-shakt/ to Kotlin for PuzzleDataHard."""
import json, os, re

BASE_DIR = "javal-shakt"
OUTPUT_DIR = "app/src/main/java/com/jadval/shahr/data"

BATCHES = [
    ("batch_8x8",       8,  "hard_8x8"),
    ("batch_10x10",    10,  "hard_10x10"),
    ("batch_12x12",    12,  "hard_12x12"),
    ("batch_14x14",    14,  "hard_14x14"),
    ("batch_16x16",    16,  "hard_16x16"),
    ("batch_18x18",    18,  "hard_18x18"),
    ("batch_20x20_v2", 20,  "hard_20x20"),
]

DIFFICULTY_MAP = {8:"آسان",10:"آسان",12:"متوسط",14:"متوسط",16:"متوسط",18:"متوسط",20:"سخت"}

def norm(ch):
    if ch in ('آ','أ','إ'): return 'ا'
    if ch == 'ي': return 'ی'
    if ch == 'ك': return 'ک'
    return ch

def esc(s):
    return s.replace('\\','\\\\').replace('"','\\"')

def load_puzzle(path):
    with open(path,'r',encoding='utf-8') as f: data=json.load(f)
    sz=data["size"]
    diff=DIFFICULTY_MAP.get(sz,"آسان")
    gl,gb=data["grid_letters"],data["grid_black"]
    flat=[]
    for r in range(sz):
        for c in range(sz):
            if gb[r][c] or gl[r][c] is None: flat.append("■")
            else: flat.append(norm(gl[r][c]))
    across,down=[],[]
    for s in data["slots"]:
        cl={"number":0,"answer":norm(s["word"]),"clueText":s["clue"],"category":s.get("category","عمومی"),"direction":s["dir"],"startRow":s["row"],"startCol":s["col"],"length":s["length"]}
        (across if s["dir"]=="across" else down).append(cl)
    # Assign numbers
    ng=[[0]*sz for _ in range(sz)]
    num=1
    for r in range(sz):
        for c in range(sz):
            if gb[r][c]: continue
            sa,sd=False,False
            if c==sz-1 or gb[r][c+1]:
                l,cc=0,c
                while cc>=0 and not gb[r][cc]: l+=1;cc-=1
                if l>=2: sa=True
            if r==0 or gb[r-1][c]:
                l,rr=0,r
                while rr<sz and not gb[rr][c]: l+=1;rr+=1
                if l>=2: sd=True
            if sa or sd: ng[r][c]=num;num+=1
    for cl in across: cl["number"]=ng[cl["startRow"]][cl["startCol"]]
    for cl in down: cl["number"]=ng[cl["startRow"]][cl["startCol"]]
    across.sort(key=lambda x:x["number"])
    down.sort(key=lambda x:x["number"])
    return {"size":sz,"difficulty":diff,"flat":flat,"across":across,"down":down}

def gen_fn(vn,pz,idx):
    sz=pz["size"];flat=pz["flat"];diff=pz["difficulty"]
    L=[]
    L.append(f"fun create_{vn}(): Puzzle = Puzzle(")
    L.append(f'    id = "{vn}",')
    L.append(f'    title = "جدول شماره {idx} ({sz}x{sz})",')
    L.append(f'    difficulty = "{diff}",')
    L.append(f'    rows = {sz},')
    L.append(f'    cols = {sz},')
    L.append('    gridSolutions = listOf(')
    for r in range(sz):
        row=flat[r*sz:(r+1)*sz]
        L.append('        '+', '.join(f'"{c}"' for c in row)+',')
    L.append('    ),')
    L.append('    acrossClues = listOf(')
    for cl in pz["across"]:
        L.append(f'        Clue({cl["number"]}, "{cl["answer"]}", "{esc(cl["clueText"])}", "{cl["category"]}", "across", {cl["startRow"]}, {cl["startCol"]}, {cl["length"]}),')
    L.append('    ),')
    L.append('    downClues = listOf(')
    for cl in pz["down"]:
        L.append(f'        Clue({cl["number"]}, "{cl["answer"]}", "{esc(cl["clueText"])}", "{cl["category"]}", "down", {cl["startRow"]}, {cl["startCol"]}, {cl["length"]}),')
    L.append('    )')
    L.append(')')
    return '\n'.join(L)

def gen_batch(bdir,gsz,vn):
    bp=os.path.join(BASE_DIR,bdir)
    if not os.path.isdir(bp): print(f'  skip {bp}');return []
    jf=sorted([f for f in os.listdir(bp) if f.endswith('.json') and 'batch_summary' not in f])
    if not jf: print(f'  skip {bp}');return []
    print(f'  {len(jf)} puzzles from {bdir}...')
    O=["package com.jadval.shahr.data\n"]
    vns=[]
    for i,j in enumerate(jf,1):
        try:
            pz=load_puzzle(os.path.join(bp,j))
            vn2=f'{vn}_{i:02d}';vns.append(vn2)
            O.append(gen_fn(vn2,pz,i));O.append("")
        except Exception as e: print(f'    err {j}: {e}')
    O.append(f'val {vn}: List<Puzzle> = listOf(')
    for cs in range(0,len(vns),4):
        ch=vns[cs:cs+4]
        O.append('    '+', '.join(f'create_{v}()' for v in ch)+',')
    O.append(')')
    op=os.path.join(OUTPUT_DIR,f'PuzzleData_batch_{vn}.kt')
    with open(op,'w',encoding='utf-8') as f: f.write('\n'.join(O)+'\n')
    print(f'  done {op}');return vns

def main():
    os.makedirs(OUTPUT_DIR,exist_ok=True)
    all_v=[]
    for bd,gs,vn in BATCHES:
        r=gen_batch(bd,gs,vn)
        if r: all_v.append(vn)
    # Update PuzzleModels.kt
    mp=os.path.join(OUTPUT_DIR,'PuzzleModels.kt')
    with open(mp,'r',encoding='utf-8') as f: c=f.read()
    new_list='    override val puzzlesList: List<Puzzle> by lazy {\n'
    if all_v: new_list+='        '+'+\n        '.join(all_v)+'\n'
    else: new_list+='        emptyList<Puzzle>()\n'
    new_list+='    }'
    c=re.sub(r'(object PuzzleDataHard : PuzzleDataSource \{.*?override val puzzlesList: List<Puzzle> by lazy \{).*?\n    \}',
             lambda m: m.group(1)+'\n'+new_list.split('\n',1)[1]+'\n}', c, flags=re.DOTALL)
    with open(mp,'w',encoding='utf-8') as f: f.write(c)
    print(f'\nDone! {len(all_v)} batches, updated PuzzleDataHard')

if __name__=='__main__': main()
