# Shahr Jadval (شهر جدول) — Game Identity & File Map

> **Purpose:** This document describes what this game is, its architecture, and — most importantly —
> **exactly which files to edit for each category of change.** Read this before making any edit
> so you never need to scan the entire codebase again.

---

## 1. 🎮 Game Overview

**Shahr Jadval (شهر جدول)** = Persian Crossword Puzzle Game.

- **Genre:** Crossword (جدول کلمات متقاطع) with Persian UI, minimal Material3 design.
- **Tech:** Native Android — **Kotlin + Jetpack Compose + Material3**.
- **Package:** `com.aistudio.jadvalian.qyuzwr` (published ID) / `com.jadval.shahr` (source namespace).
- **minSdk:** 24 | **targetSdk:** 36 | **Compose BOM:** 2024.09.00 | **Kotlin:** 2.2.10 | **AGP:** 9.1.1

### Core Loop

1. **Home Screen** → shows stats, ads, rank, "Continue" or "New Game" buttons.
2. **Difficulty Select** → choose Easy / Medium / Hard.
3. **Sections Screen** → pick a section (size 8×8 up to 26×26, each with a rank name).
4. **Levels Screen** → pick a specific puzzle within that section.
5. **Game Screen** → the crossword grid + virtual Persian keyboard + clues.
   - Tap a cell → direction toggles (across/down). Type letters.
   - Hints consume coins. Check button validates the entire grid.
   - Completion → score + coins + rank-up dialog.
6. **Settings** → sound toggle, dark mode toggle, clue font size slider, rate, share.
7. **Help** → scrolling tutorial steps.

---

## 2. 📁 Project Structure (all paths relative to project root)

```
app/src/main/java/com/jadval/shahr/
├── MainActivity.kt             # Entry point: Adivery SDK init, enableEdgeToEdge, setContent
├── ui/
│   ├── Screens.kt              ★ THE BIG ONE (4671 lines) — ALL composable screens in one file
│   │                            HomeScreen, DifficultySelectScreen, SectionsScreen,
│   │                            SectionLevelsScreen, GameScreen, SettingsScreen, HelpScreen
│   │                            Also: CoinPill, CustomAdBanner, CrosswordGrid,
│   │                            PersianOnScreenKeyboard, KeyboardKey, etc.
│   ├── PuzzleViewModel.kt      ★ ViewModel — navigation state, active puzzle state,
│   │                            timer, hints, score, DB operations, ad cooldowns
│   └── SoundManager.kt         Synthetic sound effects (AudioTrack sine waves)
│       theme/
│       ├── Color.kt            Light/dark color palette + cell/status colors
│       ├── Theme.kt            Material3 theme (LightColorScheme + static dark toggle)
│       └── Type.kt             Typography with Ziba font (single weight)
├── data/
│   ├── PuzzleModels.kt         Data classes: Clue, Puzzle, GameSection + PuzzleData object
│   │                            (aggregates all puzzle batches + sections list)
│   ├── PuzzleData_batch_*      8+ data files (~10k lines each) — hardcoded puzzle definitions
│   ├── PuzzleEntity.kt         Room entity for puzzle progress
│   ├── PuzzleDao.kt            Room DAO
│   ├── AppDatabase.kt          Room database singleton
│   ├── PuzzleRepository.kt     Repository wrapping DB + SharedPreferences
│   └── AdManager.kt            Custom ad fetcher (HTTP + SharedPreferences cache)
└── res/
    ├── drawable/               bg_size_*.jpg (backgrounds), jadval_shahr.png (ad banner),
    │                            ic_launcher_*.xml, ic_foreground.png, rounded_bg_ad.xml
    ├── font/                   b_ziba_0.ttf (Ziba Persian font, single weight)
    ├── layout/                 adivery_native_ad.xml, native_ad_container.xml
    ├── values/                 strings.xml (only app_name!), colors.xml, themes.xml
    ├── mipmap-*/               Launcher icons (mdpi..xxxhdpi + adaptive anydpi-v26)
    └── AndroidManifest.xml     Manifest: portrait, configChanges, permissions, Adivery
```

