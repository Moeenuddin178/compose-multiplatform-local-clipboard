package org.clipboard.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.clipboard.app.database.entities.ClipboardHistoryEntity
import org.clipboard.app.viewmodel.HistoryViewModel
import kotlinx.datetime.*
import kotlin.time.ExperimentalTime
import org.clipboard.app.ui.theme.*
import org.clipboard.app.ui.components.NeumorphicSurface
import org.clipboard.app.ui.screens.history.HistoryListItem
import org.clipboard.app.ui.screens.history.HistorySectionHeader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(viewModel: HistoryViewModel) {
    val searchQuery by viewModel.searchQuery.collectAsState("")
    val filteredHistory by viewModel.filteredHistory.collectAsState(emptyList())
    var showClearDialog by remember { mutableStateOf(false) }
    var selectedItem by remember { mutableStateOf<ClipboardHistoryEntity?>(null) }
    val scope = rememberCoroutineScope()
    
    // Group history by date
    val groupedHistory = remember(filteredHistory) {
        groupHistoryByDate(filteredHistory)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    androidx.compose.material3.Text(
                        "History",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    TextButton(
                        onClick = { showClearDialog = true },
                        enabled = filteredHistory.isNotEmpty()
                    ) {
                        androidx.compose.material3.Text(
                            "Clear all",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(
                horizontal = adaptivePadding(),
                vertical = adaptiveSpacing() / 2
            ),
            verticalArrangement = Arrangement.spacedBy(adaptiveSpacing())
        ) {
            // Search Bar
            item {
                NeumorphicSurface(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 16.dp
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.searchQuery.value = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        placeholder = { 
                            androidx.compose.material3.Text("Search history")
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
            
            // Grouped History Items
            if (groupedHistory.isNotEmpty()) {
                groupedHistory.forEach { (dateGroup, items) ->
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            HistorySectionHeader(title = dateGroup)
                            items.forEach { entity ->
                                HistoryListItem(
                                    item = entity,
                                    onDelete = {
                                        scope.launch { viewModel.deleteItem(entity.id) }
                                    },
                                    onClick = { selectedItem = entity }
                                )
                            }
                        }
                    }
                }
            } else {
                item {
                    EmptyHistoryCard(
                        hasSearchQuery = searchQuery.isNotEmpty()
                    )
                }
            }
        }
    }
    
    // Clear All Dialog
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { 
                androidx.compose.material3.Text(
                    "Clear All History",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            },
            text = { 
                androidx.compose.material3.Text(
                    "This will permanently delete all clipboard history. This action cannot be undone.",
                    style = MaterialTheme.typography.bodyLarge
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            viewModel.clearAll()
                        }
                        showClearDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    androidx.compose.material3.Text("Clear All")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showClearDialog = false },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    androidx.compose.material3.Text("Cancel")
                }
            }
        )
    }
    
    // Detail Dialog
    selectedItem?.let { item ->
        HistoryDetailDialog(
            item = item,
            onDismiss = { selectedItem = null },
            onDelete = {
                scope.launch {
                    viewModel.deleteItem(item.id)
                }
                selectedItem = null
            }
        )
    }
}

@Composable
private fun LoadingHistorySkeleton() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(adaptiveSpacing())
    ) {
        repeat(3) {
            NeumorphicSurface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                cornerRadius = 12.dp
            ) {}
        }
    }
}

