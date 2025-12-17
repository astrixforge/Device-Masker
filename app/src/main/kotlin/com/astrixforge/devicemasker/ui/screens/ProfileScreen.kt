package com.astrixforge.devicemasker.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.astrixforge.devicemasker.data.models.SpoofProfile
import com.astrixforge.devicemasker.data.repository.SpoofRepository
import com.astrixforge.devicemasker.ui.components.ProfileCard
import com.astrixforge.devicemasker.ui.theme.DeviceMaskerTheme
import kotlinx.coroutines.launch

/**
 * Screen for managing spoof profiles.
 *
 * Features:
 * - List of saved profiles
 * - Create new profile via FAB
 * - Edit profile details
 * - Delete profiles (except default)
 * - Set profile as default
 *
 * @param repository The SpoofRepository for data access
 * @param onProfileClick Callback when a profile is clicked
 * @param modifier Optional modifier
 */
@Composable
fun ProfileScreen(
        repository: SpoofRepository,
        onProfileClick: (SpoofProfile) -> Unit,
        modifier: Modifier = Modifier,
) {
    val profiles by repository.getAllProfiles().collectAsState(initial = emptyList())
    val activeProfileId by repository.getActiveProfileId().collectAsState(initial = null)

    var showCreateDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf<SpoofProfile?>(null) }
    var showDeleteDialog by remember { mutableStateOf<SpoofProfile?>(null) }
    val scope = rememberCoroutineScope()

    ProfileScreenContent(
            profiles = profiles,
            activeProfileId = activeProfileId,
            onProfileClick = onProfileClick,
            onCreateProfile = { showCreateDialog = true },
            onEditProfile = { showEditDialog = it },
            onDeleteProfile = { showDeleteDialog = it },
            onSetDefault = { profile -> repository.setDefaultProfile(profile.id) },
            onEnableChange = { profile, enabled ->
                scope.launch { repository.setProfileEnabled(profile.id, enabled) }
            },
            modifier = modifier,
    )

    // Create Profile Dialog
    if (showCreateDialog) {
        CreateProfileDialog(
                onDismiss = { showCreateDialog = false },
                onCreate = { name, description ->
                    repository.createProfile(name, description)
                    showCreateDialog = false
                },
        )
    }

    // Edit Profile Dialog
    showEditDialog?.let { profile ->
        EditProfileDialog(
                profile = profile,
                onDismiss = { showEditDialog = null },
                onSave = { name, description ->
                    repository.updateProfile(
                            profile.copy(
                                    name = name,
                                    description = description,
                                    updatedAt = System.currentTimeMillis(),
                            )
                    )
                    showEditDialog = null
                },
        )
    }

    // Delete Confirmation Dialog
    showDeleteDialog?.let { profile ->
        DeleteProfileDialog(
                profile = profile,
                onDismiss = { showDeleteDialog = null },
                onConfirm = {
                    repository.deleteProfile(profile.id)
                    showDeleteDialog = null
                },
        )
    }
}

/** Stateless content for ProfileScreen. */
@Composable
fun ProfileScreenContent(
        profiles: List<SpoofProfile>,
        activeProfileId: String?,
        onProfileClick: (SpoofProfile) -> Unit,
        onCreateProfile: () -> Unit,
        onEditProfile: (SpoofProfile) -> Unit,
        onDeleteProfile: (SpoofProfile) -> Unit,
        onSetDefault: (SpoofProfile) -> Unit,
        onEnableChange: (SpoofProfile, Boolean) -> Unit = { _, _ -> },
        modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Header - same style as Settings/Apps
            item {
                Text(
                        text = "Profiles",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(vertical = 8.dp),
                )
            }

            if (profiles.isEmpty()) {
                // Empty State
                item {
                    Box(
                            modifier = Modifier.fillMaxWidth().height(300.dp),
                            contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                    text = "No profiles yet",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                    text = "Tap + to create your first profile",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color =
                                            MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                                    alpha = 0.7f
                                            ),
                            )
                        }
                    }
                }
            } else {
                // Profile List
                items(items = profiles, key = { it.id }) { profile ->
                    ProfileCard(
                            profile = profile,
                            isEnabled = profile.isEnabled,
                            onClick = { onProfileClick(profile) },
                            onEdit = { onEditProfile(profile) },
                            onDelete = { onDeleteProfile(profile) },
                            onSetDefault = { onSetDefault(profile) },
                            onEnableChange = { enabled -> onEnableChange(profile, enabled) },
                    )
                }

                // Bottom spacing for FAB
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }

        // FAB positioned at bottom end
        ExtendedFloatingActionButton(
                onClick = onCreateProfile,
                icon = { Icon(imageVector = Icons.Default.Add, contentDescription = null) },
                text = { Text("New Profile") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        )
    }
}

