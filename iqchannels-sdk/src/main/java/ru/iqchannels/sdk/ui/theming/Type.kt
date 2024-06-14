package ru.iqchannels.sdk.ui.theming

import androidx.compose.material.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Set of Material typography styles to start with
val Typography = Typography(
    h1 = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp
    ),
    h2 = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp
    ),
    h3 = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp
    ),
    body1 = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp
    ),
    body2 = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp
    ),
    button = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp
    ),
    caption = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
    ),
    h6 = TextStyle(
        fontWeight = FontWeight.W600,
        fontSize = 12.sp
    ),
    subtitle1 = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp
    )
)

val Regular16 = TextStyle(
    fontWeight = FontWeight.Normal,
    fontSize = 16.sp,
)

val Regular13 = TextStyle(
    fontWeight = FontWeight.Normal,
    fontSize = 13.sp,
)

val Medium15 = TextStyle(
    fontWeight = FontWeight.Medium,
    fontSize = 15.sp,
)