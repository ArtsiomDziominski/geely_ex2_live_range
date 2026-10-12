package com.geely.ex2.range.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.geely.ex2.range.R
import com.geely.ex2.range.domain.model.AppStyle
import com.geely.ex2.range.ui.theme.LocalStyleColors
import com.geely.ex2.range.ui.theme.StyleColors

private const val APP_INFO_LABEL = "О приложении"
private const val CAR_LABEL = "Geely EX2"
private const val NORDIC_HIDE_LABEL = "Скрыть"

/**
 * Каркас приложения для оформлений-макетов на широком экране: у каждого своя навигация —
 * боковая панель с подписями, колонка кружков слева, док снизу или меню слева. Заголовка-шапки нет,
 * место отдано содержимому. Текущий раздел читается только внутри панелей навигации, чтобы
 * смена вкладки не перекомпоновывала весь каркас.
 */
@Composable
internal fun StyledAppFrame(
    style: AppStyle,
    navController: NavHostController,
    onNavigate: (String) -> Unit,
    onAppInfo: () -> Unit,
    modifier: Modifier = Modifier,
    /** Отметка на «Настройках»: найдено обновление. */
    settingsBadge: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colors = LocalStyleColors.current
    if (colors == null) {
        Box(modifier) { content() }
        return
    }
    when (style) {
        AppStyle.COCKPIT -> Row(modifier) {
            CockpitRail(navController, colors, settingsBadge, onNavigate, onAppInfo)
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            ) { content() }
        }
        AppStyle.NORDIC -> NordicFrame(navController, colors, settingsBadge, onNavigate, onAppInfo, modifier, content)
        AppStyle.BENTO -> Column(modifier) {
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) { content() }
            BentoDock(navController, colors, settingsBadge, onNavigate, onAppInfo)
        }
        AppStyle.NATIVE -> Row(modifier) {
            NativeMenu(navController, colors, settingsBadge, onNavigate, onAppInfo)
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            ) { content() }
        }
        AppStyle.CLASSIC -> Box(modifier) { content() }
    }
}

@Composable
private fun currentRoute(navController: NavHostController): String? {
    val entry by navController.currentBackStackEntryAsState()
    return entry?.destination?.route
}

