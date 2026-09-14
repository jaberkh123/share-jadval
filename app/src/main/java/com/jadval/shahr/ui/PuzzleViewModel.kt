package com.jadval.shahr.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jadval.shahr.data.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import org.json.JSONArray
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch




enum class Screen {
    HOME,
    GAME_MODE_SELECT,
    DIFFICULTY_SELECT,
    GAME,
    SETTINGS,
    HELP
}

class PuzzleViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = PuzzleRepository(application, db.puzzleDao())

    // Navigation State
    var currentScreen by mutableStateOf(Screen.HOME)
        private set

    private var previousScreen: Screen = Screen.HOME

    var selectedDifficulty by mutableStateOf("آسان")
        private set

    var selectedGameMode by mutableStateOf("easy") // "easy" or "hard" or "biggrid"
        private set

    /** Returns the PuzzleDataSource matching the currently selected game mode. */
    fun currentPuzzleData(): com.jadval.shahr.data.PuzzleDataSource {
        return when (selectedGameMode) {
            "hard" -> com.jadval.shahr.data.PuzzleDataHard
            "biggrid" -> com.jadval.shahr.data.PuzzleDataBigGrid
            else -> com.jadval.shahr.data.PuzzleData
        }
    }

    var selectedSectionSize by mutableStateOf<Int?>(null)

    // Game Preferences / Settings
    var isInstantCheckEnabled by mutableStateOf(false) // Hardcoded false (disabled instantly as per user request)
        private set
    var isSoundEnabled by mutableStateOf(repository.isSoundEnabled())
    val ad11: CustomAd?
        get() = AdManager.ad11

    val ad93: CustomAd?
        get() = AdManager.ad93

    val ad93_2: CustomAd?
        get() = AdManager.ad93_2

    val customAd: CustomAd?
        get() = AdManager.ad93_2

    val adApiResult: String
        get() = AdManager.adApiResult11

    fun fetchCustomAd() {
        AdManager.triggerManualRefresh(getApplication(), viewModelScope)
    }

    var isDarkModeEnabled by mutableStateOf(false)
        private set
    var clueFontSize by mutableStateOf(repository.getClueFontSize())
        private set

    private val sharedPrefs = application.getSharedPreferences("crossword_prefs", android.content.Context.MODE_PRIVATE)

    private var hasCheckedLastActive = false

    var globalBonusCoins by mutableStateOf(0)
        private set

    var adBonusCoins by mutableStateOf(0)
        private set

    var adCooldownSecondsLeft by mutableStateOf(0L)
        private set

    var launchCount by mutableStateOf(0)
        private set

    // DB Progress State
    private val _allProgress = MutableStateFlow<Map<String, PuzzleProgressEntity>>(emptyMap())
    val allProgress: StateFlow<Map<String, PuzzleProgressEntity>> = _allProgress.asStateFlow()

    // Active Game State
    var activePuzzle by mutableStateOf<Puzzle?>(null)
        private set
    var userGridInputs by mutableStateOf<List<Char>>(emptyList())
        private set
    var activeRow by mutableStateOf(-1)
        private set
    var activeCol by mutableStateOf(-1)
        private set
    var activeDirection by mutableStateOf("across") // "across" or "down"
        private set
    var timerSeconds by mutableStateOf(0L)
        private set
    var hintsUsed by mutableStateOf(0)
        private set
    var hintDeductions by mutableStateOf(0)
        private set
    var showCompletedDialog by mutableStateOf(false)
        private set
    var showIncorrectCompletionDialog by mutableStateOf(false)
        private set
    var manualCheckMode by mutableStateOf(false) // Triggered by clicking "Check Answer"

    var hintDialogTitle by mutableStateOf("")
    var hintDialogContent by mutableStateOf("")
    var showHintResultDialog by mutableStateOf(false)

    fun dismissHintResultDialog() {
        showHintResultDialog = false
    }

    var showOnboarding by mutableStateOf(false)
    var showDirectionPicker by mutableStateOf(false)

    var showRankUpDialog by mutableStateOf<String?>(null)

    var showRatingDialog by mutableStateOf(false)
        private set

    fun dismissRatingDialog() {
        showRatingDialog = false
        SoundManager.playClick()
    }

    fun rateApp(context: android.content.Context) {
        showRatingDialog = false
        SoundManager.playClick()
        val packageName = context.packageName
        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
            data = android.net.Uri.parse("bazaar://details?id=$packageName")
            setPackage("com.farsitel.bazaar")
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            try {
                val webIntent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                    data = android.net.Uri.parse("https://cafebazaar.ir/app/$packageName/?l=fa")
                }
                context.startActivity(webIntent)
            } catch (ex: Exception) {
                android.widget.Toast.makeText(context, "یافتن بازار مقدور نبود", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun checkRankUp(progressMap: Map<String, PuzzleProgressEntity>) {
        if (progressMap.isEmpty()) return
        val puzzles = currentPuzzleData().puzzlesList
        val firstUncompletedIndex = puzzles.indexOfFirst { puzzle ->
            !(progressMap[puzzle.id]?.isCompleted ?: false)
        }.let { if (it == -1) puzzles.size else it }
        val currentPuzzle = puzzles.getOrNull(firstUncompletedIndex) ?: puzzles.last()
        val currentRows = currentPuzzle.rows
        
        val rankName = getRankNameForRows(currentRows)
        
        val lastSavedRank = sharedPrefs.getString("last_saved_rank", null)
        if (lastSavedRank != null && lastSavedRank != rankName) {
            val oldOrder = getRankOrder(lastSavedRank)
            val newOrder = getRankOrder(rankName)
            if (newOrder > oldOrder) {
                SoundManager.playSuccess()
                showRankUpDialog = rankName
            }
        }
        sharedPrefs.edit().putString("last_saved_rank", rankName).apply()
    }

    private fun getRankNameForRows(rows: Int): String {
        return when (rows) {
            8 -> "سرباز وظیفه"
            10 -> "سرباز حرفه ای"
            12 -> "سرگرد شهر جدول"
            14 -> "سرهنگ شهر جدول"
            16 -> "فرمانده شهر جدول"
            18 -> "ژنرال شهر جدول"
            20 -> "پادشاه شهر جدول"
            22 -> "امپراطور شهر جدول"
            24 -> "خدای شهر جدول"
            26 -> "خدای شهر جدول"
            else -> "سرباز وظیفه"
        }
    }

    private fun getRankOrder(rank: String): Int {
        return when (rank) {
            "سرباز وظیفه" -> 1
            "سرباز حرفه ای" -> 2
            "سرگرد شهر جدول" -> 3
            "سرهنگ شهر جدول" -> 4
            "فرمانده شهر جدول" -> 5
            "ژنرال شهر جدول" -> 6
            "پادشاه شهر جدول" -> 7
            "امپراطور شهر جدول" -> 8
            "خدای شهر جدول" -> 9
            else -> 1
        }
    }

    // Score and Hint Audio/Visual feedback states
    var lastScoreForFeedback by mutableStateOf(-1)
    var scoreChangeAmount by mutableStateOf(0)
    var scoreChangeTriggerId by mutableStateOf(0)

    fun updateScoreFeedback() {
        val current = calculateScore()
        if (lastScoreForFeedback == -1) {
            lastScoreForFeedback = current
            return
        }
        val diff = current - lastScoreForFeedback
        if (diff != 0) {
            scoreChangeAmount = diff
            scoreChangeTriggerId++
            lastScoreForFeedback = current
            
            if (diff > 0) {
                SoundManager.playScoreGain()
            } else {
                SoundManager.playScoreLoss()
            }
        }
    }

    private var timerJob: Job? = null

    init {
        // Initialize sound manager status
        SoundManager.setSoundEnabled(isSoundEnabled)
        
        // Handle launch count tracking
        val count = sharedPrefs.getInt("launch_count", 0) + 1
        sharedPrefs.edit().putInt("launch_count", count).apply()
        launchCount = count
        
        if (!sharedPrefs.contains("global_bonus_coins")) {
            sharedPrefs.edit().putInt("global_bonus_coins", 200).apply()
            globalBonusCoins = 200
        } else {
            globalBonusCoins = sharedPrefs.getInt("global_bonus_coins", 0)
        }

        // Collect DB updates
        viewModelScope.launch {
            repository.allProgress.collectLatest { progressList ->
                val progressMap = progressList.associateBy { it.id }
                _allProgress.value = progressMap
                
                checkRankUp(progressMap)
                
                // Trigger auto resume check once on app launch when progress is loaded
                if (!hasCheckedLastActive) {
                    hasCheckedLastActive = true
                    checkAndAutoResumeActivePuzzle()
                }
            }
        }
        // Start background ad cooldown ticker
        viewModelScope.launch {
            while (true) {
                updateAdCooldownTimeLeft()
                delay(1000)
            }
        }
    }

    // Navigation triggers
    fun navigateTo(screen: Screen) {
        if (screen != Screen.HELP) {
            previousScreen = currentScreen
        }
        currentScreen = screen
        if (screen != Screen.GAME) {
            stopTimer()
        }
    }

    fun navigateBack() {
        currentScreen = previousScreen
        if (currentScreen == Screen.GAME && !isCompleted()) {
            startTimer()
        }
    }

    fun selectDifficulty(difficulty: String) {
        selectedDifficulty = difficulty
        navigateTo(Screen.DIFFICULTY_SELECT)
    }

    fun selectGameMode(mode: String) {
        selectedGameMode = mode
        selectedSectionSize = null
        navigateTo(Screen.DIFFICULTY_SELECT)
    }

    fun startPuzzle(puzzle: Puzzle) {
        activePuzzle = puzzle
        sharedPrefs.edit().putString("last_active_puzzle_id", puzzle.id).apply()
        manualCheckMode = false
        showCompletedDialog = false
        SoundManager.setSoundEnabled(isSoundEnabled)
        
        // Load progress from database if exists
        adBonusCoins = sharedPrefs.getInt("ad_bonus_coins_${puzzle.id}", 0)
        val progress = _allProgress.value[puzzle.id]
        val gridSize = puzzle.rows * puzzle.cols
        if (progress != null) {
            val loadedInputs = progress.userInput.map { it }
            userGridInputs = if (loadedInputs.size == gridSize) {
                loadedInputs
            } else {
                loadedInputs.take(gridSize) + List(maxOf(0, gridSize - loadedInputs.size)) { ' ' }
            }
            timerSeconds = progress.timeSpentSeconds
            hintsUsed = progress.hintsUsed
            hintDeductions = progress.hintsUsed
            if (progress.isCompleted) {
                showCompletedDialog = true
            }
        } else {
            // New game
            userGridInputs = List(gridSize) { ' ' }
            timerSeconds = 0
            hintsUsed = 0
            hintDeductions = 0
        }

        // Set initial selected cell to first non-block cell
        selectFirstAvailableCell(puzzle)
        
        // First-time onboarding trigger
        if (!repository.hasShownOnboarding()) {
            showOnboarding = true
        }

        // Track game screen entries for rating request (every 5 entries)
        val entries = sharedPrefs.getInt("game_screen_entry_count", 0) + 1
        sharedPrefs.edit().putInt("game_screen_entry_count", entries).apply()
        if (entries % 5 == 0) {
            showRatingDialog = true
        }

        navigateTo(Screen.GAME)
        
        // Reset feedback variables for the new session
        lastScoreForFeedback = calculateScore()
        scoreChangeAmount = 0
        scoreChangeTriggerId = 0

        if (!isCompleted()) {
            startTimer()
        }
    }

    private fun selectFirstAvailableCell(puzzle: Puzzle) {
        for (r in 0 until puzzle.rows) {
            for (c in (puzzle.cols - 1) downTo 0) { // Persian RTL preference, start from right
                val idx = r * puzzle.cols + c
                if (puzzle.gridSolutions[idx] != "■") {
                    activeRow = r
                    activeCol = c
                    activeDirection = "across"
                    return
                }
            }
        }
    }

    // Cell Selection & Interaction
    fun hasClueInDirection(row: Int, col: Int, direction: String): Boolean {
        val puzzle = activePuzzle ?: return false
        val clues = if (direction == "across") puzzle.acrossClues else puzzle.downClues
        return clues.any { clue ->
            if (direction == "across") {
                clue.startRow == row && col in (clue.startCol - clue.length + 1)..clue.startCol
            } else {
                clue.startCol == col && row in clue.startRow..(clue.startRow + clue.length - 1)
            }
        }
    }

    fun onCellClicked(row: Int, col: Int) {
        val puzzle = activePuzzle ?: return
        val idx = row * puzzle.cols + col
        if (puzzle.gridSolutions[idx] == "■") return // Block cell cannot be selected

        SoundManager.playCellSelect()

        val prevRow = activeRow
        val prevCol = activeCol

        val hasAcross = hasClueInDirection(row, col, "across")
        val hasDown = hasClueInDirection(row, col, "down")

        if (prevRow == row && prevCol == col) {
            // Tapping same cell toggles direction if both are available
            if (hasAcross && hasDown) {
                activeDirection = if (activeDirection == "across") "down" else "across"
            } else if (hasAcross) {
                activeDirection = "across"
            } else if (hasDown) {
                activeDirection = "down"
            }
        } else {
            activeRow = row
            activeCol = col

            // Smart selection of direction:
            // Prefer keeping current direction if it exists for the clicked cell.
            // Otherwise, switch to the one that exists.
            if (activeDirection == "across") {
                if (hasAcross) {
                    activeDirection = "across"
                } else if (hasDown) {
                    activeDirection = "down"
                }
            } else { // activeDirection == "down"
                if (hasDown) {
                    activeDirection = "down"
                } else if (hasAcross) {
                    activeDirection = "across"
                }
            }
        }
        manualCheckMode = false
        showDirectionPicker = true
    }

    fun selectDirection(direction: String) {
        activeDirection = direction
        showDirectionPicker = false
        SoundManager.playClick()
    }

    fun closeCompletedDialog() {
        showCompletedDialog = false
        SoundManager.playClick()
    }

    fun closeIncorrectCompletionDialog() {
        showIncorrectCompletionDialog = false
        SoundManager.playClick()
    }

    fun triggerCheckFromIncorrectDialog() {
        showIncorrectCompletionDialog = false
        triggerManualCheck()
    }

    fun normalizeCharForComparison(char: Char): Char {
        // delegate to the shared normalization (data package) so keyboard generation
        // and answer checking always agree (ئ→ی، ة→ه، ى→ی، آ→ا، ...).
        return normalizePersianChar(char)
    }

    fun isGridFullyFilled(): Boolean {
        val puzzle = activePuzzle ?: return false
        for (i in userGridInputs.indices) {
            val sol = puzzle.gridSolutions[i]
            if (sol != "■" && userGridInputs[i] == ' ') {
                return false
            }
        }
        return true
    }

    fun dismissOnboarding() {
        showOnboarding = false
        repository.setShownOnboarding(true)
        SoundManager.playClick()
    }

    // Helper to get active clue
    fun getActiveClue(): Clue? {
        val puzzle = activePuzzle ?: return null
        val r = activeRow
        val c = activeCol
        if (r == -1 || c == -1) return null

        val clues = if (activeDirection == "across") puzzle.acrossClues else puzzle.downClues
        return clues.find { clue ->
            if (activeDirection == "across") {
                clue.startRow == r && c in (clue.startCol - clue.length + 1)..clue.startCol
            } else {
                clue.startCol == c && r in clue.startRow..(clue.startRow + clue.length - 1)
            }
        }
    }

    // Helper to check if a cell is highlighted as part of the active word
    fun isCellHighlighted(row: Int, col: Int): Boolean {
        val clue = getActiveClue() ?: return false
        return if (activeDirection == "across") {
            clue.startRow == row && col in (clue.startCol - clue.length + 1)..clue.startCol
        } else {
            clue.startCol == col && row in clue.startRow..(clue.startRow + clue.length - 1)
        }
    }

    // Keyboard Inputs
    fun onKeyPressed(char: Char) {
        val puzzle = activePuzzle ?: return
        if (activeRow == -1 || activeCol == -1 || isCompleted()) return

        SoundManager.playType()
        showDirectionPicker = false // Hide direction picker on interaction

        // Normalize Persian (same rule as answer comparison — e.g. «ی» typed for «ئ» in the grid)
        val normalized = normalizeCharForComparison(char)

        // Place char
        val idx = activeRow * puzzle.cols + activeCol
        val newList = userGridInputs.toMutableList()
        newList[idx] = normalized
        userGridInputs = newList

        manualCheckMode = false
        saveCurrentProgress()

        // Check if game is completed
        if (checkAndHandleCompletion()) {
            return
        }

        // Move to the next cell in the active word
        moveToNextCell()
    }

    fun onBackspacePressed() {
        val puzzle = activePuzzle ?: return
        if (activeRow == -1 || activeCol == -1 || isCompleted()) return

        SoundManager.playDelete()
        showDirectionPicker = false // Hide direction picker on interaction

        val idx = activeRow * puzzle.cols + activeCol
        val newList = userGridInputs.toMutableList()

        if (userGridInputs[idx] != ' ') {
            // Just clear current cell if it has input
            newList[idx] = ' '
            userGridInputs = newList
        } else {
            // Move back and clear previous cell
            moveToPreviousCell()
            val newIdx = activeRow * puzzle.cols + activeCol
            if (newIdx in newList.indices && puzzle.gridSolutions[newIdx] != "■") {
                newList[newIdx] = ' '
                userGridInputs = newList
            }
        }

        manualCheckMode = false
        saveCurrentProgress()
    }

    private fun moveToNextCell() {
        val puzzle = activePuzzle ?: return
        if (activeDirection == "across") {
            if (puzzle.acrossReadsLeftToRight) {
                // LTR across: first letter is at startCol-length+1 (leftmost), next goes RIGHT
                val nextCol = activeCol + 1
                if (nextCol < puzzle.cols) {
                    val idx = activeRow * puzzle.cols + nextCol
                    if (puzzle.gridSolutions[idx] != "■") {
                        activeCol = nextCol
                    }
                }
            } else {
                // RTL across: first letter is at startCol (rightmost), next goes LEFT
                val nextCol = activeCol - 1
                if (nextCol >= 0) {
                    val idx = activeRow * puzzle.cols + nextCol
                    if (puzzle.gridSolutions[idx] != "■") {
                        activeCol = nextCol
                    }
                }
            }
        } else {
            // down goes top to bottom (rows increase)
            val nextRow = activeRow + 1
            if (nextRow < puzzle.rows) {
                val idx = nextRow * puzzle.cols + activeCol
                if (puzzle.gridSolutions[idx] != "■") {
                    activeRow = nextRow
                }
            }
        }
    }

    private fun moveToPreviousCell() {
        val puzzle = activePuzzle ?: return
        if (activeDirection == "across") {
            if (puzzle.acrossReadsLeftToRight) {
                // LTR across: previous goes LEFT
                val prevCol = activeCol - 1
                if (prevCol >= 0) {
                    val idx = activeRow * puzzle.cols + prevCol
                    if (puzzle.gridSolutions[idx] != "■") {
                        activeCol = prevCol
                    }
                }
            } else {
                // RTL across: previous goes RIGHT
                val prevCol = activeCol + 1
                if (prevCol < puzzle.cols) {
                    val idx = activeRow * puzzle.cols + prevCol
                    if (puzzle.gridSolutions[idx] != "■") {
                        activeCol = prevCol
                    }
                }
            }
        } else {
            // down previous is bottom to top (rows decrease)
            val prevRow = activeRow - 1
            if (prevRow >= 0) {
                val idx = prevRow * puzzle.cols + activeCol
                if (puzzle.gridSolutions[idx] != "■") {
                    activeRow = prevRow
                }
            }
        }
    }

    // Actions
    fun triggerManualCheck() {
        SoundManager.playCheck()
        manualCheckMode = true
    }

    fun useHintRevealLetter() {
        val puzzle = activePuzzle ?: return
        if (activeRow == -1 || activeCol == -1 || isCompleted()) return

        val idx = activeRow * puzzle.cols + activeCol
        val sol = puzzle.gridSolutions[idx]
        if (sol == "■") return

        val isAlreadyCorrect = normalizeCharForComparison(userGridInputs[idx]) == normalizeCharForComparison(sol[0])
        
        if (isAlreadyCorrect) {
            hintDialogTitle = "حرف خانه انتخاب شده"
            hintDialogContent = "حرف این خانه «${sol[0]}» است (این خانه قبلاً به درستی وارد شده است)."
            showHintResultDialog = true
            return
        }

        // Check if score is enough
        val currentCoins = getTotalCoins()
        if (currentCoins < 30) {
            hintDialogTitle = "سکه ناکافی"
            hintDialogContent = "برای آشکار شدن این حرف به 30 سکه نیاز دارید.\nسکه‌های فعلی شما: $currentCoins\n\nمی‌توانید با تماشای تبلیغ از دکمه + بالای صفحه، 100 سکه رایگان دریافت کنید."
            showHintResultDialog = true
            return
        }

        val newList = userGridInputs.toMutableList()
        newList[idx] = sol[0]
        userGridInputs = newList
        hintDeductions += 30
        hintsUsed = hintDeductions
        saveCurrentProgress()
        checkAndHandleCompletion()

        hintDialogTitle = "حرف خانه آشکار شد"
        hintDialogContent = "حرف این خانه «${sol[0]}» است."
        showHintResultDialog = true
    }

    fun useHintClearWrong() {
        val puzzle = activePuzzle ?: return
        if (isCompleted()) return

        val currentCoins = getTotalCoins()
        if (currentCoins < 30) {
            hintDialogTitle = "سکه ناکافی"
            hintDialogContent = "برای پاک کردن حروف اشتباه به 30 سکه نیاز دارید.\nسکه‌های فعلی شما: $currentCoins\n\nمی‌توانید با تماشای تبلیغ از دکمه + بالای صفحه، 100 سکه رایگان دریافت کنید."
            showHintResultDialog = true
            return
        }

        var clearedAny = false
        val newList = userGridInputs.toMutableList()
        for (i in userGridInputs.indices) {
            val sol = puzzle.gridSolutions[i]
            if (sol != "■" && userGridInputs[i] != ' ' && normalizeCharForComparison(userGridInputs[i]) != normalizeCharForComparison(sol[0])) {
                newList[i] = ' '
                clearedAny = true
            }
        }
        if (clearedAny) {
            userGridInputs = newList
            hintDeductions += 30
            hintsUsed = hintDeductions
            saveCurrentProgress()
            hintDialogTitle = "پاک‌سازی انجام شد"
            hintDialogContent = "تمام حروف اشتباه شما از جدول پاک شدند."
            showHintResultDialog = true
        } else {
            hintDialogTitle = "پاک‌سازی جدول"
            hintDialogContent = "هیچ حرف اشتباهی در جدول وجود ندارد."
            showHintResultDialog = true
        }
    }

    fun useHintRevealWord() {
        val puzzle = activePuzzle ?: return
        val clue = getActiveClue() ?: return
        if (isCompleted()) return

        // Check if any character needs to be filled
        var anyNeedsReveal = false
        val wordBuilder = StringBuilder()
        for (i in 0 until clue.length) {
            val r = if (activeDirection == "across") clue.startRow else clue.startRow + i
            val c = puzzle.clueCol(clue, i)
            val idx = r * puzzle.cols + c
            val sol = puzzle.gridSolutions[idx]
            wordBuilder.append(sol[0])
            if (normalizeCharForComparison(userGridInputs[idx]) != normalizeCharForComparison(sol[0])) {
                anyNeedsReveal = true
            }
        }
        val correctWord = wordBuilder.toString()

        if (!anyNeedsReveal) {
            hintDialogTitle = "کلمه انتخاب شده"
            hintDialogContent = "کلمه این سرنخ «$correctWord» است (این کلمه قبلاً به درستی حل شده است)."
            showHintResultDialog = true
            return
        }

        // Check if score is enough
        val currentCoins = getTotalCoins()
        if (currentCoins < 100) {
            hintDialogTitle = "سکه ناکافی"
            hintDialogContent = "برای آشکار شدن این کلمه به 100 سکه نیاز دارید.\nسکه‌های فعلی شما: $currentCoins\n\nمی‌توانید با تماشای تبلیغ از دکمه + بالای صفحه، 100 سکه رایگان دریافت کنید."
            showHintResultDialog = true
            return
        }

        val newList = userGridInputs.toMutableList()
        for (i in 0 until clue.length) {
            val r = if (activeDirection == "across") clue.startRow else clue.startRow + i
            val c = puzzle.clueCol(clue, i)
            val idx = r * puzzle.cols + c
            val sol = puzzle.gridSolutions[idx]
            newList[idx] = sol[0]
        }
        userGridInputs = newList
        hintDeductions += 100
        hintsUsed = hintDeductions
        saveCurrentProgress()
        checkAndHandleCompletion()

        hintDialogTitle = "کلمه آشکار شد"
        hintDialogContent = "کلمه کامل این سرنخ:\n«$correctWord»"
        showHintResultDialog = true
    }

    fun revealActiveCellFree() {
        val puzzle = activePuzzle ?: return
        if (activeRow == -1 || activeCol == -1 || isCompleted()) return

        val idx = activeRow * puzzle.cols + activeCol
        val sol = puzzle.gridSolutions[idx]
        if (sol == "■") return

        val isAlreadyCorrect = normalizeCharForComparison(userGridInputs[idx]) == normalizeCharForComparison(sol[0])
        
        if (isAlreadyCorrect) {
            hintDialogTitle = "حرف خانه انتخاب شده"
            hintDialogContent = "حرف این خانه «${sol[0]}» است (این خانه قبلاً به درستی وارد شده است)."
            showHintResultDialog = true
            return
        }

        val newList = userGridInputs.toMutableList()
        newList[idx] = sol[0]
        userGridInputs = newList
        saveCurrentProgress()
        checkAndHandleCompletion()

        hintDialogTitle = "حرف آشکار شده با تبلیغ"
        hintDialogContent = "حرف این خانه «${sol[0]}» است."
        showHintResultDialog = true
    }

    fun showRewardedAd(context: android.content.Context, onAdNotLoaded: () -> Unit) {
        val placementId = com.jadval.shahr.MainActivity.ADIVERY_REWARD_PLACEMENT
        if (com.adivery.sdk.Adivery.isLoaded(placementId)) {
            com.jadval.shahr.MainActivity.isShowingFullscreenAd = true
            com.adivery.sdk.Adivery.showAd(placementId)
        } else {
            com.adivery.sdk.Adivery.prepareRewardedAd(context, placementId)
            onAdNotLoaded()
        }
    }

    fun showInterstitialAd(context: android.content.Context, onAdNotLoaded: () -> Unit) {
        val placementId = com.jadval.shahr.MainActivity.ADIVERY_INTERSTITIAL_PLACEMENT
        if (com.adivery.sdk.Adivery.isLoaded(placementId)) {
            com.jadval.shahr.MainActivity.isShowingFullscreenAd = true
            
            // Award coins immediately as requested
            val rewarded = onInterstitialAdWatched()
            if (rewarded) {
                android.widget.Toast.makeText(context, "100 سکه رایگان به حساب شما اضافه شد!", android.widget.Toast.LENGTH_LONG).show()
            }
            
            com.adivery.sdk.Adivery.showAd(placementId)
        } else {
            com.adivery.sdk.Adivery.prepareInterstitialAd(context, placementId)
            onAdNotLoaded()
        }
    }

    fun countCorrectClues(): Int {
        val puzzle = activePuzzle ?: return 0
        val allClues = puzzle.acrossClues + puzzle.downClues
        var count = 0
        for (clue in allClues) {
            var isClueCorrect = true
            for (i in 0 until clue.length) {
                val r = if (clue.direction == "across") clue.startRow else clue.startRow + i
                val c = puzzle.clueCol(clue, i)
                val idx = r * puzzle.cols + c
                val userChar = userGridInputs.getOrNull(idx) ?: ' '
                val solutionChar = puzzle.gridSolutions.getOrNull(idx) ?: "■"
                if (userChar == ' ' || normalizeCharForComparison(userChar) != normalizeCharForComparison(solutionChar[0])) {
                    isClueCorrect = false
                    break
                }
            }
            if (isClueCorrect) {
                count++
            }
        }
        return count
    }

    // Calculations & Saves
    fun calculateScore(): Int {
        val baseScore = 0
        val correctCluesCount = countCorrectClues()
        val clueScore = correctCluesCount * 5
        
        val isComp = isCompleted()
        val completionScore = if (isComp) {
            val difficulty = activePuzzle?.difficulty ?: ""
            when (difficulty) {
                "آسان" -> 50
                "متوسط" -> 100
                "سخت" -> 200
                else -> 0
            }
        } else {
            0
        }
        
        return baseScore + clueScore + completionScore + adBonusCoins - hintDeductions
    }

    private fun isCompleted(): Boolean {
        val puzzle = activePuzzle ?: return false
        for (i in userGridInputs.indices) {
            val sol = puzzle.gridSolutions[i]
            if (sol != "■" && (userGridInputs[i] == ' ' || normalizeCharForComparison(userGridInputs[i]) != normalizeCharForComparison(sol[0]))) {
                return false
            }
        }
        return true
    }

    private fun checkAndHandleCompletion(): Boolean {
        if (isCompleted()) {
            stopTimer()
            SoundManager.playSuccess()
            
            // Award 100 points if completed for the first time
            val puzzle = activePuzzle
            if (puzzle != null) {
                val wasCompletedBefore = _allProgress.value[puzzle.id]?.isCompleted == true
                if (!wasCompletedBefore) {
                    awardGlobalBonusCoins()
                }
            }
            
            showCompletedDialog = true
            saveCurrentProgress(completed = true)
            return true
        } else if (isGridFullyFilled()) {
            showIncorrectCompletionDialog = true
            return false
        }
        return false
    }

    fun saveCurrentProgress(completed: Boolean = false) {
        val puzzle = activePuzzle ?: return
        val isComp = completed || isCompleted()
        if (isComp) {
            sharedPrefs.edit().remove("last_active_puzzle_id").apply()
        }
        val currentScore = calculateScore()
        val gridStr = userGridInputs.joinToString("")

        // Check for score change and play sound/trigger visual feedback
        updateScoreFeedback()

        viewModelScope.launch {
            repository.saveProgress(
                puzzleId = puzzle.id,
                userInput = gridStr,
                isCompleted = isComp,
                timeSpentSeconds = timerSeconds,
                score = currentScore,
                hintsUsed = hintsUsed
            )
        }
    }

    // Timer management
    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                timerSeconds++
                // Update cooldown
                updateAdCooldownTimeLeft()
                // Save progress every 15 seconds as a safety
                if (timerSeconds % 15 == 0L) {
                    saveCurrentProgress()
                }
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    // Settings
    fun toggleInstantCheck() {
        isInstantCheckEnabled = !isInstantCheckEnabled
        repository.setInstantCheckEnabled(isInstantCheckEnabled)
    }

    fun toggleSound() {
        isSoundEnabled = !isSoundEnabled
        repository.setSoundEnabled(isSoundEnabled)
        SoundManager.setSoundEnabled(isSoundEnabled)
        if (isSoundEnabled) {
            SoundManager.playClick()
        }
    }

    fun toggleDarkMode() {
        isDarkModeEnabled = !isDarkModeEnabled
        repository.setDarkModeEnabled(isDarkModeEnabled)
    }

    fun updateClueFontSize(increase: Boolean) {
        val newSize = if (increase) (clueFontSize + 2f).coerceAtMost(24f) else (clueFontSize - 2f).coerceAtLeast(12f)
        clueFontSize = newSize
        repository.setClueFontSize(newSize)
    }

    fun resetAllProgress() {
        viewModelScope.launch {
            repository.clearAllProgress()
            
            // Clear puzzle-specific progress and coin preferences
            val editor = sharedPrefs.edit()
            editor.remove("last_active_puzzle_id")
            editor.putInt("global_bonus_coins", 200)
            globalBonusCoins = 200
            
            // Remove any "ad_bonus_coins_" preferences
            sharedPrefs.all.keys.filter { it.startsWith("ad_bonus_coins_") }.forEach { key ->
                editor.remove(key)
            }
            editor.apply()
            
            activePuzzle = null
            userGridInputs = emptyList()
            timerSeconds = 0
            hintsUsed = 0
            hintDeductions = 0
            showCompletedDialog = false
            navigateTo(Screen.HOME)
        }
    }

    fun handleBackPress(): Boolean {
        SoundManager.playClick()
        return when (currentScreen) {
            Screen.HOME -> false // System default handles exiting the app
            Screen.GAME_MODE_SELECT -> {
                navigateTo(Screen.HOME)
                true
            }
            Screen.DIFFICULTY_SELECT -> {
                if (selectedSectionSize != null) {
                    selectedSectionSize = null
                } else {
                    navigateTo(Screen.GAME_MODE_SELECT)
                }
                true
            }
            Screen.GAME -> {
                selectedSectionSize = activePuzzle?.rows
                navigateTo(Screen.DIFFICULTY_SELECT)
                true
            }
            Screen.SETTINGS -> {
                navigateTo(Screen.HOME)
                true
            }
            Screen.HELP -> {
                navigateBack()
                true
            }
        }
    }

    fun awardGlobalBonusCoins() {
        globalBonusCoins += 100
        sharedPrefs.edit().putInt("global_bonus_coins", globalBonusCoins).apply()
    }

    fun getTotalCoins(): Int {
        val savedSum = _allProgress.value.values.filter { it.id != activePuzzle?.id }.sumOf { it.score }
        val activeScore = if (activePuzzle != null) calculateScore() else 0
        return savedSum + activeScore + globalBonusCoins
    }

    fun getTotalCoinsEarned(): Int {
        val savedSum = _allProgress.value.values.filter { it.id != activePuzzle?.id }.sumOf { it.score + it.hintsUsed }
        val activeSum = if (activePuzzle != null) {
            calculateScore() + hintDeductions
        } else {
            0
        }
        return savedSum + activeSum + globalBonusCoins
    }

    fun awardAdBonusCoins() {
        val puzzle = activePuzzle ?: return
        adBonusCoins += 100
        sharedPrefs.edit().putInt("ad_bonus_coins_${puzzle.id}", adBonusCoins).apply()
        saveCurrentProgress() // Save progress to DB with updated score
    }

    fun startAdCooldown() {
        val now = System.currentTimeMillis()
        sharedPrefs.edit().putLong("last_ad_watch_time", now).apply()
        updateAdCooldownTimeLeft()
    }

    fun updateAdCooldownTimeLeft() {
        val lastWatchTime = sharedPrefs.getLong("last_ad_watch_time", 0L)
        if (lastWatchTime == 0L) {
            adCooldownSecondsLeft = 0L
            return
        }
        val elapsedMs = System.currentTimeMillis() - lastWatchTime
        val cooldownMs = 5 * 60 * 1000L // 5 minutes
        val remainingMs = cooldownMs - elapsedMs
        adCooldownSecondsLeft = if (remainingMs > 0L) remainingMs / 1000L else 0L
    }

    fun canWatchAdForCoins(): Boolean {
        return adCooldownSecondsLeft <= 0L
    }

    fun onInterstitialAdWatched(): Boolean {
        if (canWatchAdForCoins()) {
            if (activePuzzle != null) {
                awardAdBonusCoins()
            } else {
                awardGlobalBonusCoins()
            }
            startAdCooldown()
            return true
        }
        return false
    }

    private fun checkAndAutoResumeActivePuzzle() {
        val lastActiveId = sharedPrefs.getString("last_active_puzzle_id", null) ?: return
        // Search easy, hard, and biggrid puzzle sources
        val easyPuzzle = PuzzleData.getPuzzleById(lastActiveId)
        val hardPuzzle = PuzzleDataHard.getPuzzleById(lastActiveId)
        val biggridPuzzle = PuzzleDataBigGrid.getPuzzleById(lastActiveId)
        val puzzle = easyPuzzle ?: hardPuzzle ?: biggridPuzzle ?: return

        // Set game mode based on which source has the puzzle
        selectedGameMode = when {
            biggridPuzzle != null && easyPuzzle == null && hardPuzzle == null -> "biggrid"
            hardPuzzle != null && easyPuzzle == null -> "hard"
            else -> "easy"
        }

        // Check if the puzzle is already completed
        val progress = _allProgress.value[lastActiveId] ?: return // No progress exists, do not auto-resume
        val isCompleted = progress.isCompleted

        if (!isCompleted) {
            // It's a half-finished puzzle! Let's resume it immediately on app launch
            startPuzzle(puzzle)
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopTimer()
    }
}
