package com.nangmanroad.phone.ui.share

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.nangmanroad.core.data.Category
import com.nangmanroad.core.data.LatLng
import com.nangmanroad.core.data.Product
import com.nangmanroad.core.data.SampleSpots
import com.nangmanroad.core.data.Spot
import com.nangmanroad.core.data.TipKind
import com.nangmanroad.core.data.TipStatus
import com.nangmanroad.core.data.Visibility
import com.nangmanroad.core.map.MapCamera
import com.nangmanroad.core.map.PhotoMap
import com.nangmanroad.core.ui.Palette
import com.nangmanroad.core.ui.SpotPhoto
import com.nangmanroad.core.ui.icon
import com.nangmanroad.core.ui.label
import com.nangmanroad.phone.DraftTip
import com.nangmanroad.phone.PhoneUiState
import com.nangmanroad.phone.PhoneViewModel
import com.nangmanroad.phone.ShareDraft
import com.nangmanroad.phone.data.Regions
import com.nangmanroad.phone.ui.components.Chip
import com.nangmanroad.phone.ui.components.PrimaryButton
import com.nangmanroad.phone.ui.components.SecondaryButton
import com.nangmanroad.phone.ui.components.Tag
import com.nangmanroad.phone.ui.detail.LabeledField
import com.nangmanroad.phone.ui.map.PhoneMapStyle
import java.util.UUID

private val stepTitles = listOf(
    "사진을 골라주세요",
    "장소를 알려주세요",
    "어떤 곳이었나요?",
    "방문 꿀팁을 나눠주세요",
    "챙겨 간 준비물이 있나요?",
    "누구에게 보여줄까요?",
)

/** Sample landscapes for trying the flow on an emulator with an empty gallery. */
private val samplePhotos = SampleSpots.all.flatMap { it.photos }.distinct()

@Composable
fun ShareFlow(draft: ShareDraft, state: PhoneUiState, actions: PhoneViewModel) {
    Column(Modifier.fillMaxSize().imePadding()) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (draft.step == 0) Icons.Outlined.Close else Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = if (draft.step == 0) "닫기" else "이전 단계",
                tint = Palette.TextPrimary,
                modifier = Modifier.size(44.dp).clip(CircleShape).clickable(onClick = actions::draftBack).padding(10.dp),
            )
            Text("장소 공유", style = MaterialTheme.typography.titleMedium, color = Palette.TextPrimary, modifier = Modifier.weight(1f).padding(start = 4.dp))
            Text("${draft.step + 1}/${ShareDraft.STEP_COUNT}", style = MaterialTheme.typography.labelLarge, color = Palette.TextSecondary, modifier = Modifier.padding(end = 12.dp))
        }
        Box(Modifier.fillMaxWidth().height(3.dp).background(Palette.Surface)) {
            Box(Modifier.fillMaxWidth((draft.step + 1f) / ShareDraft.STEP_COUNT).height(3.dp).background(Palette.TextPrimary))
        }
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                Text(stepTitles[draft.step], style = MaterialTheme.typography.headlineMedium, color = Palette.TextPrimary)
            }
            when (draft.step) {
                0 -> item { PhotoStep(draft, actions) }
                1 -> item { PlaceStep(draft, actions) }
                2 -> item { StoryStep(draft, actions) }
                3 -> item { TipStep(draft, actions) }
                4 -> item { ItemStep(draft, state, actions) }
                else -> item { PublishStep(draft, actions) }
            }
        }
        val last = draft.step == ShareDraft.STEP_COUNT - 1
        PrimaryButton(
            text = when {
                last && draft.publishing -> "올리는 중…"
                last -> "공유하기"
                draft.step == 4 && draft.products.isEmpty() -> "건너뛰기"
                else -> "다음 단계"
            },
            onClick = { if (last) actions.publish() else actions.draftNext() },
            enabled = draft.canContinue(),
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        )
    }
}

