package com.jinjinmory.wuwatracking.ui.main

import android.Manifest
import android.content.Intent
import android.content.Context
import android.os.Build
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jinjinmory.wuwatracking.BuildConfig
import com.jinjinmory.wuwatracking.R
import com.jinjinmory.wuwatracking.background.BackgroundRefreshScheduler
import com.jinjinmory.wuwatracking.data.preferences.NotificationSettingsManager
import com.jinjinmory.wuwatracking.data.preferences.AppPreferencesManager
import com.jinjinmory.wuwatracking.data.preferences.ProfileCacheManager
import com.jinjinmory.wuwatracking.data.preferences.UserSettingsManager
import com.jinjinmory.wuwatracking.data.remote.dto.WuwaProfile
import com.jinjinmory.wuwatracking.data.repository.ProfileFetchResult
import com.jinjinmory.wuwatracking.data.security.AuthKeyManager
import com.jinjinmory.wuwatracking.domain.ProfileResultHandler
import com.jinjinmory.wuwatracking.notifications.NotificationHelper
import com.jinjinmory.wuwatracking.util.BatteryOptimizationHelper
import kotlin.math.max
import org.json.JSONArray
import org.json.JSONObject

private val SUPPORTED_REGIONS = listOf("America", "Asia", "Europe", "HMT", "SEA")

private data class ProfileUiModel(
    val id: String,
    val uid: String,
    val region: String,
    val authKey: String
)

private data class ProfileEditorState(
    val id: String,
    val uid: String,
    val region: String,
    val authKey: String,
    val authKeyVisible: Boolean = false
)

private fun ProfileUiModel.toEditorState(): ProfileEditorState =
    ProfileEditorState(id = id, uid = uid, region = region, authKey = authKey)

private fun ProfileEditorState.toUiModel(): ProfileUiModel =
    ProfileUiModel(id = id, uid = uid.trim(), region = region.trim(), authKey = authKey.trim())

