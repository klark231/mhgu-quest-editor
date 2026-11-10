@file:OptIn(ExperimentalMaterial3Api::class)

package io.github.klark231.mhguqe

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

// The file data lives in byte arrays that compose can't watch, so every
// field reads this. It changes on each edit and the readers redraw.
/** quest text language picked on the home screen, -1 = follow the app */
val LocalQuestLang = compositionLocalOf { -1 }

val LocalRev = compositionLocalOf { 0 }
val LocalIcons = staticCompositionLocalOf<(Int) -> ImageBitmap?> { { null } }

@Composable
fun tick() = LocalRev.current

@Composable
fun Section(title: String, modifier: Modifier = Modifier, action: @Composable RowScope.() -> Unit = {}, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                action()
            }
            content()
        }
    }
}

@Composable
fun FieldRow(content: @Composable RowScope.() -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top, content = content)
}

@Composable
fun Hint(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
fun SectionLabel(t: String) {
    Text(t, Modifier.padding(start = 4.dp, top = 12.dp), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
}

/** scrolling page used by every tab */
@Composable
fun Page(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content,
    )
}

@Composable
fun NumField(
    label: String, get: () -> Int, set: (Int) -> Unit, modifier: Modifier = Modifier,
    min: Int = 0, max: Int = 255,
) {
    tick()
    val value = get()
    var text by remember { mutableStateOf(value.toString()) }
    LaunchedEffect(value) { if (text.toIntOrNull() != value) text = value.toString() }
    val ok = text.toIntOrNull()?.let { it in min..max } == true
    OutlinedTextField(
        value = text,
        onValueChange = { t ->
            text = t.filter { it.isDigit() || (it == '-' && min < 0) }.take(11)
            text.toIntOrNull()?.takeIf { it in min..max && it != value }?.let(set)
        },
        modifier = modifier,
        label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        singleLine = true,
        isError = !ok,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
    )
}

@Composable
fun FloatField(label: String, get: () -> Float, set: (Float) -> Unit, modifier: Modifier = Modifier) {
    tick()
    val value = get()
    fun parse(s: String) = s.replace(',', '.').toFloatOrNull()
    var text by remember { mutableStateOf(value.toString()) }
    LaunchedEffect(value) { if (parse(text) != value) text = value.toString() }
    OutlinedTextField(
        value = text,
        onValueChange = { t ->
            text = t.filter { it.isDigit() || it in "-.,eE" }.take(16)
            parse(text)?.takeIf { it.isFinite() && it != value }?.let(set)
        },
        modifier = modifier,
        label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        singleLine = true,
        isError = parse(text) == null,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
    )
}

@Composable
fun TextArea(label: String, get: () -> String, set: (String) -> Unit, modifier: Modifier = Modifier, single: Boolean = false) {
    tick()
    val value = get()
    var text by remember { mutableStateOf(value) }
    LaunchedEffect(value) { if (text != value) text = value }
    OutlinedTextField(
        value = text,
        onValueChange = { text = if (single) it.replace("\n", "") else it; if (text != value) set(text) },
        modifier = modifier.fillMaxWidth(),
        label = { Text(label) },
        singleLine = single,
        minLines = if (single) 1 else 3,
    )
}

@Composable
fun SmallIcon(icon: Int, size: Int = 32) {
    val img = LocalIcons.current(icon) ?: return
    Image(img, null, Modifier.size(size.dp).clip(RoundedCornerShape(6.dp)))
}

/**
 * Read-only box that opens a list to pick from. Long lists get a search
 * field. Unknown values still show up as "#123" so nothing gets lost.
 */
@Composable
fun ChoiceField(
    label: String, options: List<Opt>, get: () -> Int, set: (Int) -> Unit, modifier: Modifier = Modifier,
    fallback: ((Int) -> String)? = null,
) {
    tick()
    val value = get()
    val current = options.firstOrNull { it.value == value }
    val icon = current?.icon ?: -1
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        OutlinedTextField(
            value = current?.label ?: fallback?.invoke(value) ?: "#$value",
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            leadingIcon = if (icon >= 0) ({ SmallIcon(icon, 28) }) else null,
            trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
            modifier = Modifier.fillMaxWidth(),
        )
        // the text field eats clicks, so put a catcher over it
        Box(Modifier.matchParentSize().padding(top = 8.dp).clip(RoundedCornerShape(4.dp)).clickable { open = true })
    }
    if (open) PickerDialog(label, options, value, { set(it); open = false }) { open = false }
}

@Composable
fun PickerDialog(title: String, options: List<Opt>, selected: Int, onPick: (Int) -> Unit, onDismiss: () -> Unit) {
    var query by remember { mutableStateOf("") }
    val shown = remember(query, options) {
        if (query.isBlank()) options
        else options.filter { it.label.contains(query.trim(), ignoreCase = true) || it.value.toString() == query.trim() }
    }
    val start = remember { options.indexOfFirst { it.value == selected }.coerceAtLeast(0) }
    val list = rememberLazyListState(initialFirstVisibleItemIndex = (start - 3).coerceAtLeast(0))
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
            Column(Modifier.padding(vertical = 16.dp).heightIn(max = 560.dp)) {
                Text(title, Modifier.padding(horizontal = 24.dp), style = MaterialTheme.typography.titleLarge)
                if (options.size > 12) {
                    OutlinedTextField(
                        query, { query = it },
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        placeholder = { Text(stringResource(R.string.search)) },
                        leadingIcon = { Icon(Icons.Default.Search, null) },
                        singleLine = true,
                    )
                } else Spacer(Modifier.height(8.dp))
                LazyColumn(Modifier.weight(1f, fill = false), state = list) {
                    itemsIndexed(shown, key = { i, o -> "${o.value}:$i" }) { _, o ->
                        val sel = o.value == selected
                        Row(
                            Modifier.fillMaxWidth().clickable { onPick(o.value) }
                                .background(if (sel) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh)
                                .padding(horizontal = 24.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            if (o.icon >= 0) SmallIcon(o.icon)
                            Text(o.label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                            Text(o.value.toString(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
                }
            }
        }
    }
}

/** horizontal chips used to switch between sub-pages (languages, reward lists...) */
@Composable
fun ChipBar(labels: List<String>, selected: Int, onSelect: (Int) -> Unit, enabled: (Int) -> Boolean = { true }) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        labels.forEachIndexed { i, l ->
            FilterChip(selected = i == selected, onClick = { onSelect(i) }, label = { Text(l) }, enabled = enabled(i))
        }
    }
}

@Composable
fun ConfirmDialog(title: String, text: String, confirm: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = { TextButton(onClick = { onConfirm(); onDismiss() }) { Text(confirm) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