All puzzle data files:
- `PuzzleData_batch_strict_easy_8x8.kt`
- `PuzzleData_batch_strict_easy_10x10.kt`
- `PuzzleData_batch_medium_12x12.kt`
- `PuzzleData_batch_mix_14x14.kt`
- `PuzzleData_batch_mix_16x16.kt`
- `PuzzleData_batch_mix_18x18.kt`
- `PuzzleData_batch_all_20x20.kt`
- `PuzzleData_batch_all_22x22.kt`
- `PuzzleData_batch_all_24x24.kt`
- `PuzzleData_batch_all_26x26.kt`
---

## 3. 🔧 What to Edit Where (Change Directory)

> **IMPORTANT:** Never scan the whole codebase again. Use this table.

| # | What you want to change | File(s) to edit | Notes |
|---|---|---|---|
| **1** | **Add / remove / modify a puzzle** | One of `data/PuzzleData_batch_*.kt` by size/difficulty | Each batch has `create_*` fns; they auto-join via `PuzzleData.puzzlesList` in `PuzzleModels.kt` |
| **2** | **Change difficulty tiers** | `PuzzleModels.kt` (`sectionsList`); `PuzzleViewModel.kt` (filter strings); `Screens.kt` (display) | Rename "آسان"/"متوسط"/"سخت" |
| **3** | **Change game colors** | `ui/theme/Color.kt` (palette) + `ui/theme/Theme.kt` (scheme) | Cell/selection/correct colors in **Color.kt** |
| **4** | **Change fonts** | 1. Copy `.ttf` to `res/font/` 2. Edit `ui/theme/Type.kt` 3. Optionally `Theme.kt` | Single-weight Ziba; add Bold/Black files for real weights |
| **5** | **Change keyboard** | `ui/Screens.kt` — `PersianOnScreenKeyboard()`, `KeyboardKey()` | Key size, font, layout |
| **6** | **Change grid/cell** | `ui/Screens.kt` — `CrosswordCell()`, `CrosswordGrid()` (~L2516-3300); `Color.kt` | Cell size, colors, borders, **layout direction** |
| **7** | **Fix grid RTL/LTR issue** | `ui/Screens.kt` — `CrosswordGrid()` around line 3002 | Wrap `Row` in `CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr)` so grid data (LTR) displays correctly |
| **7** | **Change clues** | `ui/Screens.kt` — `ClueListItem()` + `GameScreen()` | Font size via `clueFontSize` in VM + Settings |
| **8** | **Change Home Screen** | `ui/Screens.kt` — `HomeScreen()` (~L213) | Stats, continue, ad, rank |
| **9** | **Change Settings** | `ui/Screens.kt` — `SettingsScreen()` (~L3778); `PuzzleViewModel.kt` | Sound, dark mode, clue font slider, rate, share |
| **10** | **Change ads** | `MainActivity.kt` (Adivery IDs); `data/AdManager.kt` (custom ads); `Screens.kt` (ad composables) | Adivery SDK in `MainActivity.onCreate()` |
| **11** | **Change navigation** | `ui/PuzzleViewModel.kt` (`Screen` enum, `navigateTo/Back`); `ui/Screens.kt` (`AppContent()`) | Simple sealed-interface nav |
| **12** | **Change sounds** | `ui/SoundManager.kt` — synthetic AudioTrack | No mp3 files currently |
| **13** | **Change score/coins** | `PuzzleViewModel.kt` — `calculateScore()`, coin state | SharedPreferences keys `global_bonus_coins`, `ad_bonus_coins_*` |
| **14** | **Change rank system** | `data/PuzzleModels.kt` (rank names); `PuzzleViewModel.kt` (`checkRankUp()`, rank fns) | "سرباز وظیفه", "سرگرد شهر جدول", etc. |
| **15** | **Change hint behavior** | `PuzzleViewModel.kt` — hint usage/cost functions | Search `hintDeductions`, `revealActiveCellFree()` |
| **16** | **Add a new screen** | 1. `Screen` enum in `PuzzleViewModel.kt` 2. Composable in `Screens.kt` 3. Route in `AppContent()` | Simple `when(screen)` |
| **17** | **Change strings/text** | `ui/Screens.kt` — most strings hardcoded in composables | Not in strings.xml (only `app_name` there) |
| **18** | **Change app icon** | `res/mipmap-*/` + `res/drawable/ic_launcher_*.xml` + `res/drawable/ic_foreground.png` + `AndroidManifest.xml` (`android:icon`/`android:roundIcon`) | **Source:** `icon.png` (در روت پروژه). با PIL همه mipmap PNGها و `ic_foreground.png` (108×108) تولید می‌شوند. ⚠️ از circular reference جلوگیری کن: `ic_launcher_foreground.xml` باید اشاره کنه به `@drawable/ic_foreground` (نه `@mipmap/ic_launcher`)|
| **19** | **Change app name/package** | `res/values/strings.xml` (name); `app/build.gradle.kts` (applicationId); `AndroidManifest.xml` | Update all imports + R references |
| **18a** | **Change font sizes (global)** | `ui/Screens.kt` — `HomeScreen()` + `SectionsScreen()` + `SectionLevelsScreen()` + `SectionCardItem()` | Since v1.6: all font sizes reduced by ~3sp for cleaner look |
| **18b** | **Center-align text on screens** | `ui/Screens.kt` — `HomeScreen()` + `SectionsScreen()` + `SectionCardItem()` + `DifficultyProgressItem()` | Since v1.6: `textAlign = TextAlign.Center` added. **2026-08-22 fix:** Changed `Alignment.End` → `Alignment.CenterHorizontally` on HomeScreen continue/new-game cards, SectionCardItem text column, and DifficultyProgressItem for full center alignment of words. |
| **18c** | **Regenerate app icon** | `icon.png` (root) + PIL script → all mipmaps + `ic_foreground.png` | Since v1.6: regenerated from 1254×1254 RGBA source |
| **20** | **Change bg images** | `res/drawable/bg_size_*.jpg` + references in `Screens.kt` | Currently bg_size_10, 15, 20 |
---

