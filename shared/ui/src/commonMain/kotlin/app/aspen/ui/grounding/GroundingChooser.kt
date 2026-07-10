package app.aspen.ui.grounding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.phosphor.icons.PhIcons
import com.phosphor.icons.duotone.HandHeartDuotone
import com.phosphor.icons.duotone.NotePencilDuotone
import com.phosphor.icons.duotone.TargetDuotone
import com.phosphor.icons.duotone.WavesDuotone
import com.phosphor.icons.duotone.WindDuotone
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import app.aspen.design.AspenTheme
import app.aspen.design.components.AspenCard
import app.aspen.design.components.AspenScreenHeader
import app.aspen.ui.generated.resources.Res
import app.aspen.ui.generated.resources.grounding_54321
import app.aspen.ui.generated.resources.grounding_breathe
import app.aspen.ui.generated.resources.grounding_reach_someone
import app.aspen.ui.generated.resources.grounding_ride_urge
import app.aspen.ui.generated.resources.grounding_subtitle
import app.aspen.ui.generated.resources.grounding_talk
import app.aspen.ui.generated.resources.grounding_title

/**
 * Flow A chooser (docs/06 §3): a quiet, few-option set of grounding routes — no scrolling pressure,
 * no metrics. Each route is a whole-card touch target. "Reach someone" is always present as the
 * human exit (CLAUDE.md #6).
 */
@Composable
fun GroundingChooser(
    onBreathe: () -> Unit,
    onGround54321: () -> Unit,
    onRideUrge: () -> Unit,
    onWriteItDown: () -> Unit,
    onReachSomeone: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = AspenTheme.spacing.l, vertical = AspenTheme.spacing.xl),
        verticalArrangement = Arrangement.spacedBy(AspenTheme.spacing.sm),
    ) {
        AspenScreenHeader(
            title = stringResource(Res.string.grounding_title),
            subtitle = stringResource(Res.string.grounding_subtitle),
        )
        Spacer(Modifier.height(AspenTheme.spacing.m))
        ChooserOption(Res.string.grounding_breathe, PhIcons.Duotone.WindDuotone, onBreathe)
        ChooserOption(Res.string.grounding_54321, PhIcons.Duotone.TargetDuotone, onGround54321)
        ChooserOption(Res.string.grounding_ride_urge, PhIcons.Duotone.WavesDuotone, onRideUrge)
        ChooserOption(Res.string.grounding_talk, PhIcons.Duotone.NotePencilDuotone, onWriteItDown)
        ChooserOption(Res.string.grounding_reach_someone, PhIcons.Duotone.HandHeartDuotone, onReachSomeone)
    }
}

@Composable
private fun ChooserOption(label: StringResource, icon: ImageVector, onClick: () -> Unit) {
    AspenCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.heightIn(min = 32.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AspenTheme.spacing.s),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = AspenTheme.colors.primary,
                modifier = Modifier.size(24.dp),
            )
            Text(
                text = stringResource(label),
                style = AspenTheme.typography.body,
                color = AspenTheme.colors.textPrimary,
            )
        }
    }
}
