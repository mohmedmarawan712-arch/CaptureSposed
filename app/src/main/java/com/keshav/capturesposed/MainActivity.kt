package com.keshav.capturesposed

import android.app.Activity.ScreenCaptureCallback
import android.os.Build
import android.os.Bundle
import android.view.WindowManager.SCREEN_RECORDING_STATE_VISIBLE
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getString
import androidx.core.graphics.drawable.toBitmap
import com.keshav.capturesposed.ui.theme.APPTheme
import com.keshav.capturesposed.utils.PrefsUtils
import com.keshav.capturesposed.utils.SuUtils
import com.keshav.capturesposed.utils.XposedChecker
import java.util.function.Consumer

class MainActivity : ComponentActivity() {

    private var screenshotCounter = mutableIntStateOf(0)
    private var screenRecordingActive = mutableStateOf("")
    private lateinit var isScreenshotSwitchOn: MutableState<Boolean>
    private lateinit var isScreenRecordSwitchOn: MutableState<Boolean>
    private lateinit var isFlagSecureSwitchOn: MutableState<Boolean>

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        setTheme(R.style.Theme_APP)
        super.onCreate(savedInstanceState)
        screenshotCounter.intValue = savedInstanceState?.getInt("counter") ?: 0
        PrefsUtils.loadPrefs()
        isScreenshotSwitchOn = mutableStateOf(PrefsUtils.isScreenshotHookOn())
        isScreenRecordSwitchOn = mutableStateOf(PrefsUtils.isScreenRecordHookOn())
        isFlagSecureSwitchOn = mutableStateOf(PrefsUtils.isFlagSecureHookOn())
        PrefsUtils.getScreenshotHookActiveAsLiveData().observe(this) { isActive ->
            isActive?.let {
                isScreenshotSwitchOn.value = it
            }
        }
        PrefsUtils.getScreenRecordHookActiveAsLiveData().observe(this) { isActive ->
            isActive?.let {
                isScreenRecordSwitchOn.value = it
            }
        }
        PrefsUtils.getFlagSecureHookActiveAsLiveData().observe(this) { isActive ->
            isActive?.let {
                isFlagSecureSwitchOn.value = it
            }
        }

        setContent {
            APPTheme {
                AppUI()
            }
        }
    }

    private val screenCaptureCallback = ScreenCaptureCallback {
        screenshotCounter.intValue++
    }

    private val screenRecordCallback = Consumer<Int> { state ->
        if (state == SCREEN_RECORDING_STATE_VISIBLE) {
            screenRecordingActive.value = "YES"
        }
        else {
            screenRecordingActive.value = "NO"
        }
    }

    override fun onStart() {
        super.onStart()
        registerScreenCaptureCallback(mainExecutor, screenCaptureCallback)

        // If Android version is 15 or newer, add screen record callback.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            screenRecordCallback.accept(
                windowManager.addScreenRecordingCallback(mainExecutor, screenRecordCallback))
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("counter", screenshotCounter.intValue)
    }

    override fun onStop() {
        super.onStop()
        unregisterScreenCaptureCallback(screenCaptureCallback)

        // If Android version is 15 or newer, remove screen record callback.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            windowManager.removeScreenRecordingCallback(screenRecordCallback)
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun AppUI(modifier: Modifier = Modifier) {
        val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                LargeTopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                bitmap = loadXmlDrawable(R.drawable.ic_launcher_round)!!,
                                contentDescription = stringResource(R.string.xposed_name)
                            )
                            Spacer(Modifier.padding(horizontal = 5.dp))
                            Text(
                                text = stringResource(R.string.xposed_name),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    },
                    colors = TopAppBarDefaults.largeTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        scrolledContainerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground
                    ),
                    scrollBehavior = scrollBehavior
                )
            },
            modifier = modifier
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.displayCutout)
        ) { p ->
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(p)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                horizontalAlignment = Alignment.Start
            ) {
                MainCard()
                TestCard()
            }
        }
    }

    @Composable
    fun MainCard() {
        OutlinedCard(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.outlinedCardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    if (XposedChecker.isEnabled()) {
                        Icon(painterResource(R.drawable.checklist_24), getString(R.string.status))
                    } else {
                        Icon(painterResource(R.drawable.error_24), getString(R.string.error))
                    }
                    Text(
                        text = getString(R.string.status_title),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                if (!SuUtils.isRootAvailable()) {
                    Text(
                        text = getString(LocalContext.current, R.string.no_root_access),
                        fontSize = 16.sp,
                        textAlign = TextAlign.Start,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                } else if (!XposedChecker.isEnabled()) {
                    Text(
                        text = getString(LocalContext.current, R.string.module_disabled),
                        fontSize = 16.sp,
                        textAlign = TextAlign.Start,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        // 1. Disable Secure Flag (First item as requested)
                        SettingToggleRow(
                            title = getString(R.string.flag_secure_title),
                            subtitle = getString(R.string.flag_secure_subtitle),
                            checked = isFlagSecureSwitchOn.value,
                            onCheckedChange = { PrefsUtils.toggleFlagSecureHookState() }
                        )

                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            thickness = 1.dp
                        )

                        // 2. Block Screenshot Detection
                        SettingToggleRow(
                            title = getString(R.string.screenshot_detection_title),
                            subtitle = if (isScreenshotSwitchOn.value) {
                                getString(R.string.screenshot_status_blocked)
                            } else {
                                getString(R.string.screenshot_status_allowed)
                            },
                            checked = isScreenshotSwitchOn.value,
                            onCheckedChange = { PrefsUtils.toggleScreenshotHookState() }
                        )

                        // 3. Block Recording Detection (Android 15+)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                thickness = 1.dp
                            )

                            SettingToggleRow(
                                title = getString(R.string.screen_record_detection_title),
                                subtitle = if (isScreenRecordSwitchOn.value) {
                                    getString(R.string.screen_record_status_blocked)
                                } else {
                                    getString(R.string.screen_record_status_allowed)
                                },
                                checked = isScreenRecordSwitchOn.value,
                                onCheckedChange = { PrefsUtils.toggleScreenRecordHookState() }
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun SettingToggleRow(
        title: String,
        subtitle: String,
        checked: Boolean,
        onCheckedChange: () -> Unit
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 12.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Start
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 17.sp,
                    textAlign = TextAlign.Start
                )
            }

            Switch(
                checked = checked,
                onCheckedChange = { onCheckedChange() }
            )
        }
    }

    @Composable
    fun TestCard() {
        OutlinedCard(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.outlinedCardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Icon(painterResource(R.drawable.test_tube_24), getString(R.string.card_title_testing))
                    Text(
                        text = getString(R.string.card_title_testing),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Screenshot Counter",
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${screenshotCounter.intValue}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            thickness = 1.dp
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Recording in Progress",
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = screenRecordingActive.value.ifEmpty { "NO" },
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (screenRecordingActive.value == "YES") {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.primary
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    /*
        loadXmlDrawable() was sourced from
        https://slack-chats.kotlinlang.org/t/506477/hello-i-am-trying-to-load-a-layer-list-drawable-with-this-co#707c4aef-021c-421b-b873-ea7ca453b61e
     */
    @Composable
    fun loadXmlDrawable(@DrawableRes resId: Int): ImageBitmap? =
        ContextCompat.getDrawable(
            LocalContext.current,
            resId
        )?.toBitmap()?.asImageBitmap()
}