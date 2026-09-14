import re

with open('app/src/main/java/com/example/ui/Screens.kt', 'r') as f:
    content = f.read()

content = content.replace("fun borderStroke() = ", "@Composable\nfun borderStroke() = ")

with open('app/src/main/java/com/example/ui/Screens.kt', 'w') as f:
    f.write(content)

print("Fixed borderStroke")
