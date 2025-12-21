package com.astrixforge.devicemasker.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.DevicesOther
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.SimCard
import androidx.compose.material.icons.outlined.TrackChanges
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
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
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.astrixforge.devicemasker.common.CorrelationGroup
import com.astrixforge.devicemasker.common.DeviceProfilePreset
import com.astrixforge.devicemasker.common.SpoofProfile
import com.astrixforge.devicemasker.common.SpoofType
import com.astrixforge.devicemasker.common.models.Carrier
import com.astrixforge.devicemasker.common.models.Country
import com.astrixforge.devicemasker.data.models.DeviceIdentifier
import com.astrixforge.devicemasker.data.models.InstalledApp
import com.astrixforge.devicemasker.data.repository.SpoofRepository
import com.astrixforge.devicemasker.ui.components.AppListItem
import com.astrixforge.devicemasker.ui.components.EmptyState
import com.astrixforge.devicemasker.ui.components.IconCircle
import com.astrixforge.devicemasker.ui.components.dialog.CountryPickerDialog
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
 * UI Display Categories - Groups spoof types by correlation for better organization.
 * 
 * This is different from SpoofCategory because it groups correlated values together:
 * - SIM Card: All SIM_CARD correlation group values (IMSI, ICCID, PHONE, CARRIER) - CORRELATED
 * - Device Hardware: DEVICE_HARDWARE group + DEVICE_PROFILE - CORRELATED
 * - Network: WiFi/Bluetooth (non-correlated network values) - INDEPENDENT
 * - Advertising: All advertising identifiers - INDEPENDENT
 * - Location: GPS + timezone/locale (location correlation group) - CORRELATED
 * 
 * Correlated categories: Single switch + single regenerate button (all values sync)
 * Independent categories: Individual switches + regenerate buttons per item
 */
