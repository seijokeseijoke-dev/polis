package se.polishandelser.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp

/** Startskärm: användaren anger ort. Förslag hämtas ur de händelser som faktiskt finns i flödet. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrtScreen(
    vm: EventsViewModel,
    onOrtSelected: (String) -> Unit
) {
    val state = vm.state
    var query by rememberSaveable { mutableStateOf("") }
    val keyboard = LocalSoftwareKeyboardController.current

    fun submit(ort: String) {
        val clean = ort.trim()
        if (clean.isNotEmpty()) {
            keyboard?.hide()
            onOrtSelected(clean)
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Polishändelser") }) }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Ort") },
                placeholder = { Text("t.ex. Göteborg") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Search
                ),
                keyboardActions = KeyboardActions(onSearch = { submit(query) })
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = { submit(query) },
                enabled = query.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) { Text("Visa händelser") }

            Spacer(Modifier.height(16.dp))

            when {
                state.isLoading -> {
                    Text("Hämtar händelser…", style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(4.dp))
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                }

                state.events.isEmpty() && state.error != null -> {
                    Text(state.error, color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = vm::retry) { Text("Försök igen") }
                }

                else -> {
                    val suggestions = vm.ortSuggestions(query)
                    Text(
                        if (query.isBlank()) "Orter med flest händelser just nu" else "Förslag",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    if (suggestions.isEmpty()) {
                        Text(
                            "Inga händelser för \"${query.trim()}\" i det aktuella flödet.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    LazyColumn {
                        items(suggestions, key = { it.first }) { (ort, count) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { query = ort; submit(ort) }
                                    .padding(vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(ort, style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    "$count",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}
