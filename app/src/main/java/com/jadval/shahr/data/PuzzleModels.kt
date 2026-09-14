package com.jadval.shahr.data

/**
 * نرمال‌سازی حروف فارسی — منبع واحد حقیقت برای:
 * ۱) مقایسهٔ جواب کاربر با سلول‌های گرید (PuzzleViewModel.normalizeCharForComparison)
 * ۲) تولید حروف کیبورد مجازی (PersianOnScreenKeyboard) تا هر حرفی که گرید لازم دارد
 *    حتماً روی کیبورد بیاید (مثلاً «ئ» در گرید → کلید «ی» روی کیبورد).
 *
 * نکته: «ء» و اعراب (ّ ً) و فاصله معادل نرمال ندارند و عیناً باید تایپ شوند —
 * این‌ها فقط وقتی داخل جواب باشند به‌عنوان کلید ظاهر می‌شوند.
 */
fun normalizePersianChar(char: Char): Char = when (char) {
    'آ', 'أ', 'إ' -> 'ا'
    'ي', 'ى', 'ئ' -> 'ی'
    'ك' -> 'ک'
    'ة' -> 'ه'
    'ؤ' -> 'و'
    else -> char
}

data class Clue(
    val number: Int,
    val answer: String,
    val clueText: String,
    val category: String,
    val direction: String, // "across" or "down"
    val startRow: Int,
    val startCol: Int,
    val length: Int
)

data class Puzzle(
    val id: String,
    val title: String,
    val difficulty: String, // "آسان" | "متوسط" | "سخت"
    val rows: Int = 10,
    val cols: Int = 10,
    val gridSolutions: List<String>,
    val acrossClues: List<Clue>,
    val downClues: List<Clue>,
    /** If true, across clue letters in gridSolutions go left-to-right (startCol = rightmost = last letter).
     *  Default false = RTL convention (startCol = rightmost = first letter, like easy puzzles). */
    val acrossReadsLeftToRight: Boolean = false
) {
    /** Returns the column index for the i-th character of a clue, respecting direction convention. */
    fun clueCol(clue: Clue, i: Int): Int {
        return if (clue.direction == "down") {
            clue.startCol                       // Down: column is always constant
        } else if (acrossReadsLeftToRight) {
            clue.startCol - clue.length + 1 + i // LTR across
        } else {
            clue.startCol - i                   // RTL across
        }
    }

    fun getCellNumbers(): List<Int> {
        val numbers = MutableList(rows * cols) { 0 }
        for (clue in acrossClues) {
            val index = clue.startRow * cols + clue.startCol
            if (index in numbers.indices) {
                numbers[index] = clue.number
            }
        }
        for (clue in downClues) {
            val index = clue.startRow * cols + clue.startCol
            if (index in numbers.indices) {
                numbers[index] = clue.number
            }
        }
        return numbers
    }
}

data class GameSection(
    val size: Int,
    val title: String,
    val difficulty: String,
    val rankName: String,
    val puzzles: List<Puzzle>
)

/**
 * Common interface for puzzle data sources (easy mode, hard mode, etc.)
 */
interface PuzzleDataSource {
    val puzzlesList: List<Puzzle>
    val sectionsList: List<GameSection>
    fun getPuzzlesByDifficulty(difficulty: String): List<Puzzle>
    fun getPuzzleById(id: String): Puzzle?
}

object PuzzleData : PuzzleDataSource {
    override val puzzlesList: List<Puzzle> by lazy {
        puzzles_batch_strict_easy_8x8 +
        puzzles_batch_strict_easy_10x10 +
        puzzles_batch_medium_12x12 +
        puzzles_batch_mix_14x14 +
        puzzles_batch_mix_16x16 +
        puzzles_batch_mix_18x18 +
        puzzles_batch_all_20x20 +
        puzzles_batch_all_22x22 +
        puzzles_batch_all_24x24 +
        puzzles_batch_all_26x26
    }