@Composable
private fun PhotoStep(draft: ShareDraft, actions: PhoneViewModel) {
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(10)) { actions.importPhotos(it) }
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("첫 번째 사진이 지도 마커와 대표 사진이 돼요.", style = MaterialTheme.typography.bodyMedium, color = Palette.TextSecondary)
        if (draft.photos.isNotEmpty()) {
            SpotPhoto(draft.photos.first(), "대표 사진", Modifier.fillMaxWidth().height(260.dp).clip(RoundedCornerShape(10.dp)))
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(draft.photos, key = { it }) { photo ->
                Box {
                    SpotPhoto(photo, null, Modifier.size(84.dp).clip(RoundedCornerShape(10.dp)))
                    Icon(
                        Icons.Outlined.Delete, "사진 빼기", tint = Palette.TextPrimary,
                        modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(28.dp).clip(CircleShape).background(Palette.Scrim)
                            .clickable { actions.removeDraftPhoto(photo) }.padding(5.dp),
                    )
                }
            }
            item {
                Box(
                    Modifier
                        .size(84.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.dp, Palette.SurfaceHigh, RoundedCornerShape(10.dp))
                        .clickable(role = Role.Button, onClickLabel = "기기에서 사진 고르기") {
                            picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    if (draft.importing) CircularProgressIndicator(Modifier.size(28.dp), color = Palette.TextSecondary)
                    else Icon(Icons.Outlined.AddPhotoAlternate, null, tint = Palette.TextSecondary)
                }
            }
        }
        Text("샘플 사진에서 고르기", style = MaterialTheme.typography.titleSmall, color = Palette.TextPrimary)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(samplePhotos) { photo ->
                val chosen = photo in draft.photos
                Box(Modifier.clickable(role = Role.Checkbox) { if (chosen) actions.removeDraftPhoto(photo) else actions.addSamplePhoto(photo) }) {
                    SpotPhoto(photo, null, Modifier.size(width = 96.dp, height = 120.dp).clip(RoundedCornerShape(10.dp)))
                    if (chosen) Icon(Icons.Outlined.CheckCircle, "선택됨", tint = Palette.TextPrimary, modifier = Modifier.align(Alignment.TopEnd).padding(6.dp))
                }
            }
        }
    }
}

@Composable
private fun PlaceStep(draft: ShareDraft, actions: PhoneViewModel) {
    var query by rememberSaveable { mutableStateOf("") }
    val pin = draft.pin
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        if (draft.photoLocation != null) {
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Palette.Success.copy(alpha = 0.14f)).padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(Icons.Outlined.MyLocation, null, tint = Palette.Success)
                Text("사진의 위치를 찾았어요. 틀리면 지도를 눌러 고쳐 주세요.", style = MaterialTheme.typography.bodySmall, color = Palette.TextPrimary)
            }
        }
        LabeledField("시·군 검색", query, "예: 강릉, 태안, 제천") { query = it }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(Regions.search(query).take(12), key = { it.short }) { region ->
                Chip(region.short, draft.region == region, { actions.chooseRegion(region) })
            }
        }
        Text("지도를 눌러 정확한 위치를 찍을 수 있어요.", style = MaterialTheme.typography.bodySmall, color = Palette.TextSecondary)
        val marker = pin?.let { draftMarker(draft, it) }
        PhotoMap(
            spots = listOfNotNull(marker),
            selectedId = marker?.id,
            here = OffMap,
            onSelect = {},
            onVisibleChange = {},
            style = PhoneMapStyle.copy(initialCamera = MapCamera(pin ?: PhoneMapStyle.initialCamera.center, if (pin != null) 2.5f else 0.9f), controlsPadding = PaddingValues(8.dp)),
            onMapTap = actions::dropPin,
            modifier = Modifier.fillMaxWidth().height(320.dp).clip(RoundedCornerShape(10.dp)),
        )
        draft.region?.let {
            Text("선택한 지역: ${it.address}", style = MaterialTheme.typography.titleSmall, color = Palette.TextPrimary)
        }
    }
}

