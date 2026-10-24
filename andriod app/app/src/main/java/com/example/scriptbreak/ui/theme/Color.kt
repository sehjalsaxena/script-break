package com.example.scriptbreak.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

// DemoPay brand palette constants
val AppBluePrimary = Color(0xFF10223F)        // Rich midnight navy blue (light mode)
val AppBlueDarkElectric = Color(0xFFA8C7FA)   // Google Pay Material 3 light blue 300 (dark mode)
val AppBlueDark = Color(0xFF0A1629)
val AppBlueElectric = Color(0xFFA8C7FA)       // Consistent with Google Blue 300

val AppOrangePrimary = Color(0xFFFD600D)      // Vibrant brand orange from the logo
val AppOrangeDarkNeon = Color(0xFFFF8533)     // Google Pay glowing warm orange (dark mode)
val AppOrangeDark = Color(0xFFE05004)

// Dynamic brand tokens: preserve light mode colors, elevate dark mode to Google Pay standards
val AppBlue: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalDarkTheme.current) AppBlueDarkElectric else AppBluePrimary

val AppOrange: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalDarkTheme.current) AppOrangeDarkNeon else AppOrangePrimary

// Brand aliases for compatibility
val AppPurple: Color
    @Composable
    @ReadOnlyComposable
    get() = AppBlue

val AppPurpleDark: Color = AppBlueDark

// Google Pay Dark Mode Palette (Pure neutral charcoal / dark grey, exactly matching Google Pay)
val AppBackground: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalDarkTheme.current) Color(0xFF111318) else Color(0xFFF8FAFC)

val AppSurface: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalDarkTheme.current) Color(0xFF1E1F22) else Color(0xFFFFFFFF)

// Typography & borders
val InkDark: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalDarkTheme.current) Color(0xFFE2E2E6) else Color(0xFF0F172A)

val BodyDark: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalDarkTheme.current) Color(0xFFC4C7C5) else Color(0xFF1E293B)

val MutedText: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalDarkTheme.current) Color(0xFF8E918F) else Color(0xFF64748B)

val FaintText: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalDarkTheme.current) Color(0xFF5F6368) else Color(0xFF94A3B8)

val LineBorder: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalDarkTheme.current) Color(0xFF36383B) else Color(0xFFE2E8F0)

val PanelBg: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalDarkTheme.current) Color(0xFF282A2F) else Color(0xFFF1F5F9)

val AppBlueLight: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalDarkTheme.current) Color(0xFF1B2A3F) else Color(0xFFEEF3FB)

val AppOrangeLight: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalDarkTheme.current) Color(0xFF382218) else Color(0xFFFFF2E8)

val AppPurpleLight: Color
    @Composable
    @ReadOnlyComposable
    get() = AppBlueLight

// Safe state (Green) - Google Green 300 in dark mode for optimal visibility
val SafeGreenBase = Color(0xFF0E7F5B)
val SafeGreenNeon = Color(0xFF6DD58C)
val SafeGreen: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalDarkTheme.current) SafeGreenNeon else SafeGreenBase

val SafeGreenTintBase = Color(0xFFE5F3ED)
val SafeGreenTintDark = Color(0xFF143526)
val SafeGreenTint: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalDarkTheme.current) SafeGreenTintDark else SafeGreenTintBase

val SafeGreenDark = Color(0xFF0B4D38)

// Nudge state (Amber)
val NudgeAmberBase = Color(0xFFA86F00)
val NudgeAmberNeon = Color(0xFFFDD663)
val NudgeAmber: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalDarkTheme.current) NudgeAmberNeon else NudgeAmberBase

val NudgeAmberBar: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalDarkTheme.current) Color(0xFFFDD663) else Color(0xFFE0A100)

val NudgeAmberTintBase = Color(0xFFFFF6DA)
val NudgeAmberTintDark = Color(0xFF382E12)
val NudgeAmberTint: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalDarkTheme.current) NudgeAmberTintDark else NudgeAmberTintBase

val NudgeAmberInk: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalDarkTheme.current) Color(0xFFFFE082) else Color(0xFF5A4300)

// Pause state (Orange)
val PauseOrangeBase = Color(0xFFC9501A)
val PauseOrangeNeon = Color(0xFFFF8533)
val PauseOrange: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalDarkTheme.current) PauseOrangeNeon else PauseOrangeBase

val PauseOrangeTintBase = Color(0xFFFCEDE5)
val PauseOrangeTintDark = Color(0xFF382218)
val PauseOrangeTint: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalDarkTheme.current) PauseOrangeTintDark else PauseOrangeTintBase

val PauseOrangeInk: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalDarkTheme.current) Color(0xFFFFCCBC) else Color(0xFF4A2410)

// Critical state (Red)
val CritRedBase = Color(0xFFBE2A2A)
val CritRedNeon = Color(0xFFF28B82)
val CritRed: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalDarkTheme.current) CritRedNeon else CritRedBase

val CritRedDark = Color(0xFF991B1B)
val CritRedTintBase = Color(0xFFFBEAEA)
val CritRedTintDark = Color(0xFF3B1818)
val CritRedTint: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalDarkTheme.current) CritRedTintDark else CritRedTintBase

val CritRedInk: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalDarkTheme.current) Color(0xFFFFCDD2) else Color(0xFF450A0A)

// Misc
val SuccessGreen: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalDarkTheme.current) Color(0xFF6DD58C) else Color(0xFF1A9E5F)

val CallGreenBase = Color(0xFF138A3C)
val CallGreenNeon = Color(0xFF81C995)
val CallGreen: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalDarkTheme.current) CallGreenNeon else CallGreenBase

val CallGreenTintBase = Color(0xFFE7F7ED)
val CallGreenTintDark = Color(0xFF143526)
val CallGreenTint: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalDarkTheme.current) CallGreenTintDark else CallGreenTintBase