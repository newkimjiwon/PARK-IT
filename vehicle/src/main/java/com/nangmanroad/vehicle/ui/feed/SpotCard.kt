package com.nangmanroad.vehicle.ui.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.TipsAndUpdates
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nangmanroad.core.data.Spot
import com.nangmanroad.core.ui.Avatar
import com.nangmanroad.core.ui.SpotPhoto
import com.nangmanroad.vehicle.ui.theme.Dimens
import com.nangmanroad.core.ui.Palette

/** Photo-first post card used by the map feed and the album grid. */
@Composable
fun SpotCard(
    spot: Spot,
    likeCount: Int,
    saved: Boolean,
    photoAspect: Float,
    onOpen: () -> Unit,
    onToggleSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(Dimens.CardRadius))
            .clickable(role = Role.Button, onClickLabel = "${spot.title} 보기", onClick = onOpen),
    ) {
        Box {
            SpotPhoto(
                url = spot.photos.first(),
                contentDescription = spot.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(photoAspect)
                    .clip(RoundedCornerShape(Dimens.CardRadius)),
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp)
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Palette.Scrim)
                    .clickable(role = Role.Button, onClickLabel = if (saved) "저장 취소" else "저장", onClick = onToggleSave),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (saved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                    contentDescription = if (saved) "저장됨" else "저장",
                    tint = if (saved) Palette.Coral else Palette.TextPrimary,
                    modifier = Modifier.size(28.dp),
                )
            }
        }
        Column(Modifier.padding(horizontal = 4.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(spot.title, style = MaterialTheme.typography.titleSmall, color = Palette.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(spot.region, style = MaterialTheme.typography.bodySmall, color = Palette.TextSecondary)
            Spacer(Modifier.size(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Avatar(spot.author.name, size = 32.dp)
                Spacer(Modifier.width(8.dp))
                Text(
                    spot.author.name,
                    style = MaterialTheme.typography.labelMedium,
                    color = Palette.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Stat(Icons.Outlined.FavoriteBorder, likeCount.toString())
                Stat(Icons.Outlined.ChatBubbleOutline, spot.commentCount.toString())
                Stat(Icons.Outlined.TipsAndUpdates, "꿀팁 ${spot.tips.size}", highlight = true)
            }
        }
    }
}

@Composable
fun Stat(icon: ImageVector, text: String, highlight: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(icon, contentDescription = null, tint = if (highlight) Palette.Peach else Palette.TextSecondary, modifier = Modifier.size(22.dp))
        Text(text, style = MaterialTheme.typography.labelMedium, color = if (highlight) Palette.Peach else Palette.TextSecondary)
    }
}