/** The map needs a "here" dot; the share flow has no live location, so park it off-screen. */
private val OffMap = LatLng(0.0, 0.0)

private fun draftMarker(draft: ShareDraft, at: LatLng) = Spot(
    id = "draft", title = draft.title.ifBlank { "새 장소" }, region = draft.region?.short.orEmpty(), address = draft.region?.address.orEmpty(),
    location = at, category = draft.category ?: Category.QUIET, author = SampleSpots.authors.first(), story = "",
    photos = draft.photos.ifEmpty { listOf("") }, likeCount = 0, saveCount = 0, postedDaysAgo = 0,
    tips = emptyList(), products = emptyList(), comments = emptyList(),
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StoryStep(draft: ShareDraft, actions: PhoneViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        LabeledField("제목", draft.title, "예: 파도 소리만 들리는 작은 해변") { title -> actions.updateDraft { it.copy(title = title) } }
        LabeledField("경험", draft.story, "무엇을 보고 어떤 시간을 보냈는지 적어 주세요", minHeight = 120) { story -> actions.updateDraft { it.copy(story = story) } }
        Text("해시태그 대신 실제 경험을 들려주세요. 아래 분류는 지도에서 찾기 쉽게 해 줘요.", style = MaterialTheme.typography.bodySmall, color = Palette.TextSecondary)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Category.entries.forEach { category ->
                Chip(category.label, draft.category == category, { actions.updateDraft { it.copy(category = category) } })
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TipStep(draft: ShareDraft, actions: PhoneViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("다음 방문자가 꼭 알아야 할 정보를 골라 한 줄씩 적어 주세요.", style = MaterialTheme.typography.bodyMedium, color = Palette.TextSecondary)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TipKind.entries.forEach { kind ->
                val chosen = kind in draft.tips
                Chip(kind.label, chosen, {
                    actions.updateDraft { it.copy(tips = if (chosen) it.tips - kind else it.tips + (kind to DraftTip())) }
                }, icon = kind.icon())
            }
        }
        draft.tips.forEach { (kind, tip) ->
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).border(1.dp, Palette.Hairline, RoundedCornerShape(10.dp)).padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(kind.icon(), null, tint = Palette.TextSecondary)
                    Text(kind.label, style = MaterialTheme.typography.titleSmall, color = Palette.TextPrimary, modifier = Modifier.weight(1f))
                    listOf(TipStatus.GOOD, TipStatus.PREPARE).forEach { status ->
                        Chip(status.label(), tip.status == status, { actions.updateDraft { it.copy(tips = it.tips + (kind to tip.copy(status = status))) } })
                    }
                }
                LabeledField("한 줄 꿀팁", tip.summary, placeholderFor(kind)) { summary ->
                    actions.updateDraft { it.copy(tips = it.tips + (kind to tip.copy(summary = summary))) }
                }
            }
        }
    }
}

private fun placeholderFor(kind: TipKind) = when (kind) {
    TipKind.TOILET -> "예: 주변에 화장실이 없어요"
    TipKind.PARKING -> "예: 차 5대 정도 세울 수 있어요"
    TipKind.POWER -> "예: 외부 전원이 없어요"
    TipKind.SIGNAL -> "예: 해변 끝에서는 데이터가 끊겨요"
    TipKind.NIGHT -> "예: 가로등이 없어 아주 어두워요"
    TipKind.STORE -> "예: 편의점까지 차로 10분"
    TipKind.ACCESS -> "예: 마지막 진입로가 좁고 비포장이에요"
    TipKind.CAUTION -> "예: 밀물 때 바위 쪽은 위험해요"
}