## 4. ⚙️ Architecture Decisions (Key Patterns)

### Navigation
```kotlin
// PuzzleViewModel.kt
enum class Screen { HOME, DIFFICULTY_SELECT, GAME, SETTINGS, HELP }
var currentScreen by mutableStateOf(Screen.HOME)

// Screens.kt — AppContent()
BackHandler(enabled = currentScreen != Screen.HOME) { viewModel.handleBackPress() }
CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
    AnimatedContent(targetState = viewModel.currentScreen) { screen ->
        when(screen) { Screen.HOME -> HomeScreen(viewModel) /* ... */ }
    }
}
```

### RTL + Font Scale Capping
```kotlin
// In AppContent() — lines ~128-133 of Screens.kt
val fontScale = minOf(density.fontScale, 1.15f)
CompositionLocalProvider(
    LocalLayoutDirection provides LayoutDirection.Rtl,
    LocalDensity provides Density(density.density, fontScale = fontScale)
)
```

### State Management
- **ViewModel** (`PuzzleViewModel`) uses `mutableStateOf<>` + `by delegated` for Compose-reactive.
- **Database** is Room (`AppDatabase`, `PuzzleDao`, `PuzzleEntity`).
- **SharedPreferences** for coins, settings, launch count, ad cache, last active puzzle.
- **Custom ads** via `AdManager.kt` (HTTP polling + SP cache).

### No external nav/lib
No Jetpack Navigation, no Hilt/Dagger, no Retrofit, no Firebase in actual use.
---

