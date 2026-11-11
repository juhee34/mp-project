package com.tinyrunners.school.stories

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Toast
import com.tinyrunners.school.MainActivity
import com.tinyrunners.school.auth.AuthService
import com.tinyrunners.school.auth.LoginActivity
import com.tinyrunners.school.data.CharacterRepository
import com.tinyrunners.school.game.GameActivity
import com.tinyrunners.school.progress.CharacterProgress
import com.tinyrunners.school.progress.ProgressStore
import com.tinyrunners.school.ui.Page

class StoryActivity : Activity() {
    override fun onPause() {
        com.tinyrunners.school.audio.Music.pauseSoon()
        super.onPause()
    }

    private lateinit var characters: CharacterRepository
    private lateinit var store: StoryStore
    private var stories = emptyList<Story>()
    private var progress = emptyMap<String, CharacterProgress>()
    private var selected = "mint"
    private var reading: String? = null
    private var pageIndex = 0
    private var loading = true
    private var progressError = false
    private var ready = false
    private var generation = 0
    private var owner: String? = null
    private var scroll: ScrollView? = null
    private var listScroll = 0
    private var readerScroll = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!AuthService.hasSession(this)) { startActivity(Intent(this, LoginActivity::class.java)); finish(); return }
        owner = AuthService.currentUid(this)
        val restored = savedInstanceState?.takeIf { it.getString("owner") == owner }
        try {
            characters = CharacterRepository(this)
            stories = assets.open("content/stories.json").bufferedReader().use { StoryParser.parse(it.readText()) }
            store = StoryStore(this)
            selected = (restored?.getString("character") ?: intent.getStringExtra("characterId"))
                ?.takeIf { characters.find(it) != null } ?: characters.characters.first().id
            reading = restored?.getString("story")?.takeIf { id ->
                stories.any { it.id == id && it.characterId == selected && it.status == "ready" &&
                    state(it) in setOf(StoryState.READ, StoryState.UNREAD) }
            }
            reading?.let { id -> pageIndex = StoryRules.bookmark(restored?.getInt("page") ?: store.page(id), find(id).pages.size) }
            listScroll = restored?.getInt("listScroll") ?: 0
            readerScroll = restored?.getInt("readerScroll") ?: 0
            ready = true
            render()
        } catch (_: Exception) {
            Page(this).apply { title("이야기를 불러오지 못했어요"); text("친구 선택으로 돌아가 다시 열어 주세요.");
                button("친구 선택으로") { startActivity(MainActivity.selectionIntent(this@StoryActivity)); finish() }; show() }
        }
    }

    override fun onResume() {
        super.onResume()
        com.tinyrunners.school.audio.Music.playStory(this)
        if (!ready) return
        if (!AuthService.hasSession(this) || owner != AuthService.currentUid(this)) { finish(); return }
        val request = ++generation
        loading = true
        val receive: (Result<Map<String, CharacterProgress>>) -> Unit = receive@{ result ->
            if (isFinishing || isDestroyed || request != generation ||
                !AuthService.hasSession(this) || owner != AuthService.currentUid(this)) return@receive
            loading = false; progressError = result.isFailure
            result.getOrNull()?.let { progress = it }
            // Unlocks are monotonic on this device. Offline reads never relock stories.
            stories.filter { it.status == "ready" && it.unlockDistance > 0 &&
                (progress[it.characterId]?.totalMeters ?: 0) >= it.unlockDistance && it.id !in store.unlocked }
                .forEach { store.unlock(it.id) }
            if (reading == null) { rememberScroll(); render() }
        }
        try { ProgressStore.forCurrentUser(this).loadAll(receive) }
        catch (error: Exception) { receive(Result.failure(error)) }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        rememberScroll()
        outState.putString("owner", owner)
        outState.putInt("listScroll", listScroll); outState.putInt("readerScroll", readerScroll)
        outState.putString("character", selected); outState.putString("story", reading); outState.putInt("page", pageIndex)
        super.onSaveInstanceState(outState)
    }

    @Deprecated("Activity back stack")
    override fun onBackPressed() { if (reading != null) showList() else super.onBackPressed() }

    private fun find(id: String) = stories.first { it.id == id }
    private fun state(s: Story) = StoryRules.state(s, progress[s.characterId]?.totalMeters ?: 0, store.unlocked, store.read)
    private fun rememberScroll() {
        val view = scroll ?: return
        // A fast distance callback can arrive before the restored view is laid out.
        if (!view.isLaidOut) return
        if (reading == null) listScroll = view.scrollY else readerScroll = view.scrollY
    }
    private fun showList() { reading = null; readerScroll = 0; render() }
    private fun open(s: Story, bypass: Boolean = false) {
        if (s.status != "ready") return
        if (state(s) == StoryState.LOCKED && !bypass) return
        if (!store.unlock(s.id)) { saveError(); return }
        rememberScroll()
        pageIndex = if (s.id in store.read) 0 else StoryRules.bookmark(store.page(s.id), s.pages.size)
        reading = s.id; readerScroll = 0; render()
    }
    private fun saveError() = Toast.makeText(this, "기기에 저장하지 못했어요. 다시 시도해 주세요.", Toast.LENGTH_SHORT).show()

    private fun run() {
        startActivity(GameActivity.intent(this, selected))
        finish()
    }

    private fun render() {
        val p = Page(this)
        val friend = characters.find(selected)!!
        val s = reading?.let(::find)
        if (s == null) {
            p.eyebrow("TINY RUNNERS · 이야기 도감")
            p.title("함께 남긴 작은 기억")
            p.text("읽고 싶은 날에만 펼쳐 보세요. 읽지 않아도 달리기는 계속할 수 있어요.")
            characters.characters.chunked(3).forEach { group ->
                val row = LinearLayout(this)
                group.forEach { c ->
                    row.addView(Button(this).apply {
                        text = if (c.id == selected) "✓ ${c.name}" else c.name
                        isAllCaps = false; isSelected = c.id == selected; minHeight = p.dp(48)
                        setTextColor(Page.LEAF)
                        contentDescription = "${c.name}의 이야기" + if (c.id == selected) ", 선택됨" else ""
                        setOnClickListener { selected = c.id; listScroll = 0; render() }
                    }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
                }
                p.body.addView(row, p.params(8))
            }
            p.title("${friend.name}의 책장")
            p.text(friend.home, 14f)
            val meter = progress[selected]?.totalMeters ?: 0
            p.text(when { loading -> "함께한 거리를 확인하고 있어요…"
                progressError -> "거리 기록을 불러오지 못했어요. 감상하기와 전에 열린 이야기는 이용할 수 있어요."
                else -> "함께한 거리 ${meter}m · 읽음 ${stories.count { it.characterId == selected && it.id in store.read && it.status == "ready" }}/2" }, 14f)
            stories.filter { it.characterId == selected }.sortedBy { it.order }.forEach { story ->
                val st = state(story)
                val card = p.card()
                p.line(card, if (story.kind == "background") "친구 소개" else "이야기 ${story.order}", 13f, Page.LEAF, true)
                p.line(card, story.title, 20f, Page.INK, true)
                p.line(card, when (st) { StoryState.READ -> "읽은 이야기"
                    StoryState.UNREAD -> "아직 읽지 않은 이야기"
                    StoryState.LOCKED -> "아직 열리지 않은 이야기"
                    StoryState.COMING_SOON -> "다음 이야기 · 준비 중" }, 14f, Page.LEAF, top = 8)
                p.line(card, if (st == StoryState.COMING_SOON) "본문을 준비하고 있어요. 거리로는 열리지 않아요." else story.summary, top = 8)
                if (st == StoryState.LOCKED) p.line(card, "이 친구와 누적 ${story.unlockDistance}m를 달리면 열려요. 실패한 거리도 모여요.")
                if (st != StoryState.COMING_SOON) {
                    val label = when (st) { StoryState.READ -> "다시 읽기"; StoryState.LOCKED -> "거리와 관계없이 감상하기"
                        else -> if (store.page(story.id) > 0) "이어서 읽기" else "읽기" }
                    card.addView(Button(this).apply { text = label; isAllCaps = false; minHeight = p.dp(48)
                        setTextColor(Page.LEAF); contentDescription = "${story.title}, $label"
                        setOnClickListener { open(story, st == StoryState.LOCKED) } }, p.params(10))
                }
            }
            p.text("읽은 표시와 감상 진도는 현재 계정별로 이 기기에 저장돼요. 다른 기기와 동기화되지는 않아요.", 13f)
            install(p, listOf("친구 선택으로" to { startActivity(MainActivity.selectionIntent(this)); finish() }, "${friend.name}와 달리기" to ::run))
        } else {
            if (state(s) !in setOf(StoryState.UNREAD, StoryState.READ)) { showList(); return }
            pageIndex = StoryRules.bookmark(pageIndex, s.pages.size)
            val page = s.pages[pageIndex]
            p.eyebrow("${friend.name}의 이야기 · ${pageIndex + 1} / ${s.pages.size}")
            p.title(s.title)
            p.text(page.title, 16f, Page.LEAF)
            page.blocks.forEach { block ->
                if (block.speaker == null) p.text(block.text, 18f, Page.INK, 18)
                else {
                    val card = p.card(14)
                    p.line(card, block.speaker, 14f, Page.LEAF, true)
                    p.line(card, block.text, 19f, Page.INK, top = 8).setLineSpacing(0f, 1.15f)
                }
            }
            if (pageIndex == s.pages.lastIndex) p.text(s.afterReading, 16f, Page.LEAF, 20)
            val next: () -> Unit = {
                if (pageIndex < s.pages.lastIndex) {
                    turnPage(s, pageIndex + 1)
                } else if (StoryRules.canFinish(s, pageIndex, state(s))) {
                    if (store.markRead(s.id)) showList() else saveError()
                }
            }
            install(p, listOf("나중에 읽기" to ::showList, "바로 달리기" to ::run),
                listOf("이전" to { if (pageIndex > 0) turnPage(s, pageIndex - 1) },
                    (if (pageIndex == s.pages.lastIndex) "읽음으로 저장" else "다음") to next), pageIndex > 0)
        }
    }

    private fun turnPage(story: Story, target: Int) {
        if (!store.savePage(story.id, target)) { saveError(); return }
        pageIndex = target; readerScroll = 0; render()
    }

    private fun install(page: Page, actions: List<Pair<String, () -> Unit>>,
                        navigation: List<Pair<String, () -> Unit>> = emptyList(), canPrevious: Boolean = true) {
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(Page.BACKGROUND) }
        page.root.setOnApplyWindowInsetsListener(null)
        root.addView(page.root, LinearLayout.LayoutParams(-1, 0, 1f))
        fun addRow(items: List<Pair<String, () -> Unit>>, nav: Boolean) {
            val row = LinearLayout(this).apply { setPadding(page.dp(12), page.dp(3), page.dp(12), page.dp(3)) }
            items.forEachIndexed { index, (label, action) ->
                row.addView(Button(this).apply {
                    text = label; isAllCaps = false; textSize = 14f; minHeight = page.dp(48)
                    setTextColor(Page.LEAF); typeface = Typeface.DEFAULT_BOLD
                    isEnabled = !(nav && index == 0 && !canPrevious)
                    setOnClickListener { action() }
                }, LinearLayout.LayoutParams(0, -2, 1f))
            }
            root.addView(row)
        }
        if (navigation.isNotEmpty()) addRow(navigation, true)
        addRow(actions, false)
        root.setOnApplyWindowInsetsListener { v, insets ->
            if (Build.VERSION.SDK_INT >= 30) {
                val bars = insets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
                v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
                WindowInsets.CONSUMED
            } else {
                @Suppress("DEPRECATION")
                v.setPadding(insets.systemWindowInsetLeft, insets.systemWindowInsetTop, insets.systemWindowInsetRight, insets.systemWindowInsetBottom)
                @Suppress("DEPRECATION")
                insets.consumeSystemWindowInsets()
            }
        }
        setContentView(root)
        scroll = page.root
        val restoreY = if (reading == null) listScroll else readerScroll
        page.root.post { page.root.scrollTo(0, restoreY) }
        root.requestApplyInsets()
    }

    companion object {
        fun intent(context: Context, characterId: String? = null) = Intent(context, StoryActivity::class.java)
            .putExtra("characterId", characterId)
    }
}
