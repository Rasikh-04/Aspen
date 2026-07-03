package app.aspen.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import app.aspen.design.AspenTheme
import app.aspen.design.components.AspenCard
import app.aspen.design.components.AspenPrimaryButton
import app.aspen.domain.sync.BackupManager
import app.aspen.domain.sync.BackupNowOutcome
import app.aspen.domain.sync.EnableBackupOutcome
import app.aspen.domain.sync.RestoreOutcome
import app.aspen.ui.generated.resources.Res
import app.aspen.ui.generated.resources.backup_code_body
import app.aspen.ui.generated.resources.backup_code_confirm
import app.aspen.ui.generated.resources.backup_code_title
import app.aspen.ui.generated.resources.backup_done
import app.aspen.ui.generated.resources.backup_enable
import app.aspen.ui.generated.resources.backup_error_no_backup
import app.aspen.ui.generated.resources.backup_error_unavailable
import app.aspen.ui.generated.resources.backup_error_weak
import app.aspen.ui.generated.resources.backup_error_wrong_secret
import app.aspen.ui.generated.resources.backup_note_keymodel
import app.aspen.ui.generated.resources.backup_now
import app.aspen.ui.generated.resources.backup_now_subtitle
import app.aspen.ui.generated.resources.backup_off
import app.aspen.ui.generated.resources.backup_off_cancel
import app.aspen.ui.generated.resources.backup_off_confirm
import app.aspen.ui.generated.resources.backup_off_dialog_body
import app.aspen.ui.generated.resources.backup_off_dialog_title
import app.aspen.ui.generated.resources.backup_off_subtitle
import app.aspen.ui.generated.resources.backup_passphrase
import app.aspen.ui.generated.resources.backup_restore_action
import app.aspen.ui.generated.resources.backup_restore_done
import app.aspen.ui.generated.resources.backup_restore_hint
import app.aspen.ui.generated.resources.backup_restore_title
import app.aspen.ui.generated.resources.backup_subtitle_off
import app.aspen.ui.generated.resources.backup_subtitle_on
import app.aspen.ui.generated.resources.backup_title
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * Optional E2E backup, shown only when signed in (docs/08 §2). The copy carries the honest key
 * model: locked on-device, we can never read or unlock it, the once-shown recovery code is the
 * only other way in, and an email reset restores sign-in — never this. Everything is manual;
 * turning it off deletes the server copy and never touches on-device writing (CLAUDE.md #5/#10).
 */
@Composable
fun BackupSection(manager: BackupManager) {
    val scope = rememberCoroutineScope()
    var revision by remember { mutableStateOf(0) }
    val configured = remember(revision) { manager.isConfigured() }

    var expanded by remember { mutableStateOf(false) }
    var passphrase by remember { mutableStateOf("") }
    var restoreSecret by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<StringResource?>(null) }
    var recoveryCode by remember { mutableStateOf<String?>(null) }
    var confirmOff by remember { mutableStateOf(false) }

    if (!configured) {
        AspenCard(onClick = { expanded = !expanded }, modifier = Modifier.fillMaxWidth()) {
            SectionText(Res.string.backup_title, Res.string.backup_subtitle_off)
        }
        if (expanded) {
            AccountSectionField(passphrase, { passphrase = it }, Res.string.backup_passphrase, isPassword = true)
            Text(
                stringResource(Res.string.backup_note_keymodel),
                style = AspenTheme.typography.caption,
                color = AspenTheme.colors.textMuted,
            )
            AspenPrimaryButton(
                label = stringResource(Res.string.backup_enable),
                enabled = !busy && passphrase.isNotEmpty(),
                onClick = {
                    busy = true
                    message = null
                    scope.launch {
                        when (val outcome = manager.enable(passphrase)) {
                            is EnableBackupOutcome.Enabled -> {
                                recoveryCode = outcome.recoveryCode
                                passphrase = ""
                                expanded = false
                            }
                            EnableBackupOutcome.WeakPassphrase -> message = Res.string.backup_error_weak
                            EnableBackupOutcome.Unavailable -> message = Res.string.backup_error_unavailable
                        }
                        busy = false
                    }
                },
            )
            Spacer(Modifier.height(AspenTheme.spacing.s))
            Text(
                stringResource(Res.string.backup_restore_title),
                style = AspenTheme.typography.caption,
                color = AspenTheme.colors.textMuted,
            )
            AccountSectionField(restoreSecret, { restoreSecret = it }, Res.string.backup_restore_hint, isPassword = true)
            AspenPrimaryButton(
                label = stringResource(Res.string.backup_restore_action),
                enabled = !busy && restoreSecret.isNotEmpty(),
                onClick = {
                    busy = true
                    message = null
                    scope.launch {
                        message = when (manager.restore(restoreSecret)) {
                            RestoreOutcome.Restored -> {
                                restoreSecret = ""
                                revision += 1
                                Res.string.backup_restore_done
                            }
                            RestoreOutcome.WrongSecret -> Res.string.backup_error_wrong_secret
                            RestoreOutcome.NoBackup -> Res.string.backup_error_no_backup
                            RestoreOutcome.Unavailable -> Res.string.backup_error_unavailable
                        }
                        busy = false
                    }
                },
            )
        }
    } else {
        AspenCard(modifier = Modifier.fillMaxWidth()) {
            SectionText(Res.string.backup_title, Res.string.backup_subtitle_on)
        }
        AspenCard(
            onClick = {
                if (busy) return@AspenCard
                busy = true
                message = null
                scope.launch {
                    message = when (manager.backUpNow()) {
                        BackupNowOutcome.Done -> Res.string.backup_done
                        else -> Res.string.backup_error_unavailable
                    }
                    busy = false
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            SectionText(Res.string.backup_now, Res.string.backup_now_subtitle)
        }
        AspenCard(onClick = { confirmOff = true }, modifier = Modifier.fillMaxWidth()) {
            SectionText(Res.string.backup_off, Res.string.backup_off_subtitle)
        }
    }

    message?.let {
        Text(
            stringResource(it),
            style = AspenTheme.typography.caption,
            color = AspenTheme.colors.textSecondary,
            // Outcomes here are deliberately quiet visually; announce them so a screen-reader
            // user isn't left wondering whether "Back up now" did anything (WCAG 4.1.3).
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
    }

    recoveryCode?.let { code ->
        // Shown exactly once (docs/08 §2). Dismissing in any way acknowledges it — no trap.
        AlertDialog(
            onDismissRequest = { recoveryCode = null; revision += 1 },
            shape = AspenTheme.shapes.large,
            containerColor = AspenTheme.colors.surface,
            titleContentColor = AspenTheme.colors.textPrimary,
            textContentColor = AspenTheme.colors.textSecondary,
            title = { Text(stringResource(Res.string.backup_code_title)) },
            text = {
                Column {
                    Text(stringResource(Res.string.backup_code_body))
                    Spacer(Modifier.height(AspenTheme.spacing.s))
                    // Selectable so it can be copied instead of hand-transcribed (24 chars is a
                    // real motor/vision burden). Copying is user-initiated — never automatic.
                    SelectionContainer {
                        Text(
                            code,
                            style = AspenTheme.typography.body,
                            color = AspenTheme.colors.textPrimary,
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { recoveryCode = null; revision += 1 }) {
                    Text(stringResource(Res.string.backup_code_confirm), color = AspenTheme.colors.primaryDark)
                }
            },
        )
    }

    if (confirmOff) {
        AlertDialog(
            onDismissRequest = { confirmOff = false },
            shape = AspenTheme.shapes.large,
            containerColor = AspenTheme.colors.surface,
            titleContentColor = AspenTheme.colors.textPrimary,
            textContentColor = AspenTheme.colors.textSecondary,
            title = { Text(stringResource(Res.string.backup_off_dialog_title)) },
            text = { Text(stringResource(Res.string.backup_off_dialog_body)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmOff = false
                    scope.launch {
                        message = if (manager.disable()) null else Res.string.backup_error_unavailable
                        revision += 1
                    }
                }) {
                    Text(stringResource(Res.string.backup_off_confirm), color = AspenTheme.colors.caution)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmOff = false }) {
                    Text(stringResource(Res.string.backup_off_cancel), color = AspenTheme.colors.textPrimary)
                }
            },
        )
    }
}

@Composable
private fun SectionText(title: StringResource, subtitle: StringResource) {
    Text(stringResource(title), style = AspenTheme.typography.body, color = AspenTheme.colors.textPrimary)
    Spacer(Modifier.height(AspenTheme.spacing.xs))
    Text(stringResource(subtitle), style = AspenTheme.typography.caption, color = AspenTheme.colors.textSecondary)
}
