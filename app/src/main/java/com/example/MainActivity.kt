package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Newspaper
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.BulletinRepository
import com.example.data.ChurchMemberDetail
import com.example.data.ChurchSchedule
import com.example.data.GalleryPhoto
import com.example.data.MemberExportFormat
import com.example.data.MemberExportUtils
import com.example.data.NewsArticle
import com.example.data.UiState
import com.example.notifications.BulletinNotificationManager
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.signOutUser
import com.example.ui.components.AdminMemberAddDialog
import com.example.ui.components.AdminMemberEditorDialog
import com.example.ui.components.AdminMemberExportDialog
import com.example.ui.components.AdminNewsEditorDialog
import com.example.ui.components.AdminPhotoEditorDialog
import com.example.ui.components.AdminScheduleEditorDialog
import com.example.ui.components.AppUpdateAvailableDialog
import com.example.ui.components.MemberQrScannerDialog
import com.example.ui.components.PushNotificationOptInDialog
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.AppLanguageManager
import com.example.ui.i18n.LocalAppLanguage
import com.example.ui.i18n.tr
import com.example.ui.screens.GalleryScreen
import com.example.ui.screens.MembersScreen
import com.example.ui.screens.NewsScreen
import com.example.ui.screens.ProfileAdminScreen
import com.example.ui.screens.SchedulesScreen
import com.example.ui.theme.BuletinSibTheme
import com.example.viewmodel.BulletinViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

private enum class BulletinTab(
    val route: String,
    val labelMs: String,
    val labelEn: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    NEWS("news", "Berita", "News", Icons.Filled.Newspaper, Icons.Outlined.Newspaper),
    SCHEDULES("schedules", "Jadual", "Schedule", Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth),
    GALLERY("gallery", "Galeri", "Gallery", Icons.Filled.PhotoLibrary, Icons.Outlined.PhotoLibrary),
    MEMBERS("members", "Ahli", "Members", Icons.Filled.Groups, Icons.Outlined.Groups),
    ACCOUNT("account", "Akaun", "Account", Icons.Filled.Person, Icons.Outlined.Person);

    fun label(lang: AppLanguage): String = if (lang == AppLanguage.EN) labelEn else labelMs
}

class MainActivity : ComponentActivity() {
    private var pendingNotificationDeepLink by mutableStateOf<Pair<String, String>?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        AppLanguageManager.init(this)
        BulletinNotificationManager.ensureNotificationChannel(this)
        pendingNotificationDeepLink = extractNotificationDeepLink(intent)

