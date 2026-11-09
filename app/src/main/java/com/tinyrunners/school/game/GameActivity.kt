package com.tinyrunners.school.game

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Build
import android.view.WindowInsets
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.tinyrunners.school.data.CharacterRepository

/** B's game entry point. Replace the pending body with the runner; keep the ID contract. */
class GameActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val characterId = intent.getStringExtra(EXTRA_CHARACTER_ID)
        val character = CharacterRepository(this).find(characterId.orEmpty())
        if (character == null) { finish(); return }
        val body = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val padding = (24 * resources.displayMetrics.density).toInt()
            setPadding(padding, padding, padding, padding)
            addView(TextView(this@GameActivity).apply {
                text = "${character.name}와 달리기\n게임 진입 ID: ${character.id}\n\n러닝 엔진 연결 준비 중\n선택한 친구의 ID는 전달되었습니다. 실제 달리기는 B의 엔진 연결 후 가능합니다."
                textSize = 20f
            })
            addView(Button(this@GameActivity).apply {
                text = "친구 선택으로 돌아가기"
                setOnClickListener { finish() }
            })
        }
        setContentView(ScrollView(this).apply {
            addView(body)
            setOnApplyWindowInsetsListener { view, insets ->
                if (Build.VERSION.SDK_INT >= 30) {
                    val bars = insets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
                    view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
                } else {
                    @Suppress("DEPRECATION")
                    view.setPadding(insets.systemWindowInsetLeft, insets.systemWindowInsetTop,
                        insets.systemWindowInsetRight, insets.systemWindowInsetBottom)
                }
                insets
            }
        })
    }

    companion object {
        const val EXTRA_CHARACTER_ID = "characterId"
        fun intent(context: Context, characterId: String) =
            Intent(context, GameActivity::class.java).putExtra(EXTRA_CHARACTER_ID, characterId)
    }
}
