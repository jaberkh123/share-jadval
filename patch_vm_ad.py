import re

with open("app/src/main/java/com/example/ui/PuzzleViewModel.kt", "r") as f:
    content = f.read()

data_class = """
data class CustomAd(
    val id: String,
    val title: String,
    val description: String,
    val imageUrl: String,
    val clickUrl: String
)
"""

if "data class CustomAd" not in content:
    content = content.replace("import kotlinx.coroutines.flow.asStateFlow", "import kotlinx.coroutines.flow.asStateFlow\nimport kotlinx.coroutines.withContext\nimport org.json.JSONArray\n" + data_class)

target_var = "    var isSoundEnabled by mutableStateOf(repository.isSoundEnabled())"

if "var customAd" not in content:
    custom_ad_var = """
    var customAd by mutableStateOf<CustomAd?>(null)
        private set

    fun fetchCustomAd() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val url = java.net.URL("https://golestanjaber.ir/api/get_ads.php")
                val connection = url.openConnection() as java.net.HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                
                if (connection.responseCode == 200) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val jsonArray = JSONArray(response)
                    if (jsonArray.length() > 0) {
                        val jsonObject = jsonArray.getJSONObject(0)
                        val ad = CustomAd(
                            id = jsonObject.optString("id", ""),
                            title = jsonObject.optString("title", ""),
                            description = jsonObject.optString("description", ""),
                            imageUrl = jsonObject.optString("image_url", ""),
                            clickUrl = jsonObject.optString("click_url", "")
                        )
                        withContext(Dispatchers.Main) {
                            customAd = ad
                        }
                    }
                }
            } catch (e: Exception) {
                // Ignore errors
                e.printStackTrace()
            }
        }
    }
"""
    content = content.replace(target_var, target_var + custom_ad_var)

init_target = """    init {
        // Initialize timer coroutine"""

init_replacement = """    init {
        fetchCustomAd()
        // Initialize timer coroutine"""
content = content.replace(init_target, init_replacement)

with open("app/src/main/java/com/example/ui/PuzzleViewModel.kt", "w") as f:
    f.write(content)

