# هندآف (۲۰۲۶-۰۹-۱۰): فیکس کیبورد — پوشش کامل حروف جواب هر سرنخ ✅

- **مشکل:** کیبورد مجازی حروف جوابِ سرنخ فعال را با لیست ثابت ۳۲ حرفی فیلتر می‌کرد؛ حروفی مثل «ئ، ء، ة، ى، ّ، ً، فاصله» بی‌صدا drop می‌شدند و هرگز روی کیبورد نمی‌آمدند → ~۲۷۶ سرنخ (در هر ۳ مود) فقط با راهنمای ۱۰۰ سکه‌ای قابل حل بودند. همچنین برای ۱۷۱ جوابِ دارای «آ» بدون «ا» ساده (مثل «آبکش»)، کلید «ا» هم از کیبورد حذف می‌شد.
- **تأیید دیتا:** حروف نادر عیناً داخل سلول‌های گرید هستند (۳۵۴ سرنخ)، جواب‌ها و تقاطع‌ها ۱۰۰٪ سازگارند (۰ تضاد واقعی؛ ۲۹۴ اختلاف ظاهری فقط آ/ا که با نرمال‌سازی حل می‌شود) → فیکس فقط در UI انجام شد، دیتا دست نخورد.
- **فیکس (روش هوشمند):**
  1. `data/PuzzleModels.kt`: تابع مشترک `normalizePersianChar()` اضافه شد (منبع واحد حقیقت): `آ/أ/إ→ا، ئ/ى/ي→ی، ة→ه، ك→ک، ؤ→و`. «ء» و اعراب و فاصله معادل نرمال ندارند.
  2. `PuzzleViewModel.kt`: `normalizeCharForComparison()` حالا delegate به `normalizePersianChar` است → همهٔ مقایسه‌ها (isCompleted، countCorrectClues، reveal hint، رنگ سلول درست/غلط در Screens.kt) یکدست شدند؛ یعنی تایپ «ی» جای سلول «ئ» قبول می‌شود. نرمال‌سازی `onKeyPressed` هم به همین تابع وصل شد.
  3. `Screens.kt` — `PersianOnScreenKeyboard`: حروف جواب قبل از فیلتر نرمال می‌شوند و فیلتر جدید «۳۲ حرف پایه + {ء، ّ، ً، فاصله}» است → هر حرفی که گرید لازم دارد حتماً روی کیبورد هست. کلید فاصله با علامت ␣ نمایش داده می‌شود.
- **اعتبارسنجی:** شبیه‌سازی منطق جدید روی کل ۱۰۴,۴۸۶ سرنخ → **۰ سرنخ بدون پوشش کیبورد**. حداکثر حروف یکتای یک جواب = ۱۰ (کمتر از ۱۵ کلید).
- ورژن: `versionCode = 13`, `versionName = "2.2"`.

---

# هندآف (۲۰۲۶-۰۹-۱۰): بازگشت قفل مراحل + فیکس پاک شدن پیشرفت بعد از آپدیت ✅

- `DEBUG_UNLOCK_ALL_LEVELS` در `Screens.kt` (خط ~۷۰) به `false` برگشت → مراحل مثل قبل قفل‌اند (فقط مرحله ۱ + مرحله بعد از آخرین حل‌شده باز است). برای تست دوباره `true` کن.
- **ریشه واقعی پاک شدن پیشرفت بعد از آپدیت:** بیلد دیباگ با debug keystore و ریلیز با `salari.jks` امضا می‌شد → نصب یکی روی دیگری خطای امضا می‌داد و باید uninstall می‌شد → uninstall کل دیتا (Room DB + سکه‌ها) را پاک می‌کند. **ذخیره‌سازی پیشرفت خودش سالم است** (Room `puzzle_progress` + `crossword_prefs`؛ هیچ پاک‌سازی خودکاری روی استارت نیست).
- **فیکس‌ها:**
  1. `app/build.gradle.kts`: بیلد **debug هم با `salari.jks`** امضا می‌شود → دیباگ/ریلیز روی هم به‌عنوان آپدیت نصب می‌شوند و پیشرفت حفظ می‌شود (تأیید شد: SHA-256 هر دو = `243891d6...`).
  2. `AppDatabase.kt`: `fallbackToDestructiveMigration()` حذف شد (اگر schema در آینده تغییر کرد Migration بنویس؛ اسم DB همیشه `crossword_database_v3` بماند).
