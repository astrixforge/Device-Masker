package com.astrixforge.devicemasker.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.TrackChanges
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.astrixforge.devicemasker.BuildConfig
import com.astrixforge.devicemasker.R
import com.astrixforge.devicemasker.data.models.DeviceIdentifier
import com.astrixforge.devicemasker.data.models.InstalledApp
import com.astrixforge.devicemasker.data.models.SpoofCategory
import com.astrixforge.devicemasker.data.models.SpoofProfile
import com.astrixforge.devicemasker.data.models.SpoofType
import com.astrixforge.devicemasker.data.repository.SpoofRepository
import com.astrixforge.devicemasker.ui.components.AppListItem
import com.astrixforge.devicemasker.ui.components.EmptyState
import com.astrixforge.devicemasker.ui.components.IconCircle
import com.astrixforge.devicemasker.ui.components.expressive.AnimatedLoadingOverlay
import com.astrixforge.devicemasker.ui.components.expressive.CompactExpressiveIconButton
import com.astrixforge.devicemasker.ui.components.expressive.ExpressiveLoadingIndicatorWithLabel
import com.astrixforge.devicemasker.ui.components.expressive.ExpressiveSwitch
import com.astrixforge.devicemasker.ui.components.expressive.animatedRoundedCornerShape
import com.astrixforge.devicemasker.ui.theme.AppMotion
import kotlinx.coroutines.launch

/**
 * Session-scoped state holder for category expansion.
 * Persists across navigation but resets when app is killed.
 */
private object CategoryExpansionState {
    private val expandedCategories = mutableSetOf<String>()
    
    fun isExpanded(categoryName: String): Boolean = categoryName in expandedCategories
    
    fun toggle(categoryName: String) {
        if (categoryName in expandedCategories) {
            expandedCategories.remove(categoryName)
        } else {
            expandedCategories.add(categoryName)
        }
    }
}

