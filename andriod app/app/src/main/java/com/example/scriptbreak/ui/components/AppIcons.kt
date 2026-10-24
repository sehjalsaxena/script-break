package com.example.scriptbreak.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Standard vector icons implemented directly with Compose ImageVector DSL.
 * Self-contained without requiring large external icon libraries.
 */
object AppIcons {

    val ArrowBack: ImageVector = ImageVector.Builder(
        name = "ArrowBack",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(fill = SolidColor(Color.White)) {
        moveTo(20f, 11f)
        horizontalLineTo(7.83f)
        lineTo(13.42f, 5.41f)
        lineTo(12f, 4f)
        lineTo(4f, 12f)
        lineTo(12f, 20f)
        lineTo(13.41f, 18.59f)
        lineTo(7.83f, 13f)
        horizontalLineTo(20f)
        close()
    }.build()

    val Close: ImageVector = ImageVector.Builder(
        name = "Close",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(fill = SolidColor(Color.White)) {
        moveTo(19f, 6.41f)
        lineTo(17.59f, 5f)
        lineTo(12f, 10.59f)
        lineTo(6.41f, 5f)
        lineTo(5f, 6.41f)
        lineTo(10.59f, 12f)
        lineTo(5f, 17.59f)
        lineTo(6.41f, 19f)
        lineTo(12f, 13.41f)
        lineTo(17.59f, 19f)
        lineTo(19f, 17.59f)
        lineTo(13.41f, 12f)
        close()
    }.build()

    val Check: ImageVector = ImageVector.Builder(
        name = "Check",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(fill = SolidColor(Color.White)) {
        moveTo(9f, 16.17f)
        lineTo(4.83f, 12f)
        lineTo(3.41f, 13.41f)
        lineTo(9f, 19f)
        lineTo(21f, 7f)
        lineTo(19.59f, 5.59f)
        close()
    }.build()

    val ChevronRight: ImageVector = ImageVector.Builder(
        name = "ChevronRight",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(fill = SolidColor(Color.White)) {
        moveTo(10f, 6f)
        lineTo(8.59f, 7.41f)
        lineTo(13.17f, 12f)
        lineTo(8.59f, 16.59f)
        lineTo(10f, 18f)
        lineTo(16f, 12f)
        close()
    }.build()

    val Warning: ImageVector = ImageVector.Builder(
        name = "Warning",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(fill = SolidColor(Color.White)) {
        moveTo(1f, 21f)
        horizontalLineTo(23f)
        lineTo(12f, 2f)
        lineTo(1f, 21f)
        close()
        moveTo(13f, 18f)
        horizontalLineTo(11f)
        verticalLineTo(16f)
        horizontalLineTo(13f)
        verticalLineTo(18f)
        close()
        moveTo(13f, 14f)
        horizontalLineTo(11f)
        verticalLineTo(10f)
        horizontalLineTo(13f)
        verticalLineTo(14f)
        close()
    }.build()

    val Security: ImageVector = ImageVector.Builder(
        name = "Security",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(fill = SolidColor(Color.White)) {
        moveTo(12f, 1f)
        lineTo(3f, 5f)
        verticalLineTo(11f)
        curveTo(3f, 16.55f, 6.84f, 21.74f, 12f, 23f)
        curveTo(17.16f, 21.74f, 21f, 16.55f, 21f, 11f)
        verticalLineTo(5f)
        lineTo(12f, 1f)
        close()
        moveTo(12f, 11.99f)
        horizontalLineTo(19f)
        curveTo(18.47f, 16.11f, 15.72f, 19.78f, 12f, 20.93f)
        verticalLineTo(12f)
        horizontalLineTo(5f)
        verticalLineTo(6.3f)
        lineTo(12f, 3.19f)
        verticalLineTo(11.99f)
        close()
    }.build()

    val Phone: ImageVector = ImageVector.Builder(
        name = "Phone",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(fill = SolidColor(Color.White)) {
        moveTo(6.62f, 10.79f)
        curveTo(8.06f, 13.62f, 10.38f, 15.93f, 13.21f, 17.38f)
        lineTo(15.41f, 15.18f)
        curveTo(15.68f, 14.91f, 16.08f, 14.82f, 16.43f, 14.94f)
        curveTo(17.55f, 15.31f, 18.76f, 15.51f, 20f, 15.51f)
        curveTo(20.55f, 15.51f, 21f, 15.96f, 21f, 16.51f)
        verticalLineTo(20f)
        curveTo(21f, 20.55f, 20.55f, 21f, 20f, 21f)
        curveTo(10.61f, 21f, 3f, 13.39f, 3f, 4f)
        curveTo(3f, 3.45f, 3.45f, 3f, 4f, 3f)
        horizontalLineTo(7.5f)
        curveTo(8.05f, 3f, 8.5f, 3.45f, 8.5f, 4f)
        curveTo(8.5f, 5.25f, 8.7f, 6.45f, 9.07f, 7.57f)
        curveTo(9.18f, 7.92f, 9.1f, 8.31f, 8.82f, 8.59f)
        lineTo(6.62f, 10.79f)
        close()
    }.build()

    val Search: ImageVector = ImageVector.Builder(
        name = "Search",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(fill = SolidColor(Color.White)) {
        moveTo(15.5f, 14f)
        horizontalLineTo(14.71f)
        lineTo(14.43f, 13.73f)
        curveTo(15.41f, 12.59f, 16f, 11.11f, 16f, 9.5f)
        curveTo(16f, 5.91f, 13.09f, 3f, 9.5f, 3f)
        curveTo(5.91f, 3f, 3f, 5.91f, 3f, 9.5f)
        curveTo(3f, 13.09f, 5.91f, 16f, 9.5f, 16f)
        curveTo(11.11f, 16f, 12.59f, 15.41f, 13.73f, 14.43f)
        lineTo(14f, 14.71f)
        verticalLineTo(15.5f)
        lineTo(19f, 20.49f)
        lineTo(20.49f, 19f)
        lineTo(15.5f, 14f)
        close()
        moveTo(9.5f, 14f)
        curveTo(7.01f, 14f, 5f, 11.99f, 5f, 9.5f)
        curveTo(5f, 7.01f, 7.01f, 5f, 9.5f, 5f)
        curveTo(11.99f, 5f, 14f, 7.01f, 14f, 9.5f)
        curveTo(14f, 11.99f, 11.99f, 14f, 9.5f, 14f)
        close()
    }.build()

    val Person: ImageVector = ImageVector.Builder(
        name = "Person",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(fill = SolidColor(Color.White)) {
        moveTo(12f, 12f)
        curveTo(14.21f, 12f, 16f, 10.21f, 16f, 8f)
        curveTo(16f, 5.79f, 14.21f, 4f, 12f, 4f)
        curveTo(9.79f, 4f, 8f, 5.79f, 8f, 8f)
        curveTo(8f, 10.21f, 9.79f, 12f, 12f, 12f)
        close()
        moveTo(12f, 14f)
        curveTo(9.33f, 14f, 4f, 15.34f, 4f, 18f)
        verticalLineTo(20f)
        horizontalLineTo(20f)
        verticalLineTo(18f)
        curveTo(20f, 15.34f, 14.67f, 14f, 12f, 14f)
        close()
    }.build()

    val AlternateEmail: ImageVector = ImageVector.Builder(
        name = "AlternateEmail",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(fill = SolidColor(Color.White)) {
        moveTo(12f, 2f)
        curveTo(6.48f, 2f, 2f, 6.48f, 2f, 12f)
        curveTo(2f, 17.52f, 6.48f, 22f, 12f, 22f)
        horizontalLineTo(17f)
        verticalLineTo(20f)
        horizontalLineTo(12f)
        curveTo(7.66f, 20f, 4f, 16.34f, 4f, 12f)
        curveTo(4f, 7.66f, 7.66f, 4f, 12f, 4f)
        curveTo(16.34f, 4f, 20f, 7.66f, 20f, 12f)
        verticalLineTo(13.43f)
        curveTo(20f, 14.22f, 19.29f, 15f, 18.5f, 15f)
        curveTo(17.71f, 15f, 17f, 14.22f, 17f, 13.43f)
        verticalLineTo(12f)
        curveTo(17f, 9.24f, 14.76f, 7f, 12f, 7f)
        curveTo(9.24f, 7f, 7f, 9.24f, 7f, 12f)
        curveTo(7f, 14.76f, 9.24f, 17f, 12f, 17f)
        curveTo(13.38f, 17f, 14.64f, 16.44f, 15.54f, 15.53f)
        curveTo(16.19f, 16.42f, 17.31f, 17f, 18.5f, 17f)
        curveTo(20.47f, 17f, 22f, 15.4f, 22f, 13.43f)
        verticalLineTo(12f)
        curveTo(22f, 6.48f, 17.52f, 2f, 12f, 2f)
        close()
        moveTo(12f, 15f)
        curveTo(10.34f, 15f, 9f, 13.66f, 9f, 12f)
        curveTo(9f, 10.34f, 10.34f, 9f, 12f, 9f)
        curveTo(13.66f, 9f, 15f, 10.34f, 15f, 12f)
        curveTo(15f, 13.66f, 13.66f, 15f, 12f, 15f)
        close()
    }.build()

    val AccountBalance: ImageVector = ImageVector.Builder(
        name = "AccountBalance",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(fill = SolidColor(Color.White)) {
        moveTo(4f, 10f)
        verticalLineTo(17f)
        horizontalLineTo(7f)
        verticalLineTo(10f)
        horizontalLineTo(4f)
        close()
        moveTo(10f, 10f)
        verticalLineTo(17f)
        horizontalLineTo(13f)
        verticalLineTo(10f)
        horizontalLineTo(10f)
        close()
        moveTo(2f, 22f)
        horizontalLineTo(21f)
        verticalLineTo(19f)
        horizontalLineTo(2f)
        verticalLineTo(22f)
        close()
        moveTo(16f, 10f)
        verticalLineTo(17f)
        horizontalLineTo(19f)
        verticalLineTo(10f)
        horizontalLineTo(16f)
        close()
        moveTo(11.5f, 1f)
        lineTo(2f, 6f)
        verticalLineTo(8f)
        horizontalLineTo(21f)
        verticalLineTo(6f)
        lineTo(11.5f, 1f)
        close()
    }.build()

    val Wallet: ImageVector = ImageVector.Builder(
        name = "Wallet",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(fill = SolidColor(Color.White)) {
        moveTo(21f, 18f)
        verticalLineTo(19f)
        curveTo(21f, 20.1f, 20.1f, 21f, 19f, 21f)
        horizontalLineTo(5f)
        curveTo(3.89f, 21f, 3f, 20.1f, 3f, 19f)
        verticalLineTo(5f)
        curveTo(3f, 3.9f, 3.89f, 3f, 5f, 3f)
        horizontalLineTo(19f)
        curveTo(20.1f, 3f, 21f, 3.9f, 21f, 5f)
        verticalLineTo(6f)
        horizontalLineTo(12f)
        curveTo(10.89f, 6f, 10f, 6.9f, 10f, 8f)
        verticalLineTo(16f)
        curveTo(10f, 17.1f, 10.89f, 18f, 12f, 18f)
        horizontalLineTo(21f)
        close()
        moveTo(12f, 16f)
        horizontalLineTo(22f)
        verticalLineTo(8f)
        horizontalLineTo(12f)
        verticalLineTo(16f)
        close()
        moveTo(16f, 13.5f)
        curveTo(15.17f, 13.5f, 14.5f, 12.83f, 14.5f, 12f)
        curveTo(14.5f, 11.17f, 15.17f, 10.5f, 16f, 10.5f)
        curveTo(16.83f, 10.5f, 17.5f, 11.17f, 17.5f, 12f)
        curveTo(17.5f, 12.83f, 16.83f, 13.5f, 16f, 13.5f)
        close()
    }.build()

    val Notifications: ImageVector = ImageVector.Builder(
        name = "Notifications",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(fill = SolidColor(Color.White)) {
        moveTo(12f, 22f)
        curveTo(13.1f, 22f, 14f, 21.1f, 14f, 20f)
        horizontalLineTo(10f)
        curveTo(10f, 21.1f, 10.9f, 22f, 12f, 22f)
        close()
        moveTo(18f, 16f)
        verticalLineTo(11f)
        curveTo(18f, 7.93f, 16.37f, 5.36f, 13.5f, 4.68f)
        verticalLineTo(4f)
        curveTo(13.5f, 3.17f, 12.83f, 2.5f, 12f, 2.5f)
        curveTo(11.17f, 2.5f, 10.5f, 3.17f, 10.5f, 4f)
        verticalLineTo(4.68f)
        curveTo(7.64f, 5.36f, 6f, 7.92f, 6f, 11f)
        verticalLineTo(16f)
        lineTo(4f, 18f)
        verticalLineTo(19f)
        horizontalLineTo(20f)
        verticalLineTo(18f)
        lineTo(18f, 16f)
        close()
    }.build()

    val History: ImageVector = ImageVector.Builder(
        name = "History",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(fill = SolidColor(Color.White)) {
        moveTo(13f, 3f)
        curveTo(8.03f, 3f, 4f, 7.03f, 4f, 12f)
        horizontalLineTo(1f)
        lineTo(4.89f, 15.89f)
        lineTo(9f, 12f)
        horizontalLineTo(6f)
        curveTo(6f, 8.13f, 9.13f, 5f, 13f, 5f)
        curveTo(16.87f, 5f, 20f, 8.13f, 20f, 12f)
        curveTo(20f, 15.87f, 16.87f, 19f, 13f, 19f)
        curveTo(11.07f, 19f, 9.32f, 18.21f, 8.06f, 16.94f)
        lineTo(6.64f, 18.36f)
        curveTo(8.27f, 19.99f, 10.51f, 21f, 13f, 21f)
        curveTo(17.97f, 21f, 22f, 16.97f, 22f, 12f)
        curveTo(22f, 7.03f, 17.97f, 3f, 13f, 3f)
        close()
        moveTo(12f, 8f)
        verticalLineTo(13f)
        lineTo(16.28f, 15.54f)
        lineTo(17f, 14.33f)
        lineTo(13.5f, 12.25f)
        verticalLineTo(8f)
        horizontalLineTo(12f)
        close()
    }.build()

    val Settings: ImageVector = ImageVector.Builder(
        name = "Settings",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(fill = SolidColor(Color.White)) {
        moveTo(19.14f, 12.94f)
        curveTo(19.18f, 12.64f, 19.2f, 12.33f, 19.2f, 12f)
        curveTo(19.2f, 11.68f, 19.18f, 11.36f, 19.13f, 11.06f)
        lineTo(21.16f, 9.48f)
        lineTo(19.24f, 6.16f)
        lineTo(16.85f, 7.12f)
        curveTo(16.35f, 6.74f, 15.82f, 6.42f, 15.23f, 6.18f)
        lineTo(14.87f, 3.64f)
        horizontalLineTo(11.03f)
        lineTo(10.67f, 6.18f)
        curveTo(10.08f, 6.42f, 9.54f, 6.75f, 9.05f, 7.12f)
        lineTo(6.66f, 6.16f)
        lineTo(4.74f, 9.48f)
        lineTo(6.77f, 11.06f)
        curveTo(6.72f, 11.36f, 6.7f, 11.69f, 6.7f, 12f)
        curveTo(6.7f, 12.31f, 6.72f, 12.64f, 6.77f, 12.94f)
        lineTo(4.74f, 14.52f)
        lineTo(6.66f, 17.84f)
        lineTo(9.05f, 16.88f)
        curveTo(9.55f, 17.26f, 10.08f, 17.58f, 10.67f, 17.82f)
        lineTo(11.03f, 20.36f)
        horizontalLineTo(14.87f)
        lineTo(15.23f, 17.82f)
        curveTo(15.82f, 17.58f, 16.36f, 17.25f, 16.85f, 16.88f)
        lineTo(19.24f, 17.84f)
        lineTo(21.16f, 14.52f)
        lineTo(19.14f, 12.94f)
        close()
        moveTo(12f, 15.6f)
        curveTo(10.02f, 15.6f, 8.4f, 13.98f, 8.4f, 12f)
        curveTo(8.4f, 10.02f, 10.02f, 8.4f, 12f, 8.4f)
        curveTo(13.98f, 8.4f, 15.6f, 10.02f, 15.6f, 12f)
        curveTo(15.6f, 13.98f, 13.98f, 15.6f, 12f, 15.6f)
        close()
    }.build()

    val Home: ImageVector = ImageVector.Builder(
        name = "Home",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(fill = SolidColor(Color.White)) {
        moveTo(10f, 20f)
        verticalLineTo(14f)
        horizontalLineTo(14f)
        verticalLineTo(20f)
        horizontalLineTo(19f)
        verticalLineTo(12f)
        horizontalLineTo(22f)
        lineTo(12f, 3f)
        lineTo(2f, 12f)
        horizontalLineTo(5f)
        verticalLineTo(20f)
        close()
    }.build()

    val ContentPaste: ImageVector = ImageVector.Builder(
        name = "ContentPaste",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(fill = SolidColor(Color.White)) {
        moveTo(19f, 2f)
        horizontalLineTo(14.82f)
        curveTo(14.4f, 0.84f, 13.3f, 0f, 12f, 0f)
        curveTo(10.7f, 0f, 9.6f, 0.84f, 9.18f, 2f)
        horizontalLineTo(5f)
        curveTo(3.9f, 2f, 3f, 2.9f, 3f, 4f)
        verticalLineTo(20f)
        curveTo(3f, 21.1f, 3.9f, 22f, 5f, 22f)
        horizontalLineTo(19f)
        curveTo(20.1f, 22f, 21f, 21.1f, 21f, 20f)
        verticalLineTo(4f)
        curveTo(21f, 2.9f, 20.1f, 2f, 19f, 2f)
        close()
        moveTo(12f, 2f)
        curveTo(12.55f, 2f, 13f, 2.45f, 13f, 3f)
        curveTo(13f, 3.55f, 12.55f, 4f, 12f, 4f)
        curveTo(11.45f, 4f, 11f, 3.55f, 11f, 3f)
        curveTo(11f, 2.45f, 11.45f, 2f, 12f, 2f)
        close()
        moveTo(19f, 20f)
        horizontalLineTo(5f)
        verticalLineTo(4f)
        horizontalLineTo(7f)
        verticalLineTo(7f)
        horizontalLineTo(17f)
        verticalLineTo(4f)
        horizontalLineTo(19f)
        verticalLineTo(20f)
        close()
    }.build()

    val VerifiedUser: ImageVector = ImageVector.Builder(
        name = "VerifiedUser",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(fill = SolidColor(Color.White)) {
        moveTo(12f, 1f)
        lineTo(3f, 5f)
        verticalLineTo(11f)
        curveTo(3f, 16.55f, 6.84f, 21.74f, 12f, 23f)
        curveTo(17.16f, 21.74f, 21f, 16.55f, 21f, 11f)
        verticalLineTo(5f)
        lineTo(12f, 1f)
        close()
        moveTo(10f, 17f)
        lineTo(6f, 13f)
        lineTo(7.41f, 11.59f)
        lineTo(10f, 14.17f)
        lineTo(16.59f, 7.58f)
        lineTo(18f, 9f)
        lineTo(10f, 17f)
        close()
    }.build()

    val VolumeUp: ImageVector = ImageVector.Builder(
        name = "VolumeUp",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(fill = SolidColor(Color.White)) {
        moveTo(3f, 9f)
        verticalLineTo(15f)
        horizontalLineTo(7f)
        lineTo(12f, 20f)
        verticalLineTo(4f)
        lineTo(7f, 9f)
        horizontalLineTo(3f)
        close()
        moveTo(16.5f, 12f)
        curveTo(16.5f, 10.23f, 15.48f, 8.71f, 14f, 7.97f)
        verticalLineTo(16.02f)
        curveTo(15.48f, 15.29f, 16.5f, 13.77f, 16.5f, 12f)
        close()
    }.build()

    val HourglassEmpty: ImageVector = ImageVector.Builder(
        name = "HourglassEmpty",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(fill = SolidColor(Color.White)) {
        moveTo(6f, 2f)
        verticalLineTo(8f)
        lineTo(10f, 12f)
        lineTo(6f, 16f)
        verticalLineTo(22f)
        horizontalLineTo(18f)
        verticalLineTo(16f)
        lineTo(14f, 12f)
        lineTo(18f, 8f)
        verticalLineTo(2f)
        horizontalLineTo(6f)
        close()
        moveTo(16f, 16.5f)
        verticalLineTo(20f)
        horizontalLineTo(8f)
        verticalLineTo(16.5f)
        lineTo(12f, 12.5f)
        lineTo(16f, 16.5f)
        close()
        moveTo(12f, 11.5f)
        lineTo(8f, 7.5f)
        verticalLineTo(4f)
        horizontalLineTo(16f)
        verticalLineTo(7.5f)
        lineTo(12f, 11.5f)
        close()
    }.build()

    val Assessment: ImageVector = ImageVector.Builder(
        name = "Assessment",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(fill = SolidColor(Color.White)) {
        moveTo(19f, 3f)
        horizontalLineTo(5f)
        curveTo(3.9f, 3f, 3f, 3.9f, 3f, 5f)
        verticalLineTo(19f)
        curveTo(3f, 20.1f, 3.9f, 21f, 5f, 21f)
        horizontalLineTo(19f)
        curveTo(20.1f, 21f, 21f, 20.1f, 21f, 19f)
        verticalLineTo(5f)
        curveTo(21f, 3.9f, 20.1f, 3f, 19f, 3f)
        close()
        moveTo(9f, 17f)
        horizontalLineTo(7f)
        verticalLineTo(10f)
        horizontalLineTo(9f)
        verticalLineTo(17f)
        close()
        moveTo(13f, 17f)
        horizontalLineTo(11f)
        verticalLineTo(7f)
        horizontalLineTo(13f)
        verticalLineTo(17f)
        close()
        moveTo(17f, 17f)
        horizontalLineTo(15f)
        verticalLineTo(13f)
        horizontalLineTo(17f)
        verticalLineTo(17f)
        close()
    }.build()

    val Description: ImageVector = ImageVector.Builder(
        name = "Description",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(fill = SolidColor(Color.White)) {
        moveTo(14f, 2f)
        horizontalLineTo(6f)
        curveTo(4.9f, 2f, 4f, 2.9f, 4f, 4f)
        verticalLineTo(20f)
        curveTo(4f, 21.1f, 4.9f, 22f, 6f, 22f)
        horizontalLineTo(18f)
        curveTo(19.1f, 22f, 20f, 21.1f, 20f, 20f)
        verticalLineTo(8f)
        lineTo(14f, 2f)
        close()
        moveTo(16f, 18f)
        horizontalLineTo(8f)
        verticalLineTo(16f)
        horizontalLineTo(16f)
        verticalLineTo(18f)
        close()
        moveTo(16f, 14f)
        horizontalLineTo(8f)
        verticalLineTo(12f)
        horizontalLineTo(16f)
        verticalLineTo(14f)
        close()
        moveTo(13f, 9f)
        verticalLineTo(3.5f)
        lineTo(18.5f, 9f)
        horizontalLineTo(13f)
        close()
    }.build()

    val Info: ImageVector = ImageVector.Builder(
        name = "Info",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(fill = SolidColor(Color.White)) {
        moveTo(12f, 2f)
        curveTo(6.48f, 2f, 2f, 6.48f, 2f, 12f)
        curveTo(2f, 17.52f, 6.48f, 22f, 12f, 22f)
        curveTo(17.52f, 22f, 22f, 17.52f, 22f, 12f)
        curveTo(22f, 6.48f, 17.52f, 2f, 12f, 2f)
        close()
        moveTo(13f, 17f)
        horizontalLineTo(11f)
        verticalLineTo(11f)
        horizontalLineTo(13f)
        verticalLineTo(17f)
        close()
        moveTo(13f, 9f)
        horizontalLineTo(11f)
        verticalLineTo(7f)
        horizontalLineTo(13f)
        verticalLineTo(9f)
        close()
    }.build()

    val Lock: ImageVector = ImageVector.Builder(
        name = "Lock",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(fill = SolidColor(Color.White)) {
        moveTo(18f, 8f)
        horizontalLineTo(17f)
        verticalLineTo(6f)
        curveTo(17f, 3.24f, 14.76f, 1f, 12f, 1f)
        curveTo(9.24f, 1f, 7f, 3.24f, 7f, 6f)
        verticalLineTo(8f)
        horizontalLineTo(6f)
        curveTo(4.9f, 8f, 4f, 8.9f, 4f, 10f)
        verticalLineTo(20f)
        curveTo(4f, 21.1f, 4.9f, 22f, 6f, 22f)
        horizontalLineTo(18f)
        curveTo(19.1f, 22f, 20f, 21.1f, 20f, 20f)
        verticalLineTo(10f)
        curveTo(20f, 8.9f, 19.1f, 8f, 18f, 8f)
        close()
        moveTo(12f, 17f)
        curveTo(10.9f, 17f, 10f, 16.1f, 10f, 15f)
        curveTo(10f, 13.9f, 10.9f, 13f, 12f, 13f)
        curveTo(13.1f, 13f, 14f, 13.9f, 14f, 15f)
        curveTo(14f, 16.1f, 13.1f, 17f, 12f, 17f)
        close()
        moveTo(15.1f, 8f)
        horizontalLineTo(8.9f)
        verticalLineTo(6f)
        curveTo(8.9f, 4.29f, 10.29f, 2.9f, 12f, 2.9f)
        curveTo(13.71f, 2.9f, 15.1f, 4.29f, 15.1f, 6f)
        verticalLineTo(8f)
        close()
    }.build()

    val QrCode: ImageVector = ImageVector.Builder(
        name = "QrCode",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(fill = SolidColor(Color.White)) {
        moveTo(3f, 3f)
        verticalLineTo(11f)
        horizontalLineTo(11f)
        verticalLineTo(3f)
        horizontalLineTo(3f)
        close()
        moveTo(9f, 9f)
        horizontalLineTo(5f)
        verticalLineTo(5f)
        horizontalLineTo(9f)
        verticalLineTo(9f)
        close()
        moveTo(13f, 3f)
        verticalLineTo(11f)
        horizontalLineTo(21f)
        verticalLineTo(3f)
        horizontalLineTo(13f)
        close()
        moveTo(19f, 9f)
        horizontalLineTo(15f)
        verticalLineTo(5f)
        horizontalLineTo(19f)
        verticalLineTo(9f)
        close()
        moveTo(3f, 13f)
        verticalLineTo(21f)
        horizontalLineTo(11f)
        verticalLineTo(13f)
        horizontalLineTo(3f)
        close()
        moveTo(9f, 19f)
        horizontalLineTo(5f)
        verticalLineTo(15f)
        horizontalLineTo(9f)
        verticalLineTo(19f)
        close()
        moveTo(13f, 13f)
        horizontalLineTo(15f)
        verticalLineTo(15f)
        horizontalLineTo(13f)
        close()
        moveTo(15f, 15f)
        horizontalLineTo(17f)
        verticalLineTo(17f)
        horizontalLineTo(15f)
        close()
        moveTo(17f, 13f)
        horizontalLineTo(19f)
        verticalLineTo(15f)
        horizontalLineTo(17f)
        close()
        moveTo(19f, 15f)
        horizontalLineTo(21f)
        verticalLineTo(17f)
        horizontalLineTo(19f)
        close()
        moveTo(17f, 17f)
        horizontalLineTo(19f)
        verticalLineTo(19f)
        horizontalLineTo(17f)
        close()
        moveTo(13f, 17f)
        horizontalLineTo(15f)
        verticalLineTo(19f)
        horizontalLineTo(13f)
        close()
        moveTo(15f, 19f)
        horizontalLineTo(17f)
        verticalLineTo(21f)
        horizontalLineTo(15f)
        close()
        moveTo(19f, 19f)
        horizontalLineTo(21f)
        verticalLineTo(21f)
        horizontalLineTo(19f)
        close()
    }.build()
}