## 5. 📋 Compliance with GAME-BUILDER-GUIDE.md

| Requirement | Status | Detail |
|---|---|---|
| **§2 Build System (gradle props, settings, toml, wrapper)** | ✅ PASS | All match guide exactly |
| **§2 compileSdk 36 / minSdk 24 / targetSdk 36** | ✅ PASS | app/build.gradle.kts |
| **§2 Java 17** | ✅ PASS | sourceCompatibility = VERSION_17 |
| **§5.2 Nav without library** | ✅ PASS | sealed Screen enum + when() |
| **§5.3 Force RTL** | ✅ PASS | `LocalLayoutDirection.Rtl` wrapper |
| **§5.4 Persian fonts** | ⚠️ PARTIAL | Has Ziba font but single weight (faux Bold) |
| **§5.5 Artwork-backed UI** | ⚠️ PARTIAL | Has bg images, no standard art-button (§9.6) |
| **§5.6 Sounds** | ⚠️ PARTIAL | Has SoundManager (synthetic), no sounds/ pool mp3s |
| **§5.7 Persistence** | ✅ PASS | Room DB + SharedPreferences |
| **§5.10 Launcher icons** | ✅ PASS | Adaptive + mipmap fallbacks (circular reference fix applied 2026-08-22) |
| **§5.12 README.md** | ✅ PASS | Has comprehensive README |
| **§8.1 Strings in strings.xml** | ❌ FAIL | Most strings hardcoded in Kotlin |
| **§8.1 Persian digits helper** | ❌ FAIL | Has `toPersianDigits()` but not `Int.faDigits()` per §9.4 |
| **§8.2 Compatibility basics** | ✅ PASS | minSdk, enableEdgeToEdge, portrait, configChanges |
| **§8.3 fontScale cap** | ✅ PASS | `minOf(density.fontScale, 1.15f)` |
| **§8.3 Responsive helpers (§9.2)** | ❌ FAIL | No `rememberFontScale/screenScale()` |
| **§8.3 safeDrawingPadding()** | ❌ FAIL | Uses `.statusBarsPadding()` instead |
| **§8.6 Back nav** | ✅ PASS | BackHandler pattern |
| **§9.1 Text glow (highlight)** | ❌ FAIL | No glow pattern |
| **§9.2 Size multipliers** | ❌ FAIL | Not present |
| **§9.3 Multi-weight fonts** | ❌ FAIL | Single weight Ziba |
| **§9.4 faDigits()** | ❌ FAIL | Uses String extension not Int extension |
| **§9.5 Back nav** | ✅ PASS | Done |
| **§9.6 ArtButton pattern** | ❌ FAIL | Not used |

**Summary:** 12 ✅ PASS, 3 ⚠️ PARTIAL, 8 ❌ FAIL

Main gaps: §8.1 strings, §8.3 responsive utilities, §9 reusable recipes (glow, helpers, multi-font, art buttons).
---

## 6. 🔨 Build & Run

```bash
cd "/home/jaber/Downloads/games/حدول-مرحله-ای (2)"

# Debug build:
setsid nohup ./gradlew assembleDebug --console=plain --no-daemon > /tmp/build.log 2>&1 < /dev/null &
tail -5 /tmp/build.log

# Release build (uses salari.jks signing config):
setsid nohup ./gradlew assembleRelease --console=plain --no-daemon > /tmp/build_rel.log 2>&1 < /dev/null &
tail -5 /tmp/build_rel.log

# Install debug:
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.aistudio.jadvalian.qyuzwr/.MainActivity

# Install release (requires uninstall of debug first or use -r):
adb install -r app/build/outputs/apk/release/app-release.apk
adb shell am start -n com.aistudio.jadvalian.qyuzwr/.MainActivity
```

APK size: ~31 MB debug, ~17 MB release (2026-08-22 build: 17MB).