    override val sectionsList: List<GameSection> by lazy {
        val sizes = listOf(8, 10, 12, 14, 16, 18, 20, 22, 24, 26)
        sizes.map { size ->
            val sectionPuzzles = puzzlesList.filter { it.rows == size }
            val rankName = when (size) {
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
            val difficulty = when (size) {
                8, 10 -> "آسان"
                12, 14, 16, 18 -> "متوسط"
                else -> "سخت"
            }
            GameSection(
                size = size,
                title = "بخش ${size}×${size}",
                difficulty = difficulty,
                rankName = rankName,
                puzzles = sectionPuzzles
            )
        }
    }

    override fun getPuzzlesByDifficulty(difficulty: String): List<Puzzle> {
        return puzzlesList.filter { it.difficulty == difficulty }
    }

    override fun getPuzzleById(id: String): Puzzle? {
        return puzzlesList.find { it.id == id }
    }
}

/**
 * PuzzleDataHard — حالت "شروع سخت"
 * همان ساختار PuzzleData اما با پازل‌های متفاوت.
 * فعلاً خالی است و بعداً پازل‌ها اضافه خواهند شد.
 */
object PuzzleDataHard : PuzzleDataSource {
    override val puzzlesList: List<Puzzle> by lazy {
        hard_8x8+
        hard_10x10+
        hard_12x12+
        hard_14x14+
        hard_16x16+
        hard_18x18+
        hard_20x20
    }

    override val sectionsList: List<GameSection> by lazy {
        val sizes = listOf(8, 10, 12, 14, 16, 18, 20)
        sizes.map { size ->
            val sectionPuzzles = puzzlesList.filter { it.rows == size }
            val rankName = when (size) {
                8 -> "سرباز وظیفه"
                10 -> "سرباز حرفه ای"
                12 -> "سرگرد شهر جدول"
                14 -> "سرهنگ شهر جدول"
                16 -> "فرمانده شهر جدول"
                18 -> "ژنرال شهر جدول"
                20 -> "پادشاه شهر جدول"
                else -> "سرباز وظیفه"
            }
            val difficulty = when (size) {
                8, 10 -> "آسان"
                12, 14, 16, 18 -> "متوسط"
                else -> "سخت"
            }
            GameSection(
                size = size,
                title = "بخش ${size}×${size}",
                difficulty = difficulty,
                rankName = rankName,
                puzzles = sectionPuzzles
            )
        }
    }

    override fun getPuzzlesByDifficulty(difficulty: String): List<Puzzle> {
        return puzzlesList.filter { it.difficulty == difficulty }
    }

    override fun getPuzzleById(id: String): Puzzle? {
        return puzzlesList.find { it.id == id }
    }
}

/**
 * PuzzleDataBigGrid — حالت "شروع با جدول بزرگ"
 * جدول‌های بزرگ از 16x16 تا 26x26
 */
object PuzzleDataBigGrid : PuzzleDataSource {
    override val puzzlesList: List<Puzzle> by lazy {
        biggrid_16x16 +
        biggrid_18x18 +
        biggrid_20x20 +
        biggrid_22x22 +
        biggrid_24x24 +
        biggrid_26x26
    }

    override val sectionsList: List<GameSection> by lazy {
        val sizes = listOf(16, 18, 20, 22, 24, 26)
        sizes.map { size ->
            val sectionPuzzles = puzzlesList.filter { it.rows == size }
            val rankName = when (size) {
                16 -> "ستوان شهر جدول"
                18 -> "سروان شهر جدول"
                20 -> "سرگرد شهر جدول"
                22 -> "سپهبد شهر جدول"
                24 -> "ارتشبد شهر جدول"
                26 -> "فیلد مارشال شهر جدول"
                else -> "ستوان شهر جدول"
            }
            GameSection(
                size = size,
                title = "بخش ${size}×${size}",
                difficulty = "سخت",
                rankName = rankName,
                puzzles = sectionPuzzles
            )
        }
    }

    override fun getPuzzlesByDifficulty(difficulty: String): List<Puzzle> {
        return puzzlesList.filter { it.difficulty == difficulty }
    }

    override fun getPuzzleById(id: String): Puzzle? {
        return puzzlesList.find { it.id == id }
    }
}
