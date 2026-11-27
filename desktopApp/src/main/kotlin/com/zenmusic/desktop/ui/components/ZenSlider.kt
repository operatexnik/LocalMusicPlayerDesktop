package com.zenmusic.desktop.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.zenmusic.desktop.ui.ZenPurple

/**
 * Ползунок без «мёртвых зон» Material3: дорожка начинается от самого края,
 * а бегунок стоит прямо на ней. Поэтому в нуле видно, что значение нулевое,
 * а не «где-то чуть от начала».
 */
@Composable
fun ZenSlider(
    fraction: Float,
    onFractionChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    modifier: Modifier = Modifier,
    activeColor: Color = ZenPurple,
    inactiveColor: Color = Color.White.copy(alpha = 0.14f),
    thumbColor: Color = activeColor,
    thumbHeight: Dp = 15.dp,
    trackHeight: Dp = 4.dp,
) {
    Box(
        modifier = modifier
            .height(24.dp)
            .pointerInput(Unit) {
                val widthPx = size.width.toFloat()
                if (widthPx <= 0f) return@pointerInput

                fun update(positionX: Float) {
                    onFractionChange((positionX / widthPx).coerceIn(0f, 1f))
                }

                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    update(down.position.x)
                    down.consume()

                    var active = true
                    while (active) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (change.pressed) {
                            update(change.position.x)
                            change.consume()
                        } else {
                            update(change.position.x)
                            change.consume()
                            onValueChangeFinished()
                            active = false
                        }
                    }
                }
            },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            if (width <= 0f) return@Canvas

            val centerY = height / 2f
            val track = trackHeight.toPx().coerceAtLeast(1f)
            val thumbWidth = 4.dp.toPx()
            val thumb = thumbHeight.toPx().coerceAtMost(height)
            val value = fraction.coerceIn(0f, 1f)
            val progressX = value * width

            // Неактивная дорожка — во всю ширину, без отступов.
            drawRoundRect(
                color = inactiveColor,
                topLeft = Offset(0f, centerY - track / 2f),
                size = Size(width, track),
                cornerRadius = CornerRadius(track / 2f),
            )

            // Активная часть дорожки заканчивается ровно под бегунком.
            if (progressX > 0f) {
                drawRoundRect(
                    color = activeColor,
                    topLeft = Offset(0f, centerY - track / 2f),
                    size = Size(progressX, track),
                    cornerRadius = CornerRadius(track / 2f),
                )
            }

            // В крайних положениях бегунок прижат к краю, а не «висит» с отступом.
            val thumbLeft = (progressX - thumbWidth / 2f)
                .coerceIn(0f, (width - thumbWidth).coerceAtLeast(0f))
            drawRoundRect(
                color = thumbColor,
                topLeft = Offset(thumbLeft, centerY - thumb / 2f),
                size = Size(thumbWidth, thumb),
                cornerRadius = CornerRadius(thumbWidth / 2f),
            )
        }
    }
}
