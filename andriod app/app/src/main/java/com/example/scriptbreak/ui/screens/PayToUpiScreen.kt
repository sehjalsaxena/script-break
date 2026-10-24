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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.example.scriptbreak.ui.theme.AppBlue
import com.example.scriptbreak.ui.theme.AppBlueLight
import com.example.scriptbreak.ui.theme.AppSurface
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
    // Picking from the app's own recent list is not a paste (it changes the "pasted" signal)
    onPickSuggestion: ((String) -> Unit)? = null,
    recentPayees: List<Pair<String, String>> = listOf(
        "suresh.verma@demoupi" to "Suresh Verma",
        "sunita.iyer@demoupi" to "Sunita Iyer",
        "sehjal.saxena@demoupi" to "Sehjal Saxena"
    ),
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
            color = AppSurface,
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
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, autoCorrectEnabled = false),
                    placeholder = {
                        Text("ramesh.k@demoupi", fontSize = 16.sp, color = MutedText.copy(alpha = 0.6f))
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = AppIcons.AlternateEmail,
                            contentDescription = null,
                            tint = AppBlue
                        )
                    },
                    trailingIcon = {
                        IconButton(onClick = onPaste) {
                            Icon(
                                imageVector = AppIcons.ContentPaste,
                                contentDescription = "Paste",
                                tint = AppBlue,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = InkDark,
                        unfocusedTextColor = InkDark,
                        focusedBorderColor = AppBlue,
                        unfocusedBorderColor = com.example.scriptbreak.ui.theme.LineBorder,
                        cursorColor = AppBlue,
                        focusedContainerColor = AppSurface,
                        unfocusedContainerColor = AppSurface
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

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            recentPayees.forEach { (id, name) ->
                Surface(
                    color = AppSurface,
                    shape = RoundedCornerShape(14.dp),
                    shadowElevation = 0.5.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { (onPickSuggestion ?: onUpiIdChange)(id) }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(AppBlueLight)
                        ) {
                            Text(
                                text = name.split(" ").mapNotNull { it.firstOrNull()?.toString() }.joinToString(""),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = AppBlue
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = name,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = InkDark
                            )
                            Spacer(modifier = Modifier.height(2.dp))
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
            enabled = upiId.isNotBlank(),
            modifier = Modifier.navigationBarsPadding()
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
