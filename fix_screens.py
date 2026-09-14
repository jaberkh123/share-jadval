import re

with open('app/src/main/java/com/example/ui/Screens.kt', 'r') as f:
    content = f.read()

# Instead of hardcoding, let's use MaterialTheme properties
# Or we can just use the specific colors from Color.kt:
# DarkSurface = 0xFF1C2541
# DarkPrimary = 0xFF5BC0BE
# DarkSecondary = 0xFF6FFFE9

# Replace Color(0xFF5BC0BE) in specific contexts:
content = content.replace("if (LocalDarkTheme.current) Color(0xFF5BC0BE) else Color(0xFFADE8F4)", "if (LocalDarkTheme.current) Color(0xFF1C2541) else Color(0xFFADE8F4)")
content = content.replace("if (LocalDarkTheme.current) Color(0xFF5BC0BE) else Color(0xFF00B4D8)", "if (LocalDarkTheme.current) Color(0xFF5BC0BE) else Color(0xFF00B4D8)")
content = content.replace("if (LocalDarkTheme.current) Color(0xFF5BC0BE) else Color(0xFF0096C7)", "if (LocalDarkTheme.current) Color(0xFF6FFFE9) else Color(0xFF0096C7)")
content = content.replace("if (LocalDarkTheme.current) Color(0xFF5BC0BE) else Color(0xFF0077B6)", "if (LocalDarkTheme.current) Color(0xFF5BC0BE) else Color(0xFF0077B6)")
content = content.replace("if (LocalDarkTheme.current) Color(0xFF5BC0BE) else Color(0xF2ADE8F4)", "if (LocalDarkTheme.current) Color(0xF21C2541) else Color(0xF2ADE8F4)")

with open('app/src/main/java/com/example/ui/Screens.kt', 'w') as f:
    f.write(content)

print("Replaced!")