private enum class UIDisplayCategory(
    val titleRes: Int,
    val descriptionRes: Int,
    val icon: ImageVector,
    val color: Color,
    val types: List<SpoofType>,
    val isCorrelated: Boolean,  // If true, values sync together
) {
    SIM_CARD(
        titleRes = R.string.category_sim_card,
        descriptionRes = R.string.category_sim_card_desc,
        icon = Icons.Outlined.SimCard,
        color = Color(0xFF00BCD4), // Cyan
        types = listOf(
            SpoofType.PHONE_NUMBER,       // Combined with Carrier Name
            SpoofType.CARRIER_NAME,       // Shows below Phone Number
            SpoofType.SIM_COUNTRY_ISO,    // NEW: SIM country code
            SpoofType.NETWORK_COUNTRY_ISO,// NEW: Network country code
            SpoofType.IMSI,               // Independent
            SpoofType.ICCID,              // Independent
            SpoofType.CARRIER_MCC_MNC,    // Independent
            SpoofType.SIM_OPERATOR_NAME,  // NEW: SIM operator name
            SpoofType.NETWORK_OPERATOR,   // NEW: Network operator (MCC+MNC)
        ),
        isCorrelated = false,  // Custom handling: Phone+Carrier combined, others independent
    ),
    DEVICE_HARDWARE(
        titleRes = R.string.category_device_hardware,
        descriptionRes = R.string.category_device_hardware_desc,
        icon = Icons.Outlined.DevicesOther,
        color = Color(0xFF9C27B0), // Purple
        types = listOf(
            SpoofType.DEVICE_PROFILE,
            SpoofType.IMEI,
            SpoofType.SERIAL,
        ),
        isCorrelated = false,  // Custom handling: Device Profile controls all, IMEI/Serial independent
    ),
    NETWORK(
        titleRes = R.string.category_network,
        descriptionRes = R.string.category_network_desc,
        icon = Icons.Outlined.Wifi,
        color = Color(0xFF4CAF50), // Green
        types = listOf(
            SpoofType.WIFI_MAC,
            SpoofType.BLUETOOTH_MAC,
            SpoofType.WIFI_SSID,
            SpoofType.WIFI_BSSID,
        ),
        isCorrelated = false,  // Each can be regenerated independently
    ),
    ADVERTISING(
        titleRes = R.string.category_advertising,
        descriptionRes = R.string.category_advertising_desc,
        icon = Icons.Outlined.TrackChanges,
        color = Color(0xFFFF9800), // Orange
        types = listOf(
            SpoofType.ANDROID_ID,
            SpoofType.GSF_ID,
            SpoofType.ADVERTISING_ID,
            SpoofType.MEDIA_DRM_ID,
        ),
        isCorrelated = false,  // Each can be regenerated independently
    ),
    LOCATION(
        titleRes = R.string.category_location,
        descriptionRes = R.string.category_location_desc,
        icon = Icons.Outlined.LocationOn,
        color = Color(0xFFE91E63), // Pink
        types = listOf(
            // Order: Timezone first (controls locale), then Lat/Long (independent GPS)
            SpoofType.TIMEZONE,
            SpoofType.LOCALE,
            SpoofType.LOCATION_LATITUDE,
            SpoofType.LOCATION_LONGITUDE,
        ),
        isCorrelated = false,  // Custom handling: Timezone+Locale sync, Lat/Long independent
    );
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
                                                                                val correlationGroup = type.correlationGroup
                                                                                
                                                                                // For SIM values: use regenerateSIMValueOnly to keep same carrier
                                                                                // This fixes the bug where phone number showed wrong country code
                                                                                val newValue = when (correlationGroup) {
                                                                                        CorrelationGroup.SIM_CARD -> 
                                                                                                repository.regenerateSIMValueOnly(type)
                                                                                        else -> {
                                                                                                // For non-SIM correlated values, reset cache first
                                                                                                if (correlationGroup != CorrelationGroup.NONE) {
                                                                                                        repository.resetCorrelationGroup(correlationGroup)
                                                                                                }
                                                                                                repository.generateValue(type)
                                                                                        }
                                                                                }
                                                                                
                                                                                val updated = p.withValue(type, newValue)
                                                                                repository.updateProfile(updated)
                                                                        }
                                                                }
                                                        },
                                                        onRegenerateCategory = { category ->
                                                                profile?.let { p ->
                                                                        scope.launch {
                                                                                // Reset the cache for this correlation group first
                                                                                if (category.isCorrelated) {
                                                                                        val correlationGroup = category.types.firstOrNull()?.correlationGroup
                                                                                        if (correlationGroup != null) {
                                                                                                repository.resetCorrelationGroup(correlationGroup)
                                                                                        }
                                                                                }
                                                                                
                                                                                // Now regenerate all types in this category
                                                                                var updatedProfile = p
                                                                                category.types.forEach { type ->
                                                                                        val newValue = repository.generateValue(type)
                                                                                        updatedProfile = updatedProfile.withValue(type, newValue)
                                                                                }
                                                                                repository.updateProfile(updatedProfile)
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
                                                        onRegenerateLocation = {
                                                                profile?.let { p ->
                                                                        scope.launch {
                                                                                repository.regenerateLocationValues(p.id)
                                                                        }
                                                                }
                                                        },
                                                        onCarrierChange = { carrier ->
                                                                profile?.let { p ->
                                                                        scope.launch {
                                                                                repository.updateProfileWithCarrier(p.id, carrier)
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
                                                                                                .addAppToProfile(
                                                                                                        p.id,
                                                                                                        app.packageName,
                                                                                                )
                                                                                } else {
                                                                                        repository
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
        onRegenerateCategory: (UIDisplayCategory) -> Unit,
        onRegenerateLocation: () -> Unit,
        onToggle: (SpoofType, Boolean) -> Unit,
        onCarrierChange: (Carrier) -> Unit,
        modifier: Modifier = Modifier,
) {
        val clipboardManager = LocalClipboardManager.current
        
        // Use session-scoped state holder - triggers recomposition on change
        var refreshTrigger by remember { mutableIntStateOf(0) }

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

                // Categories - organized by correlation groups
                UIDisplayCategory.entries.forEach { category ->
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
                                        onRegenerateCategory = { onRegenerateCategory(category) },
                                        onRegenerateLocation = onRegenerateLocation,
                                        onToggle = onToggle,
                                        onCarrierChange = onCarrierChange,
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

/** Category section for profile spoof values - organized by correlation groups. */
@Composable
private fun ProfileCategorySection(
        category: UIDisplayCategory,
        profile: SpoofProfile?,
        isExpanded: Boolean,
        onToggleExpand: () -> Unit,
        onRegenerate: (SpoofType) -> Unit,
        onRegenerateCategory: () -> Unit,
        onRegenerateLocation: () -> Unit,
        onToggle: (SpoofType, Boolean) -> Unit,
        onCarrierChange: (Carrier) -> Unit,
        onCopy: (String) -> Unit,
        modifier: Modifier = Modifier,
) {
        val rotationAngle by
                animateFloatAsState(
                        targetValue = if (isExpanded) 0f else 180f,
                        animationSpec = AppMotion.FastSpring,
                        label = "expandRotation",
                )

        val categoryShape = animatedRoundedCornerShape(
                targetRadius = if (isExpanded) 24.dp else 16.dp
        )
        
        // For correlated categories, check if ANY type is enabled
        val isCategoryEnabled = if (category.isCorrelated) {
                category.types.any { profile?.isTypeEnabled(it) ?: false }
        } else {
                false // Not used for independent categories
        }

        ElevatedCard(
                modifier = modifier.animateContentSize(animationSpec = spring()),
                colors =
                        CardDefaults.elevatedCardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        ),
                shape = categoryShape,
        ) {
                Column {
                        // Header - simplified, only icon + title + expand arrow
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
                                        modifier = Modifier.weight(1f)
                                ) {
                                        IconCircle(
                                                icon = category.icon,
                                                containerColor = category.color.copy(alpha = 0.15f),
                                                iconColor = category.color,
                                                iconSize = 22.dp,
                                        )
                                        Text(
                                                text = stringResource(id = category.titleRes),
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
                                        // For correlated categories: show switch + regenerate at top of expanded area
                                        if (category.isCorrelated) {
                                                Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically,
                                                ) {
                                                        ExpressiveSwitch(
                                                                checked = isCategoryEnabled,
                                                                onCheckedChange = { enabled ->
                                                                        // Toggle ALL types in this category together
                                                                        category.types.forEach { type ->
                                                                                onToggle(type, enabled)
                                                                        }
                                                                }
                                                        )
                                                        
                                                        if (isCategoryEnabled) {
                                                                CompactExpressiveIconButton(
                                                                        onClick = onRegenerateCategory,
                                                                        icon = Icons.Filled.Refresh,
                                                                        contentDescription = stringResource(id = R.string.action_regenerate_all),
                                                                        tint = category.color,
                                                                )
                                                        }
                                                }
                                                Spacer(modifier = Modifier.height(4.dp))
                                        }

                                        // Handle special categories
                                        when (category) {
                                                UIDisplayCategory.SIM_CARD -> {
                                                        // Special handling for SIM Card category
                                                        SIMCardCategoryContent(
                                                                profile = profile,
                                                                onToggle = onToggle,
                                                                onRegenerate = onRegenerate,
                                                                onCarrierChange = onCarrierChange,
                                                                onCopy = onCopy,
                                                        )
                                                }
                                                UIDisplayCategory.LOCATION -> {
                                                        // Special handling for Location category
                                                        LocationCategoryContent(
                                                                profile = profile,
                                                                onToggle = onToggle,
                                                                onRegenerate = onRegenerate,
                                                                onRegenerateLocation = onRegenerateLocation,
                                                                onCopy = onCopy,
                                                        )
                                                }
                                                UIDisplayCategory.DEVICE_HARDWARE -> {
                                                        // Special handling for Device Hardware category
                                                        DeviceHardwareCategoryContent(
                                                                profile = profile,
                                                                onToggle = onToggle,
                                                                onRegenerate = onRegenerate,
                                                                onRegenerateCategory = onRegenerateCategory,
                                                                onCopy = onCopy,
                                                        )
                                                }
                                                else -> {
                                                        // Standard handling for other categories
                                                        category.types.forEach { type ->
                                                                val isProfileEnabled =
                                                                        profile?.isTypeEnabled(type) ?: false
                                                                val rawValue = profile?.getValue(type) ?: ""
                                                                
                                                                // For DEVICE_PROFILE, show the preset name instead of ID
                                                                val displayValue = if (type == SpoofType.DEVICE_PROFILE) {
                                                                        DeviceProfilePreset.findById(rawValue)?.name ?: rawValue
                                                                } else {
                                                                        rawValue
                                                                }

                                                                if (category.isCorrelated) {
                                                                        // Correlated: display-only items (no individual controls)
                                                                        CorrelatedSpoofItem(
                                                                                type = type,
                                                                                value = displayValue,
                                                                                isEnabled = isCategoryEnabled,
                                                                                onCopy = { onCopy(displayValue) },
                                                                                modifier = Modifier.fillMaxWidth()
                                                                        )
                                                                } else {
                                                                        // Independent: items with individual controls
                                                                        IndependentSpoofItem(
                                                                                type = type,
                                                                                value = displayValue,
                                                                                isEnabled = isProfileEnabled,
                                                                                onToggle = { enabled -> onToggle(type, enabled) },
                                                                                onRegenerate = { onRegenerate(type) },
                                                                                onCopy = { onCopy(displayValue) },
                                                                                modifier = Modifier.fillMaxWidth()
                                                                        )
                                                                }
                                                        }
                                                }
                                        }
                                }
                        }
                }
        }
}

/**
 * Display-only spoof item for correlated categories.
 * 
 * No individual switch or regenerate - those are at the category level.
 * Long-press on value to copy.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CorrelatedSpoofItem(
        type: SpoofType,
        value: String,
        isEnabled: Boolean,
        onCopy: () -> Unit,
        modifier: Modifier = Modifier,
) {
        Card(
                modifier = modifier,
                colors = CardDefaults.cardColors(
                        containerColor = if (isEnabled) {
                                MaterialTheme.colorScheme.surface
                        } else {
                                MaterialTheme.colorScheme.surfaceContainerLow
                        }
                ),
                shape = MaterialTheme.shapes.medium,
        ) {
                Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                ) {
                        Column(modifier = Modifier.weight(1f)) {
                                Text(
                                        text = type.displayName,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = if (isEnabled) {
                                                MaterialTheme.colorScheme.onSurface
                                        } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                )
                                if (isEnabled && value.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        // Value text - long press to copy
                                        Text(
                                                text = value,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontFamily = FontFamily.Monospace,
                                                color = MaterialTheme.colorScheme.primary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.combinedClickable(
                                                        onClick = { },
                                                        onLongClick = { onCopy() }
                                                ),
                                        )
                                }
                        }
                }
        }
}

/**
 * Independent spoof item with individual controls.
 * 
 * Has its own switch and regenerate button since values don't need to sync.
 * Long-press on value to copy.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun IndependentSpoofItem(
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
                        // Header row with switch
                        Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                        ) {
                                Text(
                                        text = type.displayName,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.weight(1f),
                                )
                                ExpressiveSwitch(checked = isEnabled, onCheckedChange = { onToggle(it) })
                        }

                        // Value and regenerate (only when enabled)
                        if (isEnabled) {
                                Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                ) {
                                        // Value text - long press to copy
                                        Text(
                                                text = value.ifEmpty { stringResource(id = R.string.profile_detail_not_set) },
                                                style = MaterialTheme.typography.bodySmall,
                                                fontFamily = FontFamily.Monospace,
                                                color = MaterialTheme.colorScheme.primary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier
                                                        .weight(1f)
                                                        .combinedClickable(
                                                                onClick = { },
                                                                onLongClick = { if (value.isNotEmpty()) onCopy() }
                                                        ),
                                        )

                                        // Regenerate button (standard size)
                                        IconButton(onClick = onRegenerate) {
                                                Icon(
                                                        imageVector = Icons.Filled.Refresh,
                                                        contentDescription = stringResource(id = R.string.action_regenerate),
                                                        tint = MaterialTheme.colorScheme.primary,
                                                )
                                        }
                                }
                        }
                }
        }
}

/**
 * Content layout for SIM Card category with carrier-driven flow.
 * 
 * UI Structure:
 * 1. Carrier Selection Card - Country picker + Carrier dropdown
 * 2. Locked Values Section - Read-only derived values (no switch/regenerate)
 * 3. Regeneratable Values Section - Phone, IMSI, ICCID with regenerate buttons
 * 
 * Long-press on any value to copy.
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun SIMCardCategoryContent(
        profile: SpoofProfile?,
        onToggle: (SpoofType, Boolean) -> Unit,
        onRegenerate: (SpoofType) -> Unit,
        onCarrierChange: (Carrier) -> Unit,
        onCopy: (String) -> Unit,
) {
        // State for country picker dialog
        var showCountryPicker by remember { mutableStateOf(false) }
        
        // Get current carrier from profile
        val currentCarrierMccMnc = profile?.selectedCarrierMccMnc
        val currentCarrier = remember(currentCarrierMccMnc) {
                currentCarrierMccMnc?.let { Carrier.getByMccMnc(it) }
        }
        
        // Selected country (from current carrier or default)
        var selectedCountryIso by remember(currentCarrier) { 
                mutableStateOf(currentCarrier?.countryIso ?: "IN") 
        }
        
        // Get carriers filtered by selected country
        val carriersForCountry = remember(selectedCountryIso) {
                Carrier.getByCountry(selectedCountryIso)
        }
        
        // Values from profile
        val phoneEnabled = profile?.isTypeEnabled(SpoofType.PHONE_NUMBER) ?: false
        val phoneValue = profile?.getValue(SpoofType.PHONE_NUMBER) ?: ""
        val imsiEnabled = profile?.isTypeEnabled(SpoofType.IMSI) ?: false
        val imsiValue = profile?.getValue(SpoofType.IMSI) ?: ""
        val iccidEnabled = profile?.isTypeEnabled(SpoofType.ICCID) ?: false
        val iccidValue = profile?.getValue(SpoofType.ICCID) ?: ""
        
        // Locked/derived values
        val simCountryValue = profile?.getValue(SpoofType.SIM_COUNTRY_ISO) ?: ""
        val networkCountryValue = profile?.getValue(SpoofType.NETWORK_COUNTRY_ISO) ?: ""
        val mccMncValue = profile?.getValue(SpoofType.CARRIER_MCC_MNC) ?: ""
        val carrierNameValue = profile?.getValue(SpoofType.CARRIER_NAME) ?: ""
        val simOperatorValue = profile?.getValue(SpoofType.SIM_OPERATOR_NAME) ?: ""
        val networkOperatorValue = profile?.getValue(SpoofType.NETWORK_OPERATOR) ?: ""
        
        // Country picker dialog
        if (showCountryPicker) {
                CountryPickerDialog(
                        selectedCountryIso = selectedCountryIso,
                        onCountrySelected = { country ->
                                selectedCountryIso = country.iso
                                showCountryPicker = false
                                // Auto-select first carrier from new country
                                val newCarriers = Carrier.getByCountry(country.iso)
                                if (newCarriers.isNotEmpty()) {
                                        onCarrierChange(newCarriers.first())
                                }
                        },
                        onDismiss = { showCountryPicker = false }
                )
        }
        
        // ═══════════════════════════════════════════════════════════
        // 1. CARRIER SELECTION CARD
        // ═══════════════════════════════════════════════════════════
        Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = MaterialTheme.shapes.medium,
        ) {
                Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                        // Country picker row
                        Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                        ) {
                                Text(
                                        text = "Country",
                                        style = MaterialTheme.typography.bodyMedium,
                                )
                                
                                // Country button - opens dialog (centered flag+name, right arrow)
                                val selectedCountry = Country.getByIso(selectedCountryIso)
                                OutlinedCard(
                                        onClick = { showCountryPicker = true },
                                        modifier = Modifier.width(200.dp),
                                        shape = RoundedCornerShape(12.dp),
                                ) {
                                        Row(
                                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                                // Centered: Flag + Country name
                                                Row(
                                                        modifier = Modifier.weight(1f),
                                                        horizontalArrangement = Arrangement.Center,
                                                        verticalAlignment = Alignment.CenterVertically,
                                                ) {
                                                        Text(
                                                                text = "${selectedCountry?.emoji ?: "🌍"} ${selectedCountry?.name ?: selectedCountryIso}",
                                                                style = MaterialTheme.typography.bodyMedium,
                                                        )
                                                }
                                                // Right arrow indicator
                                                Icon(
                                                        imageVector = Icons.Filled.ChevronRight,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(20.dp),
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                )
                                        }
                                }
                        }
                        
                        // Carrier dropdown
                        Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                        ) {
                                Text(
                                        text = "Carrier",
                                        style = MaterialTheme.typography.bodyMedium,
                                )
                                
                                var carrierDropdownExpanded by remember { mutableStateOf(false) }
                                
                                ExposedDropdownMenuBox(
                                        expanded = carrierDropdownExpanded,
                                        onExpandedChange = { carrierDropdownExpanded = it },
                                        modifier = Modifier.width(200.dp),
                                ) {
                                        // Rounded container matching Country picker style
                                        OutlinedCard(
                                                onClick = { carrierDropdownExpanded = true },
                                                modifier = Modifier.menuAnchor().fillMaxWidth(),
                                                shape = RoundedCornerShape(12.dp),
                                        ) {
                                                Row(
                                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically,
                                                ) {
                                                        Text(
                                                                text = currentCarrier?.displayName ?: "Select carrier",
                                                                style = MaterialTheme.typography.bodyMedium,
                                                                modifier = Modifier.weight(1f),
                                                        )
                                                        // Keep dropdown indicator as is
                                                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = carrierDropdownExpanded)
                                                }
                                        }
                                        
                                        ExposedDropdownMenu(
                                                expanded = carrierDropdownExpanded,
                                                onDismissRequest = { carrierDropdownExpanded = false },
                                        ) {
                                                carriersForCountry.forEach { carrier ->
                                                        DropdownMenuItem(
                                                                text = { Text(carrier.displayName) },
                                                                onClick = {
                                                                        onCarrierChange(carrier)
                                                                        carrierDropdownExpanded = false
                                                                },
                                                                leadingIcon = if (carrier.mccMnc == currentCarrierMccMnc) {
                                                                        { Icon(Icons.Filled.Check, null) }
                                                                } else null,
                                                        )
                                                }
                                        }
                                }
                        }
                }
        }
        
        // ═══════════════════════════════════════════════════════════
        // 2. LOCKED VALUES (derived from carrier, no switch/regenerate)
        // ═══════════════════════════════════════════════════════════
        Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = MaterialTheme.shapes.medium,
        ) {
                Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                        Text(
                                text = "Carrier Info",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        
                        // SIM Country
                        ReadOnlyValueRow(
                                label = "SIM Country",
                                value = simCountryValue,
                                onCopy = { onCopy(simCountryValue) },
                        )
                        
                        // Network Country
                        ReadOnlyValueRow(
                                label = "Network Country",
                                value = networkCountryValue,
                                onCopy = { onCopy(networkCountryValue) },
                        )
                        
                        // MCC/MNC
                        ReadOnlyValueRow(
                                label = "MCC/MNC",
                                value = mccMncValue,
                                onCopy = { onCopy(mccMncValue) },
                        )
                        
                        // Carrier Name
                        ReadOnlyValueRow(
                                label = "Carrier Name",
                                value = carrierNameValue,
                                onCopy = { onCopy(carrierNameValue) },
                        )
                        
                        // SIM Operator
                        ReadOnlyValueRow(
                                label = "SIM Operator",
                                value = simOperatorValue,
                                onCopy = { onCopy(simOperatorValue) },
                        )
                        
                        // Network Operator
                        ReadOnlyValueRow(
                                label = "Network Operator",
                                value = networkOperatorValue,
                                onCopy = { onCopy(networkOperatorValue) },
                        )
                }
        }
        
        // ═══════════════════════════════════════════════════════════
        // 3. REGENERATABLE VALUES (Phone, IMSI, ICCID)
        // ═══════════════════════════════════════════════════════════
        
        // Phone Number
        IndependentSpoofItem(
                type = SpoofType.PHONE_NUMBER,
                value = phoneValue,
                isEnabled = phoneEnabled,
                onToggle = { enabled -> onToggle(SpoofType.PHONE_NUMBER, enabled) },
                onRegenerate = { onRegenerate(SpoofType.PHONE_NUMBER) },
                onCopy = { onCopy(phoneValue) },
                modifier = Modifier.fillMaxWidth()
        )
        
        // IMSI
        IndependentSpoofItem(
                type = SpoofType.IMSI,
                value = imsiValue,
                isEnabled = imsiEnabled,
                onToggle = { enabled -> onToggle(SpoofType.IMSI, enabled) },
                onRegenerate = { onRegenerate(SpoofType.IMSI) },
                onCopy = { onCopy(imsiValue) },
                modifier = Modifier.fillMaxWidth()
        )
        
        // ICCID
        IndependentSpoofItem(
                type = SpoofType.ICCID,
                value = iccidValue,
                isEnabled = iccidEnabled,
                onToggle = { enabled -> onToggle(SpoofType.ICCID, enabled) },
                onRegenerate = { onRegenerate(SpoofType.ICCID) },
                onCopy = { onCopy(iccidValue) },
                modifier = Modifier.fillMaxWidth()
        )
}

/**
 * Read-only value row for locked/derived values.
 * No switch, no regenerate - just label, value, and long-press to copy.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ReadOnlyValueRow(
        label: String,
        value: String,
        onCopy: () -> Unit,
) {
        Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
        ) {
                Text(
                        text = label,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                        text = value.ifEmpty { "—" },
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.combinedClickable(
                                onClick = { },
                                onLongClick = { if (value.isNotEmpty()) onCopy() }
                        ),
                )
        }
}



/**
 * Special content layout for Location category.
 * 
 * - Timezone + Locale: Single card, single switch. Regenerate button updates both.
 * - Latitude/Longitude: Each has its own Switch + Regenerate (fully independent)
 * 
 * Long-press on values to copy.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LocationCategoryContent(
        profile: SpoofProfile?,
        onToggle: (SpoofType, Boolean) -> Unit,
        onRegenerate: (SpoofType) -> Unit,
        onRegenerateLocation: () -> Unit,
        onCopy: (String) -> Unit,
) {
        val timezoneEnabled = profile?.isTypeEnabled(SpoofType.TIMEZONE) ?: false
        val timezoneValue = profile?.getValue(SpoofType.TIMEZONE) ?: ""
        val localeValue = profile?.getValue(SpoofType.LOCALE) ?: ""
        val latEnabled = profile?.isTypeEnabled(SpoofType.LOCATION_LATITUDE) ?: false
        val latValue = profile?.getValue(SpoofType.LOCATION_LATITUDE) ?: ""
        val longEnabled = profile?.isTypeEnabled(SpoofType.LOCATION_LONGITUDE) ?: false
        val longValue = profile?.getValue(SpoofType.LOCATION_LONGITUDE) ?: ""

        // 1. Timezone + Locale combined card - one switch controls both
        Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = MaterialTheme.shapes.medium,
        ) {
                Column(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                        // Header with switch - controls both Timezone AND Locale
                        Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                        ) {
                                Column {
                                        Text(
                                                text = SpoofType.TIMEZONE.displayName,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Medium,
                                        )
                                }
                                Spacer(modifier = Modifier.weight(1f))
                                ExpressiveSwitch(
                                        checked = timezoneEnabled,
                                        onCheckedChange = { enabled ->
                                                // Toggle timezone AND locale together
                                                onToggle(SpoofType.TIMEZONE, enabled)
                                                onToggle(SpoofType.LOCALE, enabled)
                                        }
                                )
                        }

                        if (timezoneEnabled) {
                                // Timezone value row - long press to copy
                                Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                ) {
                                        Text(
                                                text = timezoneValue.ifEmpty { stringResource(id = R.string.profile_detail_not_set) },
                                                style = MaterialTheme.typography.bodySmall,
                                                fontFamily = FontFamily.Monospace,
                                                color = MaterialTheme.colorScheme.primary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier
                                                        .weight(1f)
                                                        .combinedClickable(
                                                                onClick = { },
                                                                onLongClick = { if (timezoneValue.isNotEmpty()) onCopy(timezoneValue) }
                                                        ),
                                        )

                                        // Regenerate Timezone + Locale together from same country
                                        IconButton(onClick = onRegenerateLocation) {
                                                Icon(
                                                        imageVector = Icons.Filled.Refresh,
                                                        contentDescription = stringResource(id = R.string.action_regenerate),
                                                        tint = MaterialTheme.colorScheme.primary,
                                                )
                                        }
                                }
                                
                                // Locale section with its own header
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                        text = SpoofType.LOCALE.displayName,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                        text = localeValue.ifEmpty { stringResource(id = R.string.profile_detail_not_set) },
                                        style = MaterialTheme.typography.bodySmall,
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.primary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.combinedClickable(
                                                onClick = { },
                                                onLongClick = { if (localeValue.isNotEmpty()) onCopy(localeValue) }
                                        ),
                                )
                        }
                }
        }

        // 2. Latitude - independent with own switch and regenerate
        IndependentSpoofItem(
                type = SpoofType.LOCATION_LATITUDE,
                value = latValue,
                isEnabled = latEnabled,
                onToggle = { enabled -> onToggle(SpoofType.LOCATION_LATITUDE, enabled) },
                onRegenerate = { onRegenerate(SpoofType.LOCATION_LATITUDE) },
                onCopy = { onCopy(latValue) },
                modifier = Modifier.fillMaxWidth()
        )

        // 3. Longitude - independent with own switch and regenerate
        IndependentSpoofItem(
                type = SpoofType.LOCATION_LONGITUDE,
                value = longValue,
                isEnabled = longEnabled,
                onToggle = { enabled -> onToggle(SpoofType.LOCATION_LONGITUDE, enabled) },
                onRegenerate = { onRegenerate(SpoofType.LOCATION_LONGITUDE) },
                onCopy = { onCopy(longValue) },
                modifier = Modifier.fillMaxWidth()
        )
}

/**
 * Content layout for Device Hardware category.
 * 
 * All 3 items are fully independent with their own Switch + Regenerate:
 * - Device Profile (shows preset name)
 * - IMEI
 * - Serial
 * 
 * Long-press on values to copy.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DeviceHardwareCategoryContent(
        profile: SpoofProfile?,
        onToggle: (SpoofType, Boolean) -> Unit,
        onRegenerate: (SpoofType) -> Unit,
        onRegenerateCategory: () -> Unit, // Not used - kept for interface compatibility
        onCopy: (String) -> Unit,
) {
        val deviceProfileEnabled = profile?.isTypeEnabled(SpoofType.DEVICE_PROFILE) ?: false
        val deviceProfileRawValue = profile?.getValue(SpoofType.DEVICE_PROFILE) ?: ""
        val deviceProfileDisplayValue = DeviceProfilePreset.findById(deviceProfileRawValue)?.name ?: deviceProfileRawValue
        val imeiEnabled = profile?.isTypeEnabled(SpoofType.IMEI) ?: false
        val imeiValue = profile?.getValue(SpoofType.IMEI) ?: ""
        val serialEnabled = profile?.isTypeEnabled(SpoofType.SERIAL) ?: false
        val serialValue = profile?.getValue(SpoofType.SERIAL) ?: ""

        // 1. Device Profile - independent (shows preset name instead of ID)
        IndependentSpoofItem(
                type = SpoofType.DEVICE_PROFILE,
                value = deviceProfileDisplayValue,
                isEnabled = deviceProfileEnabled,
                onToggle = { enabled -> onToggle(SpoofType.DEVICE_PROFILE, enabled) },
                onRegenerate = { onRegenerate(SpoofType.DEVICE_PROFILE) },
                onCopy = { onCopy(deviceProfileDisplayValue) },
                modifier = Modifier.fillMaxWidth()
        )

        // 2. IMEI - independent
        IndependentSpoofItem(
                type = SpoofType.IMEI,
                value = imeiValue,
                isEnabled = imeiEnabled,
                onToggle = { enabled -> onToggle(SpoofType.IMEI, enabled) },
                onRegenerate = { onRegenerate(SpoofType.IMEI) },
                onCopy = { onCopy(imeiValue) },
                modifier = Modifier.fillMaxWidth()
        )

        // 3. Serial - independent
        IndependentSpoofItem(
                type = SpoofType.SERIAL,
                value = serialValue,
                isEnabled = serialEnabled,
                onToggle = { enabled -> onToggle(SpoofType.SERIAL, enabled) },
                onRegenerate = { onRegenerate(SpoofType.SERIAL) },
                onCopy = { onCopy(serialValue) },
                modifier = Modifier.fillMaxWidth()
        )
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
