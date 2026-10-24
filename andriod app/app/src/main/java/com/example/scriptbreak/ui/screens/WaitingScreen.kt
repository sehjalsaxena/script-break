package com.example.scriptbreak.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.scriptbreak.R
import com.example.scriptbreak.ui.components.AppIcons
import com.example.scriptbreak.ui.components.DemoPrimaryButton
import com.example.scriptbreak.ui.theme.AppBackground
import com.example.scriptbreak.ui.theme.InkDark
import com.example.scriptbreak.ui.theme.MutedText
import com.example.scriptbreak.ui.theme.NudgeAmber
import com.example.scriptbreak.ui.theme.NudgeAmberTint
import com.example.scriptbreak.ui.theme.ScriptBreakTheme

@Composable
fun WaitingScreen(
    onHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppBackground)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(60.dp))

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(NudgeAmberTint)
        ) {
            Icon(
                imageVector = AppIcons.HourglassEmpty,
                contentDescription = null,
                tint = NudgeAmber,
                modifier = Modifier.size(44.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(R.string.waiting_title),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = InkDark,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = stringResource(R.string.waiting_body),
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = InkDark,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.waiting_sub),
            fontSize = 16.sp,
            color = MutedText,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )

        Spacer(modifier = Modifier.weight(1f))

        DemoPrimaryButton(
            text = stringResource(R.string.back_to_home),
            onClick = onHome
        )
    }
}

@Preview(showBackground = true)
@Composable
fun WaitingScreenPreview() {
    ScriptBreakTheme {
        WaitingScreen(onHome = {})
    }
}
