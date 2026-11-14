package com.nangmanroad.phone.ui.detail

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.nangmanroad.core.data.TipKind
import com.nangmanroad.core.sync.Limits
import com.nangmanroad.core.data.TipStatus
import com.nangmanroad.core.ui.Palette
import com.nangmanroad.core.ui.icon
import com.nangmanroad.core.ui.label
import com.nangmanroad.phone.ui.components.Chip
import com.nangmanroad.phone.ui.components.PrimaryButton
import com.nangmanroad.phone.ui.components.SecondaryButton
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Inline form for adding a visitor tip to someone else's post. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TipSuggestionForm(
    onCancel: () -> Unit,
    fieldChange: Boolean = false,
    onSubmit: (TipKind, TipStatus, String, String) -> Unit,
) {
    var change by rememberSaveable { mutableStateOf("시설 변경") }
    var kind by rememberSaveable { mutableStateOf(TipKind.TOILET) }
    var status by rememberSaveable { mutableStateOf(TipStatus.PREPARE) }
    var summary by rememberSaveable { mutableStateOf("") }
    var detail by rememberSaveable { mutableStateOf("") }

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .border(1.dp, Palette.Hairline, RoundedCornerShape(4.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            if (fieldChange) "현장 변경 제보" else "어떤 정보를 알려 주실 건가요?",
            style = MaterialTheme.typography.titleMedium,
            color = Palette.TextPrimary,
        )
        if (fieldChange) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("장소 폐쇄", "시설 변경", "이용 불가").forEach { Chip(it, change == it, { change = it }) }
            }
        } else {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TipKind.entries.forEach { Chip(it.label, kind == it, { kind = it }, icon = it.icon()) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(TipStatus.GOOD, TipStatus.PREPARE).forEach { Chip(it.label(), status == it, { status = it }) }
            }
        }
        val summaryLimit = if (fieldChange) Limits.TIP_SUMMARY - change.length - 2 else Limits.TIP_SUMMARY
        LabeledField("한 줄 요약", summary, "예: 화장실이 없어요", maxLength = summaryLimit) { summary = it }
        LabeledField("자세히 (선택)", detail, "예: 가장 가까운 화장실은 차로 5분 거리 휴게소에 있어요", maxLength = Limits.TIP_DETAIL) { detail = it }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SecondaryButton("취소", onCancel, Modifier.weight(1f))
            PrimaryButton(
                "제보하기",
                {
                    if (fieldChange) onSubmit(TipKind.CAUTION, TipStatus.NEEDS_CHECK, "$change: ${summary.trim()}", detail)
                    else onSubmit(kind, status, summary, detail)
                },
                Modifier.weight(1f),
                enabled = summary.isNotBlank() && summary.length <= summaryLimit,
            )
        }
    }
}

/** Roughly how long the keyboard takes to finish sliding in. */
private const val KEYBOARD_SETTLE_MS = 320L

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LabeledField(
    label: String,
    value: String,
    placeholder: String,
    minHeight: Int = 0,
    /** Same number the server enforces (see Limits); typing stops there and a counter shows near the end. */
    maxLength: Int? = null,
    secure: Boolean = false,
    onChange: (String) -> Unit,
) {
    // The keyboard takes half the screen; once it is up, bring the focused field back into view.
    val bringIntoView = remember { BringIntoViewRequester() }
    val scope = rememberCoroutineScope()
    Column(Modifier.bringIntoViewRequester(bringIntoView), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.labelLarge, color = Palette.TextSecondary, modifier = Modifier.weight(1f))
            if (maxLength != null && value.length >= maxLength * 0.8) {
                Text(
                    "${value.length}/$maxLength",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (value.length >= maxLength) Palette.Warning else Palette.TextTertiary,
                )
            }
        }
        Box(
            Modifier
                .fillMaxWidth()
                .heightIn(min = minHeight.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Palette.Surface)
                .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            if (value.isEmpty()) Text(placeholder, style = MaterialTheme.typography.bodyMedium, color = Palette.TextTertiary)
            BasicTextField(
                value = value,
                onValueChange = { if (maxLength == null || it.length <= maxLength) onChange(it) },
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = Palette.TextPrimary),
                cursorBrush = SolidColor(Palette.TextPrimary),
                visualTransformation = if (secure) PasswordVisualTransformation() else VisualTransformation.None,
                keyboardOptions = if (secure) KeyboardOptions(keyboardType = KeyboardType.Password) else KeyboardOptions.Default,
                singleLine = secure,
                modifier = Modifier.fillMaxWidth().onFocusEvent { focus ->
                    if (focus.isFocused) scope.launch {
                        delay(KEYBOARD_SETTLE_MS)
                        bringIntoView.bringIntoView()
                    }
                },
            )
        }
    }
}
