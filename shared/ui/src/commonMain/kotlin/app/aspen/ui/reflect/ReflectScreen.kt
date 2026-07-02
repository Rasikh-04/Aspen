package app.aspen.ui.reflect

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import app.aspen.design.AspenTheme
import app.aspen.design.components.AspenCard
import app.aspen.design.components.AspenPrimaryButton
import app.aspen.design.components.AspenQuietButton
import app.aspen.design.components.AspenScreenHeader
import app.aspen.design.components.AspenTagPill
import app.aspen.design.components.AspenTextAction
import app.aspen.domain.ai.ReflectionCompanion
import app.aspen.domain.logging.LoggingService
import app.aspen.domain.logging.model.FeelingTag
import app.aspen.ui.generated.resources.Res
import app.aspen.ui.generated.resources.reflect_cancel
import app.aspen.ui.generated.resources.reflect_delete
import app.aspen.ui.generated.resources.reflect_empty
import app.aspen.ui.generated.resources.reflect_feelings_label
import app.aspen.ui.generated.resources.reflect_new_feeling_log
import app.aspen.ui.generated.resources.reflect_new_food_log
import app.aspen.ui.generated.resources.reflect_new_reflection
import app.aspen.ui.generated.resources.reflect_note_hint
import app.aspen.ui.generated.resources.reflect_save
import app.aspen.ui.generated.resources.reflect_section_feelings
import app.aspen.ui.generated.resources.reflect_section_food
import app.aspen.ui.generated.resources.reflect_section_reflections
import app.aspen.ui.generated.resources.reflect_subtitle
import app.aspen.ui.generated.resources.reflect_text_hint
import app.aspen.ui.generated.resources.reflect_title

private sealed interface ReflectMode {
    data object List : ReflectMode
    data object WriteReflection : ReflectMode
    data object FeelingLog : ReflectMode
    data object FoodLog : ReflectMode
}

/**
 * Flow B — reflection + numberless logging (docs/06 §3). All writes go through the domain
 * [LoggingService], the single enforcement point: food logging only appears when the active profile
 * permits it (`isFoodLoggingOffered`). Empty days are silent — no "you missed" (CLAUDE.md #5).
 *
 * [loggingService] is null when unwired (e.g. iOS until its DI lands) — then this shows a calm
 * placeholder rather than crashing, mirroring the Flow C pattern.
 *
 * [reflectionCompanion] (Phase 4, docs/03 FR-5) renders the cloud reflection card ONLY while its
 * consent grant is active; [onReachSomeone] is the crisis hand-off route (≤2 taps, CLAUDE.md #6).
 */
@Composable
fun ReflectScreen(
    loggingService: LoggingService?,
    reflectionCompanion: ReflectionCompanion? = null,
    onReachSomeone: () -> Unit = {},
) {
    if (loggingService == null) {
        PlaceholderColumn(stringResource(Res.string.reflect_subtitle))
        return
    }
    var mode by remember { mutableStateOf<ReflectMode>(ReflectMode.List) }
    // Bumped after every write/delete to re-read the in-memory store.
    var revision by remember { mutableStateOf(0) }

    when (mode) {
        ReflectMode.List -> ReflectList(
            service = loggingService,
            reflectionCompanion = reflectionCompanion,
            onReachSomeone = onReachSomeone,
            revision = revision,
            onWriteReflection = { mode = ReflectMode.WriteReflection },
            onFeelingLog = { mode = ReflectMode.FeelingLog },
            onFoodLog = { mode = ReflectMode.FoodLog },
            onDelete = { delete -> delete(); revision += 1 },
        )
        ReflectMode.WriteReflection -> ReflectionEditor(
            onSave = { text -> loggingService.logReflection(text); revision += 1; mode = ReflectMode.List },
            onCancel = { mode = ReflectMode.List },
        )
        ReflectMode.FeelingLog -> LogEditor(
            onSave = { note, feelings -> loggingService.logBehaviour(note, feelings); revision += 1; mode = ReflectMode.List },
            onCancel = { mode = ReflectMode.List },
        )
        ReflectMode.FoodLog -> LogEditor(
            onSave = { note, feelings -> loggingService.logFood(note, feelings); revision += 1; mode = ReflectMode.List },
            onCancel = { mode = ReflectMode.List },
        )
    }
}

