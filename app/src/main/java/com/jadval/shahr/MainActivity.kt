package com.jadval.shahr

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.jadval.shahr.ui.AppContent
import com.jadval.shahr.ui.PuzzleViewModel
import com.adivery.sdk.Adivery
import com.adivery.sdk.AdiveryListener

class MainActivity : ComponentActivity() {
    private val viewModel: PuzzleViewModel by viewModels()

    companion object {
        // شناسه اپلیکیشن ادیوری - کاربر می‌تواند این را با شناسه اپ خود جایگزین کند
        const val ADIVERY_APP_ID = "6e70ac5c-ab00-483d-a68c-cd806deebff6" 
        // شناسه تبلیغگاه جایزه‌ای (Rewarded Placement ID) که ارسال کرده‌اید
        const val ADIVERY_REWARD_PLACEMENT = "dcb2a5d1-2d2d-4eee-9ed8-764bea9faff5"
        // شناسه تبلیغگاه میان‌صفحه‌ای (Interstitial Placement ID) جدید
        const val ADIVERY_INTERSTITIAL_PLACEMENT = "f1518c82-53b5-438b-9c8f-eabdfa25f4a3"
        // شناسه تبلیغگاه همسان جدید
        const val ADIVERY_NATIVE_PLACEMENT = "cc8efa70-1e6e-42ad-ba1a-4bdac75516e8"
        // شناسه تبلیغات بازگشت به برنامه (App Open) جدید
        const val ADIVERY_APP_OPEN_PLACEMENT = "2f38f8bf-ab65-45dc-9c62-c4153aae2eae"

        @Volatile
        var isShowingFullscreenAd = false
    }

    private var lastPauseTime = 0L

    override fun onPause() {
        super.onPause()
        lastPauseTime = System.currentTimeMillis()
    }

    override fun onStart() {
        super.onStart()
        val pauseTime = System.currentTimeMillis() - lastPauseTime
        // فقط اگر بیش از ۵ ثانیه خارج از برنامه بوده، یا اولین بار است که برنامه لود می‌شود
        if (lastPauseTime == 0L || pauseTime > 5000L) {
            if (!isShowingFullscreenAd) {
                if (Adivery.isLoaded(ADIVERY_APP_OPEN_PLACEMENT)) {
                    Adivery.showAppOpenAd(this, ADIVERY_APP_OPEN_PLACEMENT)
                } else {
                    Adivery.prepareAppOpenAd(this, ADIVERY_APP_OPEN_PLACEMENT)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize Adivery SDK
        try {
            // فعال‌سازی لاگ‌ها برای عیب‌یابی دقیق‌تر
            Adivery.setLoggingEnabled(true)

            // افزودن Listener سراسری قبل از پیکربندی و درخواست تبلیغ
            Adivery.addGlobalListener(object : AdiveryListener() {
                override fun onRewardedAdLoaded(placementId: String) {
                    android.util.Log.d("Adivery", "Rewarded loaded: $placementId")
                }

                override fun onInterstitialAdLoaded(placementId: String) {
                    android.util.Log.d("Adivery", "Interstitial loaded: $placementId")
                }

                override fun onRewardedAdShown(placementId: String) {
                    isShowingFullscreenAd = true
                    android.util.Log.d("Adivery", "Rewarded shown: $placementId")
                }

                override fun onInterstitialAdShown(placementId: String) {
                    isShowingFullscreenAd = true
                    android.util.Log.d("Adivery", "Interstitial shown: $placementId")
                }

                override fun onAppOpenAdLoaded(placementId: String) {
                    android.util.Log.d("Adivery", "App Open loaded: $placementId")
                }

                override fun onAppOpenAdShown(placementId: String) {
                    isShowingFullscreenAd = true
                    android.util.Log.d("Adivery", "App Open shown: $placementId")
                }

                override fun onAppOpenAdClosed(placementId: String) {
                    isShowingFullscreenAd = false
                    android.util.Log.d("Adivery", "App Open closed: $placementId")
                    // بارگذاری مجدد تبلیغ بازگشت به برنامه پس از بسته‌شدن
                    Adivery.prepareAppOpenAd(this@MainActivity, ADIVERY_APP_OPEN_PLACEMENT)
                }

                override fun log(placementId: String, log: String) {
                    android.util.Log.d("Adivery", "$placementId -> $log")
                }

                override fun onRewardedAdClosed(placementId: String, isRewarded: Boolean) {
                    isShowingFullscreenAd = false
                    android.util.Log.d("Adivery", "Rewarded ad closed for placement: $placementId (isRewarded param: $isRewarded). Awarding anyway for a seamless experience.")
                    if (placementId == ADIVERY_REWARD_PLACEMENT) {
                        runOnUiThread {
                            viewModel.revealActiveCellFree()
                            Toast.makeText(this@MainActivity, "جایزه دریافت شد: حرف خانه فعال آشکار شد!", Toast.LENGTH_LONG).show()
                        }
                    }
                    // بارگذاری خودکار تبلیغ بعدی پس از بسته‌شدن
                    Adivery.prepareRewardedAd(this@MainActivity, ADIVERY_REWARD_PLACEMENT)
                    // همچنین بازگشت به برنامه را آماده کنید
                    Adivery.prepareAppOpenAd(this@MainActivity, ADIVERY_APP_OPEN_PLACEMENT)
                }

                override fun onInterstitialAdClosed(placementId: String) {
                    isShowingFullscreenAd = false
                    
                    // بارگذاری خودکار تبلیغ بعدی پس از بسته‌شدن
                    Adivery.prepareInterstitialAd(this@MainActivity, ADIVERY_INTERSTITIAL_PLACEMENT)
                    // همچنین بازگشت به برنامه را آماده کنید
                    Adivery.prepareAppOpenAd(this@MainActivity, ADIVERY_APP_OPEN_PLACEMENT)
                }
            })

            // پیکربندی SDK با App ID و بارگذاری اولیه تبلیغات با Placement ID مربوطه
            Adivery.configure(application, ADIVERY_APP_ID)
            Adivery.prepareRewardedAd(this, ADIVERY_REWARD_PLACEMENT)
            Adivery.prepareInterstitialAd(this, ADIVERY_INTERSTITIAL_PLACEMENT)
            Adivery.prepareAppOpenAd(this, ADIVERY_APP_OPEN_PLACEMENT)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Initialize custom AdManager for banner ads
        com.jadval.shahr.data.AdManager.init(applicationContext, lifecycleScope)

        setContent {
            AppContent(viewModel = viewModel)
        }
    }
}