@Composable
private fun ErrorHistoryBanner(message: String) {
    NeumorphicSurface(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 12.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(adaptivePadding())
        ) {
            androidx.compose.material3.Text(
                text = message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun HistoryDateSection(
    dateGroup: String,
    items: List<ClipboardHistoryEntity>,
    onDeleteItem: (ClipboardHistoryEntity) -> Unit,
    onItemClick: (ClipboardHistoryEntity) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(adaptiveCardPadding()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section Title
            androidx.compose.material3.Text(
                text = dateGroup,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            // History Items
            items.forEach { item ->
                HistoryItem(
                    item = item,
                    onDelete = { onDeleteItem(item) },
                    onItemClick = { onItemClick(item) }
                )
                
                if (item != items.last()) {
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
fun HistoryItem(
    item: ClipboardHistoryEntity,
    onDelete: () -> Unit,
    onItemClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp), // Fixed standard height
        onClick = onItemClick,
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Content
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Main content
                androidx.compose.material3.Text(
                    text = item.text.take(100) + if (item.text.length > 100) "..." else "",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                
                // Details (timestamp · device)
                androidx.compose.material3.Text(
                    text = "${formatTime(item.timestamp)} · ${item.sourceDeviceName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            
            // Delete Action
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(40.dp)
            ) {
                androidx.compose.material3.Text(
                    "🗑️",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

@Composable
fun EmptyHistoryCard(hasSearchQuery: Boolean) {
    NeumorphicSurface(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 16.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(adaptiveLargePadding()),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                androidx.compose.material3.Text(
                    "🕒",
                    style = MaterialTheme.typography.displayLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                    )
                )
                androidx.compose.material3.Text(
                    if (hasSearchQuery) "No matching items found" else "No clipboard history",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (!hasSearchQuery) {
                    androidx.compose.material3.Text(
                        "Copy some text to see it here",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalTime::class)
private fun groupHistoryByDate(history: List<ClipboardHistoryEntity>): Map<String, List<ClipboardHistoryEntity>> {
    val now = kotlin.time.Clock.System.now().toEpochMilliseconds()
    val today = now - (now % 86400000) // Start of today
    val yesterday = today - 86400000 // Start of yesterday
    
    return history.groupBy { item ->
        when {
            item.timestamp >= today -> "Today"
            item.timestamp >= yesterday -> "Yesterday"
            else -> {
                // Format as "Mon, Jan 15" for older dates
                formatDateForGrouping(item.timestamp)
            }
        }
    }
}

@OptIn(ExperimentalTime::class)
private fun formatDateForGrouping(timestamp: Long): String {
    val instant = Instant.fromEpochMilliseconds(timestamp)
    val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    
    val dayOfWeek = when (localDateTime.dayOfWeek) {
        DayOfWeek.MONDAY -> "Mon"
        DayOfWeek.TUESDAY -> "Tue"
        DayOfWeek.WEDNESDAY -> "Wed"
        DayOfWeek.THURSDAY -> "Thu"
        DayOfWeek.FRIDAY -> "Fri"
        DayOfWeek.SATURDAY -> "Sat"
        DayOfWeek.SUNDAY -> "Sun"
        else -> "Unknown"
    }

    val month = when (localDateTime.month) {
        Month.JANUARY -> "Jan"
        Month.FEBRUARY -> "Feb"
        Month.MARCH -> "Mar"
        Month.APRIL -> "Apr"
        Month.MAY -> "May"
        Month.JUNE -> "Jun"
        Month.JULY -> "Jul"
        Month.AUGUST -> "Aug"
        Month.SEPTEMBER -> "Sep"
        Month.OCTOBER -> "Oct"
        Month.NOVEMBER -> "Nov"
        Month.DECEMBER -> "Dec"
        else -> "Unknown"
    }
    
    return "$dayOfWeek, $month ${localDateTime.dayOfMonth}"
}

@Composable
fun HistoryDetailDialog(
    item: ClipboardHistoryEntity,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            androidx.compose.material3.Text(
                "Clipboard Details",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = { 
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Full text content
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    androidx.compose.material3.Text(
                        text = item.text,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Details
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        androidx.compose.material3.Text(
                            "Source Device:",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        androidx.compose.material3.Text(
                            item.sourceDeviceName,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        androidx.compose.material3.Text(
                            "Timestamp:",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        androidx.compose.material3.Text(
                            formatDetailedTime(item.timestamp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        androidx.compose.material3.Text(
                            "Length:",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        androidx.compose.material3.Text(
                            "${item.text.length} characters",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDelete,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                androidx.compose.material3.Text("Delete")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp)
            ) {
                androidx.compose.material3.Text("Close")
            }
        }
    )
}

@OptIn(ExperimentalTime::class)
private fun formatTime(timestamp: Long): String {
    val now = kotlin.time.Clock.System.now().toEpochMilliseconds()
    val date = Instant.fromEpochMilliseconds(timestamp)
        .toLocalDateTime(TimeZone.currentSystemDefault())
    
    val hour = date.hour
    val minute = date.minute
    
    val amPm = if (hour < 12) "AM" else "PM"
    val displayHour = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
    
    return "$displayHour:${minute.toString().padStart(2, '0')} $amPm"
}

@OptIn(ExperimentalTime::class)
private fun formatDetailedTime(timestamp: Long): String {
    val date = Instant.fromEpochMilliseconds(timestamp)
        .toLocalDateTime(TimeZone.currentSystemDefault())
    
    val month = when (date.month) {
        Month.JANUARY -> "Jan"
        Month.FEBRUARY -> "Feb"
        Month.MARCH -> "Mar"
        Month.APRIL -> "Apr"
        Month.MAY -> "May"
        Month.JUNE -> "Jun"
        Month.JULY -> "Jul"
        Month.AUGUST -> "Aug"
        Month.SEPTEMBER -> "Sep"
        Month.OCTOBER -> "Oct"
        Month.NOVEMBER -> "Nov"
        Month.DECEMBER -> "Dec"
        else -> "Unknown"
    }
    
    val hour = date.hour
    val minute = date.minute
    val amPm = if (hour < 12) "AM" else "PM"
    val displayHour = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
    
    return "$month ${date.dayOfMonth}, ${date.year} at $displayHour:${minute.toString().padStart(2, '0')} $amPm"
}