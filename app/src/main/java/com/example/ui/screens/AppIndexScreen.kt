package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.AppPreferenceEntity
import com.example.data.local.FluxSettingsState
import com.example.platform.InstalledAppInfo
import com.example.platform.LauncherPlatformManager
import com.example.ui.components.AppIconBadge
import com.example.ui.components.FluxSurfaceCard
import com.example.viewmodel.AppIndexSortMode

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AppIndexScreen(
    installedApps: List<InstalledAppInfo>,
    appPrefs: List<AppPreferenceEntity>,
    sortMode: AppIndexSortMode,
    settings: FluxSettingsState,
    selectedAppForSheet: InstalledAppInfo?,
    platformManager: LauncherPlatformManager,
    onSelectSortMode: (AppIndexSortMode) -> Unit,
    onLaunchApp: (String) -> Unit,
    onOpenAppSheet: (InstalledAppInfo?) -> Unit,
    onToggleDock: (String) -> Unit,
    onToggleHidden: (String) -> Unit,
    onAssignCategory: (String, String) -> Unit,
    onPinAppToActivity: (InstalledAppInfo) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    var localFilterQuery by remember { mutableStateOf("") }
    val prefMap = remember(appPrefs) { appPrefs.associateBy { it.packageName } }

    val filteredApps = remember(installedApps, prefMap, sortMode, localFilterQuery) {
        val q = localFilterQuery.trim().lowercase()
        val base = installedApps.filter { app ->
            val isHidden = prefMap[app.packageName]?.isHidden == true
            val matchesVisibility = if (sortMode == AppIndexSortMode.HIDDEN) isHidden else !isHidden
            val matchesQuery = q.isEmpty() || app.label.lowercase().contains(q) || app.packageName.lowercase().contains(q)
            matchesVisibility && matchesQuery
        }
        when (sortMode) {
            AppIndexSortMode.BY_USE -> base.sortedByDescending { prefMap[it.packageName]?.launchCount ?: 0 }
            else -> base.sortedBy { it.label.lowercase() }
        }
    }

    val clusteredApps = remember(filteredApps, prefMap) {
        filteredApps.groupBy { app ->
            prefMap[app.packageName]?.customCategory?.ifBlank { app.autoCategory } ?: app.autoCategory
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp)
            .testTag("app_index_screen")
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("app_index_back_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Column {
                    Text(
                        text = "Apps",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "${filteredApps.size} applications · Long-press any app for options",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Mode Tabs (Clusters, By Use, A-Z, Hidden) matching Screen 6 of concept
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AppIndexSortMode.entries.forEach { mode ->
                val active = mode == sortMode
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .clickable { onSelectSortMode(mode) }
                        .testTag("app_sort_${mode.name.lowercase()}")
                ) {
                    Text(
                        text = mode.label,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (sortMode == AppIndexSortMode.SPATIAL_CLUSTERS) {
                // Spatial Bubble Clusters grouped by category (Social, Media, Study, Work, Games, Tools, Shopping)
                items(clusteredApps.entries.toList(), key = { "cluster_${it.key}" }) { (categoryName, appsInCluster) ->
                    FluxSurfaceCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("app_cluster_$categoryName")
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = categoryName.uppercase(),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "${appsInCluster.size} apps",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                appsInCluster.forEach { app ->
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier
                                            .width(68.dp)
                                            .combinedClickable(
                                                onClick = { onLaunchApp(app.packageName) },
                                                onLongClick = { onOpenAppSheet(app) }
                                            )
                                            .testTag("app_tile_${app.packageName}")
                                    ) {
                                        AppIconBadge(
                                            packageName = app.packageName,
                                            label = app.label,
                                            platformManager = platformManager,
                                            sizeDp = settings.iconSizeDp.dp,
                                            shapeOption = settings.iconShape
                                        )
                                        if (settings.showAppLabels) {
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = app.label,
                                                style = MaterialTheme.typography.bodySmall,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Structured Row View for By Use, Alphabetical, or Hidden
                items(filteredApps, key = { it.packageName }) { app ->
                    val pref = prefMap[app.packageName]
                    FluxSurfaceCard(
                        onClick = { onLaunchApp(app.packageName) },
                        onLongClick = { onOpenAppSheet(app) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("app_row_${app.packageName}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AppIconBadge(
                                packageName = app.packageName,
                                label = app.label,
                                platformManager = platformManager,
                                sizeDp = 44.dp,
                                shapeOption = settings.iconShape
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = app.label,
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = "${pref?.customCategory?.ifBlank { app.autoCategory } ?: app.autoCategory} · ${pref?.launchCount ?: 0} launches",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { onOpenAppSheet(app) }) {
                                Icon(
                                    Icons.Default.Info,
                                    contentDescription = "App Options",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Bottom Search Bar inside App Index
        OutlinedTextField(
            value = localFilterQuery,
            onValueChange = { localFilterQuery = it },
            placeholder = { Text("Search installed apps…") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (localFilterQuery.isNotEmpty()) {
                    IconButton(onClick = { localFilterQuery = "" }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(22.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 14.dp)
                .testTag("app_index_search_input")
        )
    }

    // Long-press / App Options Sheet
    selectedAppForSheet?.let { app ->
        val pref = prefMap[app.packageName]
        val isDocked = pref?.isDocked == true
        val isHidden = pref?.isHidden == true
        val categories = listOf("Social", "Media", "Study", "Work", "Games", "Tools", "Shopping")

        ModalBottomSheet(
            onDismissRequest = { onOpenAppSheet(null) },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AppIconBadge(
                        packageName = app.packageName,
                        label = app.label,
                        platformManager = platformManager,
                        sizeDp = 52.dp,
                        shapeOption = settings.iconShape
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = app.label, style = MaterialTheme.typography.titleLarge)
                        Text(
                            text = "${app.packageName} · v${app.versionName}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Quick Actions: Dock, Hide, Pin to Activity, System App Info, Uninstall
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    OutlinedButton(
                        onClick = {
                            onToggleDock(app.packageName)
                            onOpenAppSheet(null)
                        }
                    ) {
                        Icon(Icons.Default.PushPin, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isDocked) "Unpin Dock" else "Pin to Dock")
                    }
                    OutlinedButton(
                        onClick = {
                            onToggleHidden(app.packageName)
                            onOpenAppSheet(null)
                        }
                    ) {
                        Icon(
                            if (isHidden) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isHidden) "Unhide" else "Hide App")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            onPinAppToActivity(app)
                            onOpenAppSheet(null)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Add to Activity")
                    }
                    OutlinedButton(
                        onClick = {
                            platformManager.openAppDetailsSettings(app.packageName)
                            onOpenAppSheet(null)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("App Info")
                    }
                    if (!app.isSystemApp) {
                        OutlinedButton(
                            onClick = {
                                platformManager.requestUninstallApp(app.packageName)
                                onOpenAppSheet(null)
                            }
                        ) {
                            Icon(
                                Icons.Default.DeleteOutline,
                                contentDescription = "Uninstall",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Dynamic App Shortcuts if exposed by the app
                if (app.shortcuts.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "APPLICATION SHORTCUTS",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    app.shortcuts.forEach { sc ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                                .clickable {
                                    platformManager.launchAppShortcut(app.packageName, sc.id)
                                    onOpenAppSheet(null)
                                }
                        ) {
                            Text(
                                text = sc.shortLabel,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "ASSIGN CLUSTER CATEGORY",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { cat ->
                        val currentCat = pref?.customCategory?.ifBlank { app.autoCategory } ?: app.autoCategory
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (cat == currentCat) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(
                                1.dp,
                                if (cat == currentCat) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                            ),
                            modifier = Modifier.clickable {
                                onAssignCategory(app.packageName, cat)
                                onOpenAppSheet(null)
                            }
                        ) {
                            Text(
                                text = cat,
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(28.dp))
            }
        }
    }
}
