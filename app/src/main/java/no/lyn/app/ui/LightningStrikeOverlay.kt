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

            val alpha = when {
                ageMin < 3  -> 255
                ageMin < 15 -> (255 * (1.0 - (ageMin - 3) / 12.0)).toInt().coerceIn(60, 255)
                ageMin < 30 -> (255 * (1.0 - (ageMin - 15) / 15.0)).toInt().coerceIn(0, 60)
                else        -> continue
            }

            val (r, g, b) = when {
                ageMin < 5  -> Triple(255, 214, 10)  // electric yellow
                ageMin < 15 -> Triple(255, 140, 0)   // orange
                else        -> Triple(255, 68,  68)  // red
            }

            val point = projection.toPixels(GeoPoint(strike.lat, strike.lon), null)
            val px = point.x.toFloat()
            val py = point.y.toFloat()

            // Soft glow halo
            glowPaint.color = Color.argb(alpha / 5, r, g, b)
            canvas.drawCircle(px, py, 10f, glowPaint)

            // Core dot
            fillPaint.color = Color.argb(alpha, r, g, b)
            canvas.drawCircle(px, py, 3.5f, fillPaint)
        }
    }
}
