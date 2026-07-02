package app.aspen.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import app.aspen.design.AspenTheme
import app.aspen.design.components.AspenCard
import app.aspen.design.components.AspenScreenHeader
import androidx.compose.foundation.layout.Row
import app.aspen.design.components.AspenChoiceChip
import app.aspen.domain.ai.ReflectionCompanion
import app.aspen.domain.companion.model.CompanionSpecies
import app.aspen.domain.consent.ConsentManager
import app.aspen.ui.companion.CompanionController
import app.aspen.ui.generated.resources.companion_species_aspen
import app.aspen.ui.generated.resources.companion_species_bunny
import app.aspen.ui.generated.resources.companion_species_cat
import app.aspen.ui.generated.resources.settings_companion_species_label
import app.aspen.ui.generated.resources.settings_companion_subtitle_off
import app.aspen.ui.generated.resources.settings_companion_subtitle_on
import app.aspen.ui.generated.resources.settings_companion_title
import app.aspen.domain.consent.model.DataCategory
import app.aspen.domain.consent.model.Recipient
import app.aspen.domain.consent.model.RecipientType
import app.aspen.domain.logging.LoggingService
import app.aspen.ui.generated.resources.Res
import app.aspen.ui.generated.resources.settings_ai_dialog_body
import app.aspen.ui.generated.resources.settings_ai_dialog_cancel
import app.aspen.ui.generated.resources.settings_ai_dialog_confirm
import app.aspen.ui.generated.resources.settings_ai_dialog_title
import app.aspen.ui.generated.resources.settings_ai_local_note
import app.aspen.ui.generated.resources.settings_ai_subtitle_off
import app.aspen.ui.generated.resources.settings_ai_subtitle_on
import app.aspen.ui.generated.resources.settings_ai_title
import app.aspen.ui.generated.resources.settings_debug_companion
import app.aspen.ui.generated.resources.settings_debug_companion_subtitle
import app.aspen.ui.generated.resources.settings_delete_all
import app.aspen.ui.generated.resources.settings_delete_all_subtitle
import app.aspen.ui.generated.resources.settings_delete_cancel
import app.aspen.ui.generated.resources.settings_delete_confirm
import app.aspen.ui.generated.resources.settings_delete_dialog_body
import app.aspen.ui.generated.resources.settings_delete_dialog_title
import app.aspen.ui.generated.resources.settings_revisit_questions
import app.aspen.ui.generated.resources.settings_revisit_subtitle
import app.aspen.ui.generated.resources.settings_title

/**
 * Settings (docs/06 §3 Flow 0.4): the questionnaire is re-runnable any time, and the user can
 * permanently delete everything they've written (FR-11). Delete is confirmed and uses calm,
 * non-shaming copy — no alarm-red (CLAUDE.md #5).
 *
 * Phase 4 (docs/04 ADR-003, FR-8): the cloud-AI row issues/revokes the explicit [ConsentManager]
 * grant, with the calm one-time warning before enabling. Off by default; revoke is immediate.
 * [onOpenDebugCompanion] appears only in debug builds.
 */
