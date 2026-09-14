# راهنمای بهینه‌سازی‌شده ادغام SDK ادیوری در Android

این فایل، نسخه‌ی ساختارمند و خواناتر مستندات ادغام کتابخانه‌ی **Adivery Android SDK** است. متن برای استفاده‌ی سریع‌تر توسعه‌دهنده‌ها بازنویسی و بخش‌بندی شده است.

---

## فهرست مطالب

1. [حداقل پیش‌نیازها](#حداقل-پیشنیازها)
2. [افزودن کتابخانه ادیوری به پروژه](#افزودن-کتابخانه-ادیوری-به-پروژه)
3. [راه‌اندازی SDK](#راهاندازی-sdk)
4. [تنظیمات ProGuard و Manifest](#تنظیمات-proguard-و-manifest)
5. [تبلیغات تمام‌صفحه](#تبلیغات-تمامصفحه)
6. [Listener و وضعیت تبلیغ‌ها](#listener-و-وضعیت-تبلیغها)
7. [نمایش تبلیغ](#نمایش-تبلیغ)
8. [بهترین روش پیاده‌سازی App Open Ad](#بهترین-روش-پیادهسازی-app-open-ad)
9. [تبلیغات بنری](#تبلیغات-بنری)
10. [تبلیغات همسان Native](#تبلیغات-همسان-native)
11. [تبلیغات Pre-Roll / VAST با ExoPlayer](#تبلیغات-pre-roll--vast-با-exoplayer)
12. [عیب‌یابی](#عیبیابی)
13. [افزودن Mintegral Mediation](#افزودن-mintegral-mediation)
14. [چک‌لیست نهایی پیاده‌سازی](#چکلیست-نهایی-پیادهسازی)

---

## حداقل پیش‌نیازها

| مورد | حداقل نسخه / مقدار |
|---|---:|
| Android Studio | `4.2` یا بالاتر |
| Gradle | `4.2.2` یا بالاتر |
| minSdk | `21` یا بالاتر |

---

## افزودن کتابخانه ادیوری به پروژه

### 1. افزودن Maven Repository

ابتدا مخزن Maven موردنیاز را به فایل `build.gradle` پروژه یا ماژولی که شامل تنظیمات `repositories` است اضافه کنید:

```groovy
allprojects {
    repositories {
        google()
        // سایر repositoryها

        // repository موردنیاز ادیوری
        mavenCentral()
    }
}
```

### 2. افزودن Dependency اصلی SDK

در فایل `build.gradle` ماژول اپلیکیشن، dependency ادیوری را اضافه کنید:

```groovy
dependencies {
    // سایر dependencyهای پروژه
    implementation 'com.adivery:sdk:4.9.0'
}
```

> **پیشنهاد:** بهتر است همیشه از آخرین نسخه‌ی پایدار SDK استفاده شود تا بهبودها و قابلیت‌های جدید در دسترس باشند.

### 3. نکته مربوط به Kotlin

ادیوری از نسخه‌ی `4.1.0` به بعد به Kotlin مهاجرت کرده است.

> **توجه:** در نسخه‌های `4.8.4` به بعد، نیازی به افزودن dependency جداگانه‌ی Kotlin نیست.

برای نسخه‌های پایین‌تر از `4.8.4`، باید `kotlin-stdlib` را نیز اضافه کنید:

```groovy
dependencies {
    // برای نسخه‌های پایین‌تر از 4.8.4
    implementation 'com.adivery:sdk:4.8.3'
    implementation 'org.jetbrains.kotlin:kotlin-stdlib:1.5.10'
}
```

---

## راه‌اندازی SDK

### 1. Import کردن کتابخانه

در Activity موردنظر، کلاس‌های ادیوری را import کنید:

```java
import com.adivery.sdk.*;
```

### 2. فراخوانی `configure`

در متد `onCreate`، SDK را مقداردهی اولیه کنید.

مقدار `APP_ID` باید با شناسه‌ی اپلیکیشن شما در داشبورد ناشرین جایگزین شود.

```java
@Override
protected void onCreate(Bundle bundle) {
    super.onCreate(bundle);

    Adivery.configure(getApplication(), APP_ID);

    // ادامه کد شما
}
```

---

## تنظیمات ProGuard و Manifest

کتابخانه‌ی ادیوری به‌صورت خودکار تنظیمات موردنیاز را به این فایل‌ها اضافه می‌کند:

- `AndroidManifest.xml`
- `proguard-rules.pro`

تنها دسترسی‌ای که به اپلیکیشن اضافه می‌شود، دسترسی اینترنت است.

```xml
<uses-permission android:name="android.permission.INTERNET" />
```

---

## تبلیغات تمام‌صفحه

تبلیغات تمام‌صفحه شامل موارد زیر است:

- تبلیغ میان‌صفحه‌ای: `Interstitial`
- تبلیغ جایزه‌ای: `Rewarded`
- تبلیغ اجرای اپلیکیشن: `App Open`

### درخواست تبلیغ میان‌صفحه‌ای

برای آماده‌سازی تبلیغ میان‌صفحه‌ای، متد زیر را صدا بزنید:

```java
Adivery.prepareInterstitialAd(context, placementId);
```

`placementId` باید با کلید تبلیغ‌گاه شما در پنل ادیوری جایگزین شود.

> **هشدار:** ادیوری پس از نمایش هر تبلیغ، تبلیغ بعدی را به‌صورت خودکار آماده می‌کند. بنابراین بعد از هر نمایش، نیازی به فراخوانی دوباره‌ی متد آماده‌سازی نیست.

### درخواست تبلیغ جایزه‌ای

برای آماده‌سازی تبلیغ جایزه‌ای، از متد زیر استفاده کنید:

```java
Adivery.prepareRewardedAd(context, placementId);
```

> **توجه:** ادیوری پس از نمایش تبلیغ، تبلیغ بعدی را خودکار آماده می‌کند.

### درخواست تبلیغ اجرای اپلیکیشن App Open

برای آماده‌سازی تبلیغ App Open، از متد زیر استفاده کنید:

```java
Adivery.prepareAppOpenAd(context, placementId);
```

> **توجه:** ادیوری پس از نمایش تبلیغ App Open نیز تبلیغ بعدی را خودکار آماده می‌کند.

---

## Listener و وضعیت تبلیغ‌ها

برای اطلاع از وضعیت بارگذاری، نمایش، کلیک، بسته‌شدن یا خطای تبلیغ‌ها، می‌توانید یک `AdiveryListener` سراسری تنظیم کنید.

```java
Adivery.addGlobalListener(new AdiveryListener() {

    @Override
    public void onAppOpenAdLoaded(String placementId) {
        // تبلیغ اجرای اپلیکیشن بارگذاری شده است.
    }

    @Override
    public void onInterstitialAdLoaded(String placementId) {
        // تبلیغ میان‌صفحه‌ای بارگذاری شده است.
    }

    @Override
    public void onRewardedAdLoaded(String placementId) {
        // تبلیغ جایزه‌ای بارگذاری شده است.
    }

    @Override
    public void onRewardedAdClosed(String placementId, boolean isRewarded) {
        // بررسی کنید کاربر جایزه دریافت کرده است یا خیر.
    }

    @Override
    public void log(String placementId, String log) {
        // پیام لاگ را چاپ کنید.
    }
});
```

> **نکته:** این listener برای تمام تبلیغ‌های میان‌صفحه‌ای، جایزه‌ای و App Open فراخوانی می‌شود. مقدار `placementId` مشخص می‌کند وضعیت کدام تبلیغ‌گاه تغییر کرده است.

### Callbackهای قابل پیاده‌سازی

| Callback | کاربرد |
|---|---|
| `onError(String placementId, String reason)` | دریافت خطا و دلیل آن |
| `onInterstitialAdLoaded(String placementId)` | آماده‌شدن تبلیغ میان‌صفحه‌ای |
| `onInterstitialAdShown(String placementId)` | نمایش تبلیغ میان‌صفحه‌ای |
| `onInterstitialAdClicked(String placementId)` | کلیک روی تبلیغ میان‌صفحه‌ای |
| `onInterstitialAdClosed(String placementId)` | بسته‌شدن تبلیغ میان‌صفحه‌ای |
| `onRewardedAdLoaded(String placementId)` | آماده‌شدن تبلیغ جایزه‌ای |
| `onRewardedAdShown(String placementId)` | نمایش تبلیغ جایزه‌ای |
| `onRewardedAdClicked(String placementId)` | کلیک روی تبلیغ جایزه‌ای |
| `onRewardedAdClosed(String placementId, boolean isRewarded)` | بسته‌شدن تبلیغ جایزه‌ای و بررسی دریافت جایزه |
| `onAppOpenAdLoaded(String placementId)` | آماده‌شدن تبلیغ App Open |
| `onAppOpenAdShown(String placementId)` | نمایش تبلیغ App Open |
| `onAppOpenAdClosed(String placementId)` | بسته‌شدن تبلیغ App Open |
| `onAppOpenAdClicked(String placementId)` | کلیک روی تبلیغ App Open |

---

## نمایش تبلیغ

### نمایش تبلیغ میان‌صفحه‌ای یا جایزه‌ای

قبل از نمایش، باید با `isLoaded` بررسی کنید که تبلیغ آماده است یا خیر:

```java
if (Adivery.isLoaded(placementId)) {
    Adivery.showAd(placementId);
}
```

### نمایش تبلیغ App Open

برای نمایش تبلیغ اجرای اپلیکیشن از متد زیر استفاده کنید:

```java
if (Adivery.isLoaded(placementId)) {
    Adivery.showAppOpenAd(activity, placementId);
}
```

> **توجه:** برای نمایش تبلیغ‌های بعدی، کافی است دوباره همین دستور نمایش را فراخوانی کنید.

> **هشدار:** به دلیل بارگذاری مجدد تبلیغ پس از نمایش، از نمایش تبلیغ داخل callbackهای `onAdLoaded` خودداری کنید.

---

## بهترین روش پیاده‌سازی App Open Ad

ادیوری پیشنهاد می‌کند تبلیغ App Open زمانی نمایش داده شود که کاربر بیش از ۵ ثانیه از اپلیکیشن خارج بوده و سپس دوباره وارد برنامه می‌شود.

نمونه پیاده‌سازی در کلاس `Application`:

```java
import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import com.adivery.sdk.Adivery;
import java.util.concurrent.TimeUnit;

class App extends Application implements Application.ActivityLifecycleCallbacks {

    private long lastPauseTime = 0L;

    @Override
    public void onCreate() {
        super.onCreate();
        registerActivityLifecycleCallbacks(this);
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        // doing nothing
    }

    @Override
    public void onActivityStarted(Activity activity) {
        // doing nothing
    }

    @Override
    public void onActivityResumed(Activity activity) {
        long pauseTime = System.currentTimeMillis() - lastPauseTime;

        if (pauseTime > TimeUnit.SECONDS.toMillis(5)) {
            Adivery.showAppOpenAd(activity, APP_OPEN_PLACEMENT_ID);
        }
    }

    @Override
    public void onActivityPaused(Activity activity) {
        lastPauseTime = System.currentTimeMillis();
    }

    @Override
    public void onActivityStopped(Activity activity) {
        // doing nothing
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
        // doing nothing
    }

    @Override
    public void onActivityDestroyed(Activity activity) {
        // doing nothing
    }
}
```

---

## تبلیغات بنری

### افزودن Banner به XML

برای نمایش تبلیغ بنری، View زیر را در layout صفحه قرار دهید:

```xml
<com.adivery.sdk.AdiveryBannerAdView
    android:id="@+id/banner_ad"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    app:placement_id="placementId"
    app:banner_size="banner" />
```

`placementId` باید با کلید تبلیغ‌گاه بنری شما جایگزین شود.

### اندازه‌های قابل استفاده برای بنر

| نوع بنر | مقدار Java | مقدار XML | اندازه تقریبی |
|---|---|---|---|
| بنر استاندارد | `BannerType.BANNER` | `banner` | `320×50` |
| بنر بزرگ | `BannerType.LARGE_BANNER` | `large_banner` | `320×100` |
| مستطیل متوسط | `BannerType.MEDIUM_RECTANGLE` | `medium_rectangle` | `300×250` |
| بنر هوشمند | `BannerType.SMART_BANNER` | `smart_banner` | `320×50` یا `320×90` |

### بارگذاری و Listener بنر

```java
AdiveryBannerAdView bannerAd = findViewById(R.id.banner_ad);

bannerAd.setBannerAdListener(new AdiveryAdListener() {
    @Override
    public void onAdLoaded() {
        // تبلیغ به‌طور خودکار نمایش داده می‌شود.
        // کارهای جانبی موردنیاز را اینجا انجام دهید.
    }

    @Override
    public void onError(String reason) {
        // خطا را چاپ کنید تا دلیل آن مشخص شود.
    }

    @Override
    public void onAdClicked() {
        // کاربر روی بنر کلیک کرده است.
    }
});

bannerAd.loadAd();
```

---

## تبلیغات همسان Native

برای پیاده‌سازی تبلیغ همسان، از کلاس `AdiveryNativeAdView` استفاده کنید.

### افزودن NativeAdView به XML

```xml
<com.adivery.sdk.AdiveryNativeAdView
    android:id="@+id/native_ad_view"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    app:placement_id="placementId"
    app:adivery_native_ad_layout="@layout/ad_layout_example" />
```

- `placement_id`: کلید تبلیغ‌گاه شما در پنل ادیوری
- `adivery_native_ad_layout`: layout اختصاصی تبلیغ همسان

### نمونه Layout تبلیغ همسان

```xml
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:id="@+id/adivery_wrapper"
    android:layout_width="300dp"
    android:layout_height="wrap_content"
    android:orientation="vertical">

    <com.adivery.sdk.AdiveryNativeAdMediaView
        android:id="@+id/adivery_image"
        android:layout_width="300dp"
        android:layout_height="wrap_content"
        android:adjustViewBounds="true"
        android:scaleType="centerCrop" />

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:gravity="center_vertical"
        android:orientation="horizontal"
        android:padding="4dp">

        <Button
            android:id="@+id/adivery_call_to_action"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_margin="4dp" />

        <TextView
            android:id="@+id/adivery_headline"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_margin="4dp"
            android:layout_weight="1" />

        <ImageView
            android:id="@id/adivery_icon"
            android:layout_width="36dp"
            android:layout_height="36dp"
            android:layout_margin="4dp" />
    </LinearLayout>
</LinearLayout>
```

### اجزای تبلیغ همسان

| عنصر | شناسه | اجباری؟ | توضیح |
|---|---|---:|---|
| Wrapper | `@+id/adivery_wrapper` | بله | بیرونی‌ترین لایه تبلیغ |
| Headline | `@+id/adivery_headline` | بله | عنوان تبلیغ |
| Call To Action | `@+id/adivery_call_to_action` | بله | دکمه فراخوان تبلیغ |
| Description | `@+id/adivery_description` | خیر | توضیحات تبلیغ |
| Advertiser | `@+id/adivery_advertiser` | خیر | نام برند یا اپلیکیشن تبلیغ‌دهنده |
| Media Image | `@+id/adivery_image` | خیر | تصویر بزرگ تبلیغ با نسبت `16:9` |
| Icon | `@id/adivery_icon` | خیر | آیکون برند یا اپلیکیشن |

### بارگذاری Native Ad

```java
AdiveryNativeAdView adView = findViewById(R.id.native_ad_view);

adView.setListener(new AdiveryAdListener() {
    @Override
    public void onAdLoaded() {
        // تبلیغ بارگذاری شد.
    }

    @Override
    public void onError(String reason) {
        // خطا را لاگ کنید تا دلیل آن مشخص شود.
    }

    @Override
    public void onAdShown() {
        // تبلیغ نمایش داده شد.
    }

    @Override
    public void onAdClicked() {
        // کاربر روی تبلیغ کلیک کرد.
    }
});

adView.loadAd();
```

---

## تبلیغات Pre-Roll / VAST با ExoPlayer

تبلیغات VAST یا `Video Ad Serving Template` معمولاً به‌صورت Pre-Roll، قبل از محتوای اصلی ویدئو، نمایش داده می‌شوند.

ادیوری امکان دریافت URL تبلیغ VAST و استفاده از آن در پلیرهایی مثل `ExoPlayer` را فراهم می‌کند.

### 1. افزودن کتابخانه‌های Media3

```groovy
dependencies {
    implementation "androidx.media3:media3-exoplayer:1.3.1"
    implementation "androidx.media3:media3-ui:1.3.1"
    implementation "androidx.media3:media3-exoplayer-ima:1.3.1"
}
```

### 2. دریافت URL تبلیغ VAST

```java
String vastUrl = Adivery.getVastUrl(placementId);
```

### 3. بررسی آماده بودن VAST

```java
if (TextUtils.isEmpty(Adivery.getVastUrl(placementId))) {
    // تبلیغ هنوز آماده نشده است.
}
```

اگر مقدار بازگشتی رشته‌ی خالی باشد، تبلیغ هنوز آماده‌ی نمایش نیست.

> **نکته:** URL تبلیغ VAST ممکن است با کمی تأخیر آماده شود. در صورت آماده نبودن، می‌توانید پس از مدت کوتاه، مثلاً ۱ ثانیه، دوباره وضعیت را بررسی کنید.

### 4. افزودن AdsConfiguration به MediaItem

```java
MediaItem mediaItem = new MediaItem.Builder()
    .setUri(contentUri)
    .setAdsConfiguration(
        new MediaItem.AdsConfiguration.Builder(Uri.parse(vastUrl)).build()
    )
    .build();
```

### 5. نمایش Companion Ad همراه ویدئو

ابتدا container بنر همراه را در XML تعریف کنید:

```xml
<FrameLayout
    android:id="@+id/companion_ad_slot"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:visibility="gone" />
```

سپس `CompanionAdSlot` را بسازید:

```java
FrameLayout adSlotContainer = findViewById(R.id.companion_ad_slot);

CompanionAdSlot companionAdSlot = ImaSdkFactory
    .getInstance()
    .createCompanionAdSlot();

companionAdSlot.setContainer(adSlotContainer);
companionAdSlot.setSize(320, 50);

List<CompanionAdSlot> companionAdSlots = new ArrayList<>();
companionAdSlots.add(companionAdSlot);
```

### 6. ساخت ImaAdsLoader

```java
ImaAdsLoader adsLoader = new ImaAdsLoader.Builder(this)
    .setCompanionAdSlots(companionAdSlots)
    .setAdEventListener(adEvent -> {
        switch (adEvent.getType()) {
            case SKIPPED:
            case COMPLETED:
            case ALL_ADS_COMPLETED:
                adSlotContainer.setVisibility(View.GONE);
                break;

            case LOADED:
                adSlotContainer.setVisibility(View.VISIBLE);
                break;
        }
    })
    .build();
```

### 7. مقداردهی ExoPlayer و شروع پخش

```java
DefaultMediaSourceFactory mediaSourceFactory = new DefaultMediaSourceFactory(this)
    .setAdsLoaderProvider(unused -> adsLoader)
    .setAdViewProvider(playerView);

ExoPlayer player = new ExoPlayer.Builder(this)
    .setMediaSourceFactory(mediaSourceFactory)
    .build();

adsLoader.setPlayer(player);

player.setMediaItem(mediaItem);
player.prepare();
player.play();
```

### 8. آزادسازی منابع

در زمان خروج کاربر از صفحه، حتماً `player` و `adsLoader` را release کنید:

```java
if (player != null) {
    adsLoader.setPlayer(null);
    playerView.setPlayer(null);
    player.release();
    player = null;
}

if (adsLoader != null) {
    adsLoader.release();
}
```

---

## عیب‌یابی

اگر تبلیغ نمایش داده نمی‌شود، ابتدا لاگ‌های ادیوری را فعال کنید:

```java
Adivery.setLoggingEnabled(true);
```

سپس Logcat را بررسی کنید تا دلیل خطا مشخص شود.

### موارد رایج برای بررسی

- مقدار `APP_ID` درست وارد شده باشد.
- مقدار `placementId` با نوع تبلیغ‌گاه هماهنگ باشد.
- اینترنت دستگاه فعال باشد.
- تبلیغ‌گاه در پنل ادیوری فعال باشد.
- قبل از نمایش، `Adivery.isLoaded(placementId)` مقدار `true` برگرداند.
- برای تبلیغ VAST، مقدار `Adivery.getVastUrl(placementId)` خالی نباشد.
- تبلیغ را مستقیماً داخل `onAdLoaded` نمایش ندهید.

---

## افزودن Mintegral Mediation

از نسخه‌های `4.8.4` به بعد امکان استفاده از `Mintegral` به‌عنوان mediation در پلتفرم ادیوری فراهم شده است.

### 1. افزودن repository مربوط به Mintegral

```groovy
allprojects {
    repositories {
        // سایر repositoryها

        // repository موردنیاز Mintegral
        maven {
            url "https://dl-maven-android.mintegral.com/repository/mbridge_android_sdk_oversea"
        }
    }
}
```

### 2. افزودن dependency کتابخانه Mintegral

```groovy
dependencies {
    // سایر dependencyهای پروژه
    implementation "com.mbridge.msdk.oversea:mbridge_android_sdk:16.9.71"
}
```

---

## چک‌لیست نهایی پیاده‌سازی

- [ ] نسخه Android Studio حداقل `4.2` باشد.
- [ ] نسخه Gradle حداقل `4.2.2` باشد.
- [ ] مقدار `minSdk` حداقل `21` باشد.
- [ ] `mavenCentral()` در repositories اضافه شده باشد.
- [ ] dependency اصلی ادیوری اضافه شده باشد.
- [ ] در صورت استفاده از نسخه‌های پایین‌تر از `4.8.4`، dependency مربوط به Kotlin اضافه شده باشد.
- [ ] `Adivery.configure(getApplication(), APP_ID)` در `onCreate` فراخوانی شده باشد.
- [ ] برای هر نوع تبلیغ، `placementId` صحیح استفاده شده باشد.
- [ ] قبل از نمایش تبلیغ، وضعیت `isLoaded` بررسی شود.
- [ ] تبلیغ داخل callbackهای `onAdLoaded` نمایش داده نشود.
- [ ] در صورت استفاده از VAST، منابع `player` و `adsLoader` در زمان خروج release شوند.
- [ ] برای عیب‌یابی، `Adivery.setLoggingEnabled(true)` فعال شود.

---

## نمونه ترتیب پیشنهادی پیاده‌سازی

1. افزودن dependency و repositoryها
2. راه‌اندازی SDK با `APP_ID`
3. ساخت تبلیغ‌گاه در پنل ادیوری و دریافت `placementId`
4. آماده‌سازی تبلیغ با متد مناسب
5. افزودن listener برای بررسی وضعیت تبلیغ
6. بررسی `isLoaded`
7. نمایش تبلیغ
8. فعال‌سازی لاگ در صورت بروز مشکل