@Composable
private fun ReflectList(
    service: LoggingService,
    reflectionCompanion: ReflectionCompanion?,
    onReachSomeone: () -> Unit,
    revision: Int,
    onWriteReflection: () -> Unit,
    onFeelingLog: () -> Unit,
    onFoodLog: () -> Unit,
    onDelete: (delete: () -> Unit) -> Unit,
) {
    val reflections = remember(revision) { service.reflections() }
    val behaviours = remember(revision) { service.behaviourLogs() }
    val foods = remember(revision) { service.foodLogs() }
    val foodOffered = remember(revision) { service.isFoodLoggingOffered() }
    val isEmpty = reflections.isEmpty() && behaviours.isEmpty() && foods.isEmpty()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = AspenTheme.spacing.l, vertical = AspenTheme.spacing.xl),
        verticalArrangement = Arrangement.spacedBy(AspenTheme.spacing.sm),
    ) {
        AspenScreenHeader(
            title = stringResource(Res.string.reflect_title),
            subtitle = stringResource(Res.string.reflect_subtitle),
        )
        Spacer(Modifier.height(AspenTheme.spacing.s))

        EntryButton(Res.string.reflect_new_reflection, onWriteReflection)
        EntryButton(Res.string.reflect_new_feeling_log, onFeelingLog)
        if (foodOffered) EntryButton(Res.string.reflect_new_food_log, onFoodLog)

        // Cloud reflection (Phase 4): visible ONLY while the explicit consent grant is active.
        if (reflectionCompanion != null && reflectionCompanion.isEnabled()) {
            Spacer(Modifier.height(AspenTheme.spacing.s))
            ReflectionCompanionCard(companion = reflectionCompanion, onReachSomeone = onReachSomeone)
        }

        Spacer(Modifier.height(AspenTheme.spacing.m))
        if (isEmpty) {
            Text(
                stringResource(Res.string.reflect_empty),
                style = AspenTheme.typography.bodyLoose,
                color = AspenTheme.colors.textSecondary,
            )
        } else {
            EntrySection(Res.string.reflect_section_reflections, reflections.map { it.id to it.text }, emptyList(), onDelete) { service.deleteReflection(it) }
            EntrySection(Res.string.reflect_section_feelings, behaviours.map { it.id to it.note }, behaviours.map { it.feelings }, onDelete) { service.deleteBehaviourLog(it) }
            EntrySection(Res.string.reflect_section_food, foods.map { it.id to it.note }, foods.map { it.feelings }, onDelete) { service.deleteFoodLog(it) }
        }
    }
}

@Composable
private fun EntrySection(
    heading: StringResource,
    entries: List<Pair<String, String>>,
    feelingsByEntry: List<Set<FeelingTag>>,
    onDelete: (delete: () -> Unit) -> Unit,
    deleteById: (String) -> Unit,
) {
    if (entries.isEmpty()) return
    Spacer(Modifier.height(AspenTheme.spacing.s))
    Text(stringResource(heading), style = AspenTheme.typography.label, color = AspenTheme.colors.textSecondary)
    entries.forEachIndexed { i, (id, text) ->
        val feelings = feelingsByEntry.getOrNull(i).orEmpty()
        EntryCard(text = text, feelings = feelings, onDelete = { onDelete { deleteById(id) } })
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EntryCard(text: String, feelings: Set<FeelingTag>, onDelete: () -> Unit) {
    AspenCard(modifier = Modifier.fillMaxWidth().padding(top = AspenTheme.spacing.xs)) {
        if (text.isNotBlank()) {
            Text(text, style = AspenTheme.typography.body, color = AspenTheme.colors.textPrimary)
        }
        if (feelings.isNotEmpty()) {
            Spacer(Modifier.height(AspenTheme.spacing.s))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(AspenTheme.spacing.s),
                verticalArrangement = Arrangement.spacedBy(AspenTheme.spacing.xs),
            ) {
                feelings.forEach { tag -> AspenTagPill(stringResource(feelingLabel(tag))) }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            AspenTextAction(label = stringResource(Res.string.reflect_delete), onClick = onDelete)
        }
    }
}

@Composable
private fun ReflectionEditor(onSave: (String) -> Unit, onCancel: () -> Unit) {
    var text by remember { mutableStateOf("") }
    EditorScaffold(
        canSave = text.isNotBlank(),
        onSave = { onSave(text) },
        onCancel = onCancel,
    ) {
        AspenTextField(
            value = text,
            onValueChange = { text = it },
            placeholder = Res.string.reflect_text_hint,
            minHeight = 220.dp,
        )
    }
}

@Composable
private fun LogEditor(onSave: (String, Set<FeelingTag>) -> Unit, onCancel: () -> Unit) {
    var note by remember { mutableStateOf("") }
    var feelings by remember { mutableStateOf(emptySet<FeelingTag>()) }
    EditorScaffold(
        canSave = note.isNotBlank() || feelings.isNotEmpty(),
        onSave = { onSave(note, feelings) },
        onCancel = onCancel,
    ) {
        AspenTextField(
            value = note,
            onValueChange = { note = it },
            placeholder = Res.string.reflect_note_hint,
            minHeight = 140.dp,
        )
        Spacer(Modifier.height(AspenTheme.spacing.m))
        Text(stringResource(Res.string.reflect_feelings_label), style = AspenTheme.typography.label, color = AspenTheme.colors.textSecondary)
        Spacer(Modifier.height(AspenTheme.spacing.s))
        FeelingTagChips(selected = feelings) { tag ->
            feelings = if (tag in feelings) feelings - tag else feelings + tag
        }
    }
}

/** The notebook's writing surface: soft corners, sage focus, warm paper fill — never a stark form field. */
@Composable
private fun AspenTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: StringResource,
    minHeight: Dp,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
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
        modifier = Modifier.fillMaxWidth().height(minHeight),
    )
}

@Composable
private fun EditorScaffold(
    canSave: Boolean,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = AspenTheme.spacing.l, vertical = AspenTheme.spacing.xl),
    ) {
        content()
        Spacer(Modifier.height(AspenTheme.spacing.l))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(AspenTheme.spacing.s)) {
            AspenQuietButton(
                label = stringResource(Res.string.reflect_cancel),
                onClick = onCancel,
                modifier = Modifier.weight(1f),
            )
            AspenPrimaryButton(
                label = stringResource(Res.string.reflect_save),
                onClick = onSave,
                enabled = canSave,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun EntryButton(label: StringResource, onClick: () -> Unit) {
    AspenQuietButton(
        label = stringResource(label),
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun PlaceholderColumn(text: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(AspenTheme.spacing.l),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text, style = AspenTheme.typography.bodyLoose, color = AspenTheme.colors.textSecondary)
    }
}
