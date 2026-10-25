package com.example.scriptbreak.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.scriptbreak.ui.theme.AppBlue
import com.example.scriptbreak.ui.theme.AppOrange
import com.example.scriptbreak.ui.theme.AppPurple
import com.example.scriptbreak.ui.theme.CallGreen
import com.example.scriptbreak.ui.theme.CallGreenTint
import com.example.scriptbreak.ui.theme.InkDark
import com.example.scriptbreak.ui.theme.MutedText
import com.example.scriptbreak.ui.theme.AppSurface
import com.example.scriptbreak.ui.theme.AppBlueElectric
import com.example.scriptbreak.ui.theme.LocalDarkTheme
import com.example.scriptbreak.ui.theme.SafeGreen
import com.example.scriptbreak.ui.theme.SafeGreenTint

@Composable
fun DemoPayLogoText(
    modifier: Modifier = Modifier,
    fontSize: androidx.compose.ui.unit.TextUnit = 22.sp,
    fontWeight: FontWeight = FontWeight.Bold
) {
    val isDark = LocalDarkTheme.current
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        Text(
            text = "Demo",
            fontSize = fontSize,
            fontWeight = fontWeight,
            color = if (isDark) Color(0xFFF1F5F9) else com.example.scriptbreak.ui.theme.AppBluePrimary,
            letterSpacing = (-0.5).sp
        )
        Text(
            text = "Pay",
            fontSize = fontSize,
            fontWeight = fontWeight,
            color = if (isDark) Color(0xFFFF8533) else com.example.scriptbreak.ui.theme.AppOrangePrimary,
            letterSpacing = (-0.5).sp
        )
    }
}

/**
 * [centerContent] replaces the title (the Amount screen puts the payee there on short phones).
 * On a narrow bar that also has [trailingContent], the ScriptBreak badge shrinks to its shield so the title fits.
 */
@Composable
fun AppTopBar(
    title: String = "",
    onBack: (() -> Unit)? = null,
    showScriptBreakBadge: Boolean = false,
    modifier: Modifier = Modifier,
    centerContent: @Composable (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val compactBadge = maxWidth < 400.dp && (trailingContent != null || centerContent != null)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = AppIcons.ArrowBack,
                        contentDescription = "Back",
                        tint = InkDark
                    )
                }
            } else {
                Spacer(modifier = Modifier.width(8.dp))
            }

            if (centerContent != null) {
                Box(modifier = Modifier.weight(1f).padding(horizontal = 4.dp)) { centerContent() }
            } else if (title.isNotEmpty()) {
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = InkDark,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 4.dp)
                )
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }

            if (showScriptBreakBadge) {
                ScriptBreakBadge(iconOnly = compactBadge)
            }

            trailingContent?.invoke()
        }
    }
}

@Composable
fun ScriptBreakBadge(modifier: Modifier = Modifier, iconOnly: Boolean = false) {
    Surface(
        color = SafeGreenTint,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Icon(
                imageVector = AppIcons.Security,
                contentDescription = if (iconOnly) "Protected by ScriptBreak" else null,
                tint = SafeGreen,
                modifier = Modifier.size(14.dp)
            )
            if (!iconOnly) {
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "ScriptBreak",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SafeGreen
                )
            }
        }
    }
}

@Composable
fun UserAvatar(
    initials: String,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    backgroundColor: Color = AppBlue,
    textColor: Color = Color.White
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(backgroundColor)
    ) {
        Text(
            text = initials,
            fontSize = (size.value * 0.4).sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}

@Composable
fun DemoPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerColor: Color = AppBlue,
    contentColor: Color = Color.White
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(24.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = containerColor.copy(alpha = 0.4f),
            disabledContentColor = contentColor.copy(alpha = 0.6f)
        ),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
    ) {
        Text(
            text = text,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun DemoSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(24.dp),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
    ) {
        Text(
            text = text,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = InkDark
        )
    }
}

@Composable
fun CallStatusBanner(
    modifier: Modifier = Modifier,
    title: String = "Active phone call detected",
    subtitle: String = "Live protection active on this payment",
    icon: androidx.compose.ui.graphics.vector.ImageVector = AppIcons.Phone,
) {
    Surface(
        color = CallGreenTint,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(CallGreen)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = CallGreen
                )
                Text(
                    text = subtitle,
                    fontSize = 14.sp,
                    color = MutedText
                )
            }
        }
    }
}

/** [keyHeight] and [spacing] shrink on short phones (see [isShortScreen]) so the lines above the keypad stay visible. */
@Composable
fun NumericKeypad(
    onDigit: (Int) -> Unit,
    onDelete: () -> Unit,
    onPeriod: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    keyHeight: Dp = 56.dp,
    spacing: Dp = 8.dp,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(spacing)
    ) {
        val rows = listOf(
            listOf(1, 2, 3),
            listOf(4, 5, 6),
            listOf(7, 8, 9)
        )

        for (row in rows) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                for (digit in row) {
                    KeypadButton(
                        onClick = { onDigit(digit) },
                        height = keyHeight,
                        modifier = Modifier.weight(1f)
                    ) { KeyText(digit.toString()) }
                }
            }
        }

        // Bottom row: period or empty, 0, backspace
        val haptic = LocalHapticFeedback.current
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp, vertical = 2.dp)
                    .height(keyHeight)
                    .clip(RoundedCornerShape(14.dp))
                    .clickable(enabled = onPeriod != null) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onPeriod?.invoke()
                    },
                contentAlignment = Alignment.Center
            ) {
                if (onPeriod != null) {
                    Text(
                        text = ".",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = InkDark
                    )
                }
            }

            KeypadButton(
                onClick = { onDigit(0) },
                height = keyHeight,
                modifier = Modifier.weight(1f)
            ) { KeyText("0") }

            // Same key as the digits (an earlier see-through background showed a white box behind the icon)
            KeypadButton(
                onClick = onDelete,
                height = keyHeight,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = AppIcons.Backspace,
                    contentDescription = "Delete",
                    tint = InkDark,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
private fun KeyText(text: String) {
    Text(
        text = text,
        fontSize = 22.sp,
        fontWeight = FontWeight.SemiBold,
        color = InkDark
    )
}

@Composable
private fun KeypadButton(
    onClick: () -> Unit,
    height: Dp,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    Surface(
        color = AppSurface,
        shape = RoundedCornerShape(14.dp),
        shadowElevation = 0.5.dp,
        modifier = modifier
            .padding(horizontal = 4.dp, vertical = 2.dp)
            .height(height)
            .clip(RoundedCornerShape(14.dp))
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            content()
        }
    }
}

/** Phones with less than this much height for a screen get the compact keypad layout (e.g. 360×640, 360×740). */
val SHORT_SCREEN_HEIGHT = 720.dp

/** True on short phones: keypad screens then use smaller keys and put the payee in the top bar. */
fun isShortScreen(maxHeight: Dp): Boolean = maxHeight < SHORT_SCREEN_HEIGHT
