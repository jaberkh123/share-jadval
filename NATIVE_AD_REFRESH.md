# ⚠️ آپدیت (۲۰۲۶-۰۹-۰۵): ریلود ۳ دقیقه‌ای حذف شد — تبلیغ فقط یک بار لود می‌شود
در `NativeAdBanner` (Screens.kt):
- تبلیغ همسان **فقط یک بار** هنگام ورود به صفحه `loadAd()` می‌شود (`adReloadKey` و بازسازی دوره‌ای View حذف شد)
- الگوی **فلاش/آلارم قرمز هر ۳ دقیقه** (۳ پالس: ۳۵۰ms روشن / ۲۵۰ms خاموش) حفظ شد
- پالس مقیاس ۳۰ ثانیه‌ای هم سر جایش است

---

# ⚠️ منقضی (قبلی): تبلیغ همسان حذف شده بود
مکانیزم ریلود ۳ دقیقه‌ای تبلیغ همسان (Native Ad) از `NativeAdBanner` در `Screens.kt` حذف شد.
الان این تابع فقط یک **نوار آلارم قرمز** است که هر ۳ دقیقه ۳ پالس قرمز می‌زند (همان الگوی فلاش قبلی).
تبلیغ‌های همسان دیگر load نمی‌شوند؛ `native_ad_container.xml` و `adivery_native_ad.xml` در ریسورس‌ها باقی مانده‌اند ولی استفاده نمی‌شوند.

---

# سند فنی (قدیمی): مکانیزم ریلود تبلیغ همسان (Native Ad Refresh)
# سند فنی: مکانیزم ریلود تبلیغ همسان (Native Ad Refresh)

