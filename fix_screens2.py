import re

with open('app/src/main/java/com/example/ui/Screens.kt', 'r') as f:
    content = f.read()

# Fix isDark colors
content = content.replace("if (isDark) Color(0xFF023E8A) else Color(0xFFADE8F4)", "if (isDark) Color(0xFF1C2541) else Color(0xFFADE8F4)")
content = content.replace("if (isDark) Color(0xFF023E8A) else Color(0xFF90E0EF)", "if (isDark) Color(0xFF0B132B) else Color(0xFF90E0EF)")

content = content.replace("if (isDark) Color(0xFF48CAE4) else Color(0xFF0077B6)", "if (isDark) Color(0xFF5BC0BE) else Color(0xFF0077B6)")
content = content.replace("if (isDark) Color(0xFF00B4D8) else Color(0xFF0077B6)", "if (isDark) Color(0xFF5BC0BE) else Color(0xFF0077B6)")

content = content.replace("if (isDark) Color(0xFF0077B6) else Color(0xFF90E0EF)", "if (isDark) Color(0xFF1C2541) else Color(0xFF90E0EF)")
content = content.replace("if (isDark) Color(0xFF0077B6) else Color(0xFF0077B6)", "if (isDark) Color(0xFF1C2541) else Color(0xFF0077B6)")

content = content.replace("isSelected -> if (isDark) Color(0xFF0077B6).copy(alpha = 0.5f) else Color(0xFFADE8F4)", "isSelected -> if (isDark) SelectedDark else SelectedLight")
content = content.replace("isHighlighted -> if (isDark) Color(0xFF0077B6).copy(alpha = 0.4f) else Color(0xFFADE8F4)", "isHighlighted -> if (isDark) HighlightedDark else HighlightedLight")

content = content.replace("if (isDark) Color(0xFF023E8A) else Color(0xFF0077B6)", "if (isDark) DarkBlock else LightBlock")
content = content.replace("if (isDark) Color(0xFF03045E) else Color(0xFF0077B6)", "if (isDark) DarkBlock else LightBlock")

# Fix LocalDarkTheme ones missed
content = content.replace("if (LocalDarkTheme.current) Color(0xFF0077B6).copy(alpha = 0.4f) else Color(0xFFADE8F4)", "if (LocalDarkTheme.current) SelectedDark else SelectedLight")
content = content.replace("if (LocalDarkTheme.current) Color(0xFF0077B6).copy(alpha = 0.35f) else Color(0xFFADE8F4)", "if (LocalDarkTheme.current) HighlightedDark else HighlightedLight")

with open('app/src/main/java/com/example/ui/Screens.kt', 'w') as f:
    f.write(content)

print("Replaced isDark colors!")
