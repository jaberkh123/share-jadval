# راهنمای جامع پیاده‌سازی بنرهای تبلیغاتی سفارشی (Custom Banner Ads)

این مستند شامل توضیحات کامل نحوه پیاده‌سازی و کارکرد سیستم تبلیغات بنری سفارشی بر پایه API اختصاصی در اپلیکیشن اندروید (با استفاده از **Kotlin** و **Jetpack Compose**) می‌باشد. می‌توانید این فایل را در اختیار هوش مصنوعی یا توسعه‌دهنده دیگر قرار دهید تا تمام این سیستم را عینا در پروژه جدید پیاده‌سازی کند.

---

## ۱. خلاصه‌ی معماری و آدرس‌های API

سیستم تبلیغات شامل **۳ بنر تبلیغاتی مجزا** در ۳ صفحه مختلف برنامه است که هر کدام از API و نسبت تصویر (Aspect Ratio) مخصوص خود استفاده می‌کنند:

1. **تبلیغ صفحه اصلی (HomeScreen):**
   - **آدرس API:** `https://golestanjaber.ir/api/get_ads_1:1.php`
   - **نسبت تصویر (Aspect Ratio):** `9:4`
2. **تبلیغ صفحه بخش‌ها / دسته‌بندی‌ها (SectionSelectionScreen):**
   - **آدرس API:** `https://golestanjaber.ir/api/get_ads_9:3.php`
   - **نسبت تصویر (Aspect Ratio):** `9:3`
3. **تبلیغ صفحه انتخاب مراحل (LevelSelectionScreen):**
   - **آدرس API:** `https://golestanjaber.ir/api/get_ads_9:3_2.php`
   - **نسبت تصویر (Aspect Ratio):** `9:3`

---

## ۲. فرمت خروجی JSON از سرور

هر سه API خروجی به فرمت آرایه‌ای از اشیاء JSON تولید می‌کنند:

```json
[
  {
    "id": "1",
    "title": "عنوان تبلیغ",
    "description": "توضیحات متنی کوتاه",
    "image_url": "https://golestanjaber.ir/ads/banner1.png",
    "click_url": "https://example.com"
  }
]
```

---

## ۳. نحوه کارکرد سیستم (Under the Hood)

1. **ذخيره‌سازی محلی و حالت آفلاین (Offline Caching):**
   - هنگام اجرای برنامه، آخرین بنر دریافت شده هر بخش از `SharedPreferences` بازیابی و بلافاصله نمایش داده می‌شود تا حتی در حالت قطعی اینترنت، بنر قبلی ذخیره‌شده بدون تاخیر نشان داده شود.
2. **دریافت خودکار در پس‌زمینه (Background Auto-Fetch):**
   - پس از شروع برنامه، یک کورتین (Coroutine) در پس‌زمینه اجرا شده و به صورت مجزا برای هر کدام از ۳ API درخواست ارسال می‌کند.
   - **مدیریت بازه زمانی (Polling):**
     - در صورت موفقیت‌آمیز بودن پاسخ API (کد HTTP 200)، بنر جدید در `SharedPreferences` ذخیره شده و UI به‌روزرسانی می‌شود. سپس هر **۱۵ دقیقه** مجدداً استعلام گرفته می‌شود.
     - در صورت بروز خطا (مانند قطعی اینترنت)، برنامه هر **۲۰ ثانیه** تلاش مجدد انجام می‌دهد.
3. **کلیلک بر روی بنر:**
   - با لمس بنر توسط کاربر، یک `Intent` تعاملی از نوع `ACTION_VIEW` صادر شده و لینک مربوطه (`clickUrl`) در مرورگر دستگاه باز می‌شود.
4. **بارگذاری تصاویر:**
   - برای بارگذاری و کش کردن تصاویر از کتابخانه standard **Coil** (`coil.compose.AsyncImage`) استفاده شده است.

---

## ۴. کدهای کامل و مراحل پیاده‌سازی جهت انتقال به پروژه دیگر

### مرحله اول: مدل داده (`CustomAd.kt`)

یک Data Class ساده برای ساختار تبلیغ ایجاد کنید:

```kotlin
package com.example.data

data class CustomAd(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val imageUrl: String = "",
    val clickUrl: String = ""
)
```

---

### مرحله دوم: مدیریت تبلیغات (`AdManager.kt`)

یک Singleton برای مدیریت دریافت داده‌ها، کش و حالت‌های UI تعریف کنید:

