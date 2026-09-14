import re

with open('app/src/main/java/com/example/ui/Screens.kt', 'r') as f:
    lines = f.readlines()

non_composables = ["fun String.toPersianDigits", "fun borderStroke", "fun formatTime", "fun formatPersianTime", "fun onAdLoaded", "fun onError"]

for i, line in enumerate(lines):
    if line.strip().startswith("fun ") and not any(nc in line for nc in non_composables):
        lines[i] = "@Composable\n" + line

with open('app/src/main/java/com/example/ui/Screens.kt', 'w') as f:
    f.writelines(lines)
print("Added @Composable back!")