@Composable
private fun ItemStep(draft: ShareDraft, state: PhoneUiState, actions: PhoneViewModel) {
    var name by rememberSaveable { mutableStateOf("") }
    var reason by rememberSaveable { mutableStateOf("") }
    var price by rememberSaveable { mutableStateOf("") }
    var sponsored by rememberSaveable { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("직접 써 본 캠핑·여행 용품이 있다면 추천해 주세요. 없으면 건너뛰어도 돼요.", style = MaterialTheme.typography.bodyMedium, color = Palette.TextSecondary)
        draft.products.forEach { product ->
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).border(1.dp, Palette.Hairline, RoundedCornerShape(10.dp)).padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(product.name, style = MaterialTheme.typography.titleSmall, color = Palette.TextPrimary)
                        if (product.isSponsored) Tag("광고·제휴", Palette.Warning)
                    }
                    Text(product.reason, style = MaterialTheme.typography.bodySmall, color = Palette.TextSecondary)
                }
                Icon(Icons.Outlined.Delete, "빼기", tint = Palette.TextSecondary, modifier = Modifier.size(36.dp).clip(CircleShape)
                    .clickable { actions.updateDraft { it.copy(products = it.products - product) } }.padding(6.dp))
            }
        }
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).border(1.dp, Palette.Hairline, RoundedCornerShape(10.dp)).padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            LabeledField("준비물 이름", name, "예: 휴대용 무드 랜턴") { name = it }
            LabeledField("추천 이유", reason, "예: 조명이 없는 곳에서 밤 풍경을 오래 볼 수 있어요") { reason = it }
            LabeledField("가격 (선택, 원)", price, "예: 39000") { price = it.filter(Char::isDigit) }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip("직접 구매", !sponsored, { sponsored = false })
                Chip("광고·제휴", sponsored, { sponsored = true })
            }
            SecondaryButton("준비물 추가", {
                val product = Product(
                    id = "p-${UUID.randomUUID()}",
                    name = name.trim(),
                    reason = reason.trim(),
                    recommender = state.me.name,
                    priceWon = price.toIntOrNull(),
                    isSponsored = sponsored,
                    imageUrl = draft.photos.firstOrNull().orEmpty(),
                )
                actions.updateDraft { it.copy(products = it.products + product) }
                name = ""; reason = ""; price = ""; sponsored = false
            }, Modifier.fillMaxWidth(), enabled = name.isNotBlank() && reason.isNotBlank(), container = Palette.Surface)
        }
    }
}

@Composable
private fun PublishStep(draft: ShareDraft, actions: PhoneViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("공개 범위", style = MaterialTheme.typography.titleSmall, color = Palette.TextPrimary)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Visibility.entries.forEach { visibility ->
                Chip(visibility.label, draft.visibility == visibility, { actions.updateDraft { it.copy(visibility = visibility) } })
            }
        }
        Text("위치 공개", style = MaterialTheme.typography.titleSmall, color = Palette.TextPrimary)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip("정확한 위치", !draft.approximate, { actions.updateDraft { it.copy(approximate = false) } })
            Chip("시·군까지만", draft.approximate, { actions.updateDraft { it.copy(approximate = true) } })
        }
        Text(
            if (draft.approximate) "지도에는 ${draft.region?.short ?: "지역"} 중심에 표시돼요. 숨은 명소를 지키고 싶을 때 좋아요." else "찍은 위치 그대로 지도에 표시돼요.",
            style = MaterialTheme.typography.bodySmall,
            color = Palette.TextSecondary,
        )
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).border(1.dp, Palette.Hairline, RoundedCornerShape(10.dp)).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text("미리보기", style = MaterialTheme.typography.labelMedium, color = Palette.TextSecondary)
            Text(draft.title, style = MaterialTheme.typography.titleLarge, color = Palette.TextPrimary)
            Text("${draft.region?.short} · ${draft.category?.label}", style = MaterialTheme.typography.bodySmall, color = Palette.TextSecondary)
            Text("사진 ${draft.photos.size}장 · 꿀팁 ${draft.tips.size}개 · 준비물 ${draft.products.size}개", style = MaterialTheme.typography.bodySmall, color = Palette.TextSecondary)
        }
    }
}
