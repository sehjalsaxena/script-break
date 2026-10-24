package com.example.scriptbreak.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.scriptbreak.R
import com.example.scriptbreak.ui.components.AppIcons
import com.example.scriptbreak.ui.components.UserAvatar
import com.example.scriptbreak.ui.theme.AppBackground
import com.example.scriptbreak.ui.theme.BodyDark
import com.example.scriptbreak.ui.theme.InkDark
import com.example.scriptbreak.ui.theme.MutedText
import com.example.scriptbreak.ui.theme.NudgeAmber
import com.example.scriptbreak.ui.theme.NudgeAmberTint
import com.example.scriptbreak.ui.theme.ScriptBreakTheme

private data class ProfileItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val initials: String? = null,
    val icon: ImageVector? = null,
    val avatarBg: Color
)

@Composable
fun UserPickerScreen(
    onPick: (profileId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val profiles = listOf(
        ProfileItem(
            id = "sharma",
            title = stringResource(R.string.profile_sharma_title),
            subtitle = stringResource(R.string.profile_sharma_sub),
            initials = "MS",
            avatarBg = Color(0xFFE88A3C)
        ),
        ProfileItem(
            id = "priya",
            title = stringResource(R.string.profile_priya_title),
            subtitle = stringResource(R.string.profile_priya_sub),
            initials = "PS",
            avatarBg = Color(0xFFD0679A)
        ),
        ProfileItem(
            id = "rohit",
            title = stringResource(R.string.profile_rohit_title),
            subtitle = stringResource(R.string.profile_rohit_sub),
            initials = "RS",
            avatarBg = Color(0xFF3D8FD1)
        ),
        ProfileItem(
            id = "judge",
            title = stringResource(R.string.profile_judge_title),
            subtitle = stringResource(R.string.profile_judge_sub),
            icon = AppIcons.Assessment,
            avatarBg = Color(0xFF2C6ECB)
        ),
        ProfileItem(
            id = "analyst",
            title = stringResource(R.string.profile_analyst_title),
            subtitle = stringResource(R.string.profile_analyst_sub),
            icon = AppIcons.Description,
            avatarBg = Color(0xFF4B5563)
        )
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppBackground)
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(R.string.user_picker_title),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = InkDark
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = stringResource(R.string.user_picker_subtitle),
            fontSize = 16.sp,
            color = MutedText
        )

        Spacer(modifier = Modifier.height(20.dp))

        LazyColumn(
            modifier = Modifier.weight(1f)
        ) {
            items(profiles.size) { index ->
                val profile = profiles[index]
                ProfileCard(
                    profile = profile,
                    onClick = { onPick(profile.id) },
                    modifier = Modifier.padding(vertical = 6.dp)
                )
            }
        }

        // Demo disclaimer chip at bottom
        Surface(
            color = NudgeAmberTint,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(NudgeAmber)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.demo_banner),
                    fontSize = 14.sp,
                    color = BodyDark,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
private fun ProfileCard(
    profile: ProfileItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 1.dp,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            if (profile.initials != null) {
                UserAvatar(
                    initials = profile.initials,
                    backgroundColor = profile.avatarBg,
                    size = 46.dp
                )
            } else if (profile.icon != null) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(profile.avatarBg)
                ) {
                    Icon(
                        imageVector = profile.icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = profile.title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = InkDark
                )
                Text(
                    text = profile.subtitle,
                    fontSize = 14.sp,
                    color = MutedText,
                    lineHeight = 18.sp
                )
            }

            Icon(
                imageVector = AppIcons.ChevronRight,
                contentDescription = null,
                tint = MutedText,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun UserPickerScreenPreview() {
    ScriptBreakTheme {
        UserPickerScreen(onPick = {})
    }
}
