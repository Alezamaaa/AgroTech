
package mx.edu.agrotech.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.view.View

/**
 * Iconos vectoriales pequeños, sin fuentes externas ni imágenes descargadas.
 */
class LineIcon(context: Context, private val kind: String, color: Int) : View(context) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        style = Paint.Style.STROKE
        strokeWidth = 1.8f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    init {
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        canvas.save()
        canvas.translate(paddingLeft.toFloat(), paddingTop.toFloat())
        canvas.scale(
            (width - paddingLeft - paddingRight) / 24f,
            (height - paddingTop - paddingBottom) / 24f
        )

        when (kind) {
            "plus" -> {
                canvas.drawLine(12f, 4f, 12f, 20f, paint)
                canvas.drawLine(4f, 12f, 20f, 12f, paint)
            }

            "search" -> {
                canvas.drawCircle(10f, 10f, 6.5f, paint)
                canvas.drawLine(15f, 15f, 21f, 21f, paint)
            }

            "chart" -> {
                canvas.drawLine(5f, 20f, 5f, 14f, paint)
                canvas.drawLine(12f, 20f, 12f, 9f, paint)
                canvas.drawLine(19f, 20f, 19f, 3f, paint)
            }

            "arrow" -> path(
                canvas,
                8f to 4f,
                12f to 12f,
                8f to 20f
            )

            "home" -> {
                path(canvas, 3f to 11f, 12f to 3f, 21f to 11f)
                path(
                    canvas,
                    5f to 10f,
                    5f to 21f,
                    10f to 21f,
                    10f to 15f,
                    14f to 15f,
                    14f to 21f,
                    19f to 21f,
                    19f to 10f
                )
            }

            "sprout" -> {
                canvas.drawLine(12f, 21f, 12f, 11f, paint)

                canvas.drawPath(Path().apply {
                    moveTo(12f, 13f)
                    cubicTo(2f, 14f, 2f, 4f, 3f, 3f)
                    cubicTo(11f, 2f, 14f, 8f, 12f, 13f)
                }, paint)

                canvas.drawPath(Path().apply {
                    moveTo(12f, 11f)
                    cubicTo(12f, 3f, 19f, 2f, 22f, 3f)
                    cubicTo(23f, 10f, 17f, 14f, 12f, 11f)
                }, paint)
            }

            // Nuevo: configuración
            "settings" -> {
                val gear = Path()

                val radii = listOf(
                    8f, 10.5f, 8f, 10.5f,
                    8f, 10.5f, 8f, 10.5f,
                    8f, 10.5f, 8f, 10.5f,
                    8f, 10.5f, 8f, 10.5f
                )

                for (i in radii.indices) {
                    val angle = Math.toRadians(i * 22.5 - 90.0)
                    val x = (12 + radii[i] * kotlin.math.cos(angle)).toFloat()
                    val y = (12 + radii[i] * kotlin.math.sin(angle)).toFloat()

                    if (i == 0) gear.moveTo(x, y)
                    else gear.lineTo(x, y)
                }

                gear.close()
                canvas.drawPath(gear, paint)
                canvas.drawCircle(12f, 12f, 3.2f, paint)
            }

            // Nuevo: usuario
            "user" -> {
                canvas.drawCircle(12f, 7.5f, 3.5f, paint)
                canvas.drawArc(4f, 13f, 20f, 23f, 180f, 180f, false, paint)
            }

            // Nuevo: cerrar sesión
            "logout" -> {
                canvas.drawLine(10f, 4f, 5f, 4f, paint)
                canvas.drawLine(5f, 4f, 5f, 20f, paint)
                canvas.drawLine(5f, 20f, 10f, 20f, paint)
                canvas.drawLine(13f, 12f, 22f, 12f, paint)
                canvas.drawLine(18f, 8f, 22f, 12f, paint)
                canvas.drawLine(18f, 16f, 22f, 12f, paint)
            }
        }

        canvas.restore()
    }

    private fun path(canvas: Canvas, vararg points: Pair<Float, Float>) {
        val path = Path()

        points.forEachIndexed { index, (x, y) ->
            if (index == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }

        canvas.drawPath(path, paint)
    }
}