private fun loadProfiles(context: Context): List<ProfileUiModel> =
    UserSettingsManager.getProfiles(context).map { stored ->
        ProfileUiModel(
            id = stored.id,
            uid = stored.uid,
            region = stored.region,
            authKey = AuthKeyManager.getAuthKeyForProfile(context, stored.id).orEmpty()
        )
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    viewModel: MainViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val waveplateName = stringResource(id = R.string.proper_waveplates)

    var optionsExpanded by remember { mutableStateOf(false) }
    var showSettingsDialog by rememberSaveable { mutableStateOf(false) }
    var showLanguageDialog by rememberSaveable { mutableStateOf(false) }
    var showThresholdDialog by rememberSaveable { mutableStateOf(false) }
    var showRawJsonDialog by rememberSaveable { mutableStateOf(false) }

    var profiles by remember { mutableStateOf(loadProfiles(context)) }
    var activeProfileId by remember { mutableStateOf(UserSettingsManager.getActiveProfileId(context)) }
    var selectedLanguage by remember { mutableStateOf(AppPreferencesManager.getSelectedLanguage(context)) }
    var waveplateThresholds by remember { mutableStateOf(NotificationSettingsManager.getWaveplateThresholds(context)) }
    var cachedRawPayload by remember { mutableStateOf(ProfileCacheManager.getPayload(context)?.rawPayload) }
    var rawJsonDialogContent by remember { mutableStateOf<String?>(null) }
    var pendingNotificationPermission by remember { mutableStateOf(false) }

    val batteryPromptNeeded = remember(context) {
        !AppPreferencesManager.isBatteryNoticeAcknowledged(context) &&
            !BatteryOptimizationHelper.isIgnoringOptimizations(context)
    }
    var showBatteryPrompt by rememberSaveable { mutableStateOf(batteryPromptNeeded) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        pendingNotificationPermission = false
    }

    LaunchedEffect(pendingNotificationPermission) {
        if (pendingNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LaunchedEffect(profiles, activeProfileId) {
        val ids = profiles.map { it.id }
        val current = activeProfileId
        when {
            ids.isEmpty() && current != null -> {
                activeProfileId = null
                UserSettingsManager.setActiveProfileId(context, null)
            }
            ids.isNotEmpty() && (current == null || current !in ids) -> {
                val fallbackId = ids.first()
                activeProfileId = fallbackId
                UserSettingsManager.setActiveProfileId(context, fallbackId)
            }
        }
    }

    val activeProfile = profiles.firstOrNull { it.id == activeProfileId } ?: profiles.firstOrNull()
    val effectiveAuthKey = activeProfile?.authKey?.ifBlank { BuildConfig.DEFAULT_AUTH_KEY } ?: BuildConfig.DEFAULT_AUTH_KEY
    val effectiveUid = activeProfile?.uid?.ifBlank { BuildConfig.DEFAULT_UID } ?: BuildConfig.DEFAULT_UID
    val effectiveRegion = activeProfile?.region?.ifBlank { BuildConfig.DEFAULT_REGION } ?: BuildConfig.DEFAULT_REGION
    val hasValidCredentials = effectiveAuthKey.isNotBlank() && effectiveUid.isNotBlank() && effectiveRegion.isNotBlank()

    val refreshAction: () -> Unit = {
        if (!hasValidCredentials) {
            Toast.makeText(
                context,
                R.string.message_missing_settings,
                Toast.LENGTH_SHORT
            ).show()
        } else {
            viewModel.refresh(
                authKey = effectiveAuthKey,
                uid = effectiveUid,
                region = effectiveRegion
            )
        }
    }

    LaunchedEffect(effectiveAuthKey, effectiveUid, effectiveRegion) {
        if (hasValidCredentials) {
            viewModel.refresh(
                authKey = effectiveAuthKey,
                uid = effectiveUid,
                region = effectiveRegion
            )
        }
    }

    if (uiState is MainUiState.Success) {
        val successState = uiState as MainUiState.Success
        LaunchedEffect(successState.rawPayload, successState.fetchedAtMillis) {
            val result = ProfileFetchResult(
                profile = successState.profile,
                rawPayload = successState.rawPayload,
                fetchedAtMillis = successState.fetchedAtMillis
            )
            ProfileResultHandler.handle(context, result)
            cachedRawPayload = successState.rawPayload
            BackgroundRefreshScheduler.scheduleNext(context)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.app_name),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface
                ),
                actions = {
                    val successState = uiState as? MainUiState.Success
                    BadgedBox(
                        badge = {
                            if (waveplateThresholds.isNotEmpty()) {
                                Badge {
                                    val badgeLabel = if (waveplateThresholds.size == 1) {
                                        waveplateThresholds.first().toString()
                                    } else {
                                        waveplateThresholds.size.toString()
                                    }
                                    Text(
                                        text = badgeLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    ) {
                        IconButton(onClick = { showThresholdDialog = true }) {
                            val contentDescription = when (waveplateThresholds.size) {
                                0 -> stringResource(
                                    id = R.string.label_waveplate_threshold_disabled,
                                    waveplateName
                                )
                                1 -> stringResource(
                                    id = R.string.label_waveplate_threshold_with_value,
                                    waveplateName,
                                    waveplateThresholds.first()
                                )
                                else -> stringResource(
                                    id = R.string.label_waveplate_threshold_with_multiple,
                                    waveplateName,
                                    waveplateThresholds.joinToString(", ")
                                )
                            }
                            Icon(
                                imageVector = Icons.Filled.Notifications,
                                contentDescription = contentDescription
                            )
                        }
                    }
                    IconButton(
                        onClick = {
                            val uri = Uri.parse("https://github.com/dlwlsdn3642/WuwaTracking")
                            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            runCatching { context.startActivity(intent) }
                                .onFailure {
                                    Toast.makeText(
                                        context,
                                        R.string.message_open_github_failed,
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                        }
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_github),
                            contentDescription = stringResource(id = R.string.label_github)
                        )
                    }
                    Box {
                        IconButton(onClick = { optionsExpanded = true }) {
                            Icon(
                                imageVector = Icons.Filled.MoreVert,
                                contentDescription = stringResource(id = R.string.label_options)
                            )
                        }
                        DropdownMenu(
                            expanded = optionsExpanded,
                            onDismissRequest = { optionsExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(id = R.string.label_profile_settings)) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Filled.Settings,
                                        contentDescription = null
                                    )
                                },
                                onClick = {
                                    optionsExpanded = false
                                    showSettingsDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(id = R.string.label_language_settings)) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Filled.Settings,
                                        contentDescription = null
                                    )
                                },
                                onClick = {
                                    optionsExpanded = false
                                    showLanguageDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(id = R.string.label_test_notification)) },
                                leadingIcon = {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_notification_waveplate),
                                        contentDescription = null
                                    )
                                },
                                enabled = successState != null,
                                onClick = {
                                    optionsExpanded = false
                                    val profile = successState?.profile
                                    if (profile != null) {
                                        NotificationHelper.notifyWaveplatesFull(context, profile)
                                    } else {
                                        Toast.makeText(context, R.string.message_test_notification_unavailable, Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val state = uiState) {
                is MainUiState.Loading -> LoadingState()
                is MainUiState.Error -> ErrorState(
                    message = state.message,
                    onRetry = refreshAction
                )
                is MainUiState.Empty -> EmptyState(message = state.message)
                is MainUiState.Success -> ProfileContent(
                    profile = state.profile,
                    onRefresh = refreshAction
                )
            }
        }
    }

    if (showSettingsDialog) {
        SettingsDialog(
            profiles = profiles,
            activeProfileId = activeProfileId,
            rawPayload = cachedRawPayload,
            onViewRawJson = {
                val payload = cachedRawPayload
                if (payload.isNullOrEmpty()) {
                    Toast.makeText(context, R.string.message_raw_json_unavailable, Toast.LENGTH_SHORT).show()
                } else {
                    rawJsonDialogContent = payload
                    showRawJsonDialog = true
                }
            },
            onDismiss = { showSettingsDialog = false },
            onSave = { updatedProfiles, selectedActiveId ->
                val previousProfiles = profiles
                val previousActiveId = activeProfileId
                val sanitizedProfiles = updatedProfiles.map { profile ->
                    profile.copy(
                        uid = profile.uid.trim(),
                        region = profile.region.trim(),
                        authKey = profile.authKey.trim()
                    )
                }
                val metadata = sanitizedProfiles.map { profile ->
                    UserSettingsManager.StoredProfile(
                        id = profile.id,
                        uid = profile.uid,
                        region = profile.region
                    )
                }
                val previousIds = previousProfiles.map { it.id }.toSet()
                val newIds = sanitizedProfiles.map { it.id }.toSet()

                UserSettingsManager.replaceProfiles(context, metadata)
                newIds.forEach { id ->
                    val authKey = sanitizedProfiles.first { it.id == id }.authKey
                    AuthKeyManager.saveAuthKeyForProfile(context, id, authKey)
                }
                (previousIds - newIds).forEach { id ->
                    AuthKeyManager.removeAuthKeyForProfile(context, id)
                }

                val resolvedActiveId = when {
                    selectedActiveId != null && newIds.contains(selectedActiveId) -> selectedActiveId
                    activeProfileId != null && newIds.contains(activeProfileId) -> activeProfileId
                    else -> sanitizedProfiles.firstOrNull()?.id
                }
                UserSettingsManager.setActiveProfileId(context, resolvedActiveId)
                profiles = sanitizedProfiles
                activeProfileId = resolvedActiveId

                if (resolvedActiveId != previousActiveId) {
                    ProfileCacheManager.clear(context)
                    cachedRawPayload = null
                } else {
                    cachedRawPayload = ProfileCacheManager.getPayload(context)?.rawPayload
                }

                BackgroundRefreshScheduler.scheduleNext(context)
                Toast.makeText(context, R.string.message_settings_saved, Toast.LENGTH_SHORT).show()
                showSettingsDialog = false
            }
        )
    }

    if (showLanguageDialog) {
        LanguageSettingsDialog(
            currentLanguage = selectedLanguage,
            onSelect = { language ->
                selectedLanguage = language
                AppPreferencesManager.setSelectedLanguage(context, language)
                Toast.makeText(context, R.string.message_language_saved, Toast.LENGTH_SHORT).show()
                showLanguageDialog = false
            },
            onDismiss = { showLanguageDialog = false }
        )
    }

    if (showBatteryPrompt) {
        BatteryOptimizationDialog(
            onMove = {
                AppPreferencesManager.markBatteryNoticeAcknowledged(context)
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                runCatching { context.startActivity(intent) }
                showBatteryPrompt = false
            },
            onDismiss = {
                AppPreferencesManager.markBatteryNoticeAcknowledged(context)
                showBatteryPrompt = false
            }
        )
    }

    if (showThresholdDialog) {
        WaveplateThresholdDialog(
            currentThresholds = waveplateThresholds,
            onDismiss = { showThresholdDialog = false },
            onSave = { newThresholds ->
                waveplateThresholds = newThresholds
                NotificationSettingsManager.saveWaveplateThresholds(context, newThresholds)
                NotificationSettingsManager.clearThresholdAlert(context)
                BackgroundRefreshScheduler.scheduleNext(context)
                showThresholdDialog = false

                val messageRes = if (newThresholds.isEmpty()) {
                    R.string.message_waveplate_threshold_cleared
                } else {
                    R.string.message_waveplate_threshold_saved
                }
                Toast.makeText(
                    context,
                    context.getString(messageRes, waveplateName),
                    Toast.LENGTH_SHORT
                ).show()

                if (newThresholds.isNotEmpty() &&
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    !NotificationHelper.canPostNotifications(context)
                ) {
                    pendingNotificationPermission = true
                }
            }
        )
    }

    if (showRawJsonDialog && rawJsonDialogContent != null) {
        RawJsonDialog(
            rawJson = rawJsonDialogContent!!,
            onDismiss = {
                showRawJsonDialog = false
                rawJsonDialogContent = null
            }
        )
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsDialog(
    profiles: List<ProfileUiModel>,
    activeProfileId: String?,
    rawPayload: String?,
    onViewRawJson: () -> Unit,
    onDismiss: () -> Unit,
    onSave: (List<ProfileUiModel>, String?) -> Unit
) {
    val regionOptions = SUPPORTED_REGIONS
    val profileStates = remember(profiles) {
        val initialStates = if (profiles.isEmpty()) {
            listOf(
                ProfileEditorState(
                    id = UserSettingsManager.generateProfileId(),
                    uid = "",
                    region = "",
                    authKey = ""
                )
            )
        } else {
            profiles.map { it.toEditorState() }
        }
        mutableStateListOf<ProfileEditorState>().apply { addAll(initialStates) }
    }
    var selectedProfileId by rememberSaveable(activeProfileId, profiles) {
        mutableStateOf(
            activeProfileId ?: profileStates.firstOrNull()?.id
        )
    }
    val regionDropdownStates = remember { mutableStateMapOf<String, Boolean>() }
    val scrollState = rememberScrollState()

    LaunchedEffect(profileStates.size) {
        if (profileStates.none { it.id == selectedProfileId }) {
            selectedProfileId = profileStates.firstOrNull()?.id
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(id = R.string.label_profile_settings)) },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.info_multi_profile_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                profileStates.forEach { state ->
                    val isActive = state.id == selectedProfileId
                    ProfileEditorCard(
                        profile = state,
                        isActive = isActive,
                        canRemove = profileStates.size > 1,
                        onActiveChange = { selectedProfileId = state.id },
                        onUpdate = { updated ->
                            val index = profileStates.indexOfFirst { it.id == updated.id }
                            if (index >= 0) {
                                profileStates[index] = updated
                            }
                        },
                        onRemove = {
                            val removeIndex = profileStates.indexOfFirst { it.id == state.id }
                            if (removeIndex >= 0) {
                                profileStates.removeAt(removeIndex)
                            }
                            regionDropdownStates.remove(state.id)
                            if (selectedProfileId == state.id) {
                                selectedProfileId = profileStates.firstOrNull()?.id
                            }
                        },
                        regionOptions = regionOptions,
                        isRegionMenuExpanded = regionDropdownStates[state.id] ?: false,
                        onRegionMenuExpandedChange = { expanded ->
                            regionDropdownStates[state.id] = expanded
                        }
                    )
                }
                OutlinedButton(
                    onClick = {
                        val newProfile = ProfileEditorState(
                            id = UserSettingsManager.generateProfileId(),
                            uid = "",
                            region = "",
                            authKey = "",
                            authKeyVisible = false
                        )
                        profileStates.add(newProfile)
                        selectedProfileId = newProfile.id
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = stringResource(id = R.string.action_add_profile))
                }
                OutlinedButton(
                    onClick = onViewRawJson,
                    enabled = rawPayload != null,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = stringResource(id = R.string.action_view_raw_json))
                }
                Text(
                    text = if (rawPayload != null) {
                        stringResource(id = R.string.info_raw_json_available)
                    } else {
                        stringResource(id = R.string.message_raw_json_unavailable)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val result = profileStates.map { it.toUiModel() }
                    val resolvedSelection = selectedProfileId?.takeIf { id ->
                        result.any { it.id == id }
                    }
                    onSave(result, resolvedSelection)
                },
                enabled = profileStates.isNotEmpty()
            ) {
                Text(text = stringResource(id = R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(id = R.string.action_cancel))
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileEditorCard(
    profile: ProfileEditorState,
    isActive: Boolean,
    canRemove: Boolean,
    onActiveChange: () -> Unit,
    onUpdate: (ProfileEditorState) -> Unit,
    onRemove: () -> Unit,
    regionOptions: List<String>,
    isRegionMenuExpanded: Boolean,
    onRegionMenuExpandedChange: (Boolean) -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RadioButton(selected = isActive, onClick = onActiveChange)
                    Text(
                        text = profile.uid.takeIf { it.isNotBlank() }
                            ?: stringResource(id = R.string.label_uid),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                IconButton(
                    onClick = onRemove,
                    enabled = canRemove
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(id = R.string.action_remove_profile)
                    )
                }
            }
            OutlinedTextField(
                value = profile.authKey,
                onValueChange = { onUpdate(profile.copy(authKey = it)) },
                label = { Text(stringResource(id = R.string.hint_auth_key)) },
                singleLine = true,
                visualTransformation = if (profile.authKeyVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                trailingIcon = {
                    val iconRes = if (profile.authKeyVisible) {
                        R.drawable.ic_visibility_off
                    } else {
                        R.drawable.ic_visibility
                    }
                    val description = if (profile.authKeyVisible) {
                        stringResource(id = R.string.action_hide_auth_key)
                    } else {
                        stringResource(id = R.string.action_show_auth_key)
                    }
                    IconButton(onClick = {
                        onUpdate(profile.copy(authKeyVisible = !profile.authKeyVisible))
                    }) {
                        Icon(
                            painter = painterResource(id = iconRes),
                            contentDescription = description
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = stringResource(id = R.string.hint_auth_key_info),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = profile.uid,
                onValueChange = { onUpdate(profile.copy(uid = it)) },
                label = { Text(stringResource(id = R.string.hint_uid)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            ExposedDropdownMenuBox(
                expanded = isRegionMenuExpanded,
                onExpandedChange = { onRegionMenuExpandedChange(!isRegionMenuExpanded) }
            ) {
                OutlinedTextField(
                    value = profile.region,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(id = R.string.hint_region)) },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = isRegionMenuExpanded)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(type = MenuAnchorType.PrimaryNotEditable)
                )
                DropdownMenu(
                    expanded = isRegionMenuExpanded,
                    onDismissRequest = { onRegionMenuExpandedChange(false) }
                ) {
                    regionOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                onUpdate(profile.copy(region = option))
                                onRegionMenuExpandedChange(false)
                            }
                        )
                    }
                    if (profile.region.isNotBlank()) {
                        DropdownMenuItem(
                            text = { Text(stringResource(id = R.string.action_clear_selection)) },
                            onClick = {
                                onUpdate(profile.copy(region = ""))
                                onRegionMenuExpandedChange(false)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LanguageSettingsDialog(
    currentLanguage: AppPreferencesManager.AppLanguage,
    onSelect: (AppPreferencesManager.AppLanguage) -> Unit,
    onDismiss: () -> Unit
) {
    val languages = remember { AppPreferencesManager.AppLanguage.values().toList() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(id = R.string.label_language_settings)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(id = R.string.info_language_placeholder),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                languages.forEach { language ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        RadioButton(
                            selected = language == currentLanguage,
                            onClick = {
                                onSelect(language)
                            }
                        )
                        val labelRes = when (language) {
                            AppPreferencesManager.AppLanguage.KOREAN -> R.string.label_language_korean
                            AppPreferencesManager.AppLanguage.ENGLISH -> R.string.label_language_english
                        }
                        Text(
                            text = stringResource(id = labelRes),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(id = R.string.action_close))
            }
        }
    )
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WaveplateThresholdDialog(
    currentThresholds: List<Int>,
    onDismiss: () -> Unit,
    onSave: (List<Int>) -> Unit
) {
    val waveplateLabel = stringResource(id = R.string.proper_waveplates)
    val thresholds = remember { mutableStateListOf<Int>() }
    var thresholdInput by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(currentThresholds) {
        thresholds.clear()
        thresholds.addAll(currentThresholds.sorted())
        thresholdInput = ""
    }

    val numericValue = thresholdInput.toIntOrNull()
    val isDuplicate = numericValue != null && numericValue in thresholds
    val isOutOfRange = numericValue != null && numericValue !in 1..240
    val isError = thresholdInput.isNotBlank() && (isDuplicate || isOutOfRange)
    val canAdd = numericValue != null && numericValue in 1..240 && !isDuplicate
    val displayedThresholds = thresholds.toList().sorted()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(id = R.string.label_waveplate_threshold_settings, waveplateLabel)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(id = R.string.info_waveplate_threshold, waveplateLabel),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(id = R.string.info_waveplate_threshold_range),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (displayedThresholds.isEmpty()) {
                    Text(
                        text = stringResource(id = R.string.message_waveplate_threshold_empty),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        displayedThresholds.forEach { value ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = value.toString(),
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = { thresholds.remove(value) }) {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = stringResource(id = R.string.action_remove_threshold)
                                    )
                                }
                            }
                        }
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = thresholdInput,
                        onValueChange = { newValue ->
                            when {
                                newValue.isEmpty() -> thresholdInput = ""
                                newValue.length <= 3 && newValue.all { it.isDigit() } -> thresholdInput = newValue
                            }
                        },
                        label = { Text(stringResource(id = R.string.hint_waveplate_threshold)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = isError,
                        supportingText = {
                            if (isDuplicate) {
                                Text(
                                    text = stringResource(id = R.string.message_waveplate_threshold_duplicate),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            } else if (isOutOfRange) {
                                Text(
                                    text = stringResource(id = R.string.message_waveplate_threshold_invalid),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = {
                            numericValue?.let { value ->
                                val updated = (thresholds + value).distinct().sorted()
                                thresholds.clear()
                                thresholds.addAll(updated)
                                thresholdInput = ""
                            }
                        },
                        enabled = canAdd
                    ) {
                        Text(text = stringResource(id = R.string.action_add_threshold))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(thresholds.toList().sorted()) }) {
                Text(text = stringResource(id = R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(id = R.string.action_cancel))
            }
        }
    )
}

@Composable
private fun RawJsonDialog(
    rawJson: String,
    onDismiss: () -> Unit
) {
    val prettyJson = remember(rawJson) { prettyPrintJson(rawJson) }
    val scrollState = rememberScrollState()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(id = R.string.label_raw_json)) },
        text = {
            SelectionContainer {
                Column(
                    modifier = Modifier
                        .heightIn(max = 360.dp)
                        .verticalScroll(scrollState)
                ) {
                    Text(
                        text = prettyJson,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(id = R.string.action_close))
            }
        }
    )
}

private fun prettyPrintJson(raw: String): String {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return trimmed
    return runCatching {
        when {
            trimmed.startsWith("{") -> JSONObject(trimmed).toString(2)
            trimmed.startsWith("[") -> JSONArray(trimmed).toString(2)
            else -> trimmed
        }
    }.getOrDefault(trimmed)
}



@Composable
private fun LoadingState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator()
        Spacer(modifier = Modifier.height(12.dp))
        Text(text = stringResource(id = R.string.message_loading))
    }
}

@Composable
private fun ErrorState(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = message.ifBlank { stringResource(id = R.string.message_generic_error) },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry) {
            Text(text = stringResource(id = R.string.label_retry))
        }
    }
}

@Composable
private fun EmptyState(message: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = message.ifBlank { stringResource(id = R.string.message_empty_placeholder) },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
private fun ProfileContent(
    profile: WuwaProfile,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            ProfileHeroCard(profile = profile, onRefresh = onRefresh)
        }
        item {
            ResourceSection(profile = profile)
        }
    }
}

@Composable
private fun ProfileHeroCard(
    profile: WuwaProfile,
    onRefresh: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = profile.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "${stringResource(id = R.string.label_uid)}: ${profile.uid}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${stringResource(id = R.string.label_resonance_level)}: ${profile.resonanceLevel}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondaryContainer,
                tonalElevation = 4.dp
            ) {
                IconButton(onClick = onRefresh) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_refresh),
                        contentDescription = stringResource(id = R.string.label_refresh),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun ResourceSection(profile: WuwaProfile) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = stringResource(id = R.string.label_resources),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(16.dp))

            val items = listOf(
                ResourceItem(
                    title = stringResource(id = R.string.label_waveplates),
                    value = profile.waveplatesCurrent,
                    maxValue = profile.waveplatesMax.takeIf { it > 0 },
                    iconRes = R.drawable.waveplates
                ),
                ResourceItem(
                    title = stringResource(id = R.string.label_wavesubstance),
                    value = profile.wavesubstance,
                    maxValue = null,
                    iconRes = R.drawable.wavesubstance
                ),
                ResourceItem(
                    title = stringResource(id = R.string.label_activity_points),
                    value = profile.activityPointsCurrent,
                    maxValue = profile.activityPointsMax.takeIf { it > 0 },
                    iconRes = R.drawable.activity_points
                ),
                ResourceItem(
                    title = stringResource(id = R.string.label_podcast),
                    value = profile.podcastCurrent,
                    maxValue = profile.podcastMax.takeIf { it > 0 },
                    iconRes = R.drawable.podcast
                )
            )

            val accentPalette = listOf(
                MaterialTheme.colorScheme.primary,
                MaterialTheme.colorScheme.secondary,
                MaterialTheme.colorScheme.tertiary,
                MaterialTheme.colorScheme.inversePrimary
            )

            items.forEachIndexed { index, item ->
                ResourceCard(
                    item = item,
                    accentColor = accentPalette[index % accentPalette.size],
                    modifier = Modifier.fillMaxWidth()
                )
                if (index < items.lastIndex) {
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
private fun ResourceCard(
    item: ResourceItem,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val clampedValue = max(item.value, 0)
    val maxValue = item.maxValue?.takeIf { it > 0 }
    val progress = maxValue?.let { (clampedValue.toFloat() / it).coerceIn(0f, 1f) } ?: 0f

    ElevatedCard(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(id = item.iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (maxValue != null) {
                            stringResource(id = R.string.format_resource_progress, clampedValue, maxValue)
                        } else {
                            stringResource(id = R.string.format_resource_progress_single, clampedValue)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(accentColor.copy(alpha = 0.16f))
            ) {
                if (progress > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progress)
                            .background(accentColor)
                    )
                }
            }
        }
    }
}

private data class ResourceItem(
    val title: String,
    val value: Int,
    val maxValue: Int?,
    val iconRes: Int
)

@Preview(showBackground = true)
@Composable
private fun ProfilePreview() {
    ProfileContent(
        profile = WuwaProfile(
            name = "Lan Seifang",
            uid = "1000000000",
            resonanceLevel = 80,
            waveplatesCurrent = 120,
            waveplatesMax = 240,
            wavesubstance = 180,
            activityPointsCurrent = 210,
            activityPointsMax = 240,
            podcastCurrent = 45,
            podcastMax = 90
        ),
        onRefresh = {}
    )
}


@Composable
private fun BatteryOptimizationDialog(
    onMove: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(id = R.string.label_battery_settings)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(id = R.string.message_battery_optimization_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(id = R.string.message_battery_optimization_steps),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onMove) {
                Text(text = stringResource(id = R.string.action_move))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(id = R.string.action_close))
            }
        }
    )
}



