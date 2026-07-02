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
import app.aspen.domain.logging.LoggingService
import app.aspen.ui.generated.resources.Res
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
 */
@Composable
fun SettingsScreen(onRevisitQuestions: () -> Unit, loggingService: LoggingService?) {
    var confirmDelete by remember { mutableStateOf(false) }

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
        if (loggingService != null) {
            SettingRow(
                title = Res.string.settings_delete_all,
                subtitle = Res.string.settings_delete_all_subtitle,
                onClick = { confirmDelete = true },
            )
        }
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
private fun SettingRow(title: StringResource, subtitle: StringResource, onClick: () -> Unit) {
    AspenCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(title), style = AspenTheme.typography.body, color = AspenTheme.colors.textPrimary)
        Spacer(Modifier.height(AspenTheme.spacing.xs))
        Text(stringResource(subtitle), style = AspenTheme.typography.caption, color = AspenTheme.colors.textSecondary)
    }
}
