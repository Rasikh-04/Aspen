package app.aspen.ui.reflect

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import app.aspen.design.AspenTheme
import app.aspen.domain.logging.model.FeelingTag
import app.aspen.ui.generated.resources.Res
import app.aspen.ui.generated.resources.feeling_alone
import app.aspen.ui.generated.resources.feeling_anxious
import app.aspen.ui.generated.resources.feeling_calm
import app.aspen.ui.generated.resources.feeling_content
import app.aspen.ui.generated.resources.feeling_frustrated
import app.aspen.ui.generated.resources.feeling_guilty
import app.aspen.ui.generated.resources.feeling_hopeful
import app.aspen.ui.generated.resources.feeling_numb
import app.aspen.ui.generated.resources.feeling_overwhelmed
import app.aspen.ui.generated.resources.feeling_relieved
import app.aspen.ui.generated.resources.feeling_sad
import app.aspen.ui.generated.resources.feeling_tired

/** Localized label for a feeling tag. The domain enum carries no copy (CLAUDE.md #11). */
fun feelingLabel(tag: FeelingTag): StringResource = when (tag) {
    FeelingTag.CALM -> Res.string.feeling_calm
    FeelingTag.CONTENT -> Res.string.feeling_content
    FeelingTag.RELIEVED -> Res.string.feeling_relieved
    FeelingTag.HOPEFUL -> Res.string.feeling_hopeful
    FeelingTag.TIRED -> Res.string.feeling_tired
    FeelingTag.NUMB -> Res.string.feeling_numb
    FeelingTag.ANXIOUS -> Res.string.feeling_anxious
    FeelingTag.OVERWHELMED -> Res.string.feeling_overwhelmed
    FeelingTag.SAD -> Res.string.feeling_sad
    FeelingTag.FRUSTRATED -> Res.string.feeling_frustrated
    FeelingTag.GUILTY -> Res.string.feeling_guilty
    FeelingTag.ALONE -> Res.string.feeling_alone
}

/** Feeling-tag picker: emotions only, no intensity scale or count (SR-1, numberless). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FeelingTagChips(selected: Set<FeelingTag>, onToggle: (FeelingTag) -> Unit) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(AspenTheme.spacing.s),
    ) {
        FeelingTag.entries.forEach { tag ->
            val isOn = tag in selected
            FilterChip(
                selected = isOn,
                onClick = { onToggle(tag) },
                label = { Text(stringResource(feelingLabel(tag))) },
                shape = AspenTheme.shapes.large,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = AspenTheme.colors.cautionBg,
                    selectedLabelColor = AspenTheme.colors.textPrimary,
                ),
            )
        }
    }
}
