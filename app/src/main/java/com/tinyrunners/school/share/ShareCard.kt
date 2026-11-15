package com.tinyrunners.school.share

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import androidx.core.content.FileProvider
import com.tinyrunners.school.R
import com.tinyrunners.school.characters.CharacterSprites
import com.tinyrunners.school.characters.SpriteFrame
import com.tinyrunners.school.data.CharacterDefinition
import com.tinyrunners.school.shop.ShopBackground
import com.tinyrunners.school.story.BackgroundThumbs
import com.tinyrunners.school.weather.WeatherReport
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Square (1080×1080, an Instagram feed post) picture cards to share outside the app: the run result (friend, distance,
 * score, the day's weather and a word from the friend) and a friend's introduction. Drawn on a bitmap with the same
 * art as the app — A's frame picture, the title flowers, the friend's home and their 48×48 sprite — so no new
 * drawing is needed; when A sends dedicated card frames they replace [frame] and [title].
 *
 * The picture is written to the app's cache (no storage permission) and handed to other apps through a FileProvider.
 */
object ShareCard {
    const val SIZE = 1080
    private val GREEN = Color.parseColor("#1E5A2A")
    private val INK = Color.parseColor("#3D3550")
    private val SOFT = Color.parseColor("#7A6E5E")
    private val HONEY = Color.parseColor("#C9A26A")
    private val TILE = Color.parseColor("#FFF8E7")
    private val LAVENDER = Color.parseColor("#E6DAF6")

    /** What one finished run looked like. */
    data class Run(val meters: Int, val score: Int, val line: String?, val weather: WeatherReport?)

    fun runCard(context: Context, friend: CharacterDefinition, run: Run): Bitmap = card(context) { c, d ->
        d.scene(c, friend.id)
        d.namePill(c, context.getString(R.string.share_ran_with, com.tinyrunners.school.talk.Josa.with(friend.name, "과", "와")))
        d.bubble(c, friend.name, run.line ?: friend.home, 600f, 708f)
        d.tile(c, 90f, 732f, 530f, 868f, context.getString(R.string.result_distance_label), context.getString(R.string.result_meters, run.meters))
        d.tile(c, 550f, 732f, 990f, 868f, context.getString(R.string.result_score_label), "%,d".format(run.score))
        val date = SimpleDateFormat("yyyy.MM.dd", Locale.KOREA).format(Date())
        val weather = run.weather?.let { w ->
            val sky = context.getString(w.sky.label)
            val temp = if (w.temperatureC.isNaN()) "" else " %.0f°C".format(w.temperatureC)
            context.getString(w.city.label) + " · " + sky + temp
        }
        d.centered(c, listOfNotNull(weather, date).joinToString("  ·  "), 908f, 32f, SOFT, bold = true)
    }

    fun friendCard(context: Context, friend: CharacterDefinition, greeting: String): Bitmap = card(context) { c, d ->
        d.scene(c, friend.id)
        d.centered(c, friend.name, 664f, 76f, GREEN, bold = true)
        d.centered(c, context.getString(R.string.characters_species_home, friend.species, friend.home), 718f, 32f, SOFT, bold = true)
        d.bubble(c, null, greeting, 742f, 852f)
        friend.copy?.traits?.take(3)?.takeIf { it.isNotEmpty() }?.let { d.pills(c, it, 878f, 932f) }
    }

    /**
     * Saves [bitmap] to the cache and opens the system share sheet with it and [text] (the share line and hashtags).
     */
    fun share(context: Context, bitmap: Bitmap, fileName: String, text: String) {
        val dir = File(context.cacheDir, "share").apply { mkdirs() }
        // Only the newest card is kept.
        dir.listFiles()?.forEach { it.delete() }
        val file = File(dir, fileName)
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        val uri = FileProvider.getUriForFile(context, context.packageName + ".share", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, text)
            clipData = ClipData.newRawUri(null, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(send, context.getString(R.string.share_chooser)))
    }

    private fun card(context: Context, body: (Canvas, Drawer) -> Unit): Bitmap {
        val bitmap = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val d = Drawer(context)
        d.frame(canvas)
        d.title(canvas)
        body(canvas, d)
        d.centered(canvas, context.getString(R.string.share_hashtags), 985f, 28f, HONEY, bold = true)
        return bitmap
    }

    /** Drawing helpers for one card. */
    private class Drawer(private val context: Context) {
        private val res = context.resources
        private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
        private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
        private val pixel = Paint().apply { isFilterBitmap = false; isAntiAlias = false }
        private val text = TextPaint(Paint.ANTI_ALIAS_FLAG)
        private val path = Path()

        fun frame(c: Canvas) {
            val art = BitmapFactory.decodeResource(res, R.drawable.bg_shop_frame)
            if (art == null) { c.drawColor(TILE); return }
            ShopBackground.drawFrame(c, art, SIZE.toFloat(), SIZE.toFloat(), Paint(Paint.FILTER_BITMAP_FLAG))
            art.recycle()
        }

        /** "Tiny Runners" between the two lavender flowers, over the diamond line. */
        fun title(c: Canvas) {
            BitmapFactory.decodeResource(res, R.drawable.ui_village_title)?.let { deco ->
                c.drawBitmap(deco, null, RectF(170f, 46f, 910f, 177f), Paint(Paint.FILTER_BITMAP_FLAG))
                deco.recycle()
            }
            centered(c, context.getString(R.string.app_name), 136f, 76f, GREEN, bold = true)
        }

        /** The friend at home: their home picture in a rounded window, the friend standing on its floor. */
        fun scene(c: Canvas, friendId: String) {
            val box = RectF(90f, 200f, 990f, 575f)
            val r = 34f
            c.save()
            path.reset(); path.addRoundRect(box, r, r, Path.Direction.CW)
            c.clipPath(path)
            var floor = box.bottom - 40f
            val home = BackgroundThumbs.of(context, friendId)
            if (home != null) {
                val (bitmap, floorFraction) = home
                val w = box.width(); val h = box.height()
                val scale = max(h / bitmap.height, w / bitmap.width)
                val visibleW = (w / scale).toInt().coerceAtMost(bitmap.width)
                val visibleH = (h / scale).coerceAtMost(bitmap.height.toFloat())
                val floorPx = bitmap.height * floorFraction
                val top = (floorPx - visibleH * 0.88f).coerceIn(0f, bitmap.height - visibleH)
                c.drawBitmap(bitmap, Rect(0, top.toInt(), visibleW, (top + visibleH).toInt()), box, pixel)
                floor = box.top + (floorPx - top) * scale
            } else {
                fill.color = Color.parseColor("#DCEBCB"); c.drawRect(box, fill)
            }
            CharacterSprites.load(res, friendId)?.let { sprites ->
                val px = 6f
                val size = CharacterSprites.SIZE * px
                val left = (box.centerX() + 110f - CharacterSprites.CENTER_X * px).roundToInt().toFloat()
                val top = (floor - CharacterSprites.FOOT_EDGE_Y * px).roundToInt().toFloat()
                fill.color = Color.argb(70, 40, 40, 20)
                c.drawOval(box.centerX() + 110f - size * 0.26f, floor - 8f, box.centerX() + 110f + size * 0.26f, floor + 8f, fill)
                c.drawBitmap(sprites[SpriteFrame.IDLE_0], null, RectF(left, top, left + size, top + size), pixel)
            }
            c.restore()
            stroke.color = HONEY; stroke.strokeWidth = 6f
            c.drawRoundRect(box, r, r, stroke)
        }

        /** A small cream pill in the window's top-left corner. */
        fun namePill(c: Canvas, label: String) {
            text.textSize = 34f; text.typeface = Typeface.DEFAULT_BOLD; text.color = GREEN
            val w = text.measureText(label) + 56f
            val box = RectF(118f, 226f, 118f + w, 290f)
            fill.color = TILE; c.drawRoundRect(box, 32f, 32f, fill)
            stroke.color = HONEY; stroke.strokeWidth = 4f; c.drawRoundRect(box, 32f, 32f, stroke)
            c.drawText(label, box.left + 28f, box.centerY() - (text.descent() + text.ascent()) / 2f, text)
        }

        /** A speech bubble with a small tail pointing up at the friend; [who] in green before the words. */
        fun bubble(c: Canvas, who: String?, words: String, top: Float, bottom: Float) {
            val box = RectF(90f, top, 990f, bottom)
            fill.color = TILE; c.drawRoundRect(box, 30f, 30f, fill)
            path.reset()
            path.moveTo(box.centerX() + 80f, top + 2f); path.lineTo(box.centerX() + 110f, top - 22f); path.lineTo(box.centerX() + 140f, top + 2f); path.close()
            c.drawPath(path, fill)
            stroke.color = HONEY; stroke.strokeWidth = 4f; c.drawRoundRect(box, 30f, 30f, stroke)
            val line = (if (who != null) "$who  " else "") + "“" + words + "”"
            text.typeface = Typeface.DEFAULT_BOLD; text.color = INK
            val width = (box.width() - 80f).toInt()
            var size = 40f
            var layout: StaticLayout
            do {
                text.textSize = size
                layout = StaticLayout.Builder.obtain(line, 0, line.length, text, width)
                    .setAlignment(Layout.Alignment.ALIGN_CENTER).setMaxLines(2).setEllipsize(TextUtils.TruncateAt.END)
                    .setLineSpacing(0f, 1.05f).build()
                size -= 2f
            } while (layout.height > box.height() - 24f && size > 24f)
            if (who != null) {
                // The friend's name in green, the words in ink.
                val styled = android.text.SpannableString(line)
                styled.setSpan(android.text.style.ForegroundColorSpan(GREEN), 0, who.length, 0)
                layout = StaticLayout.Builder.obtain(styled, 0, styled.length, text, width)
                    .setAlignment(Layout.Alignment.ALIGN_CENTER).setMaxLines(2).setEllipsize(TextUtils.TruncateAt.END)
                    .setLineSpacing(0f, 1.05f).build()
            }
            c.save()
            c.translate(box.left + 40f, box.centerY() - layout.height / 2f)
            layout.draw(c)
            c.restore()
        }

        /** A number tile: a small label over a big value. */
        fun tile(c: Canvas, l: Float, t: Float, r: Float, b: Float, label: String, value: String) {
            val box = RectF(l, t, r, b)
            fill.color = TILE; c.drawRoundRect(box, 28f, 28f, fill)
            stroke.color = HONEY; stroke.strokeWidth = 4f; c.drawRoundRect(box, 28f, 28f, stroke)
            text.typeface = Typeface.DEFAULT_BOLD
            text.textSize = 30f; text.color = SOFT
            c.drawText(label, box.centerX() - text.measureText(label) / 2f, t + 46f, text)
            text.textSize = 64f; text.color = GREEN
            while (text.measureText(value) > box.width() - 40f && text.textSize > 30f) text.textSize -= 2f
            c.drawText(value, box.centerX() - text.measureText(value) / 2f, b - 26f, text)
        }

        /** Up to three lavender pills in a centred row, smaller (or fewer) until they fit inside the frame. */
        fun pills(c: Canvas, all: List<String>, top: Float, bottom: Float) {
            text.typeface = Typeface.DEFAULT_BOLD; text.color = INK
            val gap = 16f
            val room = SIZE - 2 * 120f
            var words = all
            fun width() = words.sumOf { (text.measureText(it) + 40f).toDouble() }.toFloat() + gap * (words.size - 1)
            text.textSize = 30f
            while (width() > room && text.textSize > 24f) text.textSize -= 1f
            while (width() > room && words.size > 1) words = words.dropLast(1)
            val widths = words.map { text.measureText(it) + 40f }
            var x = SIZE / 2f - (widths.sum() + gap * (words.size - 1)) / 2f
            words.forEachIndexed { i, w ->
                val box = RectF(x, top, x + widths[i], bottom)
                fill.color = LAVENDER; c.drawRoundRect(box, 28f, 28f, fill)
                c.drawText(w, box.left + 20f, box.centerY() - (text.descent() + text.ascent()) / 2f, text)
                x += widths[i] + gap
            }
        }

        fun centered(c: Canvas, s: String, baseline: Float, size: Float, color: Int, bold: Boolean = false) {
            text.textSize = size; text.color = color; text.typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            while (text.measureText(s) > SIZE - 200f && text.textSize > 18f) text.textSize -= 1f
            c.drawText(s, SIZE / 2f - text.measureText(s) / 2f, baseline, text)
        }
    }
}
