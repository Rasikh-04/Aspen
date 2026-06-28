package app.aspen.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextDecoration
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import app.aspen.design.AspenTheme
import app.aspen.domain.safety.model.Contact
import app.aspen.domain.safety.model.CrisisResource
import app.aspen.domain.safety.model.CrisisResourceSet
import app.aspen.domain.safety.model.LocaleKey
import app.aspen.ui.generated.resources.Res
import app.aspen.ui.generated.resources.back
import app.aspen.ui.generated.resources.safety_fallback_note
import app.aspen.ui.generated.resources.safety_heading_acute
import app.aspen.ui.generated.resources.safety_heading_finder
import app.aspen.ui.generated.resources.safety_heading_support
import app.aspen.ui.generated.resources.safety_intro
import app.aspen.ui.generated.resources.safety_region_de
import app.aspen.ui.generated.resources.safety_region_intl
import app.aspen.ui.generated.resources.safety_region_label
import app.aspen.ui.generated.resources.safety_region_pk
import app.aspen.ui.generated.resources.safety_region_uk
import app.aspen.ui.generated.resources.safety_title
import app.aspen.ui.generated.resources.safety_trusted_person
import app.aspen.ui.generated.resources.safety_unverified

/** Sentinel from the registry meaning a contact value is not yet advisor-verified. */
private const val UNVERIFIED = "TODO-VERIFY"

/** Regions the user can pick from; crisis region is independent of UI language (CLAUDE.md #11). */
private val REGION_CHOICES: List<Pair<LocaleKey, StringResource>> = listOf(
    LocaleKey.PK to Res.string.safety_region_pk,
    LocaleKey.DE to Res.string.safety_region_de,
    LocaleKey.UK to Res.string.safety_region_uk,
    LocaleKey.INTL to Res.string.safety_region_intl,
)

/**
 * Flow C — the calm route to a real person (docs/06 §6, CLAUDE.md #6). Pure UI: it renders a
 * resolved [CrisisResourceSet], an explicit region picker, and an always-present trusted-person row.
 *
 * Tone is calm and serious — soft amber [crisis]/[crisisBg] tokens, never alarm-red (CLAUDE.md #5).
 * A contact whose value is still [UNVERIFIED] is shown but NOT tappable, so unverified crisis details
 * can never be dialled (docs/09 §2.5). Acute-crisis support is listed first.
 */
@Composable
fun SafetyScreen(
    resources: CrisisResourceSet,
    selectedRegion: LocaleKey,
    onRegionChange: (LocaleKey) -> Unit,
    onContact: (Contact) -> Unit,
    onReachTrustedPerson: () -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AspenTheme.colors.crisisBg)
            .verticalScroll(rememberScrollState())
            .padding(AspenTheme.spacing.l),
    ) {
        Text(
            text = stringResource(Res.string.safety_title),
            style = AspenTheme.typography.title,
            color = AspenTheme.colors.crisis,
        )
        Spacer(Modifier.height(AspenTheme.spacing.s))
        Text(
            text = stringResource(Res.string.safety_intro),
            style = AspenTheme.typography.bodyLoose,
            color = AspenTheme.colors.textPrimary,
        )

        Spacer(Modifier.height(AspenTheme.spacing.l))

        // Trusted person is always offered first, regardless of region (CLAUDE.md #6, docs/06 §6.2).
        TrustedPersonRow(onReachTrustedPerson)

        Spacer(Modifier.height(AspenTheme.spacing.l))
        RegionPicker(selectedRegion, onRegionChange)

        if (resources.isFallback) {
            Spacer(Modifier.height(AspenTheme.spacing.m))
            Text(
                text = stringResource(Res.string.safety_fallback_note),
                style = AspenTheme.typography.body,
                color = AspenTheme.colors.textSecondary,
            )
        }

        Section(Res.string.safety_heading_acute, resources.acuteCrisis, onContact)
        Section(Res.string.safety_heading_support, resources.edSupport, onContact)
        Section(Res.string.safety_heading_finder, resources.treatmentFinder, onContact)

        Spacer(Modifier.height(AspenTheme.spacing.xl))
        TextButton(onClick = onBack) {
            Text(stringResource(Res.string.back), color = AspenTheme.colors.textSecondary)
        }
    }
}

