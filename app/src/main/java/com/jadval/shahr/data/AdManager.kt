package com.jadval.shahr.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

data class CustomAd(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val imageUrl: String = "",
    val clickUrl: String = ""
)

object AdManager {

    private const val PREFS_NAME = "custom_ad_prefs"

    // 1:1 Ad (HomeScreen)
    var ad11 by mutableStateOf<CustomAd?>(null)
        private set

    // 9:3 Ad (Sections Screen)
    var ad93 by mutableStateOf<CustomAd?>(null)
        private set

    // 9:3_2 Ad (Levels Screen)
    var ad93_2 by mutableStateOf<CustomAd?>(null)
        private set

    var adApiResult11 by mutableStateOf("در حال مقداردهی اولیه...")
        private set

    var adApiResult93 by mutableStateOf("در حال مقداردهی اولیه...")
        private set

    var adApiResult93_2 by mutableStateOf("در حال مقداردهی اولیه...")
        private set

    private var isInitialized = false

    fun init(context: Context, scope: CoroutineScope) {
        if (isInitialized) return
        isInitialized = true

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // 1. Load cached ads
        val cached11 = loadFromCache(prefs, "11")
        if (cached11 != null) {
            ad11 = cached11
            adApiResult11 = "بارگذاری تبلیغ ۱:۱ از حافظه محلی"
        }

        val cached93 = loadFromCache(prefs, "93")
        if (cached93 != null) {
            ad93 = cached93
            adApiResult93 = "بارگذاری تبلیغ ۹:۳ از حافظه محلی"
        }

        val cached93_2 = loadFromCache(prefs, "93_2")
        if (cached93_2 != null) {
            ad93_2 = cached93_2
            adApiResult93_2 = "بارگذاری تبلیغ ۹:۳_۲ از حافظه محلی"
        }

        // 2. Start auto-fetch loops
        scope.launch(Dispatchers.IO) {
            launch {
                while (true) {
                    val success = fetchAndSaveAd(prefs, "11", "https://golestanjaber.ir/api/get_ads_1:1.php")
                    if (success) delay(15 * 60 * 1000L) else delay(20_000L)
                }
            }
            launch {
                while (true) {
                    val success = fetchAndSaveAd(prefs, "93", "https://golestanjaber.ir/api/get_ads_9:3.php")
                    if (success) delay(15 * 60 * 1000L) else delay(20_000L)
                }
            }
            launch {
                while (true) {
                    val success = fetchAndSaveAd(prefs, "93_2", "https://golestanjaber.ir/api/get_ads_9:3_2.php")
                    if (success) delay(15 * 60 * 1000L) else delay(20_000L)
                }
            }
        }
    }

    fun triggerManualRefresh(context: Context, scope: CoroutineScope) {
        scope.launch(Dispatchers.IO) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            launch { fetchAndSaveAd(prefs, "11", "https://golestanjaber.ir/api/get_ads_1:1.php") }
            launch { fetchAndSaveAd(prefs, "93", "https://golestanjaber.ir/api/get_ads_9:3.php") }
            launch { fetchAndSaveAd(prefs, "93_2", "https://golestanjaber.ir/api/get_ads_9:3_2.php") }
        }
    }

    private suspend fun fetchAndSaveAd(prefs: SharedPreferences, keyPrefix: String, apiUrl: String): Boolean {
        return try {
            val url = URL(apiUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000

            val responseCode = connection.responseCode
            if (responseCode == 200) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val jsonArray = JSONArray(response)
                if (jsonArray.length() > 0) {
                    val obj = jsonArray.getJSONObject(0)
                    val ad = CustomAd(
                        id = obj.optString("id", ""),
                        title = obj.optString("title", ""),
                        description = obj.optString("description", ""),
                        imageUrl = obj.optString("image_url", ""),
                        clickUrl = obj.optString("click_url", "")
                    )
                    saveToCache(prefs, keyPrefix, ad)
                    withContext(Dispatchers.Main) {
                        when (keyPrefix) {
                            "11" -> {
                                ad11 = ad
                                adApiResult11 = "پاسخ موفق ۱:۱ (کد 200)"
                            }
                            "93" -> {
                                ad93 = ad
                                adApiResult93 = "پاسخ موفق ۹:۳ (کد 200)"
                            }
                            "93_2" -> {
                                ad93_2 = ad
                                adApiResult93_2 = "پاسخ موفق ۹:۳_۲ (کد 200)"
                            }
                        }
                    }
                    true
                } else false
            } else false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun saveToCache(prefs: SharedPreferences, keyPrefix: String, ad: CustomAd) {
        prefs.edit()
            .putString("${keyPrefix}_id", ad.id)
            .putString("${keyPrefix}_title", ad.title)
            .putString("${keyPrefix}_description", ad.description)
            .putString("${keyPrefix}_image_url", ad.imageUrl)
            .putString("${keyPrefix}_click_url", ad.clickUrl)
            .apply()
    }

    private fun loadFromCache(prefs: SharedPreferences, keyPrefix: String): CustomAd? {
        val imageUrl = prefs.getString("${keyPrefix}_image_url", null) ?: return null
        val clickUrl = prefs.getString("${keyPrefix}_click_url", null) ?: return null
        if (imageUrl.isEmpty() || clickUrl.isEmpty()) return null

        return CustomAd(
            id = prefs.getString("${keyPrefix}_id", "") ?: "",
            title = prefs.getString("${keyPrefix}_title", "") ?: "",
            description = prefs.getString("${keyPrefix}_description", "") ?: "",
            imageUrl = imageUrl,
            clickUrl = clickUrl
        )
    }
}