```kotlin
package com.example.data

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

object AdManager {

    private const val PREFS_NAME = "custom_ad_prefs"

    // 1. تبلیغ صفحه اصلی (1:1 / 9:4)
    var ad11 by mutableStateOf<CustomAd?>(null)
        private set

    // 2. تبلیغ صفحه بخش‌ها (9:3)
    var ad93 by mutableStateOf<CustomAd?>(null)
        private set

    // 3. تبلیغ صفحه مراحل (9:3_2)
    var ad93_2 by mutableStateOf<CustomAd?>(null)
        private set

    private var isInitialized = false

    fun init(context: Context, scope: CoroutineScope) {
        if (isInitialized) return
        isInitialized = true

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // بارگذاری تبلیغات ذخیره‌شده برای حالت آفلاین
        ad11 = loadFromCache(prefs, "11")
        ad93 = loadFromCache(prefs, "93")
        ad93_2 = loadFromCache(prefs, "93_2")

        // شروع حلقه‌های دریافت خودکار در پس‌زمینه
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

    private suspend fun fetchAndSaveAd(prefs: SharedPreferences, keyPrefix: String, apiUrl: String): Boolean {
        return try {
            val url = URL(apiUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
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
                            "11" -> ad11 = ad
                            "93" -> ad93 = ad
                            "93_2" -> ad93_2 = ad
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
```

---

### مرحله سوم: لایه ViewModel (`PuzzleViewModel.kt` یا ViewModel دلخواه شما)

در ViewModel، مقادیر متغیرها را برای دسترسی Composables تعویض/اکسپوز کنید:

```kotlin
// در داخل ViewModel
val ad11: CustomAd? get() = AdManager.ad11
val ad93: CustomAd? get() = AdManager.ad93
val ad93_2: CustomAd? get() = AdManager.ad93_2

init {
    // مقداردهی اولیه در ساخت ViewModel
    AdManager.init(getApplication(), viewModelScope)
}
```

---

### مرحله چهارم: کامپوننت UI و بنر در Jetpack Compose (`CustomAdBanner`)

کامپوننت عمومی بنر جهت استفاده در صفحات مختلف:

```kotlin
@Composable
fun CustomAdBanner(
    ad: CustomAd?,
    modifier: Modifier = Modifier,
    aspectRatio: Float = 9f / 3f
) {
    ad?.let { customAd ->
        if (customAd.imageUrl.isNotEmpty() && customAd.clickUrl.isNotEmpty()) {
            val context = LocalContext.current
            Card(
                modifier = modifier
                    .fillMaxWidth()
                    .aspectRatio(aspectRatio)
                    .shadow(4.dp, RoundedCornerShape(16.dp))
                    .clickable {
                        try {
                            val intent = android.content.Intent(
                                android.content.Intent.ACTION_VIEW,
                                android.net.Uri.parse(customAd.clickUrl)
                            )
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                coil.compose.AsyncImage(
                    model = customAd.imageUrl,
                    contentDescription = customAd.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
            }
        }
    }
}
```

---

### مرحله پنجم: قرار دادن بنر در صفحات مختلف برنامه

#### ۱. در صفحه اصلی (`HomeScreen`):
```kotlin
viewModel.ad11?.let { ad ->
    if (ad.imageUrl.isNotEmpty() && ad.clickUrl.isNotEmpty()) {
        CustomAdBanner(ad = ad, aspectRatio = 9f / 4f)
        Spacer(modifier = Modifier.height(16.dp))
    }
}
```

#### ۲. در صفحه بخش‌ها (`SectionSelectionScreen`):
```kotlin
viewModel.ad93?.let { ad ->
    if (ad.imageUrl.isNotEmpty() && ad.clickUrl.isNotEmpty()) {
        CustomAdBanner(ad = ad, aspectRatio = 9f / 3f)
        Spacer(modifier = Modifier.height(12.dp))
    }
}
```

#### ۳. در صفحه مراحل (`LevelSelectionScreen`):
```kotlin
viewModel.ad93_2?.let { ad ->
    if (ad.imageUrl.isNotEmpty() && ad.clickUrl.isNotEmpty()) {
        CustomAdBanner(ad = ad, aspectRatio = 9f / 3f)
        Spacer(modifier = Modifier.height(12.dp))
    }
}
```

---

## ۵. چک‌لیست و دسترسی‌های لازم (Android)

1. **دسترسی اینترنت در `AndroidManifest.xml`:**
   ```xml
   <uses-permission android:name="android.permission.INTERNET" />
   ```
2. **پشتیبانی از HTTP / HTTPS در صورت نیاز:**
   تمام لینک‌های استفاده شده در این سیستم بر روی پروتکل امن `https://` است، بنابراین نیاز به تنظیمات اضافی networkSecurityConfig ندارد.
3. **وابستگی‌های Gradle (Dependencies):**
   - کتابخانه Coil برای نمایش تصویر:
     `implementation("io.coil-kt:coil-compose:2.5.0")`
