package com.tinyrunners.school

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.view.View
import android.view.WindowInsets
import android.widget.*
import com.tinyrunners.school.data.CharacterRepository

/** 연결 확인용 시작 화면. 러닝 엔진과 실제 이미지/음원은 다음 단계에서 구현한다. */
class MainActivity : Activity() {
    private lateinit var repository: CharacterRepository
    private lateinit var host: FrameLayout
    private var route = "intro"
    private var selectedId = "mint"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        host = findViewById(R.id.screen_host)
        host.setOnApplyWindowInsetsListener { view, insets ->
            if (android.os.Build.VERSION.SDK_INT >= 30) {
                val bars = insets.getInsets(WindowInsets.Type.systemBars())
                view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            } else {
                @Suppress("DEPRECATION")
                view.setPadding(insets.systemWindowInsetLeft, insets.systemWindowInsetTop, insets.systemWindowInsetRight, insets.systemWindowInsetBottom)
            }
            insets
        }
        try { repository = CharacterRepository(this) } catch (error: Exception) {
            val body = screen("자료를 읽을 수 없습니다")
            text(body, "캐릭터 JSON을 확인하세요. " + error.javaClass.simpleName)
            return
        }
        selectedId = savedInstanceState?.getString("selectedId") ?: "mint"
        when (savedInstanceState?.getString("route")) { "select" -> showSelect(); "profile" -> showProfile(selectedId); else -> showIntro() }
    }
    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("route", route); outState.putString("selectedId", selectedId)
        super.onSaveInstanceState(outState)
    }
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    private fun screen(title: String): LinearLayout {
        host.removeAllViews()
        val scroll = ScrollView(this)
        val body = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(24), dp(24), dp(24), dp(32)) }
        scroll.addView(body); host.addView(scroll)
        text(body, "TINY RUNNERS · 개발 시작 프로젝트", 12)
        text(body, title, 28)
        return body
    }
    private fun text(body: LinearLayout, value: String, size: Int = 16) {
        body.addView(TextView(this).apply { text = value; textSize = size.toFloat(); setTextColor(Color.rgb(40,56,46)); setPadding(0,dp(10),0,dp(12)) })
    }
    private fun button(body: LinearLayout, label: String, action: () -> Unit) {
        body.addView(Button(this).apply { text = label; isAllCaps = false; setOnClickListener { action() } }, LinearLayout.LayoutParams(-1,-2))
    }
    private fun showIntro() {
        route = "intro"
        val body = screen("열 친구와 작은 모험")
        text(body, "친구를 고르고, 그 친구의 고향과 이야기를 만나세요.")
        text(body, "현재는 캐릭터 10명과 화면 연결을 준비한 시작 프로젝트입니다. 달리기·음악·완성 배경은 아직 구현되지 않았습니다.", 14)
        button(body, "친구 10명 만나기") { showSelect() }
    }
    private fun showSelect() {
        route = "select"
        val body = screen("오늘 함께할 친구")
        text(body, "10명 모두 선택할 수 있습니다. 현재 그림은 색상 표식으로 대신합니다.", 14)
        repository.characters.forEach { friend ->
            val card = Button(this).apply {
                text = friend.name + " · " + friend.species + "\n" + friend.home
                isAllCaps = false; setTextColor(Color.rgb(35,40,33)); setBackgroundColor(Color.parseColor(friend.color))
                setPadding(dp(12),dp(12),dp(12),dp(12)); setOnClickListener { showProfile(friend.id) }
            }
            body.addView(card, LinearLayout.LayoutParams(-1,-2).apply { bottomMargin=dp(12) })
        }
        button(body, "소개로") { showIntro() }
    }
    private fun showProfile(id: String) {
        val friend = repository.find(id) ?: run { showSelect(); return }
        route = "profile"; selectedId = id
        val body = screen(friend.name + "의 이야기")
        text(body, friend.home, 20); text(body, friend.personality); text(body, friend.summary)
        text(body, "이야기 개요 초안입니다. 완성 에피소드와 달리기 기능은 다음 개발 단계에서 연결합니다.", 13)
        button(body, "달리기 연결 예정 · " + friend.name) { Toast.makeText(this, "개발 예정: Game 화면에 characterId=" + friend.id + " 전달", Toast.LENGTH_LONG).show() }
        button(body, "다른 친구 선택") { showSelect() }
    }
    @Deprecated("Starter navigation; replace with lifecycle-aware back handling during screen split")
    override fun onBackPressed() {
        when(route) { "profile" -> showSelect(); "select" -> showIntro(); else -> super.onBackPressed() }
    }
}