/**
 * Profile Detail Screen with tabbed interface.
 *
 * Shows profile-specific spoof values and assigned apps in a two-tab layout:
 * - Tab 0: Spoof Values - Configure per-profile spoof values
 * - Tab 1: Apps - Assign apps to this profile
 *
 * @param profileId The ID of the profile to display
 * @param repository The SpoofRepository for data access
 * @param onNavigateBack Callback to navigate back
 * @param modifier Optional modifier
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileDetailScreen(
        profileId: String,
        repository: SpoofRepository,
        onNavigateBack: () -> Unit,
        modifier: Modifier = Modifier,
) {
        val profiles by repository.profiles.collectAsState(initial = emptyList())
        val profile = profiles.find { it.id == profileId }
        val installedApps by
                repository
                        .appScopeRepository
                        .getInstalledAppsFlow()
                        .collectAsState(initial = emptyList())
        val scope = rememberCoroutineScope()

        // Tab state
        var selectedTab by remember { mutableIntStateOf(0) }
        val pagerState = rememberPagerState(pageCount = { 2 })

        // Sync pager with tab
        LaunchedEffect(selectedTab) { pagerState.animateScrollToPage(selectedTab) }
        LaunchedEffect(pagerState.currentPage) { selectedTab = pagerState.currentPage }

        Box(modifier = modifier.fillMaxSize()) {

                Column(
                        modifier = Modifier.fillMaxSize()
                                .alpha(if (profile == null) 0f else 1f)
                ) {
                        // Header
                        Row(
                                modifier =
                                        Modifier.fillMaxWidth()
                                                .padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                        ) {
                                IconButton(onClick = onNavigateBack) {
                                        Icon(
                                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                                contentDescription = stringResource(id = R.string.profile_detail_back),
                                        )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                        text = profile?.name ?: stringResource(id = R.string.profile_detail_title),
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                )
                        }

                        // Tab Row
                        SecondaryTabRow(selectedTabIndex = selectedTab) {
                                Tab(
                                        selected = selectedTab == 0,
                                        onClick = { selectedTab = 0 },
                                        text = { Text(stringResource(id = R.string.profile_detail_tab_spoof)) },
                                        icon = {
                                                Icon(Icons.Filled.Tune, contentDescription = null)
                                        },
                                )
                                Tab(
                                        selected = selectedTab == 1,
                                        onClick = { selectedTab = 1 },
                                        text = { Text(stringResource(id = R.string.profile_detail_tab_apps)) },
                                        icon = {
                                                Icon(Icons.Filled.Apps, contentDescription = null)
                                        },
                                )
                        }

                        // Pager content
                        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) {
                                page ->
                                when (page) {
                                        0 ->
                                                ProfileSpoofContent(
                                                        profile = profile,
                                                        onRegenerate = { type ->
                                                                profile?.let { p ->
                                                                        scope.launch {
                                                                                val newValue =
                                                                                        repository
                                                                                                .generateValue(
                                                                                                        type
                                                                                                )
                                                                                val updated =
                                                                                        p.withValue(
                                                                                                type,
                                                                                                newValue
                                                                                        )
                                                                                repository
                                                                                        .updateProfile(
                                                                                                updated
                                                                                        )
                                                                        }
                                                                }
                                                        },
                                                        onToggle = { type, enabled ->
                                                                profile?.let { p ->
                                                                        scope.launch {
                                                                                val identifier =
                                                                                        p.getIdentifier(
                                                                                                type
                                                                                        ) ?: DeviceIdentifier.createDefault(type)
                                                                                val updated =
                                                                                        p.withIdentifier(
                                                                                                identifier
                                                                                                        .copy(
                                                                                                                isEnabled =
                                                                                                                        enabled
                                                                                                        )
                                                                                        )
                                                                                repository
                                                                                        .updateProfile(
                                                                                                updated
                                                                                        )
                                                                        }
                                                                }
                                                        },
                                                )
                                        1 ->
                                                ProfileAppsContent(
                                                        profile = profile,
                                                        allProfiles = profiles,
                                                        installedApps = installedApps,
                                                        onAppToggle = { app, checked ->
                                                                profile?.let { p ->
                                                                        scope.launch {
                                                                                if (checked) {
                                                                                        repository
                                                                                                .profileRepository
                                                                                                .addAppToProfile(
                                                                                                        p.id,
                                                                                                        app.packageName,
                                                                                                )
                                                                                } else {
                                                                                        repository
                                                                                                .profileRepository
                                                                                                .removeAppFromProfile(
                                                                                                        p.id,
                                                                                                        app.packageName,
                                                                                                )
                                                                                }
                                                                        }
                                                                }
                                                        },
                                                )
                                }
                        }
                }

                AnimatedLoadingOverlay(isLoading = profile == null) {
                        ExpressiveLoadingIndicatorWithLabel(
                                label = "Initializing profile..."
                        )
                }
        }
}

/** Spoof Values tab content. */
@Composable
private fun ProfileSpoofContent(
        profile: SpoofProfile?,
        onRegenerate: (SpoofType) -> Unit,
        onToggle: (SpoofType, Boolean) -> Unit,
        modifier: Modifier = Modifier,
) {
        val clipboardManager = LocalClipboardManager.current
        
        // Use session-scoped state holder - triggers recomposition on change
        var refreshTrigger by remember { mutableStateOf(0) }

        LazyColumn(
                modifier = modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
                // Header info
                item {
                        if (profile != null) {
                                Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors =
                                                CardDefaults.cardColors(
                                                        containerColor =
                                                                MaterialTheme.colorScheme
                                                                        .surfaceContainerLow
                                                ),
                                        shape = MaterialTheme.shapes.medium,
                                ) {
                                        Row(
                                                modifier = Modifier.padding(12.dp),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                                Text(
                                                        text =
                                                                stringResource(id = R.string.profile_detail_spoof_desc),
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color =
                                                                MaterialTheme.colorScheme
                                                                        .onSurfaceVariant,
                                                )
                                        }
                                }
                        }
                }

                // Categories
                SpoofCategory.entries.forEach { category ->
                        item(key = "spoof_${category.name}") {
                                // Read from session state (refreshTrigger forces recomposition)
                                val isExpanded = CategoryExpansionState.isExpanded(category.name)
                                @Suppress("UNUSED_EXPRESSION")
                                refreshTrigger // Use to trigger recomposition
                                
                                ProfileCategorySection(
                                        category = category,
                                        profile = profile,
                                        isExpanded = isExpanded,
                                        onToggleExpand = {
                                                CategoryExpansionState.toggle(category.name)
                                                refreshTrigger++ // Trigger recomposition
                                        },
                                        onRegenerate = onRegenerate,
                                        onToggle = onToggle,
                                        onCopy = { value ->
                                                clipboardManager.setText(AnnotatedString(value))
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                )
                        }
                }

                item { Spacer(modifier = Modifier.height(24.dp)) }
        }
}

