package com.ony.app

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Outer corners are generous; corners where two blocks join are small, as in the reference.
private val Outer = 26.dp
private val Joined = 12.dp
private val BlockShape = RoundedCornerShape(Outer)
val TopBlock = RoundedCornerShape(Outer, Outer, Joined, Joined)
val BottomBlock = RoundedCornerShape(Joined, Joined, Outer, Outer)
private val RowShape = RoundedCornerShape(Outer)

/** A dark rounded block, the basic unit of the layout. */
@Composable
fun InkBlock(
    modifier: Modifier = Modifier,
    shape: Shape = BlockShape,
    horizontalPadding: Dp = 20.dp,
    bottomPadding: Dp = 18.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier.fillMaxWidth().clip(shape).background(Ony.Block)
            .padding(start = horizontalPadding, end = horizontalPadding, top = 18.dp, bottom = bottomPadding),
        content = content,
    )
}

/**
 * The joint between two stacked ink blocks: one wide neck across the middle, so the only
 * gaps are small rounded notches at the outer edges, like a pinched waist.
 */
@Composable
fun Joint(color: Color = Ony.Block) {
    Canvas(Modifier.fillMaxWidth().height(10.dp)) {
        val g = size.height
        val f = 5.dp.toPx()            // shoulder radius of each notch
        val x0 = Joined.toPx() + f     // neck starts where the small joined corner ends
        val x1 = size.width - x0
        val neck = Path().apply {
            moveTo(x0 - f, 0f)
            quadraticBezierTo(x0, 0f, x0, f)
            lineTo(x0, g - f)
            quadraticBezierTo(x0, g, x0 - f, g)
            lineTo(x1 + f, g)
            quadraticBezierTo(x1, g, x1, g - f)
            lineTo(x1, f)
            quadraticBezierTo(x1, 0f, x1 + f, 0f)
            close()
        }
        drawPath(neck, color)
    }
}

/** A light outlined row with a value and a chevron. */
@Composable
fun OutlineRow(
    title: String,
    onClick: () -> Unit,
    value: String? = null,
    valueColor: Color = Ony.PageMuted,
    icon: (@Composable () -> Unit)? = null,
) {
    Surface(
        onClick = onClick,
        shape = RowShape,
        color = Ony.Page,
        contentColor = Ony.OnPage,
        border = BorderStroke(1.2.dp, Ony.Outline),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.heightIn(min = 60.dp).padding(start = 20.dp, end = 13.dp), verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) { icon(); Spacer(Modifier.size(12.dp)) }
            Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            if (value != null) Text(value, style = MaterialTheme.typography.bodyMedium, color = valueColor, modifier = Modifier.padding(horizontal = 6.dp))
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, modifier = Modifier.size(20.dp))
        }
    }
}

/**
 * A row that opens in place: the header turns ink and a bridged ink panel unfolds under it,
 * the one motion the reference design leans on.
 */
@Composable
fun UnfoldingRow(
    title: String,
    value: String?,
    open: Boolean,
    onToggle: () -> Unit,
    panelPadding: Dp = 20.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val fill by animateColorAsState(if (open) Ony.Block else Ony.Page, tween(220), label = "fill")
    val ink by animateColorAsState(if (open) Ony.OnBlock else Ony.OnPage, tween(220), label = "ink")
    val edge by animateColorAsState(if (open) Ony.Block else Ony.Outline, tween(220), label = "edge")
    val lower by animateDpAsState(if (open) Joined else Outer, tween(220), label = "corner")
    val headShape = RoundedCornerShape(Outer, Outer, lower, lower)
    Column {
        Row(
            Modifier.fillMaxWidth().clip(headShape).background(fill).border(1.2.dp, edge, headShape)
                .clickable(onClick = onToggle).heightIn(min = 60.dp).padding(start = 20.dp, end = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = ink, modifier = Modifier.weight(1f))
            if (value != null && !open) Text(value, style = MaterialTheme.typography.bodyMedium, color = Ony.PageMuted, modifier = Modifier.padding(horizontal = 6.dp))
            Icon(
                if (open) Icons.Rounded.KeyboardArrowDown else Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = ink,
                modifier = Modifier.size(20.dp),
            )
        }
        AnimatedVisibility(open, enter = expandVertically(tween(260)) + fadeIn(tween(260)), exit = shrinkVertically(tween(200)) + fadeOut(tween(160))) {
            Column {
                Joint()
                InkBlock(shape = BottomBlock, horizontalPadding = panelPadding, content = content)
            }
        }
    }
}

