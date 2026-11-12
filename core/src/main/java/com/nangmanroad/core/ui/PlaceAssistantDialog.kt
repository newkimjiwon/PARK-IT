package com.nangmanroad.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nangmanroad.core.data.PlaceQuestion
import com.nangmanroad.core.data.placeQuestions

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlaceAssistantDialog(
    placeTitle: String,
    questions: List<PlaceQuestion>,
    onAnswer: (PlaceQuestion) -> String,
    onDismiss: () -> Unit,
    targetHeight: Dp = 48.dp,
) {
    var question by rememberSaveable(placeTitle) { mutableStateOf("") }
    var answer by rememberSaveable(placeTitle) { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(12.dp),
        containerColor = Palette.BackgroundRaised,
        titleContentColor = Palette.TextPrimary,
        textContentColor = Palette.TextSecondary,
        title = { Text("장소 도우미") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(placeTitle, style = MaterialTheme.typography.titleMedium)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    questions.forEach { topic ->
                        OutlinedButton(onClick = { answer = onAnswer(topic) }, modifier = Modifier.heightIn(min = targetHeight)) {
                            Text(topic.label, color = Palette.TextPrimary)
                        }
                    }
                }
                OutlinedTextField(
                    value = question,
                    onValueChange = { question = it.take(120) },
                    label = { Text("예: 밤에도 들어갈 수 있어?") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Palette.TextSecondary,
                        focusedLabelColor = Palette.TextPrimary,
                        unfocusedBorderColor = Palette.Rule,
                        cursorColor = Palette.TextPrimary,
                    ),
                )
                if (answer.isNotEmpty()) {
                    Text(answer, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val topics = placeQuestions(question).filter { it in questions }.take(2)
                    answer = if (topics.isEmpty()) "위 항목 중 궁금한 내용을 골라 주세요."
                    else topics.joinToString("\n") { onAnswer(it) }
                },
                enabled = question.isNotBlank(),
                modifier = Modifier.heightIn(min = targetHeight),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Palette.Accent,
                    contentColor = Palette.OnAccent,
                    disabledContainerColor = Palette.Surface,
                    disabledContentColor = Palette.TextTertiary,
                ),
            ) { Text("묻기") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = targetHeight)) {
                Text("닫기", color = Palette.TextPrimary)
            }
        },
    )
}
