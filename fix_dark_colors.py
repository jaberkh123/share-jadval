import re

def replacer(match):
    # This match is: if (LocalDarkTheme.current) Color(0xFF...) else Color(0xFF...)
    dark_color = match.group(1)
    light_color = match.group(2)
    # We want to replace dark_color with something sensible.
    # Let's see what light_color is, and map it.
    
    # Or simply: if we find this pattern, we can replace the dark color with a specific old dark color.
    # But it's easier to just use standard MaterialTheme colors!
    return f"if (LocalDarkTheme.current) Color(0xFF5BC0BE) else Color({light_color})"

with open('app/src/main/java/com/example/ui/Screens.kt', 'r') as f:
    content = f.read()

# Pattern: if \(LocalDarkTheme\.current\) Color\((0x[0-9A-Fa-f]{8})\) else Color\((0x[0-9A-Fa-f]{8})\)
new_content = re.sub(r'if \(LocalDarkTheme\.current\) Color\((0x[0-9A-Fa-f]{8})\) else Color\((0x[0-9A-Fa-f]{8})\)', replacer, content)

with open('app/src/main/java/com/example/ui/Screens.kt', 'w') as f:
    f.write(new_content)
print("Fixed dark colors")