@Composable
fun SettingsScreen(
    onRevisitQuestions: () -> Unit,
    loggingService: LoggingService?,
    consentManager: ConsentManager? = null,
    reflectionCompanion: ReflectionCompanion? = null,
    companion: CompanionController? = null,
    onOpenDebugCompanion: (() -> Unit)? = null,
) {
    var confirmDelete by remember { mutableStateOf(false) }
    var confirmAiEnable by remember { mutableStateOf(false) }
    // Bumped on grant/revoke so the row re-reads the consent state.
    var aiRevision by remember { mutableStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = AspenTheme.spacing.l, vertical = AspenTheme.spacing.xl),
        verticalArrangement = Arrangement.spacedBy(AspenTheme.spacing.sm),
    ) {
        AspenScreenHeader(title = stringResource(Res.string.settings_title))
        Spacer(Modifier.height(AspenTheme.spacing.m))

        SettingRow(
            title = Res.string.settings_revisit_questions,
            subtitle = Res.string.settings_revisit_subtitle,
            onClick = onRevisitQuestions,
        )
        if (consentManager != null) {
            val aiEnabled = remember(aiRevision) {
                consentManager.canAccess(ReflectionCompanion.AI_RECIPIENT_ID, DataCategory.AI_MESSAGES)
            }
            SettingRow(
                title = Res.string.settings_ai_title,
                subtitle = if (aiEnabled) Res.string.settings_ai_subtitle_on else Res.string.settings_ai_subtitle_off,
                onClick = {
                    if (aiEnabled) {
                        // Revoke is immediate and needs no confirmation friction (docs/08 §3).
                        consentManager.activeGrants()
                            .filter { it.recipient.id == ReflectionCompanion.AI_RECIPIENT_ID }
                            .forEach { consentManager.revoke(it.id) }
                        aiRevision += 1
                    } else {
                        confirmAiEnable = true
                    }
                },
            )
            Text(
                stringResource(Res.string.settings_ai_local_note),
                style = AspenTheme.typography.caption,
                color = AspenTheme.colors.textMuted,
            )
        }
        if (companion != null) {
            // Phase 5 (docs/05 §3.1): the companion is off by default; this toggle is the only way
            // it ever appears, and turning it off is instant with zero friction or guilt copy.
            SettingRow(
                title = Res.string.settings_companion_title,
                subtitle = if (companion.prefs.enabled) {
                    Res.string.settings_companion_subtitle_on
                } else {
                    Res.string.settings_companion_subtitle_off
                },
                onClick = { companion.setEnabled(!companion.prefs.enabled) },
            )
            if (companion.prefs.enabled) {
                Text(
                    stringResource(Res.string.settings_companion_species_label),
                    style = AspenTheme.typography.caption,
                    color = AspenTheme.colors.textMuted,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(AspenTheme.spacing.s)) {
                    SpeciesChip(companion, CompanionSpecies.ASPEN_SPRITE, Res.string.companion_species_aspen)
                    SpeciesChip(companion, CompanionSpecies.CAT, Res.string.companion_species_cat)
                    SpeciesChip(companion, CompanionSpecies.BUNNY, Res.string.companion_species_bunny)
                }
            }
        }
        if (loggingService != null) {
            SettingRow(
                title = Res.string.settings_delete_all,
                subtitle = Res.string.settings_delete_all_subtitle,
                onClick = { confirmDelete = true },
            )
        }
        if (onOpenDebugCompanion != null) {
            SettingRow(
                title = Res.string.settings_debug_companion,
                subtitle = Res.string.settings_debug_companion_subtitle,
                onClick = onOpenDebugCompanion,
            )
        }
    }

    if (confirmAiEnable && consentManager != null) {
        // Localized here because the grant's display name is user-facing in the audit log (CLAUDE.md #11).
        val aiDisplayName = stringResource(Res.string.settings_ai_title)
        AlertDialog(
            onDismissRequest = { confirmAiEnable = false },
            shape = AspenTheme.shapes.large,
            containerColor = AspenTheme.colors.surface,
            titleContentColor = AspenTheme.colors.textPrimary,
            textContentColor = AspenTheme.colors.textSecondary,
            title = { Text(stringResource(Res.string.settings_ai_dialog_title)) },
            text = { Text(stringResource(Res.string.settings_ai_dialog_body)) },
            confirmButton = {
                TextButton(onClick = {
                    consentManager.grant(
                        recipient = Recipient(
                            id = ReflectionCompanion.AI_RECIPIENT_ID,
                            type = RecipientType.AI_SERVICE,
                            displayName = aiDisplayName,
                        ),
                        categories = setOf(DataCategory.AI_MESSAGES),
                        purpose = "cloud reflection",
                    )
                    aiRevision += 1
                    confirmAiEnable = false
                }) {
                    Text(stringResource(Res.string.settings_ai_dialog_confirm), color = AspenTheme.colors.primaryDark)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmAiEnable = false }) {
                    Text(stringResource(Res.string.settings_ai_dialog_cancel), color = AspenTheme.colors.textPrimary)
                }
            },
        )
    }

    if (confirmDelete && loggingService != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            shape = AspenTheme.shapes.large,
            containerColor = AspenTheme.colors.surface,
            titleContentColor = AspenTheme.colors.textPrimary,
            textContentColor = AspenTheme.colors.textSecondary,
            title = { Text(stringResource(Res.string.settings_delete_dialog_title)) },
            text = { Text(stringResource(Res.string.settings_delete_dialog_body)) },
            confirmButton = {
                TextButton(onClick = {
                    loggingService.deleteEverything()
                    reflectionCompanion?.deleteEverything() // FR-11 covers the AI history too.
                    confirmDelete = false
                }) {
                    Text(stringResource(Res.string.settings_delete_confirm), color = AspenTheme.colors.caution)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) {
                    Text(stringResource(Res.string.settings_delete_cancel), color = AspenTheme.colors.textPrimary)
                }
            },
        )
    }
}

@Composable
private fun SpeciesChip(companion: CompanionController, species: CompanionSpecies, label: StringResource) {
    AspenChoiceChip(
        label = stringResource(label),
        selected = companion.prefs.species == species,
        onToggle = { companion.setSpecies(species) },
    )
}

@Composable
private fun SettingRow(title: StringResource, subtitle: StringResource, onClick: () -> Unit) {
    AspenCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(title), style = AspenTheme.typography.body, color = AspenTheme.colors.textPrimary)
        Spacer(Modifier.height(AspenTheme.spacing.xs))
        Text(stringResource(subtitle), style = AspenTheme.typography.caption, color = AspenTheme.colors.textSecondary)
    }
}
