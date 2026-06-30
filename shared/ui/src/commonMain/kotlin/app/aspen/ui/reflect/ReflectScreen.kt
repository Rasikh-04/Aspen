package app.aspen.ui.reflect

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import app.aspen.design.AspenTheme
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
 */
@Composable
fun ReflectScreen(loggingService: LoggingService?) {
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
        Text(stringResource(Res.string.reflect_title), style = AspenTheme.typography.display, color = AspenTheme.colors.textPrimary)
        Text(stringResource(Res.string.reflect_subtitle), style = AspenTheme.typography.bodyLoose, color = AspenTheme.colors.textSecondary)
        Spacer(Modifier.height(AspenTheme.spacing.s))

        EntryButton(Res.string.reflect_new_reflection, onWriteReflection)
        EntryButton(Res.string.reflect_new_feeling_log, onFeelingLog)
        if (foodOffered) EntryButton(Res.string.reflect_new_food_log, onFoodLog)

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

@Composable
private fun EntryCard(text: String, feelings: Set<FeelingTag>, onDelete: () -> Unit) {
    Surface(
        shape = AspenTheme.shapes.medium,
        color = AspenTheme.colors.surface,
        modifier = Modifier.fillMaxWidth().padding(top = AspenTheme.spacing.xs),
    ) {
        Column(Modifier.padding(AspenTheme.spacing.m)) {
            if (text.isNotBlank()) {
                Text(text, style = AspenTheme.typography.body, color = AspenTheme.colors.textPrimary)
            }
            if (feelings.isNotEmpty()) {
                Spacer(Modifier.height(AspenTheme.spacing.xs))
                Row {
                    feelings.forEach { tag ->
                        Text(
                            stringResource(feelingLabel(tag)) + "   ",
                            style = AspenTheme.typography.caption,
                            color = AspenTheme.colors.textSecondary,
                        )
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDelete) {
                    Text(stringResource(Res.string.reflect_delete), color = AspenTheme.colors.textSecondary)
                }
            }
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
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            placeholder = { Text(stringResource(Res.string.reflect_text_hint)) },
            modifier = Modifier.fillMaxWidth().height(220.dp),
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
        OutlinedTextField(
            value = note,
            onValueChange = { note = it },
            placeholder = { Text(stringResource(Res.string.reflect_note_hint)) },
            modifier = Modifier.fillMaxWidth().height(140.dp),
        )
        Spacer(Modifier.height(AspenTheme.spacing.m))
        Text(stringResource(Res.string.reflect_feelings_label), style = AspenTheme.typography.label, color = AspenTheme.colors.textSecondary)
        Spacer(Modifier.height(AspenTheme.spacing.s))
        FeelingTagChips(selected = feelings) { tag ->
            feelings = if (tag in feelings) feelings - tag else feelings + tag
        }
    }
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
            OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f).height(56.dp), shape = AspenTheme.shapes.large) {
                Text(stringResource(Res.string.reflect_cancel), color = AspenTheme.colors.textPrimary)
            }
            Button(
                onClick = onSave,
                enabled = canSave,
                modifier = Modifier.weight(1f).height(56.dp),
                shape = AspenTheme.shapes.large,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AspenTheme.colors.primary,
                    contentColor = AspenTheme.colors.textInverse,
                ),
            ) {
                Text(stringResource(Res.string.reflect_save), style = AspenTheme.typography.label)
            }
        }
    }
}

@Composable
private fun EntryButton(label: StringResource, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, shape = AspenTheme.shapes.large, modifier = Modifier.fillMaxWidth().height(56.dp)) {
        Text(stringResource(label), style = AspenTheme.typography.label, color = AspenTheme.colors.textPrimary)
    }
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
