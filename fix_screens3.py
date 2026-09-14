import re

with open('app/src/main/java/com/example/ui/Screens.kt', 'r') as f:
    content = f.read()

# Replace specific hex codes that were used for dark theme
content = content.replace("Color(0xFF1C2541)", "Color(0xFF183A37)")
content = content.replace("Color(0xFF0B132B)", "Color(0xFF04151F)")
content = content.replace("Color(0xFF5BC0BE)", "Color(0xFFC44900)")
content = content.replace("Color(0xFF6FFFE9)", "Color(0xFFEFD6AC)")
content = content.replace("Color(0xF21C2541)", "Color(0xF2183A37)")

with open('app/src/main/java/com/example/ui/Screens.kt', 'w') as f:
    f.write(content)

print("Replaced hex colors!")