/** «Кокпит»: узкая боковая панель, иконка с подписью. */
@Composable
private fun CockpitRail(
    navController: NavHostController,
    colors: StyleColors,
    settingsBadge: Boolean,
    onNavigate: (String) -> Unit,
    onAppInfo: () -> Unit,
) {
    val route = currentRoute(navController)
    Row(Modifier.fillMaxHeight()) {
        Column(
            modifier = Modifier
                .width(104.dp)
                .fillMaxHeight()
                .padding(vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Destinations.forEach { destination ->
                val selected = route == destination.route
                val tint = if (selected) colors.accentText else colors.muted
                Column(
                    modifier = Modifier
                        .size(82.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(if (selected) colors.accent.copy(alpha = 0.14f) else Color.Transparent)
                        .selectable(
                            selected = selected,
                            role = Role.Tab,
                            onClick = { onNavigate(destination.route) },
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
                ) {
                    NavIcon(
                        icon = if (selected) destination.selectedIcon else destination.icon,
                        tint = tint,
                        size = 26,
                        badge = settingsBadge && destination.route == ROUTE_SETTINGS,
                        colors = colors,
                    )
                    Text(
                        destination.label,
                        fontSize = 13.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                        color = tint,
                        maxLines = 1,
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            InfoButton(colors.muted, onAppInfo, Modifier.size(60.dp))
        }
        Box(
            Modifier
                .width(1.dp)
                .fillMaxHeight()
                .background(colors.line),
        )
    }
}

/**
 * Навигация «Нордика» скрыта на главной: главная показывает кнопку, которая вернёт панель.
 * Null вне «Нордика» — там панель не прячется.
 */
@Immutable
internal class NordicNavState(val hidden: Boolean, val onShow: () -> Unit)

internal val LocalNordicNav = compositionLocalOf<NordicNavState?> { null }

/**
 * «Нордик»: колонка разделов у левого края, иконки в кружках с подписью. На главной панель
 * можно скрыть — место отдаётся цифрам, а кнопка «Меню» внизу главной возвращает её.
 * Скрыта она только на главной: при уходе на другой раздел панель снова видна.
 */
@Composable
private fun NordicFrame(
    navController: NavHostController,
    colors: StyleColors,
    settingsBadge: Boolean,
    onNavigate: (String) -> Unit,
    onAppInfo: () -> Unit,
    modifier: Modifier,
    content: @Composable () -> Unit,
) {
    var hidden by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(navController) {
        navController.currentBackStackEntryFlow.collect { entry ->
            if (entry.destination.route != ROUTE_DASHBOARD) hidden = false
        }
    }
    val navState = remember(hidden) { NordicNavState(hidden = hidden, onShow = { hidden = false }) }
    Row(modifier) {
        if (!hidden) {
            NordicRail(navController, colors, settingsBadge, onNavigate, onAppInfo, onHide = { hidden = true })
        }
        CompositionLocalProvider(LocalNordicNav provides navState) {
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            ) { content() }
        }
    }
}

@Composable
private fun NordicRail(
    navController: NavHostController,
    colors: StyleColors,
    settingsBadge: Boolean,
    onNavigate: (String) -> Unit,
    onAppInfo: () -> Unit,
    onHide: () -> Unit,
) {
    val route = currentRoute(navController)
    Row(Modifier.fillMaxHeight()) {
        Column(
            modifier = Modifier
                .width(126.dp)
                .fillMaxHeight()
                .padding(top = 26.dp, bottom = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Destinations.forEach { destination ->
                val selected = route == destination.route
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = selected,
                            role = Role.Tab,
                            onClick = { onNavigate(destination.route) },
                        )
                        .padding(vertical = 12.dp),
                ) {
                    if (selected) {
                        Box(
                            Modifier
                                .align(Alignment.TopStart)
                                .size(width = 4.dp, height = 60.dp)
                                .background(colors.accent),
                        )
                    }
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        NordicCircle(filled = selected, colors = colors) {
                            NavIcon(
                                icon = if (selected) destination.selectedIcon else destination.icon,
                                tint = if (selected) colors.background else colors.text,
                                size = 26,
                                badge = settingsBadge && destination.route == ROUTE_SETTINGS,
                                colors = colors,
                            )
                        }
                        Text(
                            destination.label,
                            fontSize = 15.sp,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (selected) colors.text else colors.muted,
                            maxLines = 1,
                        )
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            InfoButton(
                colors.text,
                onAppInfo,
                Modifier
                    .size(54.dp)
                    .border(1.5.dp, colors.text, CircleShape),
            )
            if (route == ROUTE_DASHBOARD) {
                Box(
                    Modifier
                        .padding(vertical = 16.dp)
                        .size(width = 72.dp, height = 1.dp)
                        .background(colors.line),
                )
                NordicNavToggle(
                    icon = Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                    label = NORDIC_HIDE_LABEL,
                    description = "Скрыть навигацию",
                    colors = colors,
                    onClick = onHide,
                )
            }
        }
        Box(
            Modifier
                .width(1.dp)
                .fillMaxHeight()
                .background(colors.line),
        )
    }
}

/** Кружок «Нордика»: обводка, у выбранного раздела — заливка цветом текста. */
@Composable
private fun NordicCircle(filled: Boolean, colors: StyleColors, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .size(60.dp)
            .border(1.5.dp, colors.text, CircleShape)
            .background(if (filled) colors.text else Color.Transparent, CircleShape),
        contentAlignment = Alignment.Center,
    ) { content() }
}

/** Кнопка «Скрыть» навигацию «Нордика»: кружок со стрелкой и подпись под ним. */
@Composable
private fun NordicNavToggle(
    icon: ImageVector,
    label: String,
    description: String,
    colors: StyleColors,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(role = Role.Button, onClickLabel = description, onClick = onClick)
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        NordicCircle(filled = false, colors = colors) {
            Icon(icon, contentDescription = null, tint = colors.text, modifier = Modifier.size(28.dp))
        }
        Text(label, fontSize = 15.sp, color = colors.muted, maxLines = 1)
    }
}

/** «Бенто»: плавающий док снизу, активный раздел залит акцентом. */
@Composable
private fun BentoDock(
    navController: NavHostController,
    colors: StyleColors,
    settingsBadge: Boolean,
    onNavigate: (String) -> Unit,
    onAppInfo: () -> Unit,
) {
    val route = currentRoute(navController)
    val pill = RoundedCornerShape(percent = 50)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 27.dp, end = 27.dp, bottom = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            stringResource(R.string.app_title),
            modifier = Modifier.width(280.dp),
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold,
            color = colors.text,
        )
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
            Row(
                modifier = Modifier
                    .background(colors.panel, pill)
                    .padding(7.dp),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Destinations.forEach { destination ->
                    val selected = route == destination.route
                    val tint = if (selected) colors.onAccent else colors.muted
                    Row(
                        modifier = Modifier
                            .height(62.dp)
                            .clip(pill)
                            .background(if (selected) colors.accent else Color.Transparent)
                            .selectable(
                                selected = selected,
                                role = Role.Tab,
                                onClick = { onNavigate(destination.route) },
                            )
                            .padding(horizontal = 25.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        NavIcon(
                            icon = if (selected) destination.selectedIcon else destination.icon,
                            tint = tint,
                            size = 24,
                            badge = settingsBadge && destination.route == ROUTE_SETTINGS,
                            colors = colors,
                        )
                        Text(
                            destination.label,
                            fontSize = 18.sp,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                            color = tint,
                        )
                    }
                }
            }
        }
        Box(Modifier.width(280.dp), contentAlignment = Alignment.CenterEnd) {
            InfoButton(
                colors.text,
                onAppInfo,
                Modifier
                    .size(62.dp)
                    .background(colors.panel, CircleShape),
            )
        }
    }
}

/** «Штатный»: меню слева, у активного пункта полоса акцента и подсветка, как в настройках ГУ. */
@Composable
private fun NativeMenu(
    navController: NavHostController,
    colors: StyleColors,
    settingsBadge: Boolean,
    onNavigate: (String) -> Unit,
    onAppInfo: () -> Unit,
) {
    val route = currentRoute(navController)
    Column(
        Modifier
            .width(300.dp)
            .fillMaxHeight()
            .padding(top = 46.dp, bottom = 30.dp),
    ) {
        Column(
            modifier = Modifier.padding(start = 46.dp, end = 16.dp, bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                stringResource(R.string.app_title),
                fontSize = 25.sp,
                fontWeight = FontWeight.Bold,
                color = colors.text,
            )
            Text(CAR_LABEL, fontSize = 17.sp, fontWeight = FontWeight.Medium, color = colors.muted)
        }
        Destinations.forEach { destination ->
            val selected = route == destination.route
            NativeMenuItem(
                icon = if (selected) destination.selectedIcon else destination.icon,
                label = destination.label,
                selected = selected,
                colors = colors,
                badge = settingsBadge && destination.route == ROUTE_SETTINGS,
                modifier = Modifier.selectable(
                    selected = selected,
                    role = Role.Tab,
                    onClick = { onNavigate(destination.route) },
                ),
            )
        }
        Spacer(Modifier.weight(1f))
        NativeMenuItem(
            icon = Icons.Outlined.Info,
            label = APP_INFO_LABEL,
            selected = false,
            colors = colors,
            modifier = Modifier.clickable(role = Role.Button, onClick = onAppInfo),
        )
    }
}

@Composable
private fun NativeMenuItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    colors: StyleColors,
    modifier: Modifier = Modifier,
    badge: Boolean = false,
) {
    val tint = if (selected) colors.accentText else colors.text
    val highlight = if (selected) colors.accent.copy(alpha = 0.16f) else Color.Transparent
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(73.dp)
            .background(Brush.horizontalGradient(listOf(highlight, Color.Transparent)))
            .then(modifier),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .width(4.dp)
                .fillMaxHeight()
                .background(if (selected) colors.accent else Color.Transparent),
        )
        Spacer(Modifier.width(42.dp))
        NavIcon(icon = icon, tint = tint, size = 27, badge = badge, colors = colors)
        Spacer(Modifier.width(18.dp))
        Text(label, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = tint, maxLines = 1)
    }
}

/** Иконка раздела; с [badge] — точка в углу: в разделе есть на что посмотреть. */
@Composable
private fun NavIcon(icon: ImageVector, tint: Color, size: Int, badge: Boolean, colors: StyleColors) {
    Box {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(size.dp))
        if (badge) {
            BadgeDot(colors, Modifier.align(Alignment.TopEnd))
        }
    }
}

@Composable
private fun BadgeDot(colors: StyleColors, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(9.dp)
            .background(colors.reserve, CircleShape),
    )
}

@Composable
private fun InfoButton(tint: Color, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Outlined.Info, contentDescription = APP_INFO_LABEL, tint = tint, modifier = Modifier.size(26.dp))
    }
}