        setContent {
            val currentLanguage by AppLanguageManager.language.collectAsStateWithLifecycle()
            CompositionLocalProvider(LocalAppLanguage provides currentLanguage) {
                BuletinSibTheme {
                    AppContent(
                        pendingDeepLink = pendingNotificationDeepLink,
                        onConsumeDeepLink = { pendingNotificationDeepLink = null }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        extractNotificationDeepLink(intent)?.let { deepLink ->
            pendingNotificationDeepLink = deepLink
        }
    }

    private fun extractNotificationDeepLink(intent: Intent?): Pair<String, String>? {
        val extras = intent?.extras ?: return null
        val type = extras.getString(BulletinNotificationManager.EXTRA_NAV_TYPE)
            ?: extras.getString("eventType")
            ?: extras.getString("type")
            ?: return null
        val itemId = extras.getString(BulletinNotificationManager.EXTRA_NAV_ITEM_ID)
            ?: extras.getString("targetId")
            ?: extras.getString("itemId")
            ?: ""
        return type to itemId
    }
}

@Composable
fun AppContent(
    pendingDeepLink: Pair<String, String>? = null,
    onConsumeDeepLink: () -> Unit = {}
) {
    var currentUser by remember { mutableStateOf<FirebaseUser?>(Firebase.auth.currentUser) }
    var isGuestMode by rememberSaveable { mutableStateOf(false) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
            if (auth.currentUser != null) {
                isGuestMode = false
            }
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose { Firebase.auth.removeAuthStateListener(listener) }
    }

    // If user taps a push notification while signed out, enter Guest read-only view directly
    LaunchedEffect(pendingDeepLink, currentUser) {
        if (pendingDeepLink != null && currentUser == null) {
            isGuestMode = true
        }
    }

    val user = currentUser
    if (user == null && !isGuestMode) {
        AuthScreen(
            onAuthSuccess = {
                currentUser = Firebase.auth.currentUser
                isGuestMode = false
            },
            onContinueAsGuest = {
                isGuestMode = true
            }
        )
    } else {
        // Key by uid (or guest session key) so account switches cleanly tear down & recreate ViewModel/listeners
        val sessionKey = user?.uid ?: "guest_readonly_session"
        key(sessionKey) {
            BuletinMainShell(
                currentUser = user,
                sessionKey = sessionKey,
                pendingDeepLink = pendingDeepLink,
                onConsumeDeepLink = onConsumeDeepLink,
                onGoogleSignInSuccess = {
                    currentUser = Firebase.auth.currentUser
                    isGuestMode = false
                },
                onExitGuestMode = {
                    isGuestMode = false
                },
                onSignedOut = {
                    currentUser = null
                    isGuestMode = false
                }
            )
        }
    }
}

@Composable
private fun BuletinMainShell(
    currentUser: FirebaseUser?,
    sessionKey: String,
    pendingDeepLink: Pair<String, String>?,
    onConsumeDeepLink: () -> Unit,
    onGoogleSignInSuccess: () -> Unit,
    onExitGuestMode: () -> Unit,
    onSignedOut: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val viewModel: BulletinViewModel = viewModel(
        key = sessionKey,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                BulletinViewModel(
                    repository = BulletinRepository(db),
                    currentUserId = currentUser?.uid,
                    userEmail = currentUser?.email,
                    userDisplayName = currentUser?.displayName,
                    emailVerified = currentUser?.isEmailVerified ?: false
                )
            }
        }
    )

    val newsState by viewModel.newsState.collectAsStateWithLifecycle()
    val schedulesState by viewModel.schedulesState.collectAsStateWithLifecycle()
    val galleryState by viewModel.galleryState.collectAsStateWithLifecycle()
    val membersState by viewModel.membersState.collectAsStateWithLifecycle()
    val isAdmin by viewModel.isAdmin.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()
    val bookmarks by viewModel.bookmarks.collectAsStateWithLifecycle()
    val likedPhotoIds by viewModel.likedPhotoIds.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val adminMembers by viewModel.adminMembers.collectAsStateWithLifecycle()
    val registeredUsers by viewModel.registeredUsers.collectAsStateWithLifecycle()
    val appVersionConfig by viewModel.appVersionConfig.collectAsStateWithLifecycle()

    val newsSearchQuery by viewModel.newsSearchQuery.collectAsStateWithLifecycle()
    val selectedNewsCategory by viewModel.selectedNewsCategory.collectAsStateWithLifecycle()
    val scheduleSearchQuery by viewModel.scheduleSearchQuery.collectAsStateWithLifecycle()
    val selectedScheduleCategory by viewModel.selectedScheduleCategory.collectAsStateWithLifecycle()
    val selectedGalleryCategory by viewModel.selectedGalleryCategory.collectAsStateWithLifecycle()
    val memberSearchQuery by viewModel.memberSearchQuery.collectAsStateWithLifecycle()
    val selectedMemberDetail by viewModel.selectedMemberDetail.collectAsStateWithLifecycle()
    val ownMemberDetail by viewModel.ownMemberDetail.collectAsStateWithLifecycle()
    val unreadNewMemberCount by viewModel.unreadNewMemberCount.collectAsStateWithLifecycle()
    val isLoadingMemberDetail by viewModel.isLoadingMemberDetail.collectAsStateWithLifecycle()
    val activePhotoComments by viewModel.activePhotoComments.collectAsStateWithLifecycle()
    val isPostingComment by viewModel.isPostingComment.collectAsStateWithLifecycle()
    val activeNewsComments by viewModel.activeNewsComments.collectAsStateWithLifecycle()
    val isPostingNewsComment by viewModel.isPostingNewsComment.collectAsStateWithLifecycle()

    var currentTab by rememberSaveable { mutableStateOf(BulletinTab.NEWS) }
    var isViewingNewsDetail by remember { mutableStateOf(false) }
    var isViewingGalleryDetail by remember { mutableStateOf(false) }
    val isGuest = currentUser == null
    val visibleTabs = remember(isAdmin) {
        if (isAdmin) BulletinTab.entries
        else BulletinTab.entries.filter { it != BulletinTab.MEMBERS }
    }
    LaunchedEffect(isAdmin, currentTab) {
        if (!isAdmin && currentTab == BulletinTab.MEMBERS) {
            currentTab = BulletinTab.NEWS
        }
        if (currentTab == BulletinTab.MEMBERS) {
            viewModel.clearUnreadNewMemberCount()
        }
    }
    var focusedNewsId by remember { mutableStateOf<String?>(null) }
    var focusedScheduleId by remember { mutableStateOf<String?>(null) }

    // Admin & Self-Registration Editor Modal States
    var showNewsEditor by remember { mutableStateOf(false) }
    var editingNews by remember { mutableStateOf<NewsArticle?>(null) }
    var showScheduleEditor by remember { mutableStateOf(false) }
    var editingSchedule by remember { mutableStateOf<ChurchSchedule?>(null) }
    var showPhotoEditor by remember { mutableStateOf(false) }
    var editingPhoto by remember { mutableStateOf<GalleryPhoto?>(null) }
    var showMemberEditor by remember { mutableStateOf(false) }
    var editingMember by remember { mutableStateOf<ChurchMemberDetail?>(null) }
    var showSelfMemberEditor by remember { mutableStateOf(false) }
    var showMemberExportDialog by remember { mutableStateOf(false) }
    var isExportingFile by remember { mutableStateOf(false) }
    var pendingExportMembers by remember { mutableStateOf<List<ChurchMemberDetail>>(emptyList()) }
    var showAddAdminDialog by remember { mutableStateOf(false) }
    var showMemberQrScanner by remember { mutableStateOf(false) }
    var dismissedUpdateVersionCode by rememberSaveable { mutableStateOf(0) }
    var forceShowUpdateDialog by remember { mutableStateOf(false) }
    var dismissedPushOptInThisSession by rememberSaveable { mutableStateOf(false) }
    var shouldPromptPushOptIn by remember {
        mutableStateOf(BulletinNotificationManager.shouldShowOptInPopup(context))
    }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, context) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                shouldPromptPushOptIn = BulletinNotificationManager.shouldShowOptInPopup(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val excelExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(MemberExportFormat.EXCEL.mimeType)
    ) { uri ->
        val membersToExport = pendingExportMembers
        if (uri != null && membersToExport.isNotEmpty()) {
            scope.launch {
                isExportingFile = true
                val res = MemberExportUtils.exportMembersToUri(
                    context = context,
                    uri = uri,
                    members = membersToExport,
                    format = MemberExportFormat.EXCEL
                )
                isExportingFile = false
                pendingExportMembers = emptyList()
                res.onSuccess { count ->
                    viewModel.showSnackbar(
                        AppLanguageManager.tr(
                            "Fail Excel ($count ahli) berjaya disimpan ke storan peranti.",
                            "Excel file ($count members) saved to device storage."
                        )
                    )
                }.onFailure { err ->
                    viewModel.showSnackbar(
                        err.localizedMessage ?: AppLanguageManager.tr(
                            "Gagal menyimpan fail Excel.",
                            "Failed to save Excel file."
                        )
                    )
                }
            }
        } else {
            pendingExportMembers = emptyList()
        }
    }

