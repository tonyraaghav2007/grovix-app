package com.example.grovix.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.grovix.ui.theme.*

@Composable
fun FormattedNoteViewer(
    rawText: String,
    modifier: Modifier = Modifier
) {
    if (rawText.isBlank()) {
        Text("No additional notes provided.", color = GrovixTextMuted, fontSize = 12.sp)
        return
    }

    val paragraphs = rawText.split("\n\n")

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        paragraphs.forEach { paragraph ->
            val lines = paragraph.split("\n")
            lines.forEach { line ->
                val trimmed = line.trim()
                if (trimmed.startsWith("•") || trimmed.startsWith("-") || trimmed.startsWith("*")) {
                    val bulletContent = trimmed.removePrefix("•").removePrefix("-").removePrefix("*").trim()
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(start = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("•", color = GrovixNavy, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(
                            text = parseFormattedText(bulletContent),
                            fontSize = 13.sp,
                            color = GrovixTextDark,
                            lineHeight = 18.sp
                        )
                    }
                } else if (trimmed.startsWith("#")) {
                    val heading = trimmed.removePrefix("#").trim()
                    Text(
                        text = parseFormattedText(heading),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = GrovixNavy,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                } else if (trimmed.isNotBlank()) {
                    Text(
                        text = parseFormattedText(trimmed),
                        fontSize = 13.sp,
                        color = GrovixTextDark,
                        lineHeight = 19.sp
                    )
                }
            }
        }
    }
}

fun parseFormattedText(input: String): AnnotatedString {
    return buildAnnotatedString {
        var cursor = 0
        val len = input.length

        while (cursor < len) {
            // Check for bold **text**
            if (cursor + 1 < len && input.substring(cursor, cursor + 2) == "**") {
                val endBold = input.indexOf("**", cursor + 2)
                if (endBold != -1) {
                    val boldText = input.substring(cursor + 2, endBold)
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = GrovixTextDark)) {
                        append(boldText)
                    }
                    cursor = endBold + 2
                    continue
                }
            }

            // Check for underline <u>text</u>
            if (cursor + 3 <= len && input.substring(cursor).startsWith("<u>", ignoreCase = true)) {
                val endUnderline = input.indexOf("</u>", cursor + 3, ignoreCase = true)
                if (endUnderline != -1) {
                    val uText = input.substring(cursor + 3, endUnderline)
                    withStyle(style = SpanStyle(textDecoration = TextDecoration.Underline, color = GrovixTextDark)) {
                        append(uText)
                    }
                    cursor = endUnderline + 4
                    continue
                }
            }

            // Normal character
            append(input[cursor])
            cursor++
        }
    }
}

