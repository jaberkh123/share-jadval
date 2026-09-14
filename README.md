# شهر جدول — بازی جدول کلمات متقاطع فارسی

## 📋 نمای کلی بازی

**شهر جدول** یک بازی جدول کلمات متقاطع (crossword) فارسی با طراحی مینیمال و جذاب است.  
بازیکن با کلیک روی خانه‌های جدول و تایپ حروف از صفحه‌کلید مجازی، کلمات را در دو جهت (افقی/عمودی) پر می‌کند.

**تکنولوژی:** Android (Kotlin + Jetpack Compose + Material3)  
**پکیج:** `com.aistudio.jadvalian.qyuzwr`  
**minSdk:** 24 | **targetSdk:** 36

---

## 🔨 بیلد و نصب

```bash
cd "/home/jaber/Downloads/games/حدول-مرحله-ای (2)"
./gradlew assembleDebug                                          # بیلد دیباگ
# نصب روی شبیه‌ساز/دستگاه متصل:
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.aistudio.jadvalian.qyuzwr/.MainActivity
```

> **نکته:** بیلد با `setsid nohup ./gradlew assembleDebug > /tmp/build.log 2>&1 < /dev/null &` در پس‌زمینه اجرا شود.

---

## 🗂 نقشه فایل‌ها

### فایل‌های اصلی کاتلین (`app/src/main/java/com/jadval/shahr/`)

| فایل | توضیح |
|------|-------|
| `MainActivity.kt` | اکتیویتی اصلی — مقداردهی Adivery SDK، enableEdgeToEdge، setContent به AppContent |
| `ui/PuzzleViewModel.kt` | ViewModel اصلی — منطق بازی، تایمر، ذخیره/بارگذاری پیشرفت، راهنماها، تبلیغات |
| `ui/Screens.kt` | تمام Composableهای UI — HomeScreen، DifficultySelectScreen، GameScreen، SettingsScreen، HelpScreen |
| `ui/SoundManager.kt` | مدیریت صداها با MediaPlayer (بدون وابستگی خارجی) |
| `ui/theme/Color.kt` | پالت رنگ روشن/تاریک + رنگ‌های سلول/وضعیت‌ها |
| `ui/theme/Theme.kt` | تم Material3 با رنگ‌های سفارشی |
| `ui/theme/Type.kt` | تایپوگرافی با فونت فارسی Ziba |
| `data/AdManager.kt` | مدیریت تبلیغات بنری سفارشی |
| `data/AppDatabase.kt` | دیتابیس Room برای ذخیره پیشرفت |
| `data/PuzzleDao.kt` | DAO دیتابیس |
| `data/PuzzleEntity.kt` | Entity جدول دیتابیس |
| `data/PuzzleModels.kt` | مدل‌های داده (Puzzle, Cell, Clue, ProgressData) |
| `data/PuzzleRepository.kt` | ریپوزیتوری دیتابیس |
| `data/PuzzleData_batch_*.kt` | فایل‌های داده پازل (۸ عدد بر اساس سایز/سختی) |

### منابع (`app/src/main/res/`)

| فایل | توضیح |
|------|-------|
| `drawable/bg_size_*.jpg` | تصاویر پس‌زمینه برای سایزهای مختلف جدول |
| `drawable/icon.png` | تصویر منبع آیکون اپ (در روت پروژه، 1254×1254) |
| `drawable/ic_foreground.png` | تصویر foreground آیکون adaptive برای لانچر |
| `drawable/jadval_shahr.png` | تصویر بنر تبلیغاتی |
| `drawable/ic_launcher_*.xml` | آیکون‌های لانچر (adaptive + vector) |
| `font/b_ziba_0.ttf` | فونت فارسی Ziba |
| `mipmap-*/ic_launcher.png` | آیکون‌های legacy (mdpi..xxxhdpi) |

---

## 🎮 جریان بازی / ماشین حالت

```
  ┌──────────┐
  │  Home    │ ← نمایش آمار، ادامه بازی قبلی، انتخاب مرحله، تنظیمات، راهنما
  └────┬─────┘
       │
  ┌────▼──────────┐
  │ Difficulty    │ ← انتخاب سختی (آسان، متوسط، سخت)
  │ Select        │
  └────┬──────────┘
       │
  ┌────▼──────────┐
  │   Game       │ ← صفحه اصلی بازی: جدول + صفحه‌کلید + سرنخ‌ها
  │   Screen     │
  └───────────────┘
```

**جریان حل جدول:**
1. بازیکن خانه‌ای را لمس می‌کند → سلول فعال + جهت (افقی/عمومی)
2. حروف از صفحه‌کلید مجازی تایپ می‌شوند
3. دکمه راهنما: فاش کردن یک حرف (مصرف سکه)
4. تکمیل همه کلمات → مرحله کامل → سکه + امتیاز

---

## 📊 داده

پازل‌ها hardcoded در ۸ فایل `PuzzleData_batch_*.kt` بر اساس سایز و سختی:
- **۸×۸:** آسان | **۱۰×۱۰:** آسان
- **۱۲×۱۲:** متوسط | **۱۴×۱۴,۱۶×۱۶,۱۸×۱۸:** ترکیبی
- **۲۰×۲۰,۲۲×۲۲,۲۴×۲۴,۲۶×۲۶:** سخت

---

## 📦 دارایی‌ها

**تصاویر:** bg_size_10.jpg, bg_size_15.jpg, bg_size_20.jpg, jadval_shahr.png, icon.png, ic_foreground.png, img_app_icon.jpg, ic_launcher.png  
**فونت:** b_ziba_0.ttf (Ziba — تک وزن)  
**صداها:** از MediaPlayer استفاده می‌کند (بدون فایل صوتی در res/raw)

---

## 🔧 راهنمای تغییر

| چه چیزی | کجا | مراحل |
|----------|-----|-------|
| پازل جدید | `data/PuzzleData_batch_*.kt` | تابع `create_*` بنویسید + به `puzzlesList` اضافه کنید |
| سختی جدید | `ui/Screens.kt` + `PuzzleViewModel.kt` | کارت در DifficultySelectScreen + منطق VM |
| تغییر رنگ | `ui/theme/Color.kt` | مقادیر Light*/Dark* را تغییر دهید |
| تغییر فونت | `res/font/` + `ui/theme/Type.kt` | ttf جدید + به‌روز‌رسانی `PersianFontFamily` |
| آیکون اپ | `mipmap-*/` + `drawable/ic_foreground.png` | با PIL از `icon.png` (روت پروژه) تولید کنید — رجوع به `what_i_am.md` §3 ردیف 18 |

---

## ⚠️ محدودیت‌ها

- متن‌های فارسی هاردکد شده (نه `strings.xml`)
- فونت Ziba فقط یک وزن (فقط faux Bold)
- حجم APK ~۳۱MB (تصاویر فشرده نشده)
- BGM اضافه نشده
- Process death بازی را ریست می‌کند

---

## 📝 تغییرات

| تاریخ | تغییر |
|-------|-------|
| ۲۰۲۶-۰۸-۲۱ | تنظیم مطابق GAME-BUILDER-GUIDE.md: P0 build system + P1 RTL, fontScale, اعداد فارسی, portrait |
