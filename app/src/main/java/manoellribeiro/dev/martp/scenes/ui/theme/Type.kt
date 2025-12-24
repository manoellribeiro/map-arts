package manoellribeiro.dev.martp.scenes.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import manoellribeiro.dev.martp.R

// Nunito Font Family
val NunitoFontFamily = FontFamily(
    Font(R.font.nunito_regular, FontWeight.Normal),
    Font(R.font.nunito_medium, FontWeight.Medium),
    Font(R.font.nunito_bold, FontWeight.Bold),
    Font(R.font.nunito_extra_bold, FontWeight.ExtraBold),
)

// Text Styles matching themes.xml
val TextScreenTitle
    @Composable get() = TextStyle(
        fontFamily = NunitoFontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 16.sp,
        color = DarkD1
    )

val TextTitle
    @Composable get() = TextStyle(
        fontFamily = NunitoFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        color = DarkD1
    )

val TextGraySubTitle
    @Composable get() = TextStyle(
        fontFamily = NunitoFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        color = DarkD4
    )

val TextMiddleScreenInfoText
    @Composable get() = TextStyle(
        fontFamily = NunitoFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        color = DarkD1
    )

val TextInputLabel
    @Composable get() = TextStyle(
        fontFamily = NunitoFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        color = DarkD1
    )

val TextInputText
    @Composable get() = TextStyle(
        fontFamily = NunitoFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp, // 16sp * 1.5 line spacing multiplier
        color = DarkD1
    )

// Set of Material typography styles to start with
val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = NunitoFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    )
)
