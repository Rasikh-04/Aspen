package app.aspen.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import app.aspen.design.AspenTheme
import app.aspen.design.components.AspenAmbientBackground
import app.aspen.design.components.AspenPrimaryButton
import app.aspen.design.components.AspenQuietButton
import app.aspen.design.components.AspenTextAction
import app.aspen.domain.onboarding.model.BodyImageSalience
import app.aspen.domain.onboarding.model.EatingRelationship
import app.aspen.domain.onboarding.model.HelpWanted
import app.aspen.domain.onboarding.model.LifeImpact
import app.aspen.domain.onboarding.model.Likert
import app.aspen.domain.onboarding.model.OnboardingResult
import app.aspen.domain.onboarding.model.QuickReachPreference
import app.aspen.domain.onboarding.model.SensoryDriver
import app.aspen.domain.onboarding.model.SupportContext
import app.aspen.ui.generated.resources.Res
import app.aspen.ui.generated.resources.onb_back
import app.aspen.ui.generated.resources.onb_begin
import app.aspen.ui.generated.resources.onb_closing_body
import app.aspen.ui.generated.resources.onb_closing_continue
import app.aspen.ui.generated.resources.onb_closing_find_help
import app.aspen.ui.generated.resources.onb_closing_title
import app.aspen.ui.generated.resources.onb_intro_body
import app.aspen.ui.generated.resources.onb_intro_title
import app.aspen.ui.generated.resources.onb_likert_not_really
import app.aspen.ui.generated.resources.onb_likert_often
import app.aspen.ui.generated.resources.onb_likert_sometimes
import app.aspen.ui.generated.resources.onb_next
import app.aspen.ui.generated.resources.onb_q10_maybe_later
import app.aspen.ui.generated.resources.onb_q10_no
import app.aspen.ui.generated.resources.onb_q10_set_up
import app.aspen.ui.generated.resources.onb_q10_title
import app.aspen.ui.generated.resources.onb_q1_calmer
import app.aspen.ui.generated.resources.onb_q1_company
import app.aspen.ui.generated.resources.onb_q1_not_sure
import app.aspen.ui.generated.resources.onb_q1_private
import app.aspen.ui.generated.resources.onb_q1_reach_help
import app.aspen.ui.generated.resources.onb_q1_title
import app.aspen.ui.generated.resources.onb_q2_body_focused
import app.aspen.ui.generated.resources.onb_q2_out_of_control
import app.aspen.ui.generated.resources.onb_q2_sensory_hard
import app.aspen.ui.generated.resources.onb_q2_tense_rules
import app.aspen.ui.generated.resources.onb_q2_title
import app.aspen.ui.generated.resources.onb_q2_undo_after
import app.aspen.ui.generated.resources.onb_q2_varies
import app.aspen.ui.generated.resources.onb_q3_title
import app.aspen.ui.generated.resources.onb_q4_title
import app.aspen.ui.generated.resources.onb_q5_title
import app.aspen.ui.generated.resources.onb_q6_no
import app.aspen.ui.generated.resources.onb_q6_somewhat
import app.aspen.ui.generated.resources.onb_q6_title
import app.aspen.ui.generated.resources.onb_q6_yes
import app.aspen.ui.generated.resources.onb_q7_a_lot
import app.aspen.ui.generated.resources.onb_q7_not_much
import app.aspen.ui.generated.resources.onb_q7_sometimes
import app.aspen.ui.generated.resources.onb_q7_title
import app.aspen.ui.generated.resources.onb_q8_a_little
import app.aspen.ui.generated.resources.onb_q8_a_lot
import app.aspen.ui.generated.resources.onb_q8_some
import app.aspen.ui.generated.resources.onb_q8_title
import app.aspen.ui.generated.resources.onb_q9_none
import app.aspen.ui.generated.resources.onb_q9_professional
import app.aspen.ui.generated.resources.onb_q9_title
import app.aspen.ui.generated.resources.onb_q9_trusted
import app.aspen.ui.generated.resources.onb_skip_all

/**
 * Flow 0 — the adaptive onboarding questionnaire (docs/06 §3, docs/11). One numberless question per
 * screen, every item skippable, "skip all" always available. On finish it hands the scored
 * [OnboardingResult] back to the caller (which persists it via the domain [ProfileStore] and shows the
 * app). It never displays a profile, label, or score to the user (CLAUDE.md #9, docs/11 §0).
 *
 * [onFinish] receives the result and whether the user asked to reach real help from the closing
 * screen (so the host can route to Flow C). The questionnaire is re-runnable from Settings.
 */
