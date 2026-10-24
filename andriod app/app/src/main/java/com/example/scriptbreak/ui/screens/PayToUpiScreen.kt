package com.example.scriptbreak.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.scriptbreak.R
import com.example.scriptbreak.ui.components.AppIcons
import com.example.scriptbreak.ui.components.AppTopBar
import com.example.scriptbreak.ui.components.DemoPrimaryButton
import com.example.scriptbreak.ui.theme.AppBackground
import com.example.scriptbreak.ui.theme.AppPurple
import com.example.scriptbreak.ui.theme.AppPurpleLight
import com.example.scriptbreak.ui.theme.InkDark
import com.example.scriptbreak.ui.theme.MutedText
import com.example.scriptbreak.ui.theme.ScriptBreakTheme

@Composable
fun PayToUpiScreen(
    upiId: String,
    onUpiIdChange: (String) -> Unit,
    onPaste: () -> Unit,
    onContinue: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppBackground)
            .padding(16.dp)
    ) {
        AppTopBar(
            title = stringResource(R.string.pay_upi_title),
            onBack = onBack,
            showScriptBreakBadge = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        Surface(
            color = Color.White,
            shape = RoundedCornerShape(16.dp),
            shadowElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(R.string.pay_upi_hint),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = MutedText
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = upiId,
                    onValueChange = onUpiIdChange,
                    singleLine = true,
                    placeholder = {
                        Text("ramesh.k@demoupi", fontSize = 16.sp, color = MutedText.copy(alpha = 0.6f))
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = AppIcons.AlternateEmail,
                            contentDescription = null,
                            tint = AppPurple
                        )
                    },
                    trailingIcon = {
                        TextButton(onClick = onPaste) {
                            Icon(
                                imageVector = AppIcons.ContentPaste,
                                contentDescription = null,
                                tint = AppPurple,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.paste),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = AppPurple
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AppPurple,
                        unfocusedBorderColor = Color(0xFFDDE2EA),
                        cursorColor = AppPurple
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(R.string.recent_upi_ids),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = InkDark
        )

        Spacer(modifier = Modifier.height(10.dp))

        val suggestions = listOf(
            "ramesh.k@demoupi" to "Ramesh K.",
            "anil.mehta@demoupi" to "Anil Mehta",
            "suresh.verma@demoupi" to "Suresh Verma"
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            suggestions.forEach { (id, name) ->
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(12.dp),
                    shadowElevation = 0.5.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onUpiIdChange(id) }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(AppPurpleLight)
                        ) {
                            Text(
                                text = name.split(" ").mapNotNull { it.firstOrNull()?.toString() }.joinToString(""),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = AppPurple
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = name,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = InkDark
                            )
                            Text(
                                text = id,
                                fontSize = 14.sp,
                                color = MutedText
                            )
                        }
                    }
                }
            }
        }

        DemoPrimaryButton(
            text = stringResource(R.string.continue_btn),
            onClick = onContinue,
            enabled = upiId.isNotBlank()
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PayToUpiScreenPreview() {
    ScriptBreakTheme {
        PayToUpiScreen(
            upiId = "ramesh.k@demoupi",
            onUpiIdChange = {},
            onPaste = {},
            onContinue = {},
            onBack = {}
        )
    }
}