@Composable
private fun TrustedPersonRow(onReachTrustedPerson: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AspenTheme.spacing.s))
            .background(AspenTheme.colors.surface)
            .clickable(onClick = onReachTrustedPerson)
            .padding(AspenTheme.spacing.m),
    ) {
        Text(
            text = stringResource(Res.string.safety_trusted_person),
            style = AspenTheme.typography.body,
            color = AspenTheme.colors.textPrimary,
        )
    }
}

@Composable
private fun RegionPicker(selected: LocaleKey, onRegionChange: (LocaleKey) -> Unit) {
    Text(
        text = stringResource(Res.string.safety_region_label),
        style = AspenTheme.typography.body,
        color = AspenTheme.colors.textSecondary,
    )
    Spacer(Modifier.height(AspenTheme.spacing.s))
    Row(horizontalArrangement = Arrangement.spacedBy(AspenTheme.spacing.s)) {
        REGION_CHOICES.forEach { (region, label) ->
            val active = region == selected
            Text(
                text = stringResource(label),
                style = AspenTheme.typography.body,
                color = if (active) AspenTheme.colors.crisis else AspenTheme.colors.textSecondary,
                modifier = Modifier
                    .clip(RoundedCornerShape(AspenTheme.spacing.s))
                    .background(if (active) AspenTheme.colors.crisisBg else AspenTheme.colors.surface)
                    .clickable { onRegionChange(region) }
                    .padding(horizontal = AspenTheme.spacing.m, vertical = AspenTheme.spacing.s),
            )
        }
    }
}

@Composable
private fun Section(
    heading: StringResource,
    items: List<CrisisResource>,
    onContact: (Contact) -> Unit,
) {
    if (items.isEmpty()) return
    Spacer(Modifier.height(AspenTheme.spacing.l))
    Text(
        text = stringResource(heading),
        style = AspenTheme.typography.label,
        color = AspenTheme.colors.crisis,
    )
    items.forEach { resource ->
        Spacer(Modifier.height(AspenTheme.spacing.m))
        ResourceCard(resource, onContact)
    }
}

@Composable
private fun ResourceCard(resource: CrisisResource, onContact: (Contact) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AspenTheme.spacing.s))
            .background(AspenTheme.colors.surface)
            .padding(AspenTheme.spacing.m),
    ) {
        Text(resource.name, style = AspenTheme.typography.body, color = AspenTheme.colors.textPrimary)
        resource.notes?.let {
            Spacer(Modifier.height(AspenTheme.spacing.xs))
            Text(it, style = AspenTheme.typography.body, color = AspenTheme.colors.textSecondary)
        }
        resource.contacts.forEach { contact ->
            Spacer(Modifier.height(AspenTheme.spacing.s))
            ContactRow(contact, onContact)
        }
    }
}

@Composable
private fun ContactRow(contact: Contact, onContact: (Contact) -> Unit) {
    val verified = contact.value.isNotBlank() && contact.value != UNVERIFIED
    if (verified) {
        Text(
            text = contact.label,
            style = AspenTheme.typography.body,
            color = AspenTheme.colors.crisis,
            textDecoration = TextDecoration.Underline,
            modifier = Modifier.clickable { onContact(contact) },
        )
    } else {
        // Unverified content is shown for transparency but is non-actionable — it must not be dialled.
        Text(
            text = contact.label + " — " + stringResource(Res.string.safety_unverified),
            style = AspenTheme.typography.body,
            color = AspenTheme.colors.textSecondary,
        )
    }
}