/** Category section for profile spoof values. */
@Composable
private fun ProfileCategorySection(
        category: SpoofCategory,
        profile: SpoofProfile?,
        isExpanded: Boolean,
        onToggleExpand: () -> Unit,
        onRegenerate: (SpoofType) -> Unit,
        onToggle: (SpoofType, Boolean) -> Unit,
        onCopy: (String) -> Unit,
        modifier: Modifier = Modifier,
) {
        val rotationAngle by
                animateFloatAsState(
                        targetValue = if (isExpanded) 0f else 180f,
                        animationSpec = AppMotion.FastSpring,
                        label = "expandRotation",
                )

        val categoryIcon =
                when (category) {
                        SpoofCategory.DEVICE -> Icons.Outlined.Devices
                        SpoofCategory.NETWORK -> Icons.Outlined.Wifi
                        SpoofCategory.ADVERTISING -> Icons.Outlined.TrackChanges
                        SpoofCategory.SYSTEM -> Icons.Outlined.Settings
                        SpoofCategory.LOCATION -> Icons.Outlined.LocationOn
                }

        val categoryColor =
                when (category) {
                        SpoofCategory.DEVICE -> Color(0xFF00BCD4)
                        SpoofCategory.NETWORK -> Color(0xFF4CAF50)
                        SpoofCategory.ADVERTISING -> Color(0xFFFF9800)
                        SpoofCategory.SYSTEM -> Color(0xFF9C27B0)
                        SpoofCategory.LOCATION -> Color(0xFFE91E63)
                }

        val typesInCategory = SpoofType.byCategory(category)

        val categoryShape = animatedRoundedCornerShape(
                targetRadius = if (isExpanded) 24.dp else 16.dp
        )

        ElevatedCard(
                modifier = modifier.animateContentSize(animationSpec = spring()),
                colors =
                        CardDefaults.elevatedCardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        ),
                shape = categoryShape,
        ) {
                Column {
                        // Header
                        Row(
                                modifier =
                                        Modifier.fillMaxWidth()
                                                .clickable { onToggleExpand() }
                                                .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                        ) {
                                Row(
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                ) {
                                        IconCircle(
                                                icon = categoryIcon,
                                                containerColor = categoryColor.copy(alpha = 0.15f),
                                                iconColor = categoryColor,
                                                iconSize = 22.dp,
                                        )
                                        Text(
                                                text = category.displayName,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.SemiBold,
                                        )
                                }

                                Icon(
                                        imageVector = Icons.Filled.ExpandLess,
                                        contentDescription =
                                                if (isExpanded) stringResource(id = R.string.action_collapse) else stringResource(id = R.string.action_expand),
                                        modifier = Modifier.rotate(rotationAngle),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                        }

                        // Content
                        AnimatedVisibility(
                                visible = isExpanded,
                                enter = expandVertically(animationSpec = spring()) + fadeIn(),
                                exit = shrinkVertically(animationSpec = spring()) + fadeOut(),
                        ) {
                                Column(
                                        modifier =
                                                Modifier.padding(
                                                        start = 16.dp,
                                                        end = 16.dp,
                                                        bottom = 16.dp
                                                ),
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                        typesInCategory.forEach { type ->
                                                val isProfileEnabled =
                                                        profile?.isTypeEnabled(type) ?: false
                                                val value = profile?.getValue(type) ?: ""

                                                ProfileSpoofItem(
                                                        type = type,
                                                        value = value,
                                                        isEnabled = isProfileEnabled,
                                                        onToggle = { enabled ->
                                                                onToggle(type, enabled)
                                                        },
                                                        onRegenerate = { onRegenerate(type) },
                                                        onCopy = { onCopy(value) },
                                                        modifier = Modifier.fillMaxWidth()
                                                )
                                        }
                                }
                        }
                }
        }
}

