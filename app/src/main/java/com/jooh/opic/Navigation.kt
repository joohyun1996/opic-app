package com.jooh.opic

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavController

/** 하단 바 탭 (TASK 24). route = 탭 그래프, start = 탭 첫 화면. */
internal enum class Tab(val route: String, val start: String, val label: String, val icon: ImageVector) {
    HOME("tab/home", "home", "홈", Icons.Home),
    WORDS("tab/words", "days", "단어", Icons.Cards),
    GRAMMAR("tab/grammar", "grammar", "문법", Icons.Book),
    SHADOWING("tab/shadowing", "shadowing", "섀도잉", Icons.Play),
    SPEAKING("tab/speaking", "speaking", "스피킹", Icons.Mic),
}

/** 탭 첫 화면 오른쪽 위에 ≡ 전체 메뉴 버튼을 얹는다. */
@Composable
internal fun TabRoot(onMenu: () -> Unit, content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize()) {
        content()
        IconButton(onClick = onMenu, modifier = Modifier.align(Alignment.TopEnd).padding(top = 4.dp, end = 4.dp).size(48.dp)) {
            Icon(Icons.Menu, contentDescription = "전체 메뉴")
        }
    }
}

internal fun NavController.safeBack() {
    val current = currentBackStackEntry ?: return
    if (previousBackStackEntry != null && current.lifecycle.currentState == Lifecycle.State.RESUMED) popBackStack()
}