@Composable
fun OnboardingFlow(onFinish: (OnboardingResult, goToSafety: Boolean) -> Unit) {
    val controller = remember { OnboardingController() }

    when {
        controller.isIntro -> IntroScreen(
            onBegin = controller::next,
            onSkipAll = controller::skipAll,
        )

        controller.isClosing -> ClosingScreen(
            onBack = controller::back,
            onFindHelp = { onFinish(controller.result(), true) },
            onContinue = { onFinish(controller.result(), false) },
        )

        else -> QuestionScreen(controller)
    }
}

@Composable
private fun QuestionScreen(controller: OnboardingController) {
    val a = controller.answers
    val progress = controller.questionIndex to controller.questionCount
    val next = controller::next
    val back = controller::back

    @Composable
    fun scaffold(title: StringResource, content: @Composable ColumnScope.() -> Unit) {
        OnboardingScaffold(
            title = stringResource(title),
            onBack = back,
            onSkipQuestion = next, // "skip this one" == prefer-not-to-say: advance, leave no signal
            progress = progress,
            primaryLabel = stringResource(Res.string.onb_next),
            onPrimary = next,
            content = content,
        )
    }

    when (controller.questionIndex) {
        1 -> scaffold(Res.string.onb_q1_title) {
            MultiChoiceColumn(Q1_OPTIONS, a.helpWanted) { v ->
                controller.edit { it.copy(helpWanted = it.helpWanted.toggle(v)) }
            }
        }
        2 -> scaffold(Res.string.onb_q2_title) {
            MultiChoiceColumn(Q2_OPTIONS, a.eatingRelationship) { v ->
                controller.edit { it.copy(eatingRelationship = it.eatingRelationship.toggle(v)) }
            }
        }
        3 -> scaffold(Res.string.onb_q3_title) {
            SingleChoiceColumn(LIKERT_OPTIONS, a.holdingBack) { v ->
                controller.edit { it.copy(holdingBack = v) }
            }
        }
        4 -> scaffold(Res.string.onb_q4_title) {
            SingleChoiceColumn(LIKERT_OPTIONS, a.lossOfControl) { v ->
                controller.edit { it.copy(lossOfControl = v) }
            }
        }
        5 -> scaffold(Res.string.onb_q5_title) {
            SingleChoiceColumn(LIKERT_OPTIONS, a.urgeToCompensate) { v ->
                controller.edit { it.copy(urgeToCompensate = v) }
            }
        }
        6 -> scaffold(Res.string.onb_q6_title) {
            SingleChoiceColumn(Q6_OPTIONS, a.sensoryDriver) { v ->
                controller.edit { it.copy(sensoryDriver = v) }
            }
        }
        7 -> scaffold(Res.string.onb_q7_title) {
            SingleChoiceColumn(Q7_OPTIONS, a.bodyImageSalience) { v ->
                controller.edit { it.copy(bodyImageSalience = v) }
            }
        }
        8 -> scaffold(Res.string.onb_q8_title) {
            SingleChoiceColumn(Q8_OPTIONS, a.lifeImpact) { v ->
                controller.edit { it.copy(lifeImpact = v) }
            }
        }
        9 -> scaffold(Res.string.onb_q9_title) {
            SingleChoiceColumn(Q9_OPTIONS, a.supportContext) { v ->
                controller.edit { it.copy(supportContext = v) }
            }
        }
        else -> scaffold(Res.string.onb_q10_title) {
            SingleChoiceColumn(Q10_OPTIONS, a.quickReach) { v ->
                controller.edit { it.copy(quickReach = v) }
            }
        }
    }
}

@Composable
private fun IntroScreen(onBegin: () -> Unit, onSkipAll: () -> Unit) {
    AspenAmbientBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = AspenTheme.spacing.l, vertical = AspenTheme.spacing.xxl),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = stringResource(Res.string.onb_intro_title),
                style = AspenTheme.typography.display,
                color = AspenTheme.colors.textPrimary,
            )
            Spacer(Modifier.height(AspenTheme.spacing.l))
            OnboardingProse(stringResource(Res.string.onb_intro_body))
            Spacer(Modifier.height(AspenTheme.spacing.xxl))
            AspenPrimaryButton(
                label = stringResource(Res.string.onb_begin),
                onClick = onBegin,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(AspenTheme.spacing.s))
            AspenTextAction(
                label = stringResource(Res.string.onb_skip_all),
                onClick = onSkipAll,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }
    }
}

