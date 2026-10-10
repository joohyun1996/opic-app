package com.jooh.opic.core.ui

import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 공용 테마 (TASK 24). 밝게 = 시안 A(Babbel 톤: 흰·주황), 어둡게 = 시안 B(Speak 톤: 남색·파랑).
 * 시안: https://claude.ai/artifact/4UkaAdg75DhYj7u4CJTyZN
 */
enum class ThemeMode(val label: String) { SYSTEM("기기 설정 따름"), LIGHT("밝게"), DARK("어둡게") }

/** Material colorScheme에 없는 앱 전용 색. */
@Immutable
data class OpicColors(
    val accent: Color,        // 강조 글자·선택 탭
    val surfaceHigh: Color,   // 아이콘 칸·진행 막대 트랙
    val muted: Color,         // 보조 글자
    val success: Color, val successBg: Color,
    val error: Color, val errorBg: Color,
    val warning: Color, val warningBg: Color,
    val learning: Color,      // 학습 중 칸
)

private val LightOpic = OpicColors(
    accent = Color(0xFFC8460F), surfaceHigh = Color(0xFFE7E3DC), muted = Color(0xFF5B5B60),
    success = Color(0xFF1E7A3C), successBg = Color(0xFFE3F2E7), error = Color(0xFFB3261E), errorBg = Color(0xFFFBE9E7),
    warning = Color(0xFFA15C00), warningBg = Color(0xFFFFF1DC), learning = Color(0xFFFFF1DC),
)
private val DarkOpic = OpicColors(
    accent = Color(0xFF8EA0FF), surfaceHigh = Color(0xFF24306A), muted = Color(0xFFA3ACD3),
    success = Color(0xFF6FD39A), successBg = Color(0xFF173A2E), error = Color(0xFFFF8A80), errorBg = Color(0xFF3D1E2A),
    warning = Color(0xFFFFC46B), warningBg = Color(0xFF3A2E1A), learning = Color(0xFF3A2E1A),
)

private val LightScheme = lightColorScheme(
    primary = Color(0xFFC8460F), onPrimary = Color.White, primaryContainer = Color(0xFFFBE4D8), onPrimaryContainer = Color(0xFF3A1200),
    secondary = Color(0xFF5B5B60), onSecondary = Color.White, secondaryContainer = Color(0xFFF1EEE9), onSecondaryContainer = Color(0xFF1C1C1E),
    background = Color.White, onBackground = Color(0xFF1C1C1E), surface = Color.White, onSurface = Color(0xFF1C1C1E),
    surfaceVariant = Color(0xFFF7F5F1), onSurfaceVariant = Color(0xFF5B5B60),
    surfaceContainerLowest = Color.White, surfaceContainerLow = Color(0xFFFAF9F6), surfaceContainer = Color(0xFFF7F5F1),
    surfaceContainerHigh = Color(0xFFF2EFEA), surfaceContainerHighest = Color(0xFFECE9E4),
    outline = Color(0xFFD9D4CC), outlineVariant = Color(0xFFECE9E4), error = Color(0xFFB3261E), onError = Color.White,
)
private val DarkScheme = darkColorScheme(
    primary = Color(0xFF4C5BF0), onPrimary = Color.White, primaryContainer = Color(0xFF24306A), onPrimaryContainer = Color(0xFFE3E7FF),
    secondary = Color(0xFFA3ACD3), onSecondary = Color(0xFF0E1430), secondaryContainer = Color(0xFF1B2550), onSecondaryContainer = Color(0xFFF3F5FF),
    background = Color(0xFF0E1430), onBackground = Color(0xFFF3F5FF), surface = Color(0xFF0E1430), onSurface = Color(0xFFF3F5FF),
    surfaceVariant = Color(0xFF151D40), onSurfaceVariant = Color(0xFFA3ACD3),
    surfaceContainerLowest = Color(0xFF0B1028), surfaceContainerLow = Color(0xFF121A3A), surfaceContainer = Color(0xFF151D40),
    surfaceContainerHigh = Color(0xFF1B2550), surfaceContainerHighest = Color(0xFF24306A),
    outline = Color(0xFF3A4580), outlineVariant = Color(0xFF1F2853), error = Color(0xFFFF8A80), onError = Color(0xFF0E1430),
)

