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
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.material3.Checkbox
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
import com.jinjinmory.wuwatracking.background.ActivityReminderScheduler
import com.jinjinmory.wuwatracking.data.preferences.NotificationSettingsManager
import com.jinjinmory.wuwatracking.data.preferences.AppPreferencesManager
import com.jinjinmory.wuwatracking.data.preferences.ProfileCacheManager
import com.jinjinmory.wuwatracking.data.preferences.UserSettingsManager
import com.jinjinmory.wuwatracking.data.remote.dto.WuwaProfile
import com.jinjinmory.wuwatracking.data.repository.ProfileFetchResult
import com.jinjinmory.wuwatracking.data.security.AuthKeyManager
import com.jinjinmory.wuwatracking.domain.ProfileResultHandler
import com.jinjinmory.wuwatracking.domain.AlertResource
import com.jinjinmory.wuwatracking.notifications.NotificationHelper
import com.jinjinmory.wuwatracking.util.BatteryOptimizationHelper
import kotlin.math.max
import kotlin.math.min
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

    var optionsExpanded by remember { mutableStateOf(false) }
    var showSettingsDialog by rememberSaveable { mutableStateOf(false) }
    var showLanguageDialog by rememberSaveable { mutableStateOf(false) }
    var showThresholdDialog by rememberSaveable { mutableStateOf(false) }
    var showWidgetSettingsDialog by rememberSaveable { mutableStateOf(false) }
    var showRawJsonDialog by rememberSaveable { mutableStateOf(false) }

    var profiles by remember { mutableStateOf(loadProfiles(context)) }
    var activeProfileId by remember { mutableStateOf(UserSettingsManager.getActiveProfileId(context)) }
    var selectedLanguage by remember { mutableStateOf(AppPreferencesManager.getSelectedLanguage(context)) }
    val resourceThresholds = remember {
        mutableStateMapOf<AlertResource, List<Int>>().apply {
            AlertResource.entries.forEach { resource ->
                put(resource, NotificationSettingsManager.getThresholds(context, resource))
            }
        }
    }
    var activityReminderConfig by remember { mutableStateOf(AppPreferencesManager.getActivityReminder(context)) }
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

    LaunchedEffect(activityReminderConfig) {
        ActivityReminderScheduler.schedule(context, activityReminderConfig)
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
                    val totalThresholdCount = resourceThresholds.values.sumOf { it.size }
                    BadgedBox(
                        badge = {
                            if (totalThresholdCount > 0) {
                                Badge {
                                    val badgeLabel = min(totalThresholdCount, 99).toString()
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
                            Icon(
                                imageVector = Icons.Filled.Notifications,
                                contentDescription = stringResource(id = R.string.label_alert_settings)
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
                                text = { Text(stringResource(id = R.string.label_widget_settings)) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Filled.Settings,
                                        contentDescription = null
                                    )
                                },
                                onClick = {
                                    optionsExpanded = false
                                    showWidgetSettingsDialog = true
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

    if (showWidgetSettingsDialog) {
        WidgetSettingsDialog(
            currentFormat = AppPreferencesManager.getWidgetTimeFormat(context),
            onDismiss = { showWidgetSettingsDialog = false },
            onSelect = { format ->
                AppPreferencesManager.setWidgetTimeFormat(context, format)
                Toast.makeText(context, R.string.message_widget_settings_saved, Toast.LENGTH_SHORT).show()
                showWidgetSettingsDialog = false
                com.jinjinmory.wuwatracking.widget.WuwaWidgetProvider.updateAll(context)
                com.jinjinmory.wuwatracking.widget.WuwaMiniWidgetProvider.updateAll(context)
            }
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
        AlertSettingsDialog(
            thresholds = resourceThresholds.toMap(),
            activityReminder = activityReminderConfig,
            onDismiss = { showThresholdDialog = false },
            onSave = { newThresholds, newReminder ->
                AlertResource.entries.forEach { resource ->
                    val updated = newThresholds[resource].orEmpty()
                    resourceThresholds[resource] = updated
                    NotificationSettingsManager.saveThresholds(context, resource, updated)
                    NotificationSettingsManager.clearThresholdAlert(context, resource)
                }
                activityReminderConfig = newReminder
                AppPreferencesManager.setActivityReminder(context, newReminder)
                ActivityReminderScheduler.schedule(context, newReminder)
                BackgroundRefreshScheduler.scheduleNext(context)
                showThresholdDialog = false

                val alertMessage = context.getString(R.string.message_alert_settings_saved)
                Toast.makeText(context, alertMessage, Toast.LENGTH_SHORT).show()
                if (newReminder.enabled && newReminder.hour != null && newReminder.minute != null) {
                    Toast.makeText(
                        context,
                        R.string.message_activity_reminder_saved,
                        Toast.LENGTH_SHORT
                    ).show()
                }

                val hasAnyThresholds = newThresholds.values.any { it.isNotEmpty() }
                if (hasAnyThresholds &&
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

@Composable
private fun WidgetSettingsDialog(
    currentFormat: AppPreferencesManager.WidgetTimeFormat,
    onSelect: (AppPreferencesManager.WidgetTimeFormat) -> Unit,
    onDismiss: () -> Unit
) {
    val formats = remember { AppPreferencesManager.WidgetTimeFormat.values().toList() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(id = R.string.label_widget_settings)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(id = R.string.label_widget_time_format),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                formats.forEach { format ->
                    val labelRes = when (format) {
                        AppPreferencesManager.WidgetTimeFormat.MINUTES -> R.string.label_widget_time_minutes
                        AppPreferencesManager.WidgetTimeFormat.HOURS_MINUTES -> R.string.label_widget_time_hours_minutes
                        AppPreferencesManager.WidgetTimeFormat.ETA -> R.string.label_widget_time_eta
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        RadioButton(
                            selected = format == currentFormat,
                            onClick = { onSelect(format) }
                        )
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun AlertSettingsDialog(
    thresholds: Map<AlertResource, List<Int>>,
    activityReminder: AppPreferencesManager.ActivityReminderConfig,
    onDismiss: () -> Unit,
    onSave: (Map<AlertResource, List<Int>>, AppPreferencesManager.ActivityReminderConfig) -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { AlertResource.entries.size })
    val thresholdState = remember(thresholds) {
        mutableStateMapOf<AlertResource, List<Int>>().apply {
            AlertResource.entries.forEach { resource ->
                put(resource, thresholds[resource].orEmpty().sorted())
            }
        }
    }
    val thresholdInputs = remember { mutableStateMapOf<AlertResource, String>() }
    var reminderEnabled by rememberSaveable { mutableStateOf(activityReminder.enabled) }
    var reminderHourInput by rememberSaveable { mutableStateOf(activityReminder.hour?.toString()?.padStart(2, '0') ?: "") }
    var reminderMinuteInput by rememberSaveable { mutableStateOf(activityReminder.minute?.toString()?.padStart(2, '0') ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(id = R.string.label_alert_settings)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(id = R.string.info_alert_swipe),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                ) { page ->
                    val resource = AlertResource.entries[page]
                    val currentThresholds = thresholdState[resource].orEmpty()
                    val thresholdInput = thresholdInputs.getOrPut(resource) { "" }
                    AlertThresholdPage(
                        resource = resource,
                        thresholds = currentThresholds,
                        thresholdInput = thresholdInput,
                        onThresholdInputChange = { thresholdInputs[resource] = it },
                        onThresholdsChange = { updated -> thresholdState[resource] = updated.sorted() },
                        showActivityReminder = resource == AlertResource.ACTIVITY_POINTS,
                        reminderEnabled = reminderEnabled,
                        reminderHourInput = reminderHourInput,
                        reminderMinuteInput = reminderMinuteInput,
                        onReminderEnabledChange = { reminderEnabled = it },
                        onReminderHourChange = { reminderHourInput = it },
                        onReminderMinuteChange = { reminderMinuteInput = it }
                    )
                }
            }
        },
        confirmButton = {
            val reminderHour = reminderHourInput.toIntOrNull()
            val reminderMinute = reminderMinuteInput.toIntOrNull()
            val isReminderTimeValid = reminderHour in 0..23 && reminderMinute in 0..59
            val reminderConfig = if (reminderEnabled && isReminderTimeValid) {
                AppPreferencesManager.ActivityReminderConfig(true, reminderHour, reminderMinute)
            } else {
                AppPreferencesManager.ActivityReminderConfig(false, null, null)
            }
            TextButton(
                onClick = { onSave(thresholdState.toMap(), reminderConfig) },
                enabled = !reminderEnabled || isReminderTimeValid
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

@Composable
private fun AlertThresholdPage(
    resource: AlertResource,
    thresholds: List<Int>,
    thresholdInput: String,
    onThresholdInputChange: (String) -> Unit,
    onThresholdsChange: (List<Int>) -> Unit,
    showActivityReminder: Boolean = false,
    reminderEnabled: Boolean = false,
    reminderHourInput: String = "",
    reminderMinuteInput: String = "",
    onReminderEnabledChange: (Boolean) -> Unit = {},
    onReminderHourChange: (String) -> Unit = {},
    onReminderMinuteChange: (String) -> Unit = {}
) {
    val resourceLabel = stringResource(id = resource.titleRes)
    val displayedThresholds = thresholds.sorted()
    val maxValue = resource.maxInput
    val maxLength = maxValue.toString().length

    val numericValue = thresholdInput.toIntOrNull()
    val isDuplicate = numericValue != null && numericValue in displayedThresholds
    val isOutOfRange = numericValue != null && numericValue !in 1..maxValue
    val isError = thresholdInput.isNotBlank() && (isDuplicate || isOutOfRange)
    val canAdd = numericValue != null && numericValue in 1..maxValue && !isDuplicate
    val reminderHour = reminderHourInput.toIntOrNull()
    val reminderMinute = reminderMinuteInput.toIntOrNull()
    val isReminderTimeValid = reminderHour in 0..23 && reminderMinute in 0..59

    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = resourceLabel,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = stringResource(id = R.string.info_waveplate_threshold, resourceLabel),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = stringResource(id = R.string.info_waveplate_threshold_range, maxValue),
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
                        IconButton(onClick = { onThresholdsChange(displayedThresholds - value) }) {
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
                        newValue.isEmpty() -> onThresholdInputChange("")
                        newValue.length <= maxLength && newValue.all { it.isDigit() } -> onThresholdInputChange(newValue)
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
                            text = stringResource(id = R.string.message_waveplate_threshold_invalid, maxValue),
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
                        val updated = (displayedThresholds + value).distinct().sorted()
                        onThresholdsChange(updated)
                        onThresholdInputChange("")
                    }
                },
                enabled = canAdd
            ) {
                Text(text = stringResource(id = R.string.action_add_threshold))
            }
        }
        if (showActivityReminder) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Checkbox(
                        checked = reminderEnabled,
                        onCheckedChange = { onReminderEnabledChange(it) }
                    )
                    Column {
                        Text(
                            text = stringResource(id = R.string.label_activity_reminder),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = stringResource(id = R.string.info_activity_reminder),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = reminderHourInput,
                        onValueChange = { newValue ->
                            when {
                                newValue.isEmpty() -> onReminderHourChange("")
                                newValue.length <= 2 && newValue.all { it.isDigit() } -> onReminderHourChange(newValue)
                            }
                        },
                        label = { Text(stringResource(id = R.string.hint_hour)) },
                        singleLine = true,
                        enabled = reminderEnabled,
                        isError = reminderEnabled && reminderHour !in 0..23,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = reminderMinuteInput,
                        onValueChange = { newValue ->
                            when {
                                newValue.isEmpty() -> onReminderMinuteChange("")
                                newValue.length <= 2 && newValue.all { it.isDigit() } -> onReminderMinuteChange(newValue)
                            }
                        },
                        label = { Text(stringResource(id = R.string.hint_minute)) },
                        singleLine = true,
                        enabled = reminderEnabled,
                        isError = reminderEnabled && reminderMinute !in 0..59,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
                if (reminderEnabled && !isReminderTimeValid) {
                    Text(
                        text = stringResource(id = R.string.message_activity_reminder_invalid_time),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
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



