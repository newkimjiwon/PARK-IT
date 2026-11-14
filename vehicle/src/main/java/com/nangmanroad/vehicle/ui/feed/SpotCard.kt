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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.rounded.FavoriteBorder
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
import com.nangmanroad.core.ui.FacilityStrip
import com.nangmanroad.core.ui.SpotPhoto
import com.nangmanroad.vehicle.ui.theme.Dimens
import com.nangmanroad.core.ui.Palette
import com.nangmanroad.core.ui.tabular

/** Photo-first post card used by the map feed and the album grid. */
@Composable
fun SpotCard(
    spot: Spot,
    likeCount: Int,
    saved: Boolean,
    onOpen: () -> Unit,
    onToggleSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(Dimens.PhotoRadius))
            .clickable(role = Role.Button, onClickLabel = "${spot.title} 보기", onClick = onOpen),
    ) {
        Box {
            SpotPhoto(
                url = spot.photos.first(),
                contentDescription = spot.title,
                modifier = Modifier
                    .fillMaxWidth()
                    // Every photo frame is square, so grids line up into a steady rhythm.
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(Dimens.PhotoRadius)),
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(56.dp)
                    .clip(RoundedCornerShape(Dimens.ControlRadius))
                    .background(Palette.Scrim)
                    .clickable(role = Role.Button, onClickLabel = if (saved) "저장 취소" else "저장", onClick = onToggleSave),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (saved) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                    contentDescription = if (saved) "저장됨" else "저장",
                    tint = Palette.TextPrimary,
                    modifier = Modifier.size(28.dp),
                )
            }
        }
        Column(Modifier.padding(horizontal = 2.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(spot.title, style = MaterialTheme.typography.titleSmall, color = Palette.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                "${spot.region} · ${spot.author.name}",
                style = MaterialTheme.typography.bodySmall,
                color = Palette.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.size(2.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Stat(Icons.Rounded.FavoriteBorder, likeCount.toString())
                Stat(Icons.Outlined.ChatBubbleOutline, spot.commentCount.toString())
                Stat(Icons.Outlined.TipsAndUpdates, "꿀팁 ${spot.tips.size}")
            }
            FacilityStrip(spot.tips, Modifier.padding(top = 6.dp), iconSize = 20.dp, gap = 6.dp)
        }
    }
}

@Composable
fun Stat(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Icon(icon, contentDescription = null, tint = Palette.TextTertiary, modifier = Modifier.size(22.dp))
        Text(text, style = MaterialTheme.typography.labelMedium.tabular(), color = Palette.TextSecondary)
    }
}
