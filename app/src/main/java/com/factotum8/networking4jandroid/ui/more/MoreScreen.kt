package com.factotum8.networking4jandroid.ui.more

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * The bottom nav's 4th destination (decision #1): the source's 6-link top
 * bar doesn't fit a phone-width bottom bar, so the 3 secondary pages
 * (Дубликаты/Настройки/Импорт-экспорт) live behind this one extra tap.
 */
@Composable
fun MoreScreen(onOpenDuplicates: () -> Unit, onOpenSettings: () -> Unit, onOpenImportExport: () -> Unit) {
    Scaffold { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            ListItem(
                headlineContent = { Text("Дубликаты") },
                modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenDuplicates),
            )
            HorizontalDivider()
            ListItem(
                headlineContent = { Text("Настройки") },
                modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenSettings),
            )
            HorizontalDivider()
            ListItem(
                headlineContent = { Text("Импорт/экспорт") },
                modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenImportExport),
            )
        }
    }
}