val IbmPlexSansKr = FontFamily(
    Font(R.font.ibm_plex_sans_kr_regular, FontWeight.Normal),
    Font(R.font.ibm_plex_sans_kr_semibold, FontWeight.SemiBold),
    Font(R.font.ibm_plex_sans_kr_bold, FontWeight.Bold),
)

private fun typography(): Typography {
    val base = Typography()
    fun TextStyle.plex(weight: FontWeight? = null) = copy(fontFamily = IbmPlexSansKr, fontWeight = weight ?: fontWeight)
    return Typography(
        displayLarge = base.displayLarge.plex(FontWeight.Bold), displayMedium = base.displayMedium.plex(FontWeight.Bold),
        displaySmall = base.displaySmall.plex(FontWeight.Bold),
        headlineLarge = base.headlineLarge.plex(FontWeight.Bold), headlineMedium = base.headlineMedium.plex(FontWeight.Bold),
        headlineSmall = base.headlineSmall.copy(fontFamily = IbmPlexSansKr, fontWeight = FontWeight.Bold, fontSize = 24.sp),
        titleLarge = base.titleLarge.plex(FontWeight.Bold), titleMedium = base.titleMedium.plex(FontWeight.SemiBold),
        titleSmall = base.titleSmall.plex(FontWeight.SemiBold),
        bodyLarge = base.bodyLarge.plex(), bodyMedium = base.bodyMedium.plex(), bodySmall = base.bodySmall.plex(),
        labelLarge = base.labelLarge.plex(FontWeight.SemiBold), labelMedium = base.labelMedium.plex(FontWeight.SemiBold),
        labelSmall = base.labelSmall.plex(),
    )
}

private val OpicShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp), small = RoundedCornerShape(12.dp), medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp), extraLarge = RoundedCornerShape(28.dp),
)

val LocalOpicColors = staticCompositionLocalOf { LightOpic }

/** 화면에서 앱 전용 색 꺼내기: `Opic.colors.accent`. */
object Opic {
    val colors: OpicColors @Composable get() = LocalOpicColors.current
}

@Composable
fun OpicTheme(mode: ThemeMode, content: @Composable () -> Unit) {
    val dark = when (mode) { ThemeMode.SYSTEM -> isSystemInDarkTheme(); ThemeMode.LIGHT -> false; ThemeMode.DARK -> true }
    CompositionLocalProvider(LocalOpicColors provides if (dark) DarkOpic else LightOpic) {
        MaterialTheme(colorScheme = if (dark) DarkScheme else LightScheme, typography = typography(), shapes = OpicShapes, content = content)
    }
}

/** 화면 설정 (테마·TTS 속도). 앱 안 SharedPreferences, 바뀌면 바로 반영. */
object UiSettings {
    private const val PREFS = "opic_ui"
    private val theme = MutableStateFlow(ThemeMode.SYSTEM)
    private val speech = MutableStateFlow(1.0f)
    val themeMode: StateFlow<ThemeMode> = theme.asStateFlow()
    /** TTS 속도: 0.8 느리게 / 1.0 보통 / 1.2 빠르게. */
    val speechRate: StateFlow<Float> = speech.asStateFlow()

    fun load(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        theme.value = runCatching { ThemeMode.valueOf(prefs.getString("theme", ThemeMode.SYSTEM.name)!!) }.getOrDefault(ThemeMode.SYSTEM)
        speech.value = prefs.getFloat("speechRate", 1.0f)
    }
    fun setTheme(context: Context, mode: ThemeMode) {
        theme.value = mode
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString("theme", mode.name).apply()
    }
    fun setSpeechRate(context: Context, rate: Float) {
        speech.value = rate
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putFloat("speechRate", rate).apply()
    }
}
