package org.breezyweather.ui.common.widgets

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Neon cyan used for the focused / pressed state: high contrast on light and dark cards and on a TV. */
private val NeonCyan = Color(0xFF00E5FF)

/**
 * Alerta EC action button: no solid fill (transparent), a thin outline and a soft shadow at rest; when
 * focused (D-pad / Android TV), selected or pressed it glows with a thick neon outline and a coloured shadow,
 * so the active action is obvious on a phone and from the sofa.
 */
@Composable
fun NeonButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    selected: Boolean = false,
    content: @Composable RowScope.() -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val pressed by interaction.collectIsPressedAsState()
    val active = enabled && (focused || pressed || selected)
    val shape = RoundedCornerShape(50)
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = shape,
        interactionSource = interaction,
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color.Transparent,
            contentColor = if (active) NeonCyan else MaterialTheme.colorScheme.primary,
        ),
        border = BorderStroke(
            width = if (active) 3.dp else 1.dp,
            color = if (active) NeonCyan else MaterialTheme.colorScheme.outline,
        ),
        modifier = modifier.shadow(
            elevation = if (active) 14.dp else 4.dp,
            shape = shape,
            clip = false,
            ambientColor = if (active) NeonCyan else Color.Black,
            spotColor = if (active) NeonCyan else Color.Black,
        ),
        content = content,
    )
}
