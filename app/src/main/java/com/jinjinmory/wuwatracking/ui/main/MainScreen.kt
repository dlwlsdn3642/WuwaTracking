package com.jinjinmory.wuwatracking.ui.main

import android.Manifest
import android.os.Build
import android.content.Intent
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
    var showThresholdDialog by rememberSaveable { mutableStateOf(false) }
    var showRawJsonDialog by rememberSaveable { mutableStateOf(false) }

    var hasStoredAuthKey by remember { mutableStateOf(!AuthKeyManager.getAuthKey(context).isNullOrEmpty()) }
    var settingsAuthKey by remember { mutableStateOf(AuthKeyManager.getAuthKey(context).orEmpty()) }
    var settingsUid by remember { mutableStateOf(UserSettingsManager.getUid(context).orEmpty()) }
    var settingsRegion by remember { mutableStateOf(UserSettingsManager.getRegion(context).orEmpty()) }
    var waveplateThreshold by remember { mutableStateOf(NotificationSettingsManager.getWaveplateThreshold(context)) }
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

    val refreshAction: () -> Unit = {
        val effectiveAuthKey = settingsAuthKey.ifBlank { BuildConfig.DEFAULT_AUTH_KEY }
        val effectiveUid = settingsUid.ifBlank { BuildConfig.DEFAULT_UID }
        val effectiveRegion = settingsRegion.ifBlank { BuildConfig.DEFAULT_REGION }

        if (effectiveAuthKey.isBlank() || effectiveUid.isBlank() || effectiveRegion.isBlank()) {
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

    LaunchedEffect(settingsAuthKey, settingsUid, settingsRegion) {
        val effectiveAuthKey = settingsAuthKey.ifBlank { BuildConfig.DEFAULT_AUTH_KEY }
        val effectiveUid = settingsUid.ifBlank { BuildConfig.DEFAULT_UID }
        val effectiveRegion = settingsRegion.ifBlank { BuildConfig.DEFAULT_REGION }
        if (effectiveAuthKey.isNotBlank() && effectiveUid.isNotBlank() && effectiveRegion.isNotBlank()) {
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
                            waveplateThreshold?.let {
                                Badge {
                                    Text(
                                        text = it.toString(),
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    ) {
                        IconButton(onClick = { showThresholdDialog = true }) {
                            val contentDescription = waveplateThreshold?.let {
                                stringResource(id = R.string.label_waveplate_threshold_with_value, it)
                            } ?: stringResource(id = R.string.label_waveplate_threshold_disabled)
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
            initialAuthKey = settingsAuthKey,
            initialUid = settingsUid,
            initialRegion = settingsRegion,
            hasExistingAuthKey = hasStoredAuthKey,
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
            onSave = { authKey, uid, region ->
                val sanitizedKey = authKey.trim()
                val sanitizedUid = uid.trim()
                val sanitizedRegion = region.trim()

                val previousKey = settingsAuthKey

                AuthKeyManager.saveAuthKey(context, sanitizedKey)
                hasStoredAuthKey = sanitizedKey.isNotEmpty()
                settingsAuthKey = sanitizedKey

                UserSettingsManager.saveUid(context, sanitizedUid)
                settingsUid = sanitizedUid

                UserSettingsManager.saveRegion(context, sanitizedRegion)
                settingsRegion = sanitizedRegion

                Toast.makeText(context, R.string.message_settings_saved, Toast.LENGTH_SHORT).show()

                if (sanitizedKey != previousKey) {
                    val toastMessageRes = if (sanitizedKey.isEmpty()) {
                        R.string.message_auth_key_cleared
                    } else {
                        R.string.message_auth_key_saved
                    }
                    Toast.makeText(context, toastMessageRes, Toast.LENGTH_SHORT).show()
                }

                showSettingsDialog = false
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
        WaveplateThresholdDialog(
            currentThreshold = waveplateThreshold,
            onDismiss = { showThresholdDialog = false },
            onSave = { newThreshold ->
                waveplateThreshold = newThreshold
                NotificationSettingsManager.saveWaveplateThreshold(context, newThreshold)
                NotificationSettingsManager.clearThresholdAlert(context)
                BackgroundRefreshScheduler.scheduleNext(context)
                showThresholdDialog = false

                val messageRes = if (newThreshold == null) {
                    R.string.message_waveplate_threshold_cleared
                } else {
                    R.string.message_waveplate_threshold_saved
                }
                Toast.makeText(context, messageRes, Toast.LENGTH_SHORT).show()

                if (newThreshold != null &&
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


@Composable
private fun SettingsDialog(
    initialAuthKey: String,
    initialUid: String,
    initialRegion: String,
    hasExistingAuthKey: Boolean,
    rawPayload: String?,
    onViewRawJson: () -> Unit,
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit
) {
    var authKey by rememberSaveable(initialAuthKey) { mutableStateOf(initialAuthKey) }
    var uid by rememberSaveable(initialUid) { mutableStateOf(initialUid) }
    var region by rememberSaveable(initialRegion) { mutableStateOf(initialRegion) }
    var authKeyVisible by rememberSaveable { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(id = R.string.label_profile_settings)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (hasExistingAuthKey) {
                    Text(
                        text = stringResource(id = R.string.info_auth_key_existing),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                OutlinedTextField(
                    value = authKey,
                    onValueChange = { authKey = it },
                    label = { Text(stringResource(id = R.string.hint_auth_key)) },
                    singleLine = true,
                    visualTransformation = if (authKeyVisible) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    trailingIcon = {
                        val iconRes = if (authKeyVisible) {
                            R.drawable.ic_visibility_off
                        } else {
                            R.drawable.ic_visibility
                        }
                        val description = if (authKeyVisible) {
                            stringResource(id = R.string.action_hide_auth_key)
                        } else {
                            stringResource(id = R.string.action_show_auth_key)
                        }
                        IconButton(onClick = { authKeyVisible = !authKeyVisible }) {
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
                    value = uid,
                    onValueChange = { uid = it },
                    label = { Text(stringResource(id = R.string.hint_uid)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = region,
                    onValueChange = { region = it },
                    label = { Text(stringResource(id = R.string.hint_region)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
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
            TextButton(onClick = { onSave(authKey, uid, region) }) {
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
private fun WaveplateThresholdDialog(
    currentThreshold: Int?,
    onDismiss: () -> Unit,
    onSave: (Int?) -> Unit
) {
    var thresholdInput by rememberSaveable(currentThreshold) { mutableStateOf(currentThreshold?.toString().orEmpty()) }
    val numericValue = thresholdInput.toIntOrNull()
    val isValid = thresholdInput.isBlank() || (numericValue != null && numericValue in 1..240)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(id = R.string.label_waveplate_threshold_settings)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(id = R.string.info_waveplate_threshold),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
                    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                    isError = !isValid,
                    supportingText = {
                        val textRes = if (isValid) {
                            R.string.info_waveplate_threshold_range
                        } else {
                            R.string.message_waveplate_threshold_invalid
                        }
                        val color = if (isValid) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.error
                        }
                        Text(
                            text = stringResource(id = textRes),
                            style = MaterialTheme.typography.bodySmall,
                            color = color
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = isValid,
                onClick = {
                    val value = if (thresholdInput.isBlank()) null else thresholdInput.toIntOrNull()
                    onSave(value)
                }
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



