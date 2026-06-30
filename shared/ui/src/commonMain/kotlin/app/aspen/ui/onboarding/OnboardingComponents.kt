package app.aspen.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import app.aspen.design.AspenTheme
import app.aspen.ui.generated.resources.Res
import app.aspen.ui.generated.resources.onb_back
import app.aspen.ui.generated.resources.onb_skip_question

/** One selectable answer: an internal domain [value] paired with its localized [label]. */
data class ChoiceOption<T>(val value: T, val label: StringResource)

/**
 * The calm one-question-per-screen shell (docs/06 §3 Flow 0; §1 "a quiet room, not a dashboard").
 * Generous spacing, a soft presence-style progress (dots, never "3 of 10" — CLAUDE.md #3), an always
 * available Back/Skip, and a single forward action. Content scrolls so dynamic-type never clips (a11y).
 */
@Composable
fun OnboardingScaffold(
    title: String,
    onBack: (() -> Unit)?,
    onSkipQuestion: (() -> Unit)?,
    progress: Pair<Int, Int>?,
    primaryLabel: String,
    onPrimary: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AspenTheme.colors.background)
            .padding(horizontal = AspenTheme.spacing.l, vertical = AspenTheme.spacing.l),
    ) {
        // Top row: Back (left) + soft progress dots (right). No loud chrome.
        Row(
            modifier = Modifier.fillMaxWidth().height(40.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            if (onBack != null) {
                TextButton(onClick = onBack) {
                    Text(stringResource(Res.string.onb_back), color = AspenTheme.colors.textSecondary)
                }
            } else {
                Spacer(Modifier.size(1.dp))
            }
            if (progress != null) ProgressDots(progress.first, progress.second)
        }

        Spacer(Modifier.height(AspenTheme.spacing.xl))
        Text(
            text = title,
            style = AspenTheme.typography.title,
            color = AspenTheme.colors.textPrimary,
        )
        Spacer(Modifier.height(AspenTheme.spacing.l))

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(AspenTheme.spacing.sm),
            content = content,
        )

        Spacer(Modifier.height(AspenTheme.spacing.l))
        if (onSkipQuestion != null) {
            TextButton(
                onClick = onSkipQuestion,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            ) {
                Text(stringResource(Res.string.onb_skip_question), color = AspenTheme.colors.textSecondary)
            }
        }
        Button(
            onClick = onPrimary,
            shape = AspenTheme.shapes.large,
            colors = ButtonDefaults.buttonColors(
                containerColor = AspenTheme.colors.primary,
                contentColor = AspenTheme.colors.textInverse,
            ),
            modifier = Modifier.fillMaxWidth().height(56.dp),
        ) {
            Text(primaryLabel, style = AspenTheme.typography.label)
        }
    }
}

/** Progress shown as *presence* — filled vs. soft dots, never a number or fraction (CLAUDE.md #3). */
@Composable
private fun ProgressDots(index: Int, count: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        for (i in 1..count) {
            val color = if (i <= index) AspenTheme.colors.primary else AspenTheme.colors.border
            Box(Modifier.size(6.dp).clip(CircleShape).background(color))
        }
    }
}

/** Single-select answer list (radio semantics). Selecting one replaces any previous choice. */
@Composable
fun <T> SingleChoiceColumn(
    options: List<ChoiceOption<T>>,
    selected: T?,
    onSelect: (T) -> Unit,
) {
    options.forEach { option ->
        OptionRow(
            label = stringResource(option.label),
            selected = option.value == selected,
            onClick = { onSelect(option.value) },
            role = Role.RadioButton,
        )
    }
}

/** Multi-select answer list (checkbox semantics). Tapping toggles membership. */
@Composable
fun <T> MultiChoiceColumn(
    options: List<ChoiceOption<T>>,
    selected: Set<T>,
    onToggle: (T) -> Unit,
) {
    options.forEach { option ->
        val isSelected = option.value in selected
        OptionRow(
            label = stringResource(option.label),
            selected = isSelected,
            onClick = { onToggle(option.value) },
            role = Role.Checkbox,
        )
    }
}

/**
 * A soft, tappable answer row. Selection is shown with a calm border + a filled dot (never a hard
 * check or alarm colour). Hit target ≥ 56dp (a11y, docs/06 §7). Whole row is the target.
 */
@Composable
private fun OptionRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    role: Role,
) {
    val borderColor = if (selected) AspenTheme.colors.primary else AspenTheme.colors.border
    Surface(
        shape = AspenTheme.shapes.medium,
        color = AspenTheme.colors.surface,
        modifier = Modifier
            .fillMaxWidth()
            .let {
                if (role == Role.RadioButton) {
                    it.selectable(selected = selected, role = role, onClick = onClick)
                } else {
                    it.toggleable(value = selected, role = role, onValueChange = { onClick() })
                }
            },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = AspenTheme.spacing.m),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = label,
                style = AspenTheme.typography.body,
                color = AspenTheme.colors.textPrimary,
                modifier = Modifier.weight(1f).padding(end = AspenTheme.spacing.m),
            )
            val dotColor = if (selected) AspenTheme.colors.primary else AspenTheme.colors.border
            Box(
                Modifier.size(if (selected) 14.dp else 12.dp).clip(CircleShape).background(dotColor),
            )
        }
    }
}

/** Plain centered intro/closing body text (intro + closing screens). */
@Composable
fun OnboardingProse(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = AspenTheme.typography.bodyLoose,
        color = AspenTheme.colors.textSecondary,
        textAlign = TextAlign.Start,
        modifier = modifier,
    )
}
