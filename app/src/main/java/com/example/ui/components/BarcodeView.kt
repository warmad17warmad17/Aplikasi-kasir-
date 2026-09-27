package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.absoluteValue

/**
 * Draws a clean Barcode visual with guard bars and checksum pattern
 */
@Composable
fun BarcodeVisual(
    barcode: String,
    modifier: Modifier = Modifier,
    showText: Boolean = true
) {
    val barPattern = remember(barcode) {
        generateBarPattern(barcode)
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
        ) {
            val totalBars = barPattern.size
            if (totalBars == 0) return@Canvas
            val barWidth = size.width / totalBars

            barPattern.forEachIndexed { index, isBlack ->
                if (isBlack) {
                    drawRect(
                        color = Color.Black,
                        topLeft = Offset(index * barWidth, 0f),
                        size = Size(barWidth.coerceAtLeast(1.5f), size.height)
                    )
                }
            }
        }
        if (showText) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = barcode,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 2.sp,
                    color = Color.Black
                )
            )
        }
    }
}

/**
 * Generates a deterministic barcode pattern for any alphanumeric barcode.
 */
private fun generateBarPattern(code: String): List<Boolean> {
    val pattern = mutableListOf<Boolean>()
    // Start guard: 101
    pattern.addAll(listOf(true, false, true))

    val cleanCode = code.ifEmpty { "12345678" }
    var hash = cleanCode.hashCode().absoluteValue
    for (char in cleanCode) {
        val charVal = char.code
        val bits = (charVal xor (hash and 0x7F))
        for (i in 0 until 6) {
            pattern.add(((bits shr i) and 1) == 1)
        }
        pattern.add(false) // separator
        hash = (hash * 31 + charVal).absoluteValue
    }

    // Center guard: 01010
    pattern.addAll(listOf(false, true, false, true, false))

    for (char in cleanCode.reversed()) {
        val charVal = char.code
        val bits = (charVal xor (hash and 0x3F))
        for (i in 0 until 6) {
            pattern.add(((bits shr i) and 1) == 1)
        }
        pattern.add(false)
    }

    // End guard: 101
    pattern.addAll(listOf(true, false, true))
    return pattern
}

/**
 * Draws a clean QR code style matrix representation with corner targets.
 */
@Composable
fun QrCodeVisual(
    data: String,
    modifier: Modifier = Modifier,
    sizeDp: Int = 140
) {
    val matrixSize = 25
    val grid = remember(data) {
        generateQrMatrix(data, matrixSize)
    }

    Box(
        modifier = modifier
            .size(sizeDp.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(8.dp))
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size((sizeDp - 16).dp)) {
            val cellSize = size.width / matrixSize

            for (row in 0 until matrixSize) {
                for (col in 0 until matrixSize) {
                    if (grid[row][col]) {
                        drawRect(
                            color = Color.Black,
                            topLeft = Offset(col * cellSize, row * cellSize),
                            size = Size(cellSize + 0.5f, cellSize + 0.5f)
                        )
                    }
                }
            }
        }
    }
}

private fun generateQrMatrix(content: String, size: Int): Array<BooleanArray> {
    val matrix = Array(size) { BooleanArray(size) }

    fun drawFinder(topRow: Int, leftCol: Int) {
        for (r in 0 until 7) {
            for (c in 0 until 7) {
                val isOuter = r == 0 || r == 6 || c == 0 || c == 6
                val isInner = r in 2..4 && c in 2..4
                matrix[topRow + r][leftCol + c] = isOuter || isInner
            }
        }
    }

    // Top-left, top-right, bottom-left finders
    drawFinder(0, 0)
    drawFinder(0, size - 7)
    drawFinder(size - 7, 0)

    // Timing lines
    for (i in 8 until size - 8) {
        matrix[6][i] = (i % 2 == 0)
        matrix[i][6] = (i % 2 == 0)
    }

    // Fill data area with deterministic hash bits
    val seed = content.ifEmpty { "QR_DATA" }
    var hash = seed.hashCode().absoluteValue
    for (r in 0 until size) {
        for (c in 0 until size) {
            val inFinder1 = r < 8 && c < 8
            val inFinder2 = r < 8 && c >= size - 8
            val inFinder3 = r >= size - 8 && c < 8
            val inTiming = r == 6 || c == 6

            if (!inFinder1 && !inFinder2 && !inFinder3 && !inTiming) {
                hash = (hash * 1103515245 + 12345).absoluteValue
                val bit = ((hash shr (r + c)) and 1) == 1
                matrix[r][c] = bit
            }
        }
    }
    return matrix
}