- خروجی تست: `jadval-shahr-v2.2-debug-locked.apk` در روت (مراحل قفل + فیکس کیبورد).

---

# هندآف (۲۰۲۶-۰۹-۰۵): حذف تبلیغ همسان + رنگ بنفش مود BigGrid ✅
# هندآف (۲۰۲۶-۰۹-۰۵): فیکس برعکس بودن drag افقی در جدول زوم‌شده ✅

- **مشکل:** در حالت زوم، کشیدن (drag) افقی جدول برعکس بود (بالا/پایین درست بود).
- **ریشه:** کانتینر اسکرول `CrosswordGrid` (Screens.kt ~خط ۳۰۵۶) جهت RTL اپ را به ارث می‌برد، ولی همه محاسبات (drag delta، auto-scroll، افست اورلی‌ها، edge glowها) بر اساس مختصات LTR نوشته شده بودند. در RTL، مقدار اسکرول افقی از لبه راست شمرده می‌شود → `dispatchRawDelta(-dragAmount.x)` افقی را معکوس اعمال می‌کرد. عمودی تحت تأثیر RTL نیست، برای همین درست بود.
- **فیکس:** کل کانتینر اسکرول (Box داخلی BoxWithConstraints شامل viewport + گرید + اورلی‌ها + glowها) در `CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr)` قرار گرفت. با این کار همه معادلات LTR کد یکدست معتبر شدند؛ drag کد دست‌نخورده ماند و درست شد.
- ورژن: `versionCode = 12`, `versionName = "2.1"`.
- بیلد: `BUILD SUCCESSFUL in 1m 47s` — امضا با salari.jks تأیید شد.
- APK ریلز: روت پروژه → `jadval-shahr-game-v2.1.apk`

---


# هندآف (۲۰۲۶-۰۹-۰۵): فیکس امنیتی رمز keystore + نسخه 2.0 ✅