> **بازی:** جدول بزرگسالان (جدول-تقاطع)  
> **مسیر فایل اصلی:** `app/src/main/java/com/example/ui/Screens.kt` ← تابع `NativeAdBanner`  
> **SDK تبلیغات:** [Adivery SDK v4.9.0](https://adivery.com)  
> **تاریخ آخرین بررسی:** ۲۷ مرداد ۱۴۰۵ (۲۰۲۶-۰۸-۲۷)

---

## ۱. نمای کلی 🎯

تبلیغ همسان (Native Ad) در **بالای صفحه حل جدول** نمایش داده می‌شود. ویژگی کلیدی آن **ریلود خودکار هر ۳ دقیقه** است به‌طوری که اگر کاربر در صفحه بازی بماند، تبلیغ جدیدی جایگزین تبلیغ قبلی می‌شود.

---

## ۲. معماری کارکرد ⚙️

```
┌──────────────────────────────────────────────┐
│              صفحه حل جدول (GameScreen)        │
├──────────────────────────────────────────────┤
│  ┌────────────────────────────────────────┐  │
│  │     NativeAdBanner (تبلیغ همسان)       │  │  ← بالای صفحه
│  │  ┌──────────┬─────────────┬──────────┐ │  │
│  │  │ آیکون ۶۰ │ عنوان+توضیح │ دکمه CTA │ │  │  ← لایوت XML
│  │  │   dp     │  ۱۹sp/۱۷sp │  ۴۰dp    │ │  │
│  │  └──────────┴─────────────┴──────────┘ │  │
│  │  ██████ قرمز (فلاش ریلود) ████████████  │  │  ← overlay هر ۳ دقیقه
│  └────────────────────────────────────────┘  │
├──────────────────────────────────────────────┤
│              جدول کلمات متقاطع               │
│                  (Grid)                       │
├──────────────────────────────────────────────┤
│        سرنخ فعال + صفحه‌کلید فارسی           │
└──────────────────────────────────────────────┘
```

---

## ۳. لایوت XML 📐

### ۳.۱ کانتینر اصلی — `native_ad_container.xml`

فایل: `app/src/main/res/layout/native_ad_container.xml`

```xml
<com.adivery.sdk.AdiveryNativeAdView
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:id="@+id/native_ad_view"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    app:placement_id="2380b512-a0ee-4c85-8e15-5f128cadca2a"
    app:adivery_native_ad_layout="@layout/adivery_native_ad" />
```

- **`placement_id`**: شناسه یکتای تبلیغگاه همسان در Adivery
- **`adivery_native_ad_layout`**: لایوت سفارشی ظاهر تبلیغ

### ۳.۲ لایوت سفارشی — `adivery_native_ad.xml`

ساختار RTL افقی:

| عنصر | اندازه | توضیح |
|------|--------|-------|
| `ImageView` (آیکون) | `60×60 dp` | آیکون اپلیکیشن تبلیغ‌کننده |
| `TextView` (عنوان) | `19sp` bold | عنوان تبلیغ — حداکثر ۱ خط |
| `TextView` (توضیحات) | `17sp` | توضیحات تبلیغ — حداکثر ۱ خط |
| `Button` (CTA) | `40dp` ارتفاع | دکمه عمل — `#2563EB` آبی |

---

## ۴. مکانیزم ریلود خودکار ۳ دقیقه‌ای 🔄

این بخش هسته اصلی کارکرد است. در تابع `NativeAdBanner()` (سطر ۴۰۶۷ `Screens.kt`):

### ۴.۱ وضعیت‌های کلیدی (State)

```kotlin
// شمارنده ریلود — هر بار +۱ شود، کل AndroidView دوباره ساخته می‌شود
var adReloadKey by remember { mutableIntStateOf(0) }

// وضعیت فلاش قرمز — برای جلب توجه کاربر
var showRedFlash by remember { mutableStateOf(false) }
```

### ۴.۲ حلقه ریلود — `LaunchedEffect`

```kotlin
LaunchedEffect(Unit) {
    while (true) {
        delay(180_000L)          // ⏱ ۱۸۰,۰۰۰ میلی‌ثانیه = ۳ دقیقه
        adReloadKey++            // 🔑 افزایش کلید = نابودی + بازسازی View
        showRedFlash = true      // 🔴 شروع فلاش قرمز
        delay(400)               // ۴۰۰ms روشن
        showRedFlash = false     // خاموش
        delay(200)               // ۲۰۰ms وقفه
        showRedFlash = true      // 🔴 فلاش دوم
        delay(400)               // ۴۰۰ms روشن
        showRedFlash = false     // خاموش → ریلود کامل شد
    }
}
```

### ۴.۳ بازسازی View با `key(adReloadKey)`

```kotlin
key(adReloadKey) {                          // ← کلید عوض شود = Composable نابود + از نو
    AndroidView(
        factory = { context ->
            val adView = inflater.inflate(
                R.layout.native_ad_container, null
            ) as AdiveryNativeAdView
            adView.loadAd()                  // ← درخواست تبلیغ جدید از Adivery
            adView
        }
    )
}
```

> **نکته کلیدی:** Compose با تغییر مقدار `key`، کل `AndroidView` قبلی را dispose و یک نمونه کاملاً جدید می‌سازد. این یعنی `loadAd()` دوباره فراخوانی می‌شود و تبلیغ تازه‌ای دریافت می‌شود.

---

## ۵. جلب توجه کاربر — فلاش قرمز 🔴

### ۵.۱ انیمیشن فلاش

```kotlin
val redAlpha by animateFloatAsState(
    targetValue = if (showRedFlash) 0.45f else 0f,
    animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
    label = "RedFlash"
)
```

### ۵.۲ نمایش overlay

```kotlin
if (redAlpha > 0f) {
    Box(
        modifier = Modifier
            .matchParentSize()
            .background(Color.Red.copy(alpha = redAlpha))
    )
}
```

**رفتار:** هر ۳ دقیقه، ۲ پالس قرمز ظاهر می‌شود:

```
۰ms ─── ۴۰۰ms ─── ۶۰۰ms ─── ۱۰۰۰ms
█████░░░█████░░░
 فلاش۱   فلاش۲
 (α=0.45) (α=0.45)
```

---

## ۶. پالس مقیاس ۳۰ ثانیه‌ای 📐

علاوه بر ریلود ۳ دقیقه‌ای، هر **۳۰ ثانیه** یک پالس مقیاس ملایم روی تبلیغ اعمال می‌شود:

```kotlin
LaunchedEffect(Unit) {
    while (true) {
        delay(30_000)                              // هر ۳۰ ثانیه
        adViewRef[0]?.let { adView ->
            val pulseX = ObjectAnimator.ofFloat(adView, "scaleX", 1f, 1.03f, 1f)
            pulseX.duration = 600
            pulseX.start()
            val pulseY = ObjectAnimator.ofFloat(adView, "scaleY", 1f, 1.03f, 1f)
            pulseY.duration = 600
            pulseY.start()
        }
    }
}
```

**رفتار:** هر ۳۰ ثانیه، تبلیغ ۳٪ بزرگ‌تر و به اندازه اصلی برمی‌گردد (۶۰۰ms).

---

## ۷. جایگاه تبلیغ در صفحه بازی 📍

در تابع `GameScreen` (حدود سطر ۱۸۰۱):

```kotlin
Scaffold(
    topBar = { /* هدر بازی: عنوان + سکه + تایمر */ },
    floatingActionButton = { /* دکمه‌های شناور */ },
) { innerPadding ->
    Column {
        // ۱. تبلیغ همسان — بالای صفحه
        NativeAdBanner(
            modifier = Modifier.fillMaxWidth()
        )

        // ۲. جدول کلمات متقاطع (وزن باقیمانده)
        CrosswordGrid(...)

        // ۳. سرنخ فعال + جهت افقی/عمودی
        // ۴. صفحه‌کلید فارسی
    }
}
```

**چیدمان صفحه بازی (از بالا به پایین):**

| ردیف | محتوا | ارتفاع |
|------|-------|--------|
| ۱ | هدر (عنوان + سکه + تایمر + دکمه‌ها) | ~۶۰dp |
| ۲ | **تبلیغ همسان (NativeAdBanner)** | **`wrap_content`** |
| ۳ | جدول کلمات متقاطع | `weight(1f)` |
| ۴ | سرنخ + دکمه‌های جهت | ~۸۰dp |
| ۵ | صفحه‌کلید فارسی | ~۱۲۰dp |

---

## ۸. زمان‌بندی رویدادها 📅

```
تایمر ۰:۰۰   تایمر ۰:۳۰   تایمر ۱:۰۰  ...  تایمر ۲:۳۰  تایمر ۳:۰۰
    │             │                              │              │
    ▼             ▼                              ▼              ▼
  loadAd()     پالس       پالس              پالس         🔴 فلاش قرمز
             مقیاس     مقیاس             مقیاس         + adReloadKey++
             (۳٪)     (۳٪)             (۳٪)         + loadAd() جدید
                                                        + ۲ پالس قرمز
```

| رویداد | فاصله | توضیح |
|--------|-------|-------|
| پالس مقیاس | هر ۳۰ ثانیه | بزرگ‌نمایی ۳٪ + بازگشت (۶۰۰ms) |
| **ریلود تبلیغ** | **هر ۳ دقیقه** | **نابودی View + ساخت جدید + loadAd()** |
| فلاش قرمز | هر ۳ دقیقه | ۲ پالس سریع (~۱ ثانیه مجموع) |

---

## ۹. فلوچارت تصمیم‌گیری 💡

```
کاربر وارد صفحه حل جدول می‌شود
        │
        ▼
NativeAdBanner() فراخوانی می‌شود
        │
        ├──→ AndroidView ساخته می‌شود → adView.loadAd() → تبلیغ نمایش داده می‌شود
        │
        ├──→ LaunchedEffect شروع حلقه ریلود (۳ دقیقه)
        │
        ├──→ LaunchedEffect شروع حلقه پالس مقیاس (۳۰ ثانیه)
        │
        ▼
   ┌─────────────────────────────────────────┐
   │  آیا کاربر هنوز در صفحه بازی هست؟       │
   │                                         │
   │  بله ← ادامه حلقه‌ها                    │
   │  خیر ← LaunchedEffect متوقف می‌شود      │
   │         (dispose شدن Composable)         │
   └─────────────────────────────────────────┘
        │ (بعد از ۳ دقیقه)
        ▼
  adReloadKey++ ← Compose متوجه تغییر key می‌شود
        │
        ▼
  AndroidView قبلی dispose می‌شود
        │
        ▼
  AndroidView جدید ساخته می‌شود → loadAd() → تبلیغ جدید
        │
        ▼
  فلاش قرمز (۲ پالس) → کاربر متوجه تغییر می‌شود
        │
        ▼
  چرخه تکرار...
```

---

## ۱۰. مشخصات فنی 📋

| پارامتر | مقدار |
|---------|-------|
| نام SDK | Adivery SDK |
| نسخه SDK | `4.9.0` |
| Placement ID | `2380b512-a0ee-4c85-8e15-5f128cadca2a` |
| زمان ریلود | `180,000ms` (۳ دقیقه) |
| زمان پالس مقیاس | `30,000ms` (۳۰ ثانیه) |
| مقیاس پالس | `1.0 → 1.03 → 1.0` |
| مدت پالس | `600ms` |
| شدت فلاش قرمز | `alpha = 0.45` |
| مدت هر پالس فلاش | `400ms` روشن + `200ms` خاموش |
| تعداد پالس فلاش | `۲` |
| اندازه آیکون CTA | `40dp` ارتفاع |
| اندازه آیکون تبلیغ | `60×60 dp` |
| راست به چپ | ✅ (`android:layoutDirection="rtl"`) |
| Color CTA | `#2563EB` (آبی) |
| رنگ پس‌زمینه | `#FFFFFF` (سفید) |
| حداکثر خطوط عنوان | ۱ |
| حداکثر خطوط توضیحات | ۱ |

---

## ۱۱. نکات مهم ⚠️

1. **کل `key(adReloadKey)`** مهم‌ترین بخش است — بدون آن، Compose `AndroidView` را reuse می‌کند و `loadAd()` دوباره فراخوانی نمی‌شود.

2. **`LaunchedEffect(Unit)`** فقط زمانی فعال است که `NativeAdBanner` در صفحه باشد. وقتی کاربر صفحه را ترک کند، حلقه‌ها متوقف می‌شوند.

3. **`adViewRef`** یک `arrayOfNulls` است که reference به `AdiveryNativeAdView` فعلی را نگه می‌دارد تا پالس مقیاس بتواند روی View اعمال شود.

4. **فلاش قرمز** صرفاً یک overlay شفاف قرمز است که روی تبلیغ نمایش داده می‌شود — هدف آن جلب توجه کاربر به تبلیغ جدید است.

5. **Adivery Listener** در `factory` تنظیم شده:
   - `onAdLoaded()`: reference ذخیره می‌شود
   - `onError()`: خطا در logcat ثبت می‌شود

---

## ۱۲. مقایسه با انواع تبلیغ دیگر در اپ 📊

| ویژگی | تبلیغ همسان (Native) | تبلیغ بنری سفارشی | تبلیغ میان‌صفحه‌ای | تبلیغ بازگشت به برنامه |
|--------|---------------------|-------------------|-------------------|----------------------|
| فایل | `NativeAdBanner()` | `CustomAdBanner()` | `ADIVERY_INTERSTITIAL_PLACEMENT` | `ADIVERY_APP_OPEN_PLACEMENT` |
| ریلود خودکار | ✅ هر ۳ دقیقه | ❌ هر ۱۵ دقیقه (API) | ❌ دستی | ❌ خودکار (بازگشت) |
| فلاش قرمز | ✅ | ❌ | ❌ | ❌ |
| پالس مقیاس | ✅ هر ۳۰ ثانیه | ❌ | ❌ | ❌ |
| SDK | Adivery | HTTP API سفارشی | Adivery | Adivery |
| جایگاه | بالای صفحه بازی | صفحه اصلی/بخش‌ها | بین صفحات | بازگشت به اپ |
