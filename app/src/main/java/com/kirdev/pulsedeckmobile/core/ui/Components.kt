package com.kirdev.pulsedeckmobile.core.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import com.kirdev.pulsedeckmobile.core.theme.Palette

/** Material 3 linear indicator (6 dp, rounded); drawn in one Canvas instead of nested layouts. */
@Composable
fun ThinProgress(fraction: Float, modifier: Modifier = Modifier) {
    Canvas(modifier.height(6.dp)) {
        val radius = CornerRadius(size.height / 2, size.height / 2)
        drawRoundRect(Palette.Track, cornerRadius = radius)
        val width = size.width * fraction.coerceIn(0f, 1f)
        if (width > 0f) drawRoundRect(Palette.accent, size = Size(width, size.height), cornerRadius = radius)
    }
}

@Composable
fun IconButtonSmall(icon: ImageVector, label: String, modifier: Modifier = Modifier, size: Dp = 48.dp, onClick: () -> Unit) {
    Box(
        modifier.size(size).clip(CircleShape).clickable(onClick = onClick).semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = Palette.Text2, modifier = Modifier.size(24.dp))
    }
}

/** Title row of secondary screens: back arrow + title, optional actions on the right. */
@Composable
fun ScreenHeader(title: String, onBack: () -> Unit, actions: @Composable RowScope.() -> Unit = {}) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButtonSmall(AppIcons.Back, "Назад", onClick = onBack)
        Spacer(Modifier.width(8.dp))
        Text(title, fontSize = 22.sp, fontWeight = FontWeight.Normal, color = Palette.Text, modifier = Modifier.weight(1f),
            maxLines = 1, overflow = TextOverflow.Ellipsis)
        actions()
    }
}

/** Title row of top-level tabs (no back arrow), with optional actions on the right. */
@Composable
fun TopTitle(title: String, actions: @Composable RowScope.() -> Unit = {}) {
    Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 16.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Text(title, fontSize = 28.sp, fontWeight = FontWeight.Normal, color = Palette.Text, modifier = Modifier.weight(1f))
        actions()
    }
}

@Composable
fun VSpace(height: Dp) = Spacer(Modifier.height(height))

/** Material 3 filled card: a tonal surface, 20 dp corners, no outline, no shadow. */
@Composable
fun PanelCard(
    modifier: Modifier = Modifier, onClick: (() -> Unit)? = null,
    color: androidx.compose.ui.graphics.Color = Palette.Surface, content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier.clip(RoundedCornerShape(20.dp)).background(color)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(16.dp),
        content = content,
    )
}

/** Round tonal badge for a leading icon (secondary container), as in the mockups. */
@Composable
fun IconBadge(icon: ImageVector, modifier: Modifier = Modifier, size: Dp = 40.dp,
              color: androidx.compose.ui.graphics.Color = Palette.Tonal, tint: androidx.compose.ui.graphics.Color = Palette.OnTonal) {
    Box(modifier.size(size).clip(CircleShape).background(color), contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(size * 0.55f))
    }
}