/** Dialog for creating a new profile. */
@Composable
fun CreateProfileDialog(
        onDismiss: () -> Unit,
        onCreate: (name: String, description: String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    val isValid = name.isNotBlank()

    AlertDialog(
            onDismissRequest = onDismiss,
            icon = { Icon(imageVector = Icons.Default.Add, contentDescription = null) },
            title = { Text("Create Profile") },
            text = {
                Column {
                    val maxNameLength = 12
                    OutlinedTextField(
                            value = name,
                            onValueChange = { if (it.length <= maxNameLength) name = it },
                            label = { Text("Profile Name") },
                            placeholder = { Text("e.g., Samsung") },
                            singleLine = true,
                            supportingText = { Text("${name.length}/$maxNameLength") },
                            modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text("Description (optional)") },
                            placeholder = { Text("e.g., For banking apps") },
                            modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                TextButton(
                        onClick = { onCreate(name.trim(), description.trim()) },
                        enabled = isValid
                ) { Text("Create") }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/** Dialog for editing a profile. */
@Composable
fun EditProfileDialog(
        profile: SpoofProfile,
        onDismiss: () -> Unit,
        onSave: (name: String, description: String) -> Unit,
) {
    var name by remember { mutableStateOf(profile.name) }
    var description by remember { mutableStateOf(profile.description) }
    val isValid = name.isNotBlank()

    AlertDialog(
            onDismissRequest = onDismiss,
            icon = { Icon(imageVector = Icons.Default.Person, contentDescription = null) },
            title = { Text("Edit Profile") },
            text = {
                Column {
                    OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Profile Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text("Description (optional)") },
                            modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                TextButton(
                        onClick = { onSave(name.trim(), description.trim()) },
                        enabled = isValid
                ) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/** Dialog for confirming profile deletion. */
@Composable
fun DeleteProfileDialog(profile: SpoofProfile, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
            onDismissRequest = onDismiss,
            icon = {
                Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                )
            },
            title = { Text("Delete Profile?") },
            text = {
                Text(
                        "Are you sure you want to delete \"${profile.name}\"? " +
                                "Apps using this profile will switch to the default profile."
                )
            },
            confirmButton = {
                TextButton(onClick = onConfirm) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

// ═══════════════════════════════════════════════════════════
// Previews
// ═══════════════════════════════════════════════════════════

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
private fun ProfileScreenContentPreview() {
    DeviceMaskerTheme {
        ProfileScreenContent(
                profiles =
                        listOf(
                                SpoofProfile.createDefaultProfile(),
                                SpoofProfile.createNew("Samsung Galaxy S24"),
                                SpoofProfile.createNew("Pixel 9 Pro"),
                        ),
                activeProfileId = null,
                onProfileClick = {},
                onCreateProfile = {},
                onEditProfile = {},
                onDeleteProfile = {},
                onSetDefault = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
private fun ProfileScreenEmptyPreview() {
    DeviceMaskerTheme {
        ProfileScreenContent(
                profiles = emptyList(),
                activeProfileId = null,
                onProfileClick = {},
                onCreateProfile = {},
                onEditProfile = {},
                onDeleteProfile = {},
                onSetDefault = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
private fun CreateProfileDialogPreview() {
    DeviceMaskerTheme { CreateProfileDialog(onDismiss = {}, onCreate = { _, _ -> }) }
}