/** Small lavender tag with spaced caps, for status. */
@Composable
fun Tag(text: String) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelLarge,
        color = Ony.OnAccent,
        modifier = Modifier.clip(RoundedCornerShape(7.dp)).background(Ony.Accent).padding(horizontal = 10.dp, vertical = 5.dp),
    )
}

/** Pill button with spaced caps. Filled ink, outlined, or lavender for use on ink. */
enum class PillStyle { INK, OUTLINE, LAVENDER, GHOST_ON_INK }

@Composable
fun PillButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, style: PillStyle = PillStyle.INK, enabled: Boolean = true) {
    val (bg, fg, border) = when (style) {
        PillStyle.INK -> Triple(Ony.Block, Ony.OnBlock, null)
        PillStyle.OUTLINE -> Triple(Ony.Page, Ony.OnPage, Ony.Outline)
        PillStyle.LAVENDER -> Triple(Ony.Accent, Ony.OnAccent, null)
        PillStyle.GHOST_ON_INK -> Triple(Color.Transparent, Ony.OnBlock, Ony.BlockMuted)
    }
    Box(
        modifier.heightIn(min = 48.dp).clip(RowShape).background(bg)
            .then(if (border != null) Modifier.border(1.2.dp, border, RowShape) else Modifier)
            .clickable(enabled = enabled, onClick = onClick).alpha(if (enabled) 1f else 0.45f)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) { Text(text.uppercase(), style = MaterialTheme.typography.labelLarge, color = fg) }
}

/**
 * A segmented pill, as in the reference's bottom control: an ink track with the chosen
 * segment in lavender.
 */
@Composable
fun Segmented(options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Row(Modifier.clip(RowShape).background(Ony.Block).padding(4.dp)) {
        options.forEachIndexed { i, label ->
            val on = i == selected
            val bg by animateColorAsState(if (on) Ony.Accent else Color.Transparent, tween(180), label = "seg$i")
            val fg by animateColorAsState(if (on) Ony.OnAccent else Ony.OnBlock, tween(180), label = "segText$i")
            Box(
                Modifier.clip(RowShape).background(bg)
                    .selectable(selected = on, role = Role.Tab, onClick = { onSelect(i) })
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) { Text(label.uppercase(), style = MaterialTheme.typography.labelLarge, color = fg) }
        }
    }
}

/** A choice inside an ink panel: app icon, name, tick when chosen. */
@Composable
fun InkChoice(app: MusicApp, chosen: Boolean, installed: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
            .background(if (chosen) Ony.BlockRaised else Color.Transparent)
            .clickable(enabled = installed, onClick = onClick).alpha(if (installed) 1f else 0.4f)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcon(app, 34.dp)
        Text(app.label, style = MaterialTheme.typography.bodyLarge, color = Ony.OnBlock, modifier = Modifier.padding(start = 12.dp).weight(1f))
        when {
            chosen -> Icon(Icons.Rounded.Check, contentDescription = "Chosen", tint = Ony.Accent, modifier = Modifier.size(20.dp))
            !installed -> Text("Not installed", style = MaterialTheme.typography.bodySmall, color = Ony.BlockMuted)
        }
    }
}

/** The app's real launcher icon, read from the phone. A neutral tile if it isn't installed. */
@Composable
fun AppIcon(app: MusicApp, size: Dp = 30.dp) {
    val context = LocalContext.current
    val bitmap = remember(app) { Launch.appIcon(context, app.packageName) }
    val shape = RoundedCornerShape(size * 0.26f)
    if (bitmap != null) {
        Image(bitmap, contentDescription = null, modifier = Modifier.size(size).clip(shape))
    } else {
        Box(Modifier.size(size).clip(shape).background(Ony.BlockRaised))
    }
}

@Composable
fun Gap(height: Dp) = Spacer(Modifier.height(height))