/** Material 3 filter chip: tonal and checked when chosen, outlined otherwise. */
@Composable
fun ChoiceChip(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(8.dp)
    Row(
        modifier.height(32.dp).clip(shape)
            .then(if (selected) Modifier.background(Palette.Tonal) else Modifier.border(1.dp, Palette.Outline, shape))
            .clickable(onClick = onClick).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (selected) {
            Icon(AppIcons.Check, contentDescription = null, tint = Palette.OnTonal, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(label, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = if (selected) Palette.OnTonal else Palette.Text2, maxLines = 1)
    }
}

/** Material 3 segmented buttons: one choice of a few. */
@Composable
fun Segmented(options: List<Pair<String, String>>, selected: String, modifier: Modifier = Modifier, onPick: (String) -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Row(modifier.fillMaxWidth().height(40.dp).clip(shape).border(1.dp, Palette.Outline, shape)) {
        options.forEachIndexed { index, (key, label) ->
            if (index > 0) Box(Modifier.width(1.dp).fillMaxHeight().background(Palette.Outline))
            val on = key == selected
            Row(
                Modifier.weight(1f).fillMaxHeight().background(if (on) Palette.Tonal else androidx.compose.ui.graphics.Color.Transparent)
                    .clickable { onPick(key) },
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
            ) {
                if (on) {
                    Icon(AppIcons.Check, contentDescription = null, tint = Palette.OnTonal, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                }
                Text(label, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = if (on) Palette.OnTonal else Palette.Text, maxLines = 1)
            }
        }
    }
}

/**
 * Material 3 slider (the thick one of Android 15+): a 16 dp track split by an upright handle.
 * The value is sent when the finger lifts; [onMove] follows it live.
 */
@Composable
fun M3Slider(value: Float, onMove: (Float) -> Unit, onDone: (Float) -> Unit, modifier: Modifier = Modifier, label: String = "") {
    var current by remember { mutableStateOf(value) }
    val doneNow by androidx.compose.runtime.rememberUpdatedState(onDone)
    val moveNow by androidx.compose.runtime.rememberUpdatedState(onMove)
    Box(
        modifier.fillMaxWidth().height(44.dp)
            .pointerInput(Unit) {
                detectTapGestures { current = (it.x / size.width).coerceIn(0f, 1f); moveNow(current); doneNow(current) }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { current = (it.x / size.width).coerceIn(0f, 1f); moveNow(current) },
                    onHorizontalDrag = { change, _ -> change.consume(); current = (change.position.x / size.width).coerceIn(0f, 1f); moveNow(current) },
                    onDragEnd = { doneNow(current) }, onDragCancel = { doneNow(current) },
                )
            }
            .semantics { contentDescription = label },
        contentAlignment = Alignment.CenterStart,
    ) {
        val active = Palette.accent
        val rest = Palette.Tonal
        Canvas(Modifier.fillMaxWidth().height(44.dp)) {
            val v = value.coerceIn(0f, 1f)
            val gap = 6.dp.toPx()
            val handle = 4.dp.toPx()
            val track = 16.dp.toPx()
            val top = (size.height - track) / 2
            val x = (size.width - handle) * v
            val r = track / 2
            if (x - gap > 0) drawRoundRect(active, androidx.compose.ui.geometry.Offset(0f, top), Size(x - gap, track), CornerRadius(r))
            if (x + handle + gap < size.width) drawRoundRect(rest, androidx.compose.ui.geometry.Offset(x + handle + gap, top),
                Size(size.width - x - handle - gap, track), CornerRadius(r))
            drawRoundRect(active, androidx.compose.ui.geometry.Offset(x, 0f), Size(handle, size.height), CornerRadius(handle / 2))
        }
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier, action: String? = null, onAction: (() -> Unit)? = null) {
    Row(modifier.fillMaxWidth().padding(start = 4.dp, top = 20.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Palette.accent, modifier = Modifier.weight(1f))
        if (action != null && onAction != null) {
            Text(action, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Palette.accent, modifier = Modifier.clip(RoundedCornerShape(20.dp))
                .clickable(onClick = onAction).padding(horizontal = 12.dp, vertical = 6.dp))
        }
    }
}

/** List row: icon, title + subtitle, optional trailing content; chevron when it opens something. */
@Composable
fun ListRow(
    icon: ImageVector?, title: String, subtitle: String? = null, modifier: Modifier = Modifier,
    chevron: Boolean = false, onClick: (() -> Unit)? = null, trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 8.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = Palette.Text2, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(16.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp, color = Palette.Text, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (!subtitle.isNullOrBlank()) {
                Text(subtitle, fontSize = 12.sp, color = Palette.Text3, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        trailing?.invoke()
        if (chevron) Icon(AppIcons.Chevron, contentDescription = null, tint = Palette.Text3, modifier = Modifier.size(18.dp))
    }
}

/** Dangerous actions (shutdown, kill, delete) always pass through this dialog. */
@Composable
fun ConfirmDialog(
    title: String, text: String, confirm: String, onConfirm: () -> Unit,
    icon: ImageVector? = null, danger: Boolean = true, onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Palette.Surface2,
        shape = RoundedCornerShape(28.dp),
        icon = icon?.let { { Icon(it, contentDescription = null, tint = if (danger) Palette.Error else Palette.accent) } },
        title = { Text(title, color = Palette.Text, fontSize = 24.sp, textAlign = TextAlign.Center) },
        text = { Text(text, color = Palette.Text2, fontSize = 14.sp) },
        confirmButton = {
            Box(Modifier.height(40.dp).clip(RoundedCornerShape(20.dp)).background(if (danger) Palette.Error else Palette.accent)
                .clickable { onDismiss(); onConfirm() }.padding(horizontal = 20.dp), contentAlignment = Alignment.Center) {
                Text(confirm, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                    color = if (danger) Palette.roles.onErr else Palette.onAccent)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена", color = Palette.accent, fontWeight = FontWeight.SemiBold) } },
    )
}

/** Text input dialog (folder name, rename, URL). */
@Composable
fun InputDialog(title: String, initial: String, confirm: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    // The old name comes selected: typing replaces it, arrows keep it.
    var field by remember { mutableStateOf(TextFieldValue(initial, TextRange(0, initial.length))) }
    val value = field.text
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Palette.Surface2,
        shape = RoundedCornerShape(28.dp),
        title = { Text(title, color = Palette.Text) },
        text = {
            OutlinedTextField(
                value = field, onValueChange = { field = it }, singleLine = true,
                modifier = Modifier.fillMaxWidth().focusRequester(focus), shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Palette.accent, unfocusedBorderColor = Palette.Outline, cursorColor = Palette.accent,
                    focusedTextColor = Palette.Text, unfocusedTextColor = Palette.Text),
            )
        },
        confirmButton = {
            TextButton(onClick = { onDismiss(); onConfirm(value.trim()) }, enabled = value.isNotBlank()) {
                Text(confirm, color = if (value.isNotBlank()) Palette.accent else Palette.Disabled, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена", color = Palette.accent) } },
    )
}

@Composable
fun PanelTextField(value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier, placeholder: String = "",
                   singleLine: Boolean = true, keyboard: KeyboardOptions = KeyboardOptions.Default) {
    OutlinedTextField(
        value = value, onValueChange = onChange, modifier = modifier.fillMaxWidth(), singleLine = singleLine,
        placeholder = { Text(placeholder, color = Palette.Text3) }, keyboardOptions = keyboard,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Palette.accent, unfocusedBorderColor = Palette.Outline, cursorColor = Palette.accent,
            focusedTextColor = Palette.Text, unfocusedTextColor = Palette.Text,
            focusedContainerColor = androidx.compose.ui.graphics.Color.Transparent, unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent),
    )
}

@Composable
fun PanelSwitch(checked: Boolean, onChange: (Boolean) -> Unit, enabled: Boolean = true) {
    Switch(checked = checked, onCheckedChange = onChange, enabled = enabled, colors = SwitchDefaults.colors(
        checkedThumbColor = Palette.onAccent, checkedTrackColor = Palette.accent, checkedBorderColor = Palette.accent,
        uncheckedThumbColor = Palette.Outline, uncheckedTrackColor = Palette.Track, uncheckedBorderColor = Palette.Outline))
}

/** Material 3 filled (primary) or outlined (secondary) button. */
@Composable
fun PillButton(text: String, modifier: Modifier = Modifier, primary: Boolean = true, enabled: Boolean = true, onClick: () -> Unit) {
    val shape = RoundedCornerShape(50)
    Box(
        modifier.height(44.dp).clip(shape)
            .then(if (primary) Modifier.background(if (enabled) Palette.accent else Palette.Disabled.copy(alpha = 0.12f))
                  else Modifier.border(1.dp, if (enabled) Palette.Outline else Palette.Disabled, shape))
            .clickable(enabled = enabled, onClick = onClick).padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
            color = if (!enabled) Palette.Disabled else if (primary) Palette.onAccent else Palette.accent)
    }
}

/** Empty or error state in the middle of a screen. */
@Composable
fun Placeholder(text: String, modifier: Modifier = Modifier, action: String? = null, onAction: (() -> Unit)? = null) {
    Column(modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text, fontSize = 14.sp, color = Palette.Text2, textAlign = TextAlign.Center)
        if (action != null && onAction != null) {
            Spacer(Modifier.height(14.dp))
            PillButton(action, primary = false, onClick = onAction)
        }
    }
}