@Composable
private fun ClosingScreen(onBack: () -> Unit, onFindHelp: () -> Unit, onContinue: () -> Unit) {
    AspenAmbientBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = AspenTheme.spacing.l, vertical = AspenTheme.spacing.xxl),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = stringResource(Res.string.onb_closing_title),
                style = AspenTheme.typography.display,
                color = AspenTheme.colors.textPrimary,
            )
            Spacer(Modifier.height(AspenTheme.spacing.l))
            OnboardingProse(stringResource(Res.string.onb_closing_body))
            Spacer(Modifier.height(AspenTheme.spacing.xxl))
            // Route toward real help first (docs/11 §3 closing): the human exit is foregrounded.
            AspenQuietButton(
                label = stringResource(Res.string.onb_closing_find_help),
                onClick = onFindHelp,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(AspenTheme.spacing.sm))
            AspenPrimaryButton(
                label = stringResource(Res.string.onb_closing_continue),
                onClick = onContinue,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(AspenTheme.spacing.s))
            AspenTextAction(
                label = stringResource(Res.string.onb_back),
                onClick = onBack,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }
    }
}

private fun <T> Set<T>.toggle(value: T): Set<T> = if (value in this) this - value else this + value

// ── Answer option lists (internal enum → localized label). Order mirrors docs/11 §3. ──

private val Q1_OPTIONS = listOf(
    ChoiceOption(HelpWanted.CALMER_MOMENT, Res.string.onb_q1_calmer),
    ChoiceOption(HelpWanted.PRIVATE_SPACE, Res.string.onb_q1_private),
    ChoiceOption(HelpWanted.LOW_DEMAND_COMPANY, Res.string.onb_q1_company),
    ChoiceOption(HelpWanted.REACH_REAL_HELP, Res.string.onb_q1_reach_help),
    ChoiceOption(HelpWanted.NOT_SURE, Res.string.onb_q1_not_sure),
)

private val Q2_OPTIONS = listOf(
    ChoiceOption(EatingRelationship.TENSE_RULES, Res.string.onb_q2_tense_rules),
    ChoiceOption(EatingRelationship.OUT_OF_CONTROL, Res.string.onb_q2_out_of_control),
    ChoiceOption(EatingRelationship.UNDO_AFTER, Res.string.onb_q2_undo_after),
    ChoiceOption(EatingRelationship.SENSORY_HARD, Res.string.onb_q2_sensory_hard),
    ChoiceOption(EatingRelationship.BODY_FOCUSED, Res.string.onb_q2_body_focused),
    ChoiceOption(EatingRelationship.VARIES, Res.string.onb_q2_varies),
)

private val LIKERT_OPTIONS = listOf(
    ChoiceOption(Likert.NOT_REALLY, Res.string.onb_likert_not_really),
    ChoiceOption(Likert.SOMETIMES, Res.string.onb_likert_sometimes),
    ChoiceOption(Likert.OFTEN, Res.string.onb_likert_often),
)

private val Q6_OPTIONS = listOf(
    ChoiceOption(SensoryDriver.YES, Res.string.onb_q6_yes),
    ChoiceOption(SensoryDriver.SOMEWHAT, Res.string.onb_q6_somewhat),
    ChoiceOption(SensoryDriver.NO, Res.string.onb_q6_no),
)

private val Q7_OPTIONS = listOf(
    ChoiceOption(BodyImageSalience.NOT_MUCH, Res.string.onb_q7_not_much),
    ChoiceOption(BodyImageSalience.SOMETIMES, Res.string.onb_q7_sometimes),
    ChoiceOption(BodyImageSalience.A_LOT, Res.string.onb_q7_a_lot),
)

private val Q8_OPTIONS = listOf(
    ChoiceOption(LifeImpact.A_LITTLE, Res.string.onb_q8_a_little),
    ChoiceOption(LifeImpact.SOME, Res.string.onb_q8_some),
    ChoiceOption(LifeImpact.A_LOT, Res.string.onb_q8_a_lot),
)

private val Q9_OPTIONS = listOf(
    ChoiceOption(SupportContext.HAS_PROFESSIONAL, Res.string.onb_q9_professional),
    ChoiceOption(SupportContext.HAS_TRUSTED_PERSON, Res.string.onb_q9_trusted),
    ChoiceOption(SupportContext.NONE_RIGHT_NOW, Res.string.onb_q9_none),
)

private val Q10_OPTIONS = listOf(
    ChoiceOption(QuickReachPreference.SET_UP_NOW, Res.string.onb_q10_set_up),
    ChoiceOption(QuickReachPreference.MAYBE_LATER, Res.string.onb_q10_maybe_later),
    ChoiceOption(QuickReachPreference.NO, Res.string.onb_q10_no),
)
