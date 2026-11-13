package com.tinyrunners.school.stories

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.ScrollView
import android.widget.Toast
import com.tinyrunners.school.MainActivity
import com.tinyrunners.school.auth.AuthService
import com.tinyrunners.school.auth.LoginActivity
import com.tinyrunners.school.data.CharacterRepository
import com.tinyrunners.school.game.GameActivity
import com.tinyrunners.school.progress.CharacterProgress
import com.tinyrunners.school.progress.ProgressStore
import com.tinyrunners.school.R
import com.tinyrunners.school.shop.ShopActivity
import com.tinyrunners.school.ui.CardShelf
import com.tinyrunners.school.ui.HomeButton
import com.tinyrunners.school.ui.Page
import com.tinyrunners.school.ui.PaperBackdropView

class StoryActivity : Activity(), com.tinyrunners.school.audio.MusicScreen {
    /** Story screens play the main theme. */
    override fun musicTrack(): Int = com.tinyrunners.school.R.raw.main_theme

    override fun onPause() {
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
            renderList()
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

    /**
     * The story book's shelf, laid out like 내 기록: a top bar (◀ 돌아가기, the title, where this friend's shelf stands,
     * a run with this friend | bag), the six friends as pills to switch shelves, and one card per story in a row that
     * slides sideways (CardShelf): the friend's home (darker while closed), the title and a state chip, what kind of
     * story it is, its summary or what opens it, and one button.
     */
    private fun renderList() {
        val friend = characters.find(selected)!!
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(6), dp(16), dp(10)) }

        val top = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        top.addView(HomeButton.create(this, getString(R.string.common_back_pill)) { finish() })
        top.addView(TextView(this).apply {
            text = "이야기 도감"; textSize = 22f; setTextColor(Page.INK); setTypeface(typeface, Typeface.BOLD)
            if (Build.VERSION.SDK_INT >= 28) isAccessibilityHeading = true
            setPadding(dp(12), 0, dp(12), 0)
        })
        val mine = stories.filter { it.characterId == selected && it.status == "ready" }
        val meter = progress[selected]?.totalMeters ?: 0
        top.addView(TextView(this).apply {
            text = when {
                loading -> "함께한 거리를 확인하고 있어요…"
                progressError -> "거리 기록을 불러오지 못했어요."
                else -> "${friend.name}의 책장 · 함께한 거리 ${meter}m · 읽음 ${mine.count { it.id in store.read }}/${mine.size}"
            }
            textSize = 14f; setTextColor(if (progressError) Page.ERROR else Page.LEAF); setTypeface(typeface, Typeface.BOLD)
            maxLines = 1; ellipsize = android.text.TextUtils.TruncateAt.END
        }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        top.addView(Button(this).apply {
            text = getString(R.string.characters_copy_run, com.tinyrunners.school.talk.Josa.with(friend.name, "이랑", "랑"))
            com.tinyrunners.school.ui.TopPill.style(this)
            setOnClickListener { run() }
        })
        top.addView(com.tinyrunners.school.ui.TopPill.divider(this))
        top.addView(com.tinyrunners.school.ui.BagButton.create(this))
        content.addView(top)

        // The friends' shelves, as pills; the open one is green. The saving note sits beside them.
        val tabs = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        characters.characters.forEach { c ->
            val on = c.id == selected
            tabs.addView(Button(this).apply {
                text = c.name
                com.tinyrunners.school.ui.TopPill.style(this, sizeSp = 13f)
                minHeight = dp(32); minimumHeight = dp(32); setPadding(dp(14), 0, dp(14), dp(2))
                if (on) {
                    setTextColor(Color.parseColor("#FFF8E7"))
                    background = GradientDrawable().apply { setColor(com.tinyrunners.school.ui.TopPill.INK); cornerRadius = dp(100).toFloat() }
                }
                contentDescription = "${c.name}의 이야기" + if (on) ", 선택됨" else ""
                setOnClickListener { if (!on) { selected = c.id; listScroll = 0; render() } }
            }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { marginEnd = dp(6) })
        }
        val tabRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        tabRow.addView(tabs)
        tabRow.addView(TextView(this).apply {
            text = "읽은 표시와 진도는 계정별로 이 기기에 저장돼요."
            textSize = 13f; setTextColor(Page.MUTED); maxLines = 1; ellipsize = android.text.TextUtils.TruncateAt.END
            setPadding(dp(10), dp(3), dp(10), dp(3))
            background = GradientDrawable().apply { setColor(Color.argb(215, 255, 249, 235)); cornerRadius = dp(12).toFloat() }
        }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = dp(6) })
        content.addView(tabRow, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(6) })

        val shelf = CardShelf(this)
        content.addView(shelf, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f).apply { topMargin = dp(8) })
        shelf.setCards(stories.filter { it.characterId == selected }.sortedBy { it.order }.map { storyCard(it, meter) })

        setContentView(FrameLayout(this).apply {
            setBackgroundColor(Page.BACKGROUND)
            addView(PaperBackdropView(this@StoryActivity), FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
            addView(content, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
            setOnApplyWindowInsetsListener { v, insets ->
                if (Build.VERSION.SDK_INT >= 30) {
                    val bars = insets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
                    v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
                } else {
                    @Suppress("DEPRECATION")
                    v.setPadding(insets.systemWindowInsetLeft, insets.systemWindowInsetTop, insets.systemWindowInsetRight, insets.systemWindowInsetBottom)
                }
                insets
            }
        })
        scroll = null
    }

    /** One story's card on the shelf. */
    private fun storyCard(story: Story, meter: Long): View {
        val st = state(story)
        val card = CardShelf.card(this)
        card.addView(com.tinyrunners.school.story.SceneThumb(this, story.characterId,
            locked = st == StoryState.LOCKED || st == StoryState.COMING_SOON).apply {
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        val nameRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        nameRow.addView(CardShelf.title(this, story.title), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        val (chip, fill) = when (st) {
            StoryState.READ -> "읽음" to ShopActivity.DISABLED
            StoryState.UNREAD -> "새 이야기" to ShopActivity.OWNED
            StoryState.LOCKED -> "잠김" to ShopActivity.DISABLED
            StoryState.COMING_SOON -> "준비 중" to ShopActivity.DISABLED
        }
        nameRow.addView(CardShelf.chip(this, chip, fill))
        card.addView(nameRow)
        card.addView(CardShelf.body(this, if (story.kind == "background") "친구 소개" else "이야기 ${story.order}", lines = 1).apply {
            setTextColor(CardShelf.GREEN); setTypeface(typeface, Typeface.BOLD)
        })
        card.addView(CardShelf.body(this, when (st) {
            StoryState.COMING_SOON -> "본문을 준비하고 있어요."
            StoryState.LOCKED -> "함께 ${story.unlockDistance}m 달리면 열려요 · 지금 ${meter}m"
            else -> story.summary
        }, lines = 2))
        if (st != StoryState.COMING_SOON) {
            val label = when (st) {
                StoryState.READ -> "다시 읽기"
                StoryState.LOCKED -> "미리 감상하기"
                else -> if (store.page(story.id) > 0) "이어서 읽기" else "읽기"
            }
            card.addView(CardShelf.button(this, label, primary = st == StoryState.UNREAD) { open(story, st == StoryState.LOCKED) }
                .apply { contentDescription = "${story.title}, $label" }, CardShelf.buttonParams(this, 6))
        } else {
            card.addView(CardShelf.button(this, "준비 중", primary = false, enabled = false) {}, CardShelf.buttonParams(this, 6))
        }
        return card
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

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
