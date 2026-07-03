package app.aspen.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import app.aspen.design.AspenTheme
import app.aspen.design.components.AspenCard
import app.aspen.design.components.AspenChoiceChip
import app.aspen.design.components.AspenPrimaryButton
import app.aspen.domain.account.AccountManager
import app.aspen.domain.account.AccountResult
import app.aspen.ui.generated.resources.Res
import app.aspen.ui.generated.resources.account_delete
import app.aspen.ui.generated.resources.account_delete_cancel
import app.aspen.ui.generated.resources.account_delete_confirm
import app.aspen.ui.generated.resources.account_delete_dialog_body
import app.aspen.ui.generated.resources.account_delete_dialog_title
import app.aspen.ui.generated.resources.account_delete_subtitle
import app.aspen.ui.generated.resources.account_email_optional
import app.aspen.ui.generated.resources.account_error_denied
import app.aspen.ui.generated.resources.account_error_email_taken
import app.aspen.ui.generated.resources.account_error_unavailable
import app.aspen.ui.generated.resources.account_error_weak_password
import app.aspen.ui.generated.resources.account_id_caption
import app.aspen.ui.generated.resources.account_identifier
import app.aspen.ui.generated.resources.account_mode_create
import app.aspen.ui.generated.resources.account_mode_signin
import app.aspen.ui.generated.resources.account_note_local
import app.aspen.ui.generated.resources.account_password
import app.aspen.ui.generated.resources.account_signout
import app.aspen.ui.generated.resources.account_signout_subtitle
import app.aspen.ui.generated.resources.account_submit_create
import app.aspen.ui.generated.resources.account_submit_signin
import app.aspen.ui.generated.resources.settings_account_subtitle_off
import app.aspen.ui.generated.resources.settings_account_subtitle_on
import app.aspen.ui.generated.resources.settings_account_title
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * The OPTIONAL account (Phase 6, docs/08 §1). Lives only here in Settings — never proposed
 * during onboarding or any arrival flow (FR-9 + CLAUDE.md #10). Calm throughout: a denial is
 * "try again whenever", offline degrades quietly, and no path ever gates a feature on signing in.
 * Deleting the account uses soft amber and never touches on-device writing (CLAUDE.md #5).
 */
@Composable
fun AccountSection(manager: AccountManager, backupManager: app.aspen.domain.sync.BackupManager? = null) {
    val scope = rememberCoroutineScope()
    var revision by remember { mutableStateOf(0) }
    val current = remember(revision) { manager.current() }

    var expanded by remember { mutableStateOf(false) }
    var createMode by remember { mutableStateOf(true) }
    var email by remember { mutableStateOf("") }
    var identifier by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<StringResource?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }

    fun handle(result: AccountResult) {
        busy = false
        when (result) {
            is AccountResult.SignedIn -> {
                expanded = false
                password = ""
                error = null
                revision += 1
            }
            AccountResult.Denied -> error = Res.string.account_error_denied
            AccountResult.EmailTaken -> error = Res.string.account_error_email_taken
            AccountResult.WeakPassword -> error = Res.string.account_error_weak_password
            AccountResult.Unavailable -> error = Res.string.account_error_unavailable
        }
    }

    if (current == null) {
        AspenCard(onClick = { expanded = !expanded }, modifier = Modifier.fillMaxWidth()) {
            Text(
                stringResource(Res.string.settings_account_title),
                style = AspenTheme.typography.body,
                color = AspenTheme.colors.textPrimary,
            )
            Spacer(Modifier.height(AspenTheme.spacing.xs))
            Text(
                stringResource(Res.string.settings_account_subtitle_off),
                style = AspenTheme.typography.caption,
                color = AspenTheme.colors.textSecondary,
            )
        }
        if (expanded) {
            Row(horizontalArrangement = Arrangement.spacedBy(AspenTheme.spacing.s)) {
                AspenChoiceChip(
                    label = stringResource(Res.string.account_mode_create),
                    selected = createMode,
                    onToggle = { createMode = true; error = null },
                )
                AspenChoiceChip(
                    label = stringResource(Res.string.account_mode_signin),
                    selected = !createMode,
                    onToggle = { createMode = false; error = null },
                )
            }
            if (createMode) {
                AccountSectionField(
                    email, { email = it }, Res.string.account_email_optional,
                    keyboardType = KeyboardType.Email,
                )
            } else {
                AccountSectionField(identifier, { identifier = it }, Res.string.account_identifier)
            }
            AccountSectionField(password, { password = it }, Res.string.account_password, isPassword = true)
            error?.let {
                Text(
                    stringResource(it),
                    style = AspenTheme.typography.caption,
                    color = AspenTheme.colors.textSecondary,
                    // Announced (politely) when it appears — the visual cue alone is deliberately
                    // quiet, so without this a screen-reader user gets silence (WCAG 4.1.3).
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                )
            }
            AspenPrimaryButton(
                label = stringResource(
                    if (createMode) Res.string.account_submit_create else Res.string.account_submit_signin,
                ),
                enabled = !busy && password.isNotEmpty() && (createMode || identifier.isNotBlank()),
                onClick = {
                    busy = true
                    error = null
                    scope.launch {
                        handle(
                            if (createMode) {
                                manager.register(password, email.trim().ifEmpty { null })
                            } else {
                                manager.signIn(identifier.trim(), password)
                            },
                        )
                    }
                },
            )
            Text(
                stringResource(Res.string.account_note_local),
                style = AspenTheme.typography.caption,
                color = AspenTheme.colors.textMuted,
            )
        }
    } else {
        AspenCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                stringResource(Res.string.settings_account_title),
                style = AspenTheme.typography.body,
                color = AspenTheme.colors.textPrimary,
            )
            Spacer(Modifier.height(AspenTheme.spacing.xs))
            Text(
                stringResource(Res.string.settings_account_subtitle_on),
                style = AspenTheme.typography.caption,
                color = AspenTheme.colors.textSecondary,
            )
            Spacer(Modifier.height(AspenTheme.spacing.xs))
            // Shown so a no-email account can always be signed into again (docs/08 §1).
            Text(
                stringResource(Res.string.account_id_caption, current.accountId),
                style = AspenTheme.typography.caption,
                color = AspenTheme.colors.textMuted,
            )
        }
        AspenCard(
            onClick = { scope.launch { manager.signOut(); revision += 1 } },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                stringResource(Res.string.account_signout),
                style = AspenTheme.typography.body,
                color = AspenTheme.colors.textPrimary,
            )
            Spacer(Modifier.height(AspenTheme.spacing.xs))
            Text(
                stringResource(Res.string.account_signout_subtitle),
                style = AspenTheme.typography.caption,
                color = AspenTheme.colors.textSecondary,
            )
        }
        AspenCard(onClick = { confirmDelete = true }, modifier = Modifier.fillMaxWidth()) {
            Text(
                stringResource(Res.string.account_delete),
                style = AspenTheme.typography.body,
                color = AspenTheme.colors.textPrimary,
            )
            Spacer(Modifier.height(AspenTheme.spacing.xs))
            Text(
                stringResource(Res.string.account_delete_subtitle),
                style = AspenTheme.typography.caption,
                color = AspenTheme.colors.textSecondary,
            )
        }
        error?.let {
            Text(
                stringResource(it),
                style = AspenTheme.typography.caption,
                color = AspenTheme.colors.textSecondary,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
        if (backupManager != null) {
            // E2E backup (docs/08 §2) — only meaningful once signed in; absent on iOS until its
            // sync-crypto actual lands (a passthrough would upload readable content).
            BackupSection(backupManager)
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            shape = AspenTheme.shapes.large,
            containerColor = AspenTheme.colors.surface,
            titleContentColor = AspenTheme.colors.textPrimary,
            textContentColor = AspenTheme.colors.textSecondary,
            title = { Text(stringResource(Res.string.account_delete_dialog_title)) },
            text = { Text(stringResource(Res.string.account_delete_dialog_body)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    scope.launch {
                        error = if (manager.deleteAccount()) null else Res.string.account_error_unavailable
                        revision += 1
                    }
                }) {
                    Text(stringResource(Res.string.account_delete_confirm), color = AspenTheme.colors.caution)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) {
                    Text(stringResource(Res.string.account_delete_cancel), color = AspenTheme.colors.textPrimary)
                }
            },
        )
    }
}

/** Single-line sibling of the Reflect notebook field — same warm styling, never a stark form. */
@Composable
internal fun AccountSectionField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: StringResource,
    isPassword: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        // Declaring the type gets the right IME layout and keeps keyboards/autofill from
        // suggesting or learning secrets (Password fields are excluded from personalisation).
        keyboardOptions = KeyboardOptions(
            keyboardType = if (isPassword) KeyboardType.Password else keyboardType,
        ),
        visualTransformation = if (isPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        placeholder = {
            Text(
                stringResource(placeholder),
                style = AspenTheme.typography.body,
                color = AspenTheme.colors.textMuted,
            )
        },
        textStyle = AspenTheme.typography.body,
        shape = AspenTheme.shapes.medium,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AspenTheme.colors.primary,
            unfocusedBorderColor = AspenTheme.colors.border,
            focusedContainerColor = AspenTheme.colors.surface,
            unfocusedContainerColor = AspenTheme.colors.surface,
            cursorColor = AspenTheme.colors.primary,
        ),
        modifier = Modifier.fillMaxWidth(),
    )
}
