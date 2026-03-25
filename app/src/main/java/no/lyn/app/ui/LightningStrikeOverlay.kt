package no.lyn.app.ui

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import no.lyn.app.data.LightningStrike
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Overlay

/**
 * Custom OSMDroid overlay that draws age-coloured dots for each lightning strike.
 * Yellow = recent (< 5 min), Orange = 5–15 min, Red = 15–30 min, fades out.
 */
class LightningStrikeOverlay : Overlay() {

    @Volatile
    private var strikes: List<LightningStrike> = emptyList()

    // Pre-computed opaque base colors — alpha is set separately per-strike via Paint.alpha,
    // avoiding Triple/object allocation inside the per-frame draw loop.
    private val colorYoung = Color.rgb(255, 214, 10)
    private val colorMid   = Color.rgb(255, 140,  0)
    private val colorOld   = Color.rgb(255,  68, 68)

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

    fun setStrikes(newStrikes: List<LightningStrike>) {
        strikes = newStrikes
    }

    override fun draw(canvas: Canvas, mapView: MapView, shadow: Boolean) {
        if (shadow) return
        val projection = mapView.projection
        val now = System.currentTimeMillis()

        for (strike in strikes) {
            val ageMin = (now - strike.timeMs) / 60_000.0
            if (ageMin >= 30) continue

            val baseColor: Int
            val alpha: Int
            when {
                ageMin < 3  -> { baseColor = colorYoung; alpha = 255 }
                ageMin < 15 -> { baseColor = colorMid;   alpha = (255 * (1.0 - (ageMin - 3) / 12.0)).toInt().coerceIn(60, 255) }
                else        -> { baseColor = colorOld;   alpha = (255 * (1.0 - (ageMin - 15) / 15.0)).toInt().coerceIn(0, 60) }
            }

            val point = projection.toPixels(GeoPoint(strike.lat, strike.lon), null)
            val px = point.x.toFloat()
            val py = point.y.toFloat()

            glowPaint.color = baseColor
            glowPaint.alpha = alpha / 5
            canvas.drawCircle(px, py, 10f, glowPaint)

            fillPaint.color = baseColor
            fillPaint.alpha = alpha
            canvas.drawCircle(px, py, 3.5f, fillPaint)
        }
    }
}
