package com.tinyrunners.school.characters

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.tinyrunners.school.R

internal enum class SpriteFrame {
    IDLE_0, IDLE_1, RUN_0, RUN_1, RUN_2, RUN_3, JUMP_0, JUMP_1, FAIL_0
}

/** Immutable frame sets shared by portraits and runs, including restarted games. */
internal class CharacterSprites private constructor(private val frames: List<Bitmap>) {
    operator fun get(frame: SpriteFrame): Bitmap = frames[frame.ordinal]

    companion object {
        const val SIZE = 48
        const val CENTER_X = 24
        // Row 45 is the last opaque row. Its lower edge touches the ground.
        const val FOOT_EDGE_Y = 46
        private val cache = mutableMapOf<String, CharacterSprites>()

        @Synchronized
        fun load(resources: Resources, characterId: String): CharacterSprites? {
            cache[characterId]?.let { return it }
            val ids = when (characterId) {
                "mint" -> intArrayOf(
                    R.drawable.sprite_mint_idle_0,
                    R.drawable.sprite_mint_idle_1,
                    R.drawable.sprite_mint_run_0,
                    R.drawable.sprite_mint_run_1,
                    R.drawable.sprite_mint_run_2,
                    R.drawable.sprite_mint_run_3,
                    R.drawable.sprite_mint_jump_0,
                    R.drawable.sprite_mint_jump_1,
                    R.drawable.sprite_mint_fail_0,
                )
                "cheese" -> intArrayOf(
                    R.drawable.sprite_cheese_idle_0,
                    R.drawable.sprite_cheese_idle_1,
                    R.drawable.sprite_cheese_run_0,
                    R.drawable.sprite_cheese_run_1,
                    R.drawable.sprite_cheese_run_2,
                    R.drawable.sprite_cheese_run_3,
                    R.drawable.sprite_cheese_jump_0,
                    R.drawable.sprite_cheese_jump_1,
                    R.drawable.sprite_cheese_fail_0,
                )
                "popo" -> intArrayOf(
                    R.drawable.sprite_popo_idle_0,
                    R.drawable.sprite_popo_idle_1,
                    R.drawable.sprite_popo_run_0,
                    R.drawable.sprite_popo_run_1,
                    R.drawable.sprite_popo_run_2,
                    R.drawable.sprite_popo_run_3,
                    R.drawable.sprite_popo_jump_0,
                    R.drawable.sprite_popo_jump_1,
                    R.drawable.sprite_popo_fail_0,
                )
                "lumi" -> intArrayOf(
                    R.drawable.sprite_lumi_idle_0,
                    R.drawable.sprite_lumi_idle_1,
                    R.drawable.sprite_lumi_run_0,
                    R.drawable.sprite_lumi_run_1,
                    R.drawable.sprite_lumi_run_2,
                    R.drawable.sprite_lumi_run_3,
                    R.drawable.sprite_lumi_jump_0,
                    R.drawable.sprite_lumi_jump_1,
                    R.drawable.sprite_lumi_fail_0,
                )
                "nunu" -> intArrayOf(
                    R.drawable.sprite_nunu_idle_0,
                    R.drawable.sprite_nunu_idle_1,
                    R.drawable.sprite_nunu_run_0,
                    R.drawable.sprite_nunu_run_1,
                    R.drawable.sprite_nunu_run_2,
                    R.drawable.sprite_nunu_run_3,
                    R.drawable.sprite_nunu_jump_0,
                    R.drawable.sprite_nunu_jump_1,
                    R.drawable.sprite_nunu_fail_0,
                )
                "piko" -> intArrayOf(
                    R.drawable.sprite_piko_idle_0,
                    R.drawable.sprite_piko_idle_1,
                    R.drawable.sprite_piko_run_0,
                    R.drawable.sprite_piko_run_1,
                    R.drawable.sprite_piko_run_2,
                    R.drawable.sprite_piko_run_3,
                    R.drawable.sprite_piko_jump_0,
                    R.drawable.sprite_piko_jump_1,
                    R.drawable.sprite_piko_fail_0,
                )
                else -> return null
            }
            val options = BitmapFactory.Options().apply { inScaled = false }
            val bitmaps = ids.map { id ->
                val bitmap = BitmapFactory.decodeResource(resources, id, options) ?: return null
                require(bitmap.width == SIZE && bitmap.height == SIZE) { "Expected 48x48 sprite" }
                bitmap
            }
            return CharacterSprites(bitmaps).also { cache[characterId] = it }
        }
    }
}
