import re

with open("app/src/main/java/com/example/ui/PuzzleViewModel.kt", "r") as f:
    content = f.read()

# Fix imports
content = content.replace("import org.json.JSONArray\n\nimport kotlinx.coroutines.flow.collectLatest", "import kotlinx.coroutines.flow.collectLatest")
content = content.replace("import kotlinx.coroutines.withContext\nimport kotlinx.coroutines.Dispatchers\nimport org.json.JSONArray", "")
content = content.replace("import kotlinx.coroutines.withContext", "import kotlinx.coroutines.withContext\nimport kotlinx.coroutines.Dispatchers\nimport org.json.JSONArray")

# Fix the dangling private set
target = """    }
        private set

    var isDarkModeEnabled by mutableStateOf(false)"""

replacement = """    }

    var isSoundEnabled by mutableStateOf(repository.isSoundEnabled())
        private set

    var isDarkModeEnabled by mutableStateOf(false)"""

content = content.replace(target, replacement)

with open("app/src/main/java/com/example/ui/PuzzleViewModel.kt", "w") as f:
    f.write(content)