/** Individual spoof value item in profile. */
@Composable
private fun ProfileSpoofItem(
        type: SpoofType,
        value: String,
        isEnabled: Boolean,
        onToggle: (Boolean) -> Unit,
        onRegenerate: () -> Unit,
        onCopy: () -> Unit,
        modifier: Modifier = Modifier,
) {
        Card(
                modifier = modifier,
                colors =
                        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = MaterialTheme.shapes.medium,
        ) {
                Column(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                        // Header row
                        Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                        ) {
                                Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                                text = type.displayName,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Medium,
                                        )
                                }
                                ExpressiveSwitch(checked = isEnabled, onCheckedChange = { onToggle(it) })
                        }

                        // Value and actions
                        if (isEnabled) {
                                Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                ) {
                                        Text(
                                                text = value.ifEmpty { stringResource(id = R.string.profile_detail_not_set) },
                                                style = MaterialTheme.typography.bodySmall,
                                                fontFamily = FontFamily.Monospace,
                                                color = MaterialTheme.colorScheme.primary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f),
                                        )

                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                CompactExpressiveIconButton(
                                                        onClick = onCopy,
                                                        icon = Icons.Filled.ContentCopy,
                                                        contentDescription = stringResource(id = R.string.action_copy),
                                                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                                )
                                                CompactExpressiveIconButton(
                                                        onClick = onRegenerate,
                                                        icon = Icons.Filled.Refresh,
                                                        contentDescription = stringResource(id = R.string.action_regenerate),
                                                        tint = MaterialTheme.colorScheme.primary,
                                                )
                                        }
                                }
                        }
                }
        }
}

/** Apps tab content. */
@Composable
private fun ProfileAppsContent(
        profile: SpoofProfile?,
        allProfiles: List<SpoofProfile>,
        installedApps: List<InstalledApp>,
        onAppToggle: (InstalledApp, Boolean) -> Unit,
        modifier: Modifier = Modifier,
) {
        var searchQuery by remember { mutableStateOf("") }

        // Filter apps - exclude system apps and own app
        // Sort with assigned apps first for better UX
        val filteredApps =
                remember(installedApps, searchQuery, profile) {
                        installedApps
                                .filter { app ->
                                        // Always exclude our own app and system apps
                                        if (app.packageName == BuildConfig.APPLICATION_ID) {
                                                return@filter false
                                        }
                                        if (app.isSystemApp) {
                                                return@filter false
                                        }
                                        // Match search query
                                        searchQuery.isEmpty() ||
                                                app.label.contains(
                                                        searchQuery,
                                                        ignoreCase = true
                                                ) ||
                                                app.packageName.contains(
                                                        searchQuery,
                                                        ignoreCase = true
                                                )
                                }
                                // Sort: assigned apps first, then alphabetically
                                .sortedWith(
                                        compareByDescending<InstalledApp> {
                                                profile?.isAppAssigned(it.packageName) == true
                                        }
                                                .thenBy { it.label.lowercase() }
                                )
                }

        Column(modifier = modifier.fillMaxSize()) {
                // Search and filter
                Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                        // Search bar
                        OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text(stringResource(id = R.string.profile_detail_search_hint)) },
                                leadingIcon = {
                                        Icon(Icons.Filled.Search, contentDescription = null)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                        )

                        Text(
                                text =
                                        pluralStringResource(
                                                id = R.plurals.profile_detail_apps_assigned_stats,
                                                count = filteredApps.size,
                                                filteredApps.size,
                                                profile?.assignedAppCount() ?: 0
                                        ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                }

                // App list with empty state
                if (installedApps.isEmpty()) {
                        // Loading state
                        Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                        ) {
                                ExpressiveLoadingIndicatorWithLabel(
                                        label = stringResource(id = R.string.profile_detail_loading_apps)
                                )
                        }
                } else if (filteredApps.isEmpty()) {
                        // Empty state for search results
                        EmptyState(
                                icon = Icons.Filled.Search,
                                title = stringResource(id = R.string.profile_detail_no_apps_found),
                                subtitle = stringResource(id = R.string.profile_detail_adjust_search),
                        )
                } else {
                        LazyColumn(
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                                items(filteredApps, key = { it.packageName }) { app ->
                                        val isAssignedToThisProfile =
                                                profile?.isAppAssigned(app.packageName) == true
                                        val assignedToOtherProfile =
                                                allProfiles.filter { it.id != profile?.id }.find {
                                                        it.isAppAssigned(app.packageName)
                                                }

                                        AppListItem(
                                                app = app,
                                                isAssigned = isAssignedToThisProfile,
                                                assignedToOtherProfileName =
                                                        assignedToOtherProfile?.name,
                                                onToggle = { checked -> onAppToggle(app, checked) },
                                                modifier = Modifier.fillMaxWidth()
                                        )
                                }

                                item { Spacer(modifier = Modifier.height(24.dp)) }
                        }
                }
        }
}