- پسوردهای keystore از `app/build.gradle.kts` حذف و به `local.properties` (gitignored) منتقل شدند: `KEYSTORE_PATH`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`. خواندن با UTF-8 (چون مسیر پروژه حروف فارسی دارد). override با متغیر محیطی هم پشتیبانی می‌شود؛ اگر هیچ‌کدام نباشد بیلد با `GradleException` fail می‌شود.
- `.gitignore`: الگوهای `*.jks` و `*.keystore` اضافه شدند.
- ورژن: `versionCode = 11`, `versionName = "2.0"`.
- بیلد: `BUILD SUCCESSFUL in 4m 13s` — امضا با `salari.jks` تأیید شد (SHA-256 گواهی = `243891d6...`).
- APK ریلز: روت پروژه → `jadval-shahr-game-v2.0.apk` (~۱۹MB)

---



- `NativeAdBanner` در `Screens.kt`: تبلیغ همسان Adivery و ریلود ۳ دقیقه‌ای آن حذف شد. فقط یک نوار آلارم قرمز باریک (10dp) مانده که هر ۳ دقیقه ۳ پالس قرمز می‌زند (همان الگوی قبلی). مستندات قدیمی در `NATIVE_AD_REFRESH.md` منقضی علامت‌گذاری شد.
- `BigGridModeColors` در `theme/Color.kt`: از سبز به **بنفش** تغییر کرد (primary=6A1B9A, container=CE93D8, cardBg=F3E5F5, starTint=AB47BC). کارت «شروع با جدول بزرگ» در `DifficultySelectScreen` هم بنفش شد (0xFFAB47BC).
- رنگ‌های سبز `SectionCardItem` (وضعیت «کامل‌شده» و بج «آسان») به همه مودها مشترک‌اند و عمداً دست نخورده‌اند.
- بیلد: `BUILD SUCCESSFUL` — APK جدید در روت: `jadval-shahr-game-debug.apk`

---


# هندآف — فیکس کامل مود BigGrid (جدول‌های برعکس + جواب‌های عمودی خراب) ✅ FIXED

## آخرین فیکس (۲۰۲۶-۰۹-۰۵): برعکس بودن جدول‌های BigGrid
### ریشه واقعی مشکل
JSON اصلی (`batch_*-done`) کلمات افقی را **LTR** ذخیره می‌کند (حرف اول در ستون چپ‌ترین خانه) — یعنی JSON آینه‌ی جدول واقعی RTL فارسی است.

`convert_biggrid.py` قبلی به‌جای آینه‌کردن **کل جدول**، حروف هر کلمه‌ی افقی را **درجا (in-place) برعکس** می‌کرد (خطوط ~۱۰۵–۱۱۴). این کار:
- حروف **تقاطع‌ها** را خراب می‌کرد
- جواب‌های **عمودی** که از جدولِ خراب بازمحاسبه می‌شدند به gibberish تبدیل می‌شدند (مثل `اانحوینر` به‌جای `ازبکستان`)

### فیکس انجام‌شده
در `convert_biggrid.py`:
- **کل گرید افقی آینه می‌شود** (`flat[r][c] = raw[r][sz-1-c]`) → تقاطع‌ها حفظ می‌شوند
- `mgb` (grid_black آینه‌شده) برای شماره‌گذاری grid-based استفاده می‌شود
- `startCol` همه سرنخ‌ها (across و down) = `sz - 1 - s["col"]`
- جواب‌ها مستقیم از slot words می‌آیند (بدون بازمحاسبه از گرید)
- هر ۶ فایل بازتولید شد: **۲۰۰ پازل، ۰ خطا** در verification
- خروجی APK دیباگ جدید با دیتای جدید compile شد (تأیید شد `classes3.dex` شامل `ازبکستان` است و `اانحوینر` ندارد)

---

# هندآف قبلی — مشکل عمودی‌ها در BigGrid ✅ FIXED
# هندآف — مشکل عمودی‌ها در BigGrid ✅ FIXED

## وضعیت فعلی
پروژه: `/home/jaber/Downloads/games/حدول-مرحله-ای (2)`
بیلد آخر: `BUILD SUCCESSFUL` ✅
وضعیت باگ: **رفع شده** ✅

## مشکل (قبلاً باقی‌مانده)
در جدول‌های **شروع با جدول بزرگ** (BigGrid)، سرنخ‌های **عمودی (down)** جابه‌جا نمایش داده می‌شن. حروف افقی درست هستن ولی عمودی‌ها مشکل دارن. این مشکل **فقط** در BigGrid هست (Easy/Hard درستن).

## ریشه مشکل
فایل‌های `PuzzleData_batch_biggrid_*.kt` توسط یک فرآیند متفاوت از `convert_biggrid.py` تولید شده بودن. این فرآیند:
- شماره سرنخ‌ها (clue numbers) را به صورت **ترتیبی** (1, 2, 3, ...) تنظیم می‌کرد
- به جای شماره‌گذاری بر اساس **موقعیت در جدول** (grid-based)

مثال:
| | فایل قدیمی (اشتباه) | فایل جدید (درست) |
|---|---|---|
| سرنخ down در (0,0) | num=1 | num=1 |
| سرنخ down در (0,1) | num=2 | num=2 |
| سرنخ down در (7,0) | num=3 ❌ | num=39 ✅ |
| سرنخ down در (12,0) | num=4 ❌ | num=65 ✅ |

## فکس انجام‌شده
اجرای مجدد `convert_biggrid.py` برای بازتولید تمام 6 فایل BigGrid:
- `PuzzleData_batch_biggrid_16x16.kt` (50 پازل)
- `PuzzleData_batch_biggrid_18x18.kt` (50 پازل)
- `PuzzleData_batch_biggrid_20x20.kt` (30 پازل)
- `PuzzleData_batch_biggrid_22x22.kt` (30 پازل)
- `PuzzleData_batch_biggrid_24x24.kt` (30 پازل)
- `PuzzleData_batch_biggrid_26x26.kt` (10 پازل)

**نتیجه:** 200 پازل، 0 خطا در verification ✅

## فکس‌های قبلاً انجام‌شده

### ۱. نمایش جدول (RTL → LTR)
فایل: `app/src/main/java/com/jadval/shahr/ui/Screens.kt` ~خط ۳۱۲۰
```kotlin
CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
    Column(...) {
        for (row in 0 until rows) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (col in 0 until cols) { ... }
            }
        }
    }
}
```
بدون این، کلمات افقی ۱۸۰ درجه برعکس بودن.

### ۲. تابع `clueCol()` در PuzzleModels.kt
```kotlin
fun clueCol(clue: Clue, i: Int): Int {
    return if (clue.direction == "down") {
        clue.startCol                       // عمودی: ستون ثابت
    } else if (acrossReadsLeftToRight) {
        clue.startCol - clue.length + 1 + i // LTR across
    } else {
        clue.startCol - i                   // RTL across
    }
}
```
این تابع در `countCorrectClues`، `useHintRevealWord`، `highlightedCells`، و `isPartOfCorrectCompletedClue` استفاده می‌شه.

### ۳. بازتولید داده‌های BigGrid
فایل: `convert_biggrid.py` — از JSON اصلی بازتولید شد:
- حروف کلمات افقی در grid برعکس شدن (RTL)
- جواب سرنخ‌های عمودی از grid جدید بازمحاسبه شدن
- نرمال‌سازی `آ→ا` در جواب‌ها
- ۲۰۰ پازل، **۰ خطا** در verification

### ۴. `moveToNextCell` / `moveToPreviousCell` شرطی
در `PuzzleViewModel.kt` — جهت حرکت cursor بر اساس `acrossReadsLeftToRight`.

### ۵. حافظه کامپایلر
`gradle.properties`: `org.gradle.jvmargs=-Xmx4096m`, `kotlin.daemon.jvmargs=-Xmx3072m`

## ساختار داده‌ها
- **Easy/Hard**: `acrossReadsLeftToRight = false` (پیش‌فرض) — حروف RTL در grid
- **BigGrid**: `acrossReadsLeftToRight = false` (بعد از بازتولید داده) — حروف هم RTL شدن
- `startCol` همیشه = **ستون سمت راست** کلمه افقی
- کد از `startCol` به چپ می‌خونه: `startCol - i`

## فایل‌های کلیدی
| فایل | توضیح |
|------|--------|
| `app/.../data/PuzzleModels.kt` | `Puzzle`، `Clue`، `clueCol()`، `PuzzleDataBigGrid` |
| `app/.../ui/PuzzleViewModel.kt` | منطق بازی، `moveToNextCell`، `countCorrectClues` |
| `app/.../ui/Screens.kt` | `CrosswordGrid`، `highlightedCells`، `isPartOfCorrectCompletedClue` |
| `convert_biggrid.py` | بازتولید داده BigGrid از JSON |

## مسیر JSON اصلی
`/home/jaber/Downloads/tools/jadval-saz/crossword_scripts/download/جدول مرحله ای/batch_*-done/`

## دستور بیلد
```bash
cd "/home/jaber/Downloads/games/حدول-مرحله-ای (2)"
setsid nohup ./gradlew assembleDebug --console=plain --no-daemon > /tmp/build.log 2>&1 < /dev/null &
tail -5 /tmp/build.log
```

## دستور verification
```bash
python3 << 'PYEOF'
import re, glob
for filepath in sorted(glob.glob('app/src/main/java/com/jadval/shahr/data/PuzzleData_batch_biggrid_*.kt')):
    with open(filepath) as f:
        content = f.read()
    blocks = re.split(r'(?=fun create_)', content)
    total_errors = 0
    total_puzzles = 0
    for block in blocks[1:]:
        if 'gridSolutions' not in block: continue
        total_puzzles += 1
        grid_m = re.search(r'gridSolutions = listOf\((.*?)\)', block, re.DOTALL)
        cells = re.findall(r'"([^"]*)"', grid_m.group(1))
        rows = int(re.search(r'rows = (\d+)', block).group(1))
        cols = int(re.search(r'cols = (\d+)', block).group(1))
        for m in re.finditer(r'Clue\((\d+),\s*"([^"]*)",\s*"[^"]*",\s*"[^"]*",\s*"(across|down)",\s*(\d+),\s*(\d+),\s*(\d+)\)', block):
            num, answer, d, sr, sc, ln = m.group(1), m.group(2), m.group(3), int(m.group(4)), int(m.group(5)), int(m.group(6))
            if d == 'across':
                actual = ''.join(cells[sr*cols + sc - i] for i in range(ln))
            else:
                actual = ''.join(cells[(sr+i)*cols + sc] for i in range(ln))
            if actual != answer:
                total_errors += 1
    fname = filepath.split('/')[-1]
    print(f'{fname}: {total_puzzles} puzzles, {total_errors} errors')
PYEOF
```

## کاری که انجام شد
مشکل عمودی‌ها در BigGrid پیدا و فکس شد. ریشه مشکل: فایل‌های `PuzzleData_batch_biggrid_*.kt` شماره سرنخ‌ها را به صورت ترتیبی (1,2,3,...) داشتن به جای شماره‌گذاری بر اساس موقعیت در جدول. با اجرای مجدد `convert_biggrid.py` تمام شماره‌ها اصلاح شدن.
