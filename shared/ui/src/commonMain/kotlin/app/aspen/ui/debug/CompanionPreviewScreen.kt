package app.aspen.ui.debug

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import app.aspen.design.AspenTheme
import app.aspen.design.components.AspenCard
import app.aspen.design.components.AspenChoiceChip
import app.aspen.design.components.AspenQuietButton
import app.aspen.design.components.AspenScreenHeader
import app.aspen.design.components.AspenTextAction
import app.aspen.domain.ai.CompanionMoment
import app.aspen.domain.ai.CompanionVoice
import app.aspen.domain.onboarding.AppConfigProvider
import app.aspen.domain.safety.CrisisSignals
import app.aspen.domain.safety.SafetyEngine
import app.aspen.domain.safety.SafetyVerdict
import app.aspen.ui.generated.resources.Res
import app.aspen.ui.generated.resources.back
import app.aspen.ui.generated.resources.debug_companion_line
import app.aspen.ui.generated.resources.debug_companion_moment
import app.aspen.ui.generated.resources.debug_companion_title
import app.aspen.ui.generated.resources.debug_companion_tone
import app.aspen.ui.generated.resources.debug_guard_crisis
import app.aspen.ui.generated.resources.debug_guard_hint
import app.aspen.ui.generated.resources.debug_guard_no_crisis
import app.aspen.ui.generated.resources.debug_guard_pass
import app.aspen.ui.generated.resources.debug_guard_rewrite
import app.aspen.ui.generated.resources.debug_guard_run
import app.aspen.ui.generated.resources.debug_guard_title
import app.aspen.ui.companion.companionLineText
import org.jetbrains.compose.resources.stringResource

/**
 * DEBUG-ONLY (docs/07 Phase 4, per approval: "content testable in the application under debug").
 * Only reachable when [app.aspen.ui.AspenDeps.isDebugBuild]; never linked in release builds.
 *
 * Two panels: (1) the Tier-1 companion voice — cycle moments, override tone, see which curated line
 * the selector picks (and its key, for the review sheet); (2) a guard playground — run any text
 * through [CrisisSignals] and [SafetyEngine.guardOutput] and see the verdicts the pipeline enforces.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CompanionPreviewScreen(
    voice: CompanionVoice,
    appConfigProvider: AppConfigProvider,
    safetyEngine: SafetyEngine?,
    crisisSignals: CrisisSignals?,
    onBack: () -> Unit,
) {
    var moment by remember { mutableStateOf(CompanionMoment.GREETING) }
    var variant by remember { mutableStateOf(0) }
    val baseConfig = remember { appConfigProvider.current() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = AspenTheme.spacing.l, vertical = AspenTheme.spacing.xl),
        verticalArrangement = Arrangement.spacedBy(AspenTheme.spacing.sm),
    ) {
        AspenScreenHeader(title = stringResource(Res.string.debug_companion_title))
        AspenTextAction(label = stringResource(Res.string.back), onClick = onBack)
        Spacer(Modifier.height(AspenTheme.spacing.s))

        Text(
            stringResource(Res.string.debug_companion_moment),
            style = AspenTheme.typography.label,
            color = AspenTheme.colors.textSecondary,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(AspenTheme.spacing.s),
            verticalArrangement = Arrangement.spacedBy(AspenTheme.spacing.xs),
        ) {
            CompanionMoment.entries.forEach { m ->
                AspenChoiceChip(label = m.name, selected = m == moment, onToggle = { moment = m })
            }
        }

        Text(
            "${stringResource(Res.string.debug_companion_tone)}: ${baseConfig.companionTone.name}",
            style = AspenTheme.typography.caption,
            color = AspenTheme.colors.textMuted,
        )

        val line = remember(moment, variant) { voice.line(moment, baseConfig, variant) }
        AspenCard(modifier = Modifier.fillMaxWidth(), onClick = { variant += 1 }) {
            Text(
                stringResource(Res.string.debug_companion_line) + ": ${line.key}",
                style = AspenTheme.typography.caption,
                color = AspenTheme.colors.textMuted,
            )
            Spacer(Modifier.height(AspenTheme.spacing.s))
            Text(
                companionLineText(line.key),
                style = AspenTheme.typography.bodyLoose,
                color = AspenTheme.colors.textPrimary,
            )
        }

        if (safetyEngine != null && crisisSignals != null) {
            Spacer(Modifier.height(AspenTheme.spacing.m))
            GuardPlayground(safetyEngine, crisisSignals)
        }
    }
}

@Composable
private fun GuardPlayground(safetyEngine: SafetyEngine, crisisSignals: CrisisSignals) {
    var input by remember { mutableStateOf("") }
    var verdict by remember { mutableStateOf<String?>(null) }
    val passLabel = stringResource(Res.string.debug_guard_pass)
    val rewriteLabel = stringResource(Res.string.debug_guard_rewrite)
    val crisisLabel = stringResource(Res.string.debug_guard_crisis)
    val noCrisisLabel = stringResource(Res.string.debug_guard_no_crisis)

    Text(
        stringResource(Res.string.debug_guard_title),
        style = AspenTheme.typography.label,
        color = AspenTheme.colors.textSecondary,
    )
    OutlinedTextField(
        value = input,
        onValueChange = { input = it },
        placeholder = { Text(stringResource(Res.string.debug_guard_hint), color = AspenTheme.colors.textMuted) },
        textStyle = AspenTheme.typography.body,
        shape = AspenTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
    )
    AspenQuietButton(
        label = stringResource(Res.string.debug_guard_run),
        onClick = {
            val guard = when (safetyEngine.guardOutput(input)) {
                is SafetyVerdict.Pass -> passLabel
                is SafetyVerdict.Rewrite -> rewriteLabel
            }
            val crisis = if (crisisSignals.suggestsCrisis(input)) crisisLabel else noCrisisLabel
            verdict = "$guard\n$crisis"
        },
        enabled = input.isNotBlank(),
        modifier = Modifier.fillMaxWidth(),
    )
    verdict?.let {
        Text(it, style = AspenTheme.typography.body, color = AspenTheme.colors.textPrimary)
    }
}
