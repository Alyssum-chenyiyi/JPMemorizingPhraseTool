package com.example.memophrasestool

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import kotlinx.coroutines.launch

data class QuizState(
    val books: List<PhraseBook> = emptyList(),
    val currentBook: PhraseBook? = null,
    val phrases: List<Phrase> = emptyList(),
    val current: Phrase? = null,

    val screen: String = "home",

    val phase: Int = 0,
    val showAnswer: Boolean = false,
    val stage2Mask: String = "",
    val stage2Expected: String = "",
    val feedback: String? = null,
    val finished: Boolean = false,

    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
class PhraseViewModel : ViewModel() {

    private val _state = MutableStateFlow(QuizState())
    val state: StateFlow<QuizState> = _state

    private val stage1Queue = mutableListOf<Phrase>()
    private val stage2Queue = mutableListOf<Phrase>()
    private val stage3Queue = mutableListOf<Phrase>()

    fun importFromText(text: String) {
        val parsed = text
            .lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .mapIndexedNotNull { index, line ->
                val cleanLine = line
                    .replace(Regex("^\\d+[.、]\\s*"), "")
                    .trim()

                val parts = cleanLine.split(Regex("\\s+"), limit = 2)

                if (parts.size < 2) {
                    null
                } else {
                    Phrase(
                        id = System.currentTimeMillis() + index,
                        chinese = parts[0].trim(),
                        english = parts[1].trim()
                    )
                }
            }

        if (parsed.isEmpty()) {
            _state.value = _state.value.copy(
                screen = "import",
                phrases = emptyList(),
                errorMessage = "普通导入失败：请按“中文 英文”的格式输入",
                isLoading = false
            )
            return
        }

        _state.value = _state.value.copy(
            screen = "import",
            phrases = parsed,
            phase = 0,
            isLoading = false,
            errorMessage = "普通导入成功，已识别 ${parsed.size} 个词组"
        )
    }

    fun startQuiz() {
        val list = _state.value.phrases
        if (list.isEmpty()) return

        stage1Queue.clear()
        stage2Queue.clear()
        stage3Queue.clear()

        stage1Queue.addAll(list.shuffled())
        nextQuestion()
    }

    private fun nextQuestion() {
        when {
            stage1Queue.isNotEmpty() -> {
                val item = stage1Queue.removeAt(0)
                _state.value = _state.value.copy(
                    current = item,
                    phase = 1,
                    showAnswer = false,
                    feedback = null
                )
            }

            stage2Queue.isNotEmpty() -> {
                val item = stage2Queue.removeAt(0)
                val prompt = makeStage2Prompt(item.english)

                if (prompt == null) {
                    stage3Queue.add(item)
                    nextQuestion()
                    return
                }

                _state.value = _state.value.copy(
                    current = item,
                    phase = 2,
                    showAnswer = false,
                    feedback = null,
                    stage2Mask = prompt.first,
                    stage2Expected = prompt.second
                )
            }

            stage3Queue.isNotEmpty() -> {
                val item = stage3Queue.removeAt(0)

                _state.value = _state.value.copy(
                    current = item,
                    phase = 3,
                    showAnswer = false,
                    feedback = null
                )
            }

            else -> {
                _state.value = _state.value.copy(
                    current = null,
                    phase = 4,
                    finished = true,
                    feedback = null
                )
            }
        }
    }

    fun showAnswer() {
        _state.value = _state.value.copy(showAnswer = true)
    }

    fun remembered() {
        val item = _state.value.current ?: return
        stage2Queue.add(item)
        nextQuestion()
    }

    fun forgot() {
        val item = _state.value.current ?: return
        val updated = item.copy(stage1Forgot = item.stage1Forgot + 1)

        updatePhrase(updated)

        stage1Queue.add(updated)
        stage1Queue.shuffle()
        nextQuestion()
    }

    fun submitStage2(answer: String) {
        val item = _state.value.current ?: return
        val expected = _state.value.stage2Expected

        if (normalize(answer) == normalize(expected)) {
            stage3Queue.add(item)
            _state.value = _state.value.copy(
                feedback = "回答正确，进入第三关。"
            )
        } else {
            val updated = item.copy(stage2Wrong = item.stage2Wrong + 1)
            updatePhrase(updated)

            stage2Queue.add(updated)
            stage2Queue.shuffle()

            _state.value = _state.value.copy(
                feedback = "回答错误。正确答案：$expected"
            )
        }
    }

    fun submitStage3(answer: String) {
        val item = _state.value.current ?: return

        if (normalize(answer) == normalize(item.english)) {
            val updated = item.copy(passed = true)
            updatePhrase(updated)

            _state.value = _state.value.copy(
                feedback = "拼写正确，这个词组已通过。"
            )
        } else {
            val updated = item.copy(stage3Wrong = item.stage3Wrong + 1)
            updatePhrase(updated)

            stage3Queue.add(updated)
            stage3Queue.shuffle()

            _state.value = _state.value.copy(
                feedback = "拼写错误。正确答案：${item.english}"
            )
        }
    }

    fun nextAfterFeedback() {
        nextQuestion()
    }

    fun reset() {
        stage1Queue.clear()
        stage2Queue.clear()
        stage3Queue.clear()
        _state.value = QuizState()
    }

    private fun updatePhrase(updated: Phrase) {
        _state.value = _state.value.copy(
            phrases = _state.value.phrases.map {
                if (it.id == updated.id) updated else it
            },
            current = updated
        )
    }

    private fun normalize(text: String): String {
        return text.trim().lowercase().replace(Regex("\\s+"), " ")
    }

    fun aiImportFromText(text: String, apiKey: String) {
        if (text.isBlank()) {
            _state.value = _state.value.copy(errorMessage = "请先输入词组内容")
            return
        }

        if (apiKey.isBlank()) {
            _state.value = _state.value.copy(errorMessage = "请先输入 DeepSeek API Key")
            return
        }

        viewModelScope.launch {
            try {
                _state.value = _state.value.copy(
                    isLoading = true,
                    errorMessage = null
                )

                val prompt = """
                请把下面的中英文词组列表解析成 JSON。
                
                要求：
                1. 只返回 JSON，不要解释。
                2. JSON 格式必须是：
                {
                  "phrases": [
                    {
                      "chinese": "中文释义",
                      "english": "英文词组"
                    }
                  ]
                }
                3. english 必须只包含英文词组，不要包含中文。
                4. chinese 必须只包含中文释义。
                
                待解析内容：
                $text
            """.trimIndent()

                val response = DeepSeekClient.api.parsePhrases(
                    authorization = "Bearer $apiKey",
                    request = DeepSeekRequest(
                        messages = listOf(
                            DeepSeekMessage(
                                role = "system",
                                content = "你是一个英语词组整理助手，必须严格返回 JSON。"
                            ),
                            DeepSeekMessage(
                                role = "user",
                                content = prompt
                            )
                        )
                    )
                )

                val content = response.choices.firstOrNull()
                    ?.message
                    ?.content
                    ?: throw Exception("DeepSeek 没有返回内容")

                val result = Gson().fromJson(content, AiPhraseResult::class.java)

                val parsed = result.phrases.mapIndexed { index, item ->
                    Phrase(
                        id = index.toLong() + 1,
                        chinese = item.chinese.trim(),
                        english = item.english.trim()
                    )
                }.filter {
                    it.chinese.isNotBlank() && it.english.isNotBlank()
                }

                _state.value = _state.value.copy(
                    screen = "import",
                    phrases = parsed,
                    phase = 0,
                    isLoading = false,
                    errorMessage = "AI 已识别 ${parsed.size} 个词组"
                )

            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    errorMessage = "AI 导入失败：${e.message}"
                )
            }
        }
    }
    fun loadBooks(context: android.content.Context) {
        val books = PhraseBookStorage.loadBooks(context)
        _state.value = _state.value.copy(
            books = books,
        )
    }

    fun goHome() {
        stage1Queue.clear()
        stage2Queue.clear()
        stage3Queue.clear()

        _state.value = _state.value.copy(
            screen = "home",
            phase = 0,
            current = null,
            showAnswer = false,
            feedback = null,
            finished = false
        )
    }

    fun goImport() {
        _state.value = _state.value.copy(
            screen = "import",
            phase = 0,
            current = null,
            finished = false,
            errorMessage = null
        )
    }

    fun goBookList() {
        _state.value = _state.value.copy(
            screen = "bookList",
            phase = 0,
            current = null,
            finished = false
        )
    }

    fun saveNewBook(
        context: android.content.Context,
        name: String,
        phrases: List<Phrase>
    ) {
        if (name.isBlank()) {
            _state.value = _state.value.copy(errorMessage = "请先输入词组库名称")
            return
        }

        if (phrases.isEmpty()) {
            _state.value = _state.value.copy(errorMessage = "没有可保存的词组")
            return
        }

        val oldBooks = PhraseBookStorage.loadBooks(context)

        val newBook = PhraseBook(
            id = System.currentTimeMillis(),
            name = name.trim(),
            phrases = phrases
        )

        val newBooks = oldBooks + newBook
        PhraseBookStorage.saveBooks(context, newBooks)

        _state.value = _state.value.copy(
            books = newBooks,
            screen = "home",
            errorMessage = "已保存词组库：${newBook.name}"
        )
    }

    fun selectBook(book: PhraseBook) {
        _state.value = _state.value.copy(
            currentBook = book,
            phrases = book.phrases,
            screen = "bookDetail",
            phase = 0,
            finished = false,
            current = null
        )
    }

    fun startSelectedBook() {
        val book = _state.value.currentBook ?: return

        if (book.phrases.isEmpty()) {
            _state.value = _state.value.copy(
                errorMessage = "这个词组库没有词组"
            )
            return
        }

        stage1Queue.clear()
        stage2Queue.clear()
        stage3Queue.clear()

        stage1Queue.addAll(book.phrases.shuffled())

        _state.value = _state.value.copy(
            phrases = book.phrases,
            screen = "quiz",
            phase = 0,
            current = null,
            showAnswer = false,
            feedback = null,
            finished = false
        )

        nextQuestion()
    }

    fun deleteBook(context: android.content.Context, bookId: Long) {
        val newBooks = _state.value.books.filter { it.id != bookId }
        PhraseBookStorage.saveBooks(context, newBooks)

        _state.value = _state.value.copy(
            books = newBooks,
            screen = "bookList",
            currentBook = null
        )
    }

    fun addPhraseToCurrentBook(
        context: android.content.Context,
        chinese: String,
        english: String
    ) {
        val book = _state.value.currentBook ?: return

        if (chinese.isBlank() || english.isBlank()) return

        val newPhrase = Phrase(
            id = System.currentTimeMillis(),
            chinese = chinese.trim(),
            english = english.trim()
        )

        val updatedBook = book.copy(
            phrases = book.phrases + newPhrase
        )

        updateBook(context, updatedBook)
    }

    fun deletePhraseFromCurrentBook(
        context: android.content.Context,
        phraseId: Long
    ) {
        val book = _state.value.currentBook ?: return

        val updatedBook = book.copy(
            phrases = book.phrases.filter { it.id != phraseId }
        )

        updateBook(context, updatedBook)
    }
    fun goApiKey() {
        _state.value = _state.value.copy(
            screen = "apiKey",
            errorMessage = null
        )
    }

    private fun updateBook(
        context: android.content.Context,
        updatedBook: PhraseBook
    ) {
        val newBooks = _state.value.books.map {
            if (it.id == updatedBook.id) updatedBook else it
        }

        PhraseBookStorage.saveBooks(context, newBooks)

        _state.value = _state.value.copy(
            books = newBooks,
            currentBook = updatedBook,
            phrases = updatedBook.phrases
        )
    }
    private fun makeStage2Prompt(english: String): Pair<String, String>? {
        val tokens = english.trim().split(Regex("\\s+"))
        if (tokens.isEmpty()) return null

        val protected = setOf(
            "sth", "sb", "one", "one's", "oneself",
            "someone", "somebody", "something",
            "a", "the", "be", "am", "is", "are"
        )

        val eligible = tokens.mapIndexedNotNull { index, token ->
            val check = token.lowercase()
                .replace(Regex("^[^a-z]+|[^a-z]+$"), "")

            if (check.isNotBlank() && check !in protected) index else null
        }

        if (eligible.isEmpty()) return null

        val pickedIndex = eligible.random()
        val expected = tokens[pickedIndex]

        val display = tokens.toMutableList()
        display[pickedIndex] = "______"

        return display.joinToString(" ") to expected
    }
}