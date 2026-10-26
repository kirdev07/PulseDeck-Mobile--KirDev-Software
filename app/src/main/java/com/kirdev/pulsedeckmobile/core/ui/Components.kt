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
import com.kirdev.pulsedeckmobile.core.theme.Palette

/** 3 dp bar; drawn in one Canvas instead of nested layouts. */
@Composable
fun ThinProgress(fraction: Float, modifier: Modifier = Modifier) {
    Canvas(modifier.height(3.dp)) {
        val radius = CornerRadius(size.height / 2, size.height / 2)
        drawRoundRect(Palette.Track, cornerRadius = radius)
        val width = size.width * fraction.coerceIn(0f, 1f)
        if (width > 0f) drawRoundRect(Palette.accent, size = Size(width, size.height), cornerRadius = radius)
    }
}

@Composable
fun IconButtonSmall(icon: ImageVector, label: String, modifier: Modifier = Modifier, size: Dp = 40.dp, onClick: () -> Unit) {
    Box(
        modifier.size(size).clip(CircleShape).clickable(onClick = onClick).semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = Palette.Text2, modifier = Modifier.size(22.dp))
    }
}

/** Title row of secondary screens: back arrow + title, optional actions on the right. */
@Composable
fun ScreenHeader(title: String, onBack: () -> Unit, actions: @Composable RowScope.() -> Unit = {}) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButtonSmall(AppIcons.Back, "Назад", onClick = onBack)
        Spacer(Modifier.width(8.dp))
        Text(title, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Palette.Text, modifier = Modifier.weight(1f),
            maxLines = 1, overflow = TextOverflow.Ellipsis)
        actions()
    }
}

/** Title row of top-level tabs (no back arrow), with optional actions on the right. */
@Composable
fun TopTitle(title: String, actions: @Composable RowScope.() -> Unit = {}) {
    Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 12.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Text(title, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = Palette.Text, modifier = Modifier.weight(1f))
        actions()
    }
}

@Composable
fun VSpace(height: Dp) = Spacer(Modifier.height(height))

/** Dark rounded card of the mockups: 1 dp outline, no shadow. */
@Composable
fun PanelCard(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier.clip(RoundedCornerShape(18.dp)).background(Palette.Surface)
            .border(1.dp, Palette.Border, RoundedCornerShape(18.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(14.dp),
        content = content,
    )
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier, action: String? = null, onAction: (() -> Unit)? = null) {
    Row(modifier.fillMaxWidth().padding(top = 18.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Palette.Text, modifier = Modifier.weight(1f))
        if (action != null && onAction != null) {
            Text(action, fontSize = 12.sp, color = Palette.Text2, modifier = Modifier.clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onAction).padding(horizontal = 6.dp, vertical = 2.dp))
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
        modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = Palette.Text2, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(14.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 14.sp, color = Palette.Text, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
fun ConfirmDialog(title: String, text: String, confirm: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Palette.Surface2,
        title = { Text(title, color = Palette.Text) },
        text = { Text(text, color = Palette.Text2) },
        confirmButton = { TextButton(onClick = { onDismiss(); onConfirm() }) { Text(confirm, color = Palette.Text) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена", color = Palette.Text2) } },
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
        title = { Text(title, color = Palette.Text) },
        text = {
            OutlinedTextField(
                value = field, onValueChange = { field = it }, singleLine = true,
                modifier = Modifier.fillMaxWidth().focusRequester(focus), shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Palette.Text2, unfocusedBorderColor = Palette.Border, cursorColor = Palette.Text,
                    focusedTextColor = Palette.Text, unfocusedTextColor = Palette.Text),
            )
        },
        confirmButton = {
            TextButton(onClick = { onDismiss(); onConfirm(value.trim()) }, enabled = value.isNotBlank()) {
                Text(confirm, color = if (value.isNotBlank()) Palette.Text else Palette.Disabled)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена", color = Palette.Text2) } },
    )
}

@Composable
fun PanelTextField(value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier, placeholder: String = "",
                   singleLine: Boolean = true, keyboard: KeyboardOptions = KeyboardOptions.Default) {
    OutlinedTextField(
        value = value, onValueChange = onChange, modifier = modifier.fillMaxWidth(), singleLine = singleLine,
        placeholder = { Text(placeholder, color = Palette.Text3) }, keyboardOptions = keyboard,
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Palette.Text2, unfocusedBorderColor = Palette.Border, cursorColor = Palette.Text,
            focusedTextColor = Palette.Text, unfocusedTextColor = Palette.Text,
            focusedContainerColor = Palette.Surface, unfocusedContainerColor = Palette.Surface),
    )
}

@Composable
fun PanelSwitch(checked: Boolean, onChange: (Boolean) -> Unit, enabled: Boolean = true) {
    Switch(checked = checked, onCheckedChange = onChange, enabled = enabled, colors = SwitchDefaults.colors(
        checkedThumbColor = Palette.Black, checkedTrackColor = Palette.accent,
        uncheckedThumbColor = Palette.Text2, uncheckedTrackColor = Palette.Surface2, uncheckedBorderColor = Palette.Border))
}

/** Filled pill (primary) or outlined (secondary) button. */
@Composable
fun PillButton(text: String, modifier: Modifier = Modifier, primary: Boolean = true, enabled: Boolean = true, onClick: () -> Unit) {
    val shape = RoundedCornerShape(50)
    Box(
        modifier.height(46.dp).clip(shape)
            .background(if (primary && enabled) Palette.accent else Palette.Surface)
            .border(1.dp, if (primary && enabled) Palette.accent else Palette.Border, shape)
            .clickable(enabled = enabled, onClick = onClick).padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
            color = if (primary && enabled) Palette.Black else if (enabled) Palette.Text else Palette.Disabled)
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
