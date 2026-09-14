import re

with open('app/src/main/java/com/example/ui/Screens.kt', 'r') as f:
    content = f.read()

content = content.replace("Color(0xFF48CAE4)", "HighlightedDark")
content = content.replace("if (LocalDarkTheme.current) Color(0xFF183A37)", "if (LocalDarkTheme.current) DarkSurface")
content = content.replace("if (LocalDarkTheme.current) Color(0xFFC44900)", "if (LocalDarkTheme.current) DarkPrimary")
content = content.replace("if (LocalDarkTheme.current) Color(0xFFEFD6AC)", "if (LocalDarkTheme.current) DarkOnPrimary")
content = content.replace("if (LocalDarkTheme.current) Color(0xF2183A37)", "if (LocalDarkTheme.current) DarkSurface.copy(alpha=0.95f)")

content = content.replace("if (isDark) Color(0xFF183A37)", "if (isDark) DarkSurface")
content = content.replace("if (isDark) Color(0xFF04151F)", "if (isDark) DarkBackground")
content = content.replace("if (isDark) Color(0xFFC44900)", "if (isDark) DarkPrimary")

with open('app/src/main/java/com/example/ui/Screens.kt', 'w') as f:
    f.write(content)

print("Replaced variables!")
