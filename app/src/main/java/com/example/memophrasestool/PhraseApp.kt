package com.example.memophrasestool

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PhraseApp(vm: PhraseViewModel) {
    val state by vm.state.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        vm.loadBooks(context)
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        when (state.screen) {
            "home" -> HomeScreen(vm)
            "import" -> ImportScreen(state, vm)
            "bookList" -> BookListScreen(state, vm)
            "bookDetail" -> BookDetailScreen(state, vm)
            "apiKey" -> ApiKeyScreen(vm)
            "quiz" -> {
                if (state.finished) SummaryScreen(state, vm)
                else QuizScreen(state, vm)
            }
        }
    }
}

@Composable
fun AppCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            content = content
        )
    }
}

@Composable
fun PageTitle(title: String, subtitle: String? = null) {
    Text(
        text = title,
        fontSize = 30.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center
    )

    if (subtitle != null) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = subtitle,
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun HomeScreen(vm: PhraseViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        AppCard {
            PageTitle(
                title = "JPhrase",
                subtitle = "让大默写背诵更轻松！ヾ(≧▽≦*)o"
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { vm.goBookList() },
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text("选择词组库", fontSize = 17.sp)
            }

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = { vm.goImport() },
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text("导入新词组库", fontSize = 17.sp)
            }

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedButton(
                onClick = { vm.goApiKey() },
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text("设置 DeepSeek API Key", fontSize = 17.sp)
            }
        }
    }
}

