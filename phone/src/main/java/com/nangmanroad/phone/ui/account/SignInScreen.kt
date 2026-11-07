package com.nangmanroad.phone.ui.account

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.nangmanroad.core.ui.Palette
import com.nangmanroad.phone.AccountUi
import com.nangmanroad.phone.PhoneViewModel
import com.nangmanroad.phone.ui.components.PrimaryButton
import com.nangmanroad.phone.ui.detail.LabeledField

@Composable
fun SignInScreen(account: AccountUi, actions: PhoneViewModel) {
    var nickname by rememberSaveable { mutableStateOf("윤슬을따라") }
    Column(
        Modifier
            .fillMaxSize()
            .imePadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Spacer(Modifier.weight(1f))
        Box(Modifier.size(64.dp).clip(RoundedCornerShape(18.dp)).background(Palette.Coral), contentAlignment = Alignment.Center) {
            Box(Modifier.size(20.dp).clip(CircleShape).background(Palette.Background))
        }
        Text("낭만로드", style = MaterialTheme.typography.headlineLarge, color = Palette.TextPrimary)
        Text(
            "휴대폰과 차량이 같은 계정을 쓰면 저장한 장소, 내 게시물, 보낸 장소가 함께 보여요.",
            style = MaterialTheme.typography.bodyLarge,
            color = Palette.TextSecondary,
        )
        LabeledField("닉네임", nickname, "2~20자") { nickname = it }
        Text(
            "개발용 서버라 비밀번호 없이 닉네임으로 들어가요. '윤슬을따라'는 샘플 게시물이 있는 데모 계정이에요.",
            style = MaterialTheme.typography.bodySmall,
            color = Palette.TextTertiary,
        )
        account.error?.let { error ->
            Text(
                "$error\n서버 주소: ${account.serverUrl}\n저장소 루트에서 sh gradlew :server:run 으로 서버를 켜 주세요.",
                style = MaterialTheme.typography.bodySmall,
                color = Palette.Coral,
            )
        }
        PrimaryButton(
            text = if (account.busy) "연결하는 중…" else "시작하기",
            onClick = { actions.signIn(nickname) },
            enabled = !account.busy && nickname.trim().length in 2..20,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            "서버 없이 둘러보기",
            style = MaterialTheme.typography.labelLarge,
            color = Palette.TextSecondary,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .clip(RoundedCornerShape(10.dp))
                .clickable(role = Role.Button, onClick = actions::continueOffline)
                .padding(horizontal = 12.dp, vertical = 10.dp),
        )
        Spacer(Modifier.weight(1f))
    }
}
