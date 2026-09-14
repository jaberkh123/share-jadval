with open('app/src/main/java/com/example/ui/Screens.kt', 'r') as f:
    lines = f.readlines()

new_lines = []
skip = False
for i, line in enumerate(lines):
    if skip:
        skip = False
        continue
    if "@Composable" in line and i+1 < len(lines) and "@Composable" in lines[i+1]:
        new_lines.append(line)
        skip = True
    else:
        new_lines.append(line)

with open('app/src/main/java/com/example/ui/Screens.kt', 'w') as f:
    f.writelines(new_lines)
print("Removed duplicate annotations")