@Composable
fun ApiKeyScreen(vm: PhraseViewModel) {
    val context = LocalContext.current
    var apiKey by remember { mutableStateOf(ApiKeyStorage.getApiKey(context)) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {


        Spacer(modifier = Modifier.height(16.dp))

        AppCard {
            PageTitle("设置 API Key", "deepseek官网 https://www.deepseek.com/ \n-> API开放平台 -> API keys")

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = apiKey,
                onValueChange = { apiKey = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("请输入 DeepSeek API Key") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    ApiKeyStorage.saveApiKey(context, apiKey)
                    vm.goHome()
                },
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text("保存")
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
        OutlinedButton(
            onClick = { vm.goHome() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("返回首页")
        }
    }
}

@Composable
fun ImportScreen(state: QuizState, vm: PhraseViewModel) {
    val context = LocalContext.current
    var bookName by remember { mutableStateOf("") }
    var text by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {


        Spacer(modifier = Modifier.height(16.dp))

        AppCard {
            PageTitle("导入新词组库", "普通导入需严格按照示例格式且可能出错；ai导入较准确")

            Spacer(modifier = Modifier.height(20.dp))

            OutlinedTextField(
                value = bookName,
                onValueChange = { bookName = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("词组库名称") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp),
                placeholder = {
                    Text(
                        "例如：\n" +
                                "1. 决赛 the final match\n" +
                                "   发现某事正在发生 find sth doing\n" +
                                "2. 把 A 重新定义为 B reframe A as B"
                    )
                },
                shape = RoundedCornerShape(16.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { vm.importFromText(text) },
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text("普通导入")
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = {
                    val savedApiKey = ApiKeyStorage.getApiKey(context)
                    vm.aiImportFromText(text, savedApiKey)
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled = !state.isLoading
            ) {
                Text(if (state.isLoading) "AI 正在分割..." else "AI 分割导入")
            }

            state.errorMessage?.let {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            if (state.phrases.isNotEmpty()) {
                Spacer(modifier = Modifier.height(20.dp))

                AssistChip(
                    onClick = {},
                    label = { Text("已识别 ${state.phrases.size} 个词组") }
                )

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = { vm.saveNewBook(context, bookName, state.phrases) },
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    Text("保存词组库")
                }
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
        OutlinedButton(
            onClick = { vm.goHome() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("返回首页")
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BookListScreen(state: QuizState, vm: PhraseViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {


        Spacer(modifier = Modifier.height(16.dp))

        PageTitle("选择词组库", "点击进入详情并开始背诵♪(´▽｀)")

        Spacer(modifier = Modifier.height(20.dp))

        if (state.books.isEmpty()) {
            AppCard {
                Text(
                    "还没有词组库，请先导入(┬┬﹏┬┬)",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(state.books) { book ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 14.dp)
                            .combinedClickable(
                                onClick = { vm.selectBook(book) },
                                onLongClick = { vm.selectBook(book) }
                            ),
                        shape = RoundedCornerShape(20.dp),
                        elevation = CardDefaults.cardElevation(3.dp)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Text(
                                text = book.name,
                                fontSize = 21.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "${book.phrases.size} 个词组",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
        OutlinedButton(
            onClick = { vm.goHome() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("返回首页")
        }
    }
}

@Composable
fun BookDetailScreen(state: QuizState, vm: PhraseViewModel) {
    val context = LocalContext.current
    val book = state.currentBook ?: return

    var chinese by remember { mutableStateOf("") }
    var english by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {


        Spacer(modifier = Modifier.height(16.dp))

        AppCard {
            PageTitle(book.name, "${book.phrases.size} 个词组")

            Spacer(modifier = Modifier.height(18.dp))

            Button(
                onClick = { vm.startSelectedBook() },
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text("开始背诵")
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = chinese,
                onValueChange = { chinese = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("中文") },
                shape = RoundedCornerShape(16.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = english,
                onValueChange = { english = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("英文") },
                shape = RoundedCornerShape(16.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = {
                    vm.addPhraseToCurrentBook(context, chinese, english)
                    chinese = ""
                    english = ""
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("添加词组")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(book.phrases) { phrase ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Row(modifier = Modifier.padding(16.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(phrase.chinese, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(phrase.english)
                        }

                        TextButton(
                            onClick = {
                                vm.deletePhraseFromCurrentBook(context, phrase.id)
                            }
                        ) {
                            Text("删除")
                        }
                    }
                }
            }
        }

        OutlinedButton(
            onClick = { vm.deleteBook(context, book.id) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("删除整个词组库")
        }
        Spacer(modifier = Modifier.height(20.dp))
        OutlinedButton(
            onClick = { vm.goBookList() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("返回词组库列表")
        }
    }
}

@Composable
fun QuizScreen(state: QuizState, vm: PhraseViewModel) {
    val item = state.current

    if (item == null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("正在加载题目...(～￣▽￣)～")
            Spacer(modifier = Modifier.height(20.dp))
            OutlinedButton(onClick = { vm.goHome() }) {
                Text("返回首页")
            }
        }
        return
    }

    var input by remember(item.id, state.phase) {
        mutableStateOf("")
    }

    val hasFeedback = state.feedback != null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {


        Spacer(modifier = Modifier.height(16.dp))

        AppCard {
            Text(
                text = stageName(state.phase),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = item.chinese,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            when (state.phase) {
                1 -> {
                    if (state.showAnswer) {
                        Text(
                            text = item.english,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    Stage1Buttons(state, vm)
                }

                2 -> {
                    Text(
                        text = state.stage2Mask,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    OutlinedTextField(
                        value = input,
                        onValueChange = { input = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("填写被挖空的单词") },
                        enabled = !hasFeedback,
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = { vm.submitStage2(input) },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        enabled = !hasFeedback
                    ) {
                        Text("提交校对")
                    }
                }

                3 -> {
                    OutlinedTextField(
                        value = input,
                        onValueChange = { input = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("填写完整英文词组") },
                        enabled = !hasFeedback,
                        shape = RoundedCornerShape(16.dp)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = { vm.submitStage3(input) },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        enabled = !hasFeedback
                    ) {
                        Text("提交校对")
                    }
                }
            }

            state.feedback?.let {
                Spacer(modifier = Modifier.height(22.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Text(
                        text = it,
                        modifier = Modifier.padding(16.dp),
                        fontSize = 17.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = { vm.nextAfterFeedback() },
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    Text("下一题")
                }
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
        OutlinedButton(
            onClick = { vm.goHome() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("退出背诵，返回首页")
        }
    }
}

@Composable
fun Stage1Buttons(state: QuizState, vm: PhraseViewModel) {
    if (!state.showAnswer) {
        Button(
            onClick = { vm.showAnswer() },
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            Text("看答案")
        }
    } else {
        Button(
            onClick = { vm.remembered() },
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            Text("记住了")
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = { vm.forgot() },
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            Text("没记住")
        }
    }
}

@Composable
fun SummaryScreen(state: QuizState, vm: PhraseViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
            .verticalScroll(rememberScrollState())
    ) {
        PageTitle("全部完成", "本轮背诵结果ヾ(≧▽≦*)")

        Spacer(modifier = Modifier.height(20.dp))

        state.phrases.forEach { phrase ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(phrase.chinese, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(phrase.english)

                    Spacer(modifier = Modifier.height(10.dp))

                    Text("第一部分没记住：${phrase.stage1Forgot} 次")
                    Text("第二部分填错：${phrase.stage2Wrong} 次")
                    Text("第三部分拼错：${phrase.stage3Wrong} 次")

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        "总错误：${phrase.stage1Forgot + phrase.stage2Wrong + phrase.stage3Wrong} 次",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = { vm.reset() },
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            Text("返回首页")
        }
    }
}

fun stageName(phase: Int): String = when (phase) {
    1 -> "第一部分：中文回忆英文"
    2 -> "第二部分：词组挖空填词"
    3 -> "第三部分：整句拼写"
    else -> "已完成"
}