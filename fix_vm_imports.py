import re

with open("app/src/main/java/com/example/ui/PuzzleViewModel.kt", "r") as f:
    content = f.read()

# Remove the incorrectly placed data class
content = re.sub(r'data class CustomAd\(.*?\)[\s]*', '', content, flags=re.DOTALL)

# Add imports properly
new_imports = """
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import org.json.JSONArray

data class CustomAd(
    val id: String,
    val title: String,
    val description: String,
    val imageUrl: String,
    val clickUrl: String
)
"""

content = content.replace("enum class Screen {", new_imports + "\n\nenum class Screen {")

with open("app/src/main/java/com/example/ui/PuzzleViewModel.kt", "w") as f:
    f.write(content)