    val pdfExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(MemberExportFormat.PDF.mimeType)
    ) { uri ->
        val membersToExport = pendingExportMembers
        if (uri != null && membersToExport.isNotEmpty()) {
            scope.launch {
                isExportingFile = true
                val res = MemberExportUtils.exportMembersToUri(
                    context = context,
                    uri = uri,
                    members = membersToExport,
                    format = MemberExportFormat.PDF
                )
                isExportingFile = false
                pendingExportMembers = emptyList()
                res.onSuccess { count ->
                    viewModel.showSnackbar(
                        AppLanguageManager.tr(
                            "Fail PDF ($count ahli) berjaya disimpan ke storan peranti.",
                            "PDF file ($count members) saved to device storage."
                        )
                    )
                }.onFailure { err ->
                    viewModel.showSnackbar(
                        err.localizedMessage ?: AppLanguageManager.tr(
                            "Gagal menyimpan fail PDF.",
                            "Failed to save PDF file."
                        )
                    )
                }
            }
        } else {
            pendingExportMembers = emptyList()
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(viewModel) {
        viewModel.snackbarMessage.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    // Apply deep-link from tapped push notification
    LaunchedEffect(pendingDeepLink, isAdmin) {
        val deepLink = pendingDeepLink ?: return@LaunchedEffect
        val (type, itemId) = deepLink
        when {
            type.equals(BulletinNotificationManager.TYPE_MEMBER_REGISTERED, ignoreCase = true) -> {
                if (isAdmin) {
                    currentTab = BulletinTab.MEMBERS
                    viewModel.clearUnreadNewMemberCount()
                    if (itemId.isNotBlank()) viewModel.loadMemberDetail(itemId)
                }
            }
            type.equals(BulletinNotificationManager.TYPE_SCHEDULE, ignoreCase = true) -> {
                if (itemId.isNotBlank()) focusedScheduleId = itemId
                currentTab = BulletinTab.SCHEDULES
            }
            else -> {
                if (itemId.isNotBlank()) focusedNewsId = itemId
                currentTab = BulletinTab.NEWS
            }
        }
        onConsumeDeepLink()
    }

    // Sync FCM topic subscriptions & register token for this session (including Admin topic)
    LaunchedEffect(sessionKey, isAdmin) {
        val masterEnabled = BulletinNotificationManager.isMasterPushEnabled(context)
        val newsEnabled = BulletinNotificationManager.isNewsNotificationsEnabled(context)
        val schedulesEnabled = BulletinNotificationManager.isScheduleNotificationsEnabled(context)
        BulletinNotificationManager.syncTopicSubscriptions(
            context = context,
            newsEnabled = newsEnabled,
            schedulesEnabled = schedulesEnabled,
            isAdmin = isAdmin,
            masterEnabled = masterEnabled
        )
        BulletinNotificationManager.fetchAndSyncFcmToken(context) { token, news, sched ->
            viewModel.syncFcmToken(token, news, sched)
        }
    }

    // Receive live real-time published news/schedule/member-registration alerts and dispatch Android push notifications + snackbar
    LaunchedEffect(viewModel) {
        viewModel.liveAlerts.collect { alert ->
            if (!BulletinNotificationManager.isAlertEnabledForType(context, alert.type)) {
                return@collect
            }
            BulletinNotificationManager.showBulletinNotification(
                context = context,
                type = alert.type,
                itemId = alert.itemId,
                title = alert.title,
                body = alert.body
            )
            val result = snackbarHostState.showSnackbar(
                message = "${alert.title} — ${alert.body}",
                actionLabel = AppLanguageManager.tr("Buka", "Open"),
                duration = SnackbarDuration.Long
            )
            if (result == SnackbarResult.ActionPerformed) {
                when {
                    alert.type.equals(BulletinNotificationManager.TYPE_MEMBER_REGISTERED, ignoreCase = true) -> {
                        if (isAdmin) {
                            currentTab = BulletinTab.MEMBERS
                            viewModel.clearUnreadNewMemberCount()
                            if (alert.itemId.isNotBlank()) viewModel.loadMemberDetail(alert.itemId)
                        }
                    }
                    alert.type.equals(BulletinNotificationManager.TYPE_SCHEDULE, ignoreCase = true) -> {
                        if (alert.itemId.isNotBlank()) focusedScheduleId = alert.itemId
                        currentTab = BulletinTab.SCHEDULES
                    }
                    else -> {
                        if (alert.itemId.isNotBlank()) focusedNewsId = alert.itemId
                        currentTab = BulletinTab.NEWS
                    }
                }
            }
        }
    }

    val bookmarkedNewsIds = remember(bookmarks) {
        bookmarks.filter { it.itemType == "NEWS" }.map { it.itemId }.toSet()
    }
    val bookmarkedScheduleIds = remember(bookmarks) {
        bookmarks.filter { it.itemType == "SCHEDULE" }.map { it.itemId }.toSet()
    }
    val featuredSchedules = remember(schedulesState, isAdmin) {
        (schedulesState as? UiState.Success)?.data.orEmpty().filter { schedule ->
            schedule.featuredInNews && (schedule.published || isAdmin)
        }
    }

    val appLang = LocalAppLanguage.current

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavRail = maxWidth >= 600.dp
        val isExpandedScreen = maxWidth >= 840.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing.exclude(WindowInsets.ime),
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                if (!useNavRail) {
                    NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest) {
                        visibleTabs.forEach { tab ->
                            val selected = currentTab == tab
                            val label = tab.label(appLang)
                            NavigationBarItem(
                                selected = selected,
                                onClick = { currentTab = tab },
                                icon = {
                                    if (tab == BulletinTab.MEMBERS && unreadNewMemberCount > 0) {
                                        BadgedBox(
                                            badge = {
                                                Badge {
                                                    Text(
                                                        text = if (unreadNewMemberCount > 9) "9+" else unreadNewMemberCount.toString()
                                                    )
                                                }
                                            }
                                        ) {
                                            Icon(
                                                imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                                contentDescription = label
                                            )
                                        }
                                    } else {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = label
                                        )
                                    }
                                },
                                label = { Text(label) },
                                modifier = Modifier.testTag("nav_tab_${tab.route}")
                            )
                        }
                    }
                }
            },
            floatingActionButton = {
                val hideNewsDetailFab = currentTab == BulletinTab.NEWS && isViewingNewsDetail && !isExpandedScreen
                val hideGalleryDetailFab = currentTab == BulletinTab.GALLERY && isViewingGalleryDetail
                if (isAdmin && currentTab != BulletinTab.ACCOUNT && !hideNewsDetailFab && !hideGalleryDetailFab) {
                    if (currentTab == BulletinTab.MEMBERS) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ExtendedFloatingActionButton(
                                onClick = { showMemberExportDialog = true },
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                icon = { Icon(Icons.Filled.Download, contentDescription = null) },
                                text = { Text(tr("Eksport", "Export")) },
                                modifier = Modifier.testTag("admin_export_members_button")
                            )
                            ExtendedFloatingActionButton(
                                onClick = {
                                    editingMember = null
                                    showMemberEditor = true
                                },
                                containerColor = MaterialTheme.colorScheme.secondary,
                                contentColor = MaterialTheme.colorScheme.onSecondary,
                                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                                text = { Text(tr("Tambah Ahli", "Add Member")) },
                                modifier = Modifier.testTag("admin_fab_members")
                            )
                        }
                    } else {
                        val (fabLabel, onFabClick) = when (currentTab) {
                            BulletinTab.NEWS -> Pair(tr("Tulis Berita", "Write News")) {
                                editingNews = null
                                showNewsEditor = true
                            }
                            BulletinTab.SCHEDULES -> Pair(tr("Tambah Jadual", "Add Schedule")) {
                                editingSchedule = null
                                showScheduleEditor = true
                            }
                            BulletinTab.GALLERY -> Pair(tr("Muat Naik Foto", "Upload Photo")) {
                                editingPhoto = null
                                showPhotoEditor = true
                            }
                            BulletinTab.MEMBERS, BulletinTab.ACCOUNT -> Pair("", {})
                        }
                        ExtendedFloatingActionButton(
                            onClick = onFabClick,
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = MaterialTheme.colorScheme.onSecondary,
                            icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                            text = { Text(fabLabel) },
                            modifier = Modifier.testTag("admin_fab_${currentTab.route}")
                        )
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .consumeWindowInsets(innerPadding)
            ) {
                if (useNavRail) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                        header = {
                            Surface(
                                shape = CircleShape,
                                color = Color.White,
                                shadowElevation = 2.dp,
                                modifier = Modifier
                                    .padding(top = 12.dp, bottom = 8.dp)
                                    .size(44.dp)
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.app_icon_buletin),
                                    contentDescription = stringResource(R.string.app_name),
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(2.dp)
                                        .clip(CircleShape)
                                )
                            }
                        },
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        Spacer(modifier = Modifier.height(8.dp))
                        visibleTabs.forEach { tab ->
                            val selected = currentTab == tab
                            val label = tab.label(appLang)
                            NavigationRailItem(
                                selected = selected,
                                onClick = { currentTab = tab },
                                icon = {
                                    if (tab == BulletinTab.MEMBERS && unreadNewMemberCount > 0) {
                                        BadgedBox(
                                            badge = {
                                                Badge {
                                                    Text(
                                                        text = if (unreadNewMemberCount > 9) "9+" else unreadNewMemberCount.toString()
                                                    )
                                                }
                                            }
                                        ) {
                                            Icon(
                                                imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                                contentDescription = label
                                            )
                                        }
                                    } else {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = label
                                        )
                                    }
                                },
                                label = { Text(label) },
                                modifier = Modifier.testTag("rail_tab_${tab.route}")
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .imePadding()
                ) {
                    when (currentTab) {
                        BulletinTab.NEWS -> {
                            NewsScreen(
                                newsState = newsState,
                                featuredSchedules = featuredSchedules,
                                searchQuery = newsSearchQuery,
                                selectedCategory = selectedNewsCategory,
                                bookmarkedIds = bookmarkedNewsIds,
                                commentsState = activeNewsComments,
                                isPostingComment = isPostingNewsComment,
                                currentUserId = currentUser?.uid,
                                isGuest = isGuest,
                                isAdmin = isAdmin,
                                isExpandedScreen = isExpandedScreen,
                                focusedNewsId = focusedNewsId,
                                onConsumeFocusedNews = { focusedNewsId = null },
                                onSearchQueryChange = viewModel::setNewsSearchQuery,
                                onSelectCategory = viewModel::selectNewsCategory,
                                onToggleBookmark = { article ->
                                    viewModel.toggleBookmark(article.id, "NEWS", article.title)
                                },
                                onObserveComments = viewModel::observeCommentsForNews,
                                onPostComment = { newsId, text, parentId, replyToName, onDone ->
                                    viewModel.postNewsComment(newsId, text, parentId, replyToName, onDone)
                                },
                                onDeleteComment = viewModel::deleteNewsComment,
                                onDetailVisibilityChange = { visible ->
                                    isViewingNewsDetail = visible
                                },
                                onOpenFeaturedSchedule = { schedule ->
                                    focusedScheduleId = schedule.id
                                    currentTab = BulletinTab.SCHEDULES
                                },
                                onRemoveFeaturedSchedule = viewModel::toggleScheduleFeaturedInNews,
                                onEditNews = { article ->
                                    editingNews = article
                                    showNewsEditor = true
                                },
                                onDeleteNews = { article ->
                                    viewModel.deleteNews(article.id)
                                },
                                onAddNews = {
                                    editingNews = null
                                    showNewsEditor = true
                                }
                            )
                        }

                        BulletinTab.SCHEDULES -> {
                            SchedulesScreen(
                                schedulesState = schedulesState,
                                searchQuery = scheduleSearchQuery,
                                selectedCategory = selectedScheduleCategory,
                                bookmarkedIds = bookmarkedScheduleIds,
                                isAdmin = isAdmin,
                                isBusy = isBusy,
                                isExpandedScreen = isExpandedScreen,
                                focusedScheduleId = focusedScheduleId,
                                onConsumeFocusedSchedule = { focusedScheduleId = null },
                                onSearchQueryChange = viewModel::setScheduleSearchQuery,
                                onSelectCategory = viewModel::selectScheduleCategory,
                                onToggleBookmark = { schedule ->
                                    viewModel.toggleBookmark(schedule.id, "SCHEDULE", schedule.title)
                                },
                                onToggleFeaturedInNews = viewModel::toggleScheduleFeaturedInNews,
                                onEditSchedule = { schedule ->
                                    editingSchedule = schedule
                                    showScheduleEditor = true
                                },
                                onDeleteSchedule = { schedule ->
                                    viewModel.deleteSchedule(schedule.id)
                                },
                                onAddSchedule = {
                                    editingSchedule = null
                                    showScheduleEditor = true
                                }
                            )
                        }

                        BulletinTab.GALLERY -> {
                            GalleryScreen(
                                galleryState = galleryState,
                                selectedCategory = selectedGalleryCategory,
                                likedPhotoIds = likedPhotoIds,
                                commentsState = activePhotoComments,
                                isPostingComment = isPostingComment,
                                currentUserId = currentUser?.uid,
                                isGuest = isGuest,
                                isAdmin = isAdmin,
                                onSelectCategory = viewModel::selectGalleryCategory,
                                onToggleLike = { photo ->
                                    viewModel.togglePhotoLike(photo.id)
                                },
                                onObserveComments = viewModel::observeCommentsForPhoto,
                                onPostComment = { photoId, text, parentId, replyToName, onDone ->
                                    viewModel.postGalleryComment(photoId, text, parentId, replyToName, onDone)
                                },
                                onDeleteComment = viewModel::deleteGalleryComment,
                                onDetailVisibilityChange = { visible ->
                                    isViewingGalleryDetail = visible
                                },
                                onEditPhoto = { photo ->
                                    editingPhoto = photo
                                    showPhotoEditor = true
                                },
                                onDeletePhoto = { photo ->
                                    viewModel.deleteGalleryPhoto(photo.id)
                                },
                                onAddPhoto = {
                                    editingPhoto = null
                                    showPhotoEditor = true
                                }
                            )
                        }

                        BulletinTab.MEMBERS -> {
                            if (isAdmin) {
                                MembersScreen(
                                    membersState = membersState,
                                    searchQuery = memberSearchQuery,
                                    isBusy = isBusy,
                                    selectedMemberDetail = selectedMemberDetail,
                                    isLoadingDetail = isLoadingMemberDetail,
                                    onSearchQueryChange = viewModel::setMemberSearchQuery,
                                    onMemberClick = viewModel::loadMemberDetail,
                                    onDismissMemberDetail = viewModel::clearSelectedMemberDetail,
                                    onAddMember = {
                                        editingMember = null
                                        showMemberEditor = true
                                    },
                                    onEditMember = { detail ->
                                        viewModel.clearSelectedMemberDetail()
                                        editingMember = detail
                                        showMemberEditor = true
                                    },
                                    onDeleteMember = { memberId ->
                                        viewModel.deleteChurchMember(memberId)
                                    },
                                    onScanMemberQrClick = {
                                        showMemberQrScanner = true
                                    }
                                )
                            }
                        }

                        BulletinTab.ACCOUNT -> {
                            ProfileAdminScreen(
                                isGuest = currentUser == null,
                                uid = currentUser?.uid,
                                email = currentUser?.email,
                                googleDisplayName = currentUser?.displayName,
                                userProfile = userProfile,
                                ownMemberDetail = ownMemberDetail,
                                bookmarks = bookmarks,
                                isAdmin = isAdmin,
                                adminMembers = adminMembers,
                                registeredUsers = registeredUsers,
                                appVersionConfig = appVersionConfig,
                                newsCount = (newsState as? UiState.Success)?.data?.size ?: 0,
                                schedulesCount = (schedulesState as? UiState.Success)?.data?.size ?: 0,
                                galleryCount = (galleryState as? UiState.Success)?.data?.size ?: 0,
                                membersCount = (membersState as? UiState.Success)?.data?.size ?: 0,
                                isBusy = isBusy,
                                onSaveProfile = viewModel::saveProfile,
                                onFillOrEditMemberDetails = { showSelfMemberEditor = true },
                                onUpdateProfilePhoto = viewModel::updateProfilePhoto,
                                onProfilePhotoError = viewModel::showSnackbar,
                                onOpenBookmark = { bm ->
                                    if (bm.itemType == "SCHEDULE") {
                                        focusedScheduleId = bm.itemId
                                        currentTab = BulletinTab.SCHEDULES
                                    } else {
                                        focusedNewsId = bm.itemId
                                        currentTab = BulletinTab.NEWS
                                    }
                                },
                                onRemoveBookmark = { bm ->
                                    viewModel.toggleBookmark(bm.itemId, bm.itemType, bm.title)
                                },
                                onAddNewsClick = {
                                    editingNews = null
                                    showNewsEditor = true
                                },
                                onAddScheduleClick = {
                                    editingSchedule = null
                                    showScheduleEditor = true
                                },
                                onAddPhotoClick = {
                                    editingPhoto = null
                                    showPhotoEditor = true
                                },
                                onAddMemberClick = {
                                    editingMember = null
                                    showMemberEditor = true
                                },
                                onOpenAddAdminDialog = { showAddAdminDialog = true },
                                onRemoveAdminMember = viewModel::removeAdminMember,
                                onOpenUpdateNotice = { forceShowUpdateDialog = true },
                                onUidCopied = {
                                    viewModel.showSnackbar(
                                        AppLanguageManager.tr(
                                            "UID pengguna disalin ke papan klip.",
                                            "User UID copied to clipboard."
                                        )
                                    )
                                },
                                onNotificationPrefsUpdated = { newsEnabled, schedulesEnabled ->
                                    shouldPromptPushOptIn = BulletinNotificationManager.shouldShowOptInPopup(context)
                                    scope.launch {
                                        BulletinNotificationManager.fetchAndSyncFcmToken(context) { token, _, _ ->
                                            viewModel.syncFcmToken(token, newsEnabled, schedulesEnabled)
                                        }
                                        val message = if (!newsEnabled && !schedulesEnabled) {
                                            AppLanguageManager.tr(
                                                "Notifikasi tolak telah dimatikan. Anda boleh hidupkannya semula bila-bila masa.",
                                                "Push notifications disabled. You can re-enable them anytime here."
                                            )
                                        } else {
                                            AppLanguageManager.tr(
                                                "Notifikasi tolak FCM aktif & disegerakkan.",
                                                "FCM push notifications active & synced."
                                            )
                                        }
                                        snackbarHostState.showSnackbar(message)
                                    }
                                },
                                onGoogleSignInSuccess = onGoogleSignInSuccess,
                                onExitGuestMode = onExitGuestMode,
                                onSignOutClick = {
                                    signOutUser(
                                        credentialManager = credentialManager,
                                        scope = scope,
                                        onSignOutComplete = onSignedOut
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // --- ADMIN EDITOR DIALOGS ---
    if (showNewsEditor && currentUser != null) {
        AdminNewsEditorDialog(
            initial = editingNews,
            defaultAuthorName = userProfile?.displayName ?: currentUser.displayName ?: tr("Sekretariat SIB Toburon", "SIB Toburon Secretariat"),
            isBusy = isBusy,
            onDismiss = { showNewsEditor = false },
            onSave = { title, summary, content, category, author, preset, url, b64, pinned, published ->
                viewModel.saveNews(
                    existingId = editingNews?.id,
                    title = title,
                    summary = summary,
                    content = content,
                    category = category,
                    authorName = author,
                    coverPreset = preset,
                    imageUrl = url,
                    imageBase64 = b64,
                    pinned = pinned,
                    published = published,
                    onComplete = { showNewsEditor = false }
                )
            }
        )
    }

    if (showScheduleEditor && currentUser != null) {
        AdminScheduleEditorDialog(
            initial = editingSchedule,
            isBusy = isBusy,
            onDismiss = { showScheduleEditor = false },
            onSave = { title, desc, cat, date, time, loc, speaker, recurring, published ->
                viewModel.saveSchedule(
                    existingId = editingSchedule?.id,
                    title = title,
                    description = desc,
                    category = cat,
                    dateLabel = date,
                    timeLabel = time,
                    location = loc,
                    speaker = speaker,
                    recurring = recurring,
                    published = published,
                    onComplete = { showScheduleEditor = false }
                )
            }
        )
    }

    if (showPhotoEditor && currentUser != null) {
        AdminPhotoEditorDialog(
            initial = editingPhoto,
            defaultPhotographer = userProfile?.displayName ?: currentUser.displayName ?: tr("Pasukan Multimedia SIB Toburon", "SIB Toburon Media Team"),
            isBusy = isBusy,
            onDismiss = { showPhotoEditor = false },
            onSave = { title, caption, cat, date, preset, url, b64, extraB64, photographer, published ->
                viewModel.saveGalleryPhoto(
                    existingId = editingPhoto?.id,
                    title = title,
                    caption = caption,
                    category = cat,
                    eventDate = date,
                    coverPreset = preset,
                    imageUrl = url,
                    imageBase64 = b64,
                    extraImagesBase64 = extraB64,
                    photographerName = photographer,
                    published = published,
                    onComplete = { showPhotoEditor = false }
                )
            }
        )
    }

    if (showMemberEditor && currentUser != null) {
        AdminMemberEditorDialog(
            initial = editingMember,
            registeredUsers = registeredUsers,
            isBusy = isBusy,
            onDismiss = { showMemberEditor = false },
            onSave = { fullName, myKadNumber, gender, birthDateIso, baptismStatus, denomination, maritalStatus, spouseName, sibMembershipNumber, passportPhotoBase64, memberEmail ->
                viewModel.saveChurchMember(
                    existingId = editingMember?.id,
                    fullName = fullName,
                    myKadNumber = myKadNumber,
                    gender = gender,
                    birthDateIso = birthDateIso,
                    baptismStatus = baptismStatus,
                    denomination = denomination,
                    maritalStatus = maritalStatus,
                    spouseName = spouseName,
                    sibMembershipNumber = sibMembershipNumber,
                    passportPhotoBase64 = passportPhotoBase64,
                    memberEmail = memberEmail,
                    onComplete = { showMemberEditor = false }
                )
            }
        )
    }

    if (showSelfMemberEditor && currentUser != null) {
        AdminMemberEditorDialog(
            initial = ownMemberDetail,
            isSelfRegistration = true,
            defaultFullName = userProfile?.displayName?.takeIf { it.isNotBlank() } ?: currentUser.displayName,
            defaultEmail = currentUser.email,
            isBusy = isBusy,
            onDismiss = { showSelfMemberEditor = false },
            onSave = { fullName, myKadNumber, gender, birthDateIso, baptismStatus, denomination, maritalStatus, spouseName, sibMembershipNumber, passportPhotoBase64, _ ->
                viewModel.saveOwnChurchMember(
                    fullName = fullName,
                    myKadNumber = myKadNumber,
                    gender = gender,
                    birthDateIso = birthDateIso,
                    baptismStatus = baptismStatus,
                    denomination = denomination,
                    maritalStatus = maritalStatus,
                    spouseName = spouseName,
                    sibMembershipNumber = sibMembershipNumber,
                    passportPhotoBase64 = passportPhotoBase64,
                    onComplete = { showSelfMemberEditor = false }
                )
            }
        )
    }

    if (showMemberExportDialog && isAdmin && currentUser != null) {
        val currentMembersCount = (membersState as? UiState.Success)?.data?.size ?: 0
        AdminMemberExportDialog(
            membersCount = currentMembersCount,
            isBusy = isBusy || isExportingFile,
            onDismiss = { showMemberExportDialog = false },
            onSelectFormat = { format ->
                viewModel.prepareMembersForExport { details ->
                    showMemberExportDialog = false
                    pendingExportMembers = details
                    val fileName = MemberExportUtils.suggestedFileName(format)
                    when (format) {
                        MemberExportFormat.EXCEL -> excelExportLauncher.launch(fileName)
                        MemberExportFormat.PDF -> pdfExportLauncher.launch(fileName)
                    }
                }
            }
        )
    }

    if (showAddAdminDialog && currentUser != null) {
        AdminMemberAddDialog(
            isBusy = isBusy,
            onDismiss = { showAddAdminDialog = false },
            onAdd = { targetUid, name, role ->
                viewModel.addAdminMember(targetUid, name, role) {
                    showAddAdminDialog = false
                }
            }
        )
    }

    if (showMemberQrScanner && isAdmin && currentUser != null) {
        MemberQrScannerDialog(
            onDismiss = { showMemberQrScanner = false },
            onValidMemberQrScanned = { rawQr ->
                showMemberQrScanner = false
                viewModel.loadMemberDetailFromQr(rawQr)
            }
        )
    }

    val versionNotice = appVersionConfig
    val shouldShowUpdatePrompt = versionNotice != null &&
        versionNotice.isUpdateAvailable(BuildConfig.VERSION_CODE) &&
        (versionNotice.forceUpdate || forceShowUpdateDialog || dismissedUpdateVersionCode < versionNotice.latestVersionCode)

    if (shouldShowUpdatePrompt && versionNotice != null) {
        AppUpdateAvailableDialog(
            config = versionNotice,
            onDismiss = {
                forceShowUpdateDialog = false
                dismissedUpdateVersionCode = versionNotice.latestVersionCode
            },
            onOpenUrlError = viewModel::showSnackbar
        )
    } else if (shouldPromptPushOptIn && !dismissedPushOptInThisSession) {
        PushNotificationOptInDialog(
            isAdmin = isAdmin,
            onEnabled = { newsEnabled, schedulesEnabled ->
                shouldPromptPushOptIn = false
                dismissedPushOptInThisSession = true
                scope.launch {
                    BulletinNotificationManager.fetchAndSyncFcmToken(context) { token, _, _ ->
                        viewModel.syncFcmToken(token, newsEnabled, schedulesEnabled)
                    }
                    snackbarHostState.showSnackbar(
                        AppLanguageManager.tr(
                            "Notifikasi masa nyata SIB Toburon berjaya diaktifkan!",
                            "Real-time SIB Toburon push notifications enabled!"
                        )
                    )
                }
            },
            onOptOut = {
                shouldPromptPushOptIn = false
                dismissedPushOptInThisSession = true
                scope.launch {
                    BulletinNotificationManager.fetchAndSyncFcmToken(context) { token, _, _ ->
                        viewModel.syncFcmToken(token, false, false)
                    }
                    snackbarHostState.showSnackbar(
                        AppLanguageManager.tr(
                            "Notifikasi dimatikan. Anda boleh mengaktifkannya di tab Akaun.",
                            "Notifications opted out. You can enable them anytime in the Account tab."
                        )
                    )
                }
            },
            onDismissForNow = {
                dismissedPushOptInThisSession = true
            }
        )
    }
}
