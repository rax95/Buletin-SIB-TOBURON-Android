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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
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
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    NEWS("news", R.string.nav_news, Icons.Filled.Newspaper, Icons.Outlined.Newspaper),
    SCHEDULES("schedules", R.string.nav_schedules, Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth),
    GALLERY("gallery", R.string.nav_gallery, Icons.Filled.PhotoLibrary, Icons.Outlined.PhotoLibrary),
    MEMBERS("members", R.string.nav_members, Icons.Filled.Groups, Icons.Outlined.Groups),
    ACCOUNT("account", R.string.nav_account, Icons.Filled.Person, Icons.Outlined.Person)
}

class MainActivity : ComponentActivity() {
    private var pendingNotificationDeepLink by mutableStateOf<Pair<String, String>?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        BulletinNotificationManager.ensureNotificationChannel(this)
        pendingNotificationDeepLink = extractNotificationDeepLink(intent)

        setContent {
            BuletinSibTheme {
                AppContent(
                    pendingDeepLink = pendingNotificationDeepLink,
                    onConsumeDeepLink = { pendingNotificationDeepLink = null }
                )
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

    val newsSearchQuery by viewModel.newsSearchQuery.collectAsStateWithLifecycle()
    val selectedNewsCategory by viewModel.selectedNewsCategory.collectAsStateWithLifecycle()
    val scheduleSearchQuery by viewModel.scheduleSearchQuery.collectAsStateWithLifecycle()
    val selectedScheduleCategory by viewModel.selectedScheduleCategory.collectAsStateWithLifecycle()
    val selectedGalleryCategory by viewModel.selectedGalleryCategory.collectAsStateWithLifecycle()
    val memberSearchQuery by viewModel.memberSearchQuery.collectAsStateWithLifecycle()
    val selectedMemberDetail by viewModel.selectedMemberDetail.collectAsStateWithLifecycle()
    val isLoadingMemberDetail by viewModel.isLoadingMemberDetail.collectAsStateWithLifecycle()

    var currentTab by rememberSaveable { mutableStateOf(BulletinTab.NEWS) }
    val isGuest = currentUser == null
    val visibleTabs = remember(isGuest) {
        if (isGuest) BulletinTab.entries.filter { it != BulletinTab.MEMBERS }
        else BulletinTab.entries
    }
    LaunchedEffect(isGuest, currentTab) {
        if (isGuest && currentTab == BulletinTab.MEMBERS) {
            currentTab = BulletinTab.NEWS
        }
    }
    var focusedNewsId by remember { mutableStateOf<String?>(null) }
    var focusedScheduleId by remember { mutableStateOf<String?>(null) }

    // Admin Editor Modal States
    var showNewsEditor by remember { mutableStateOf(false) }
    var editingNews by remember { mutableStateOf<NewsArticle?>(null) }
    var showScheduleEditor by remember { mutableStateOf(false) }
    var editingSchedule by remember { mutableStateOf<ChurchSchedule?>(null) }
    var showPhotoEditor by remember { mutableStateOf(false) }
    var editingPhoto by remember { mutableStateOf<GalleryPhoto?>(null) }
    var showMemberEditor by remember { mutableStateOf(false) }
    var editingMember by remember { mutableStateOf<ChurchMemberDetail?>(null) }
    var showMemberExportDialog by remember { mutableStateOf(false) }
    var isExportingFile by remember { mutableStateOf(false) }
    var pendingExportMembers by remember { mutableStateOf<List<ChurchMemberDetail>>(emptyList()) }
    var showAddAdminDialog by remember { mutableStateOf(false) }

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
                    viewModel.showSnackbar("Fail Excel ($count ahli) berjaya disimpan ke storan peranti.")
                }.onFailure { err ->
                    viewModel.showSnackbar(err.localizedMessage ?: "Gagal menyimpan fail Excel.")
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
                    viewModel.showSnackbar("Fail PDF ($count ahli) berjaya disimpan ke storan peranti.")
                }.onFailure { err ->
                    viewModel.showSnackbar(err.localizedMessage ?: "Gagal menyimpan fail PDF.")
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
    LaunchedEffect(pendingDeepLink) {
        val deepLink = pendingDeepLink ?: return@LaunchedEffect
        val (type, itemId) = deepLink
        if (type.equals(BulletinNotificationManager.TYPE_SCHEDULE, ignoreCase = true)) {
            if (itemId.isNotBlank()) focusedScheduleId = itemId
            currentTab = BulletinTab.SCHEDULES
        } else {
            if (itemId.isNotBlank()) focusedNewsId = itemId
            currentTab = BulletinTab.NEWS
        }
        onConsumeDeepLink()
    }

    // Sync FCM topic subscriptions & register token for this session
    LaunchedEffect(sessionKey) {
        val newsEnabled = BulletinNotificationManager.isNewsNotificationsEnabled(context)
        val schedulesEnabled = BulletinNotificationManager.isScheduleNotificationsEnabled(context)
        BulletinNotificationManager.syncTopicSubscriptions(context, newsEnabled, schedulesEnabled)
        BulletinNotificationManager.fetchAndSyncFcmToken(context) { token, news, sched ->
            viewModel.syncFcmToken(token, news, sched)
        }
    }

    // Receive live real-time published news/schedule alerts and dispatch Android push notifications + snackbar
    LaunchedEffect(viewModel) {
        viewModel.liveAlerts.collect { alert ->
            BulletinNotificationManager.showBulletinNotification(
                context = context,
                type = alert.type,
                itemId = alert.itemId,
                title = alert.title,
                body = alert.body
            )
            val result = snackbarHostState.showSnackbar(
                message = "${alert.title} — ${alert.body}",
                actionLabel = "Buka",
                duration = SnackbarDuration.Long
            )
            if (result == SnackbarResult.ActionPerformed) {
                if (alert.type.equals(BulletinNotificationManager.TYPE_SCHEDULE, ignoreCase = true)) {
                    if (alert.itemId.isNotBlank()) focusedScheduleId = alert.itemId
                    currentTab = BulletinTab.SCHEDULES
                } else {
                    if (alert.itemId.isNotBlank()) focusedNewsId = alert.itemId
                    currentTab = BulletinTab.NEWS
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

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavRail = maxWidth >= 600.dp
        val isExpandedScreen = maxWidth >= 840.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                if (!useNavRail) {
                    NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest) {
                        visibleTabs.forEach { tab ->
                            val selected = currentTab == tab
                            val label = stringResource(tab.labelRes)
                            NavigationBarItem(
                                selected = selected,
                                onClick = { currentTab = tab },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = label
                                    )
                                },
                                label = { Text(label) },
                                modifier = Modifier.testTag("nav_tab_${tab.route}")
                            )
                        }
                    }
                }
            },
            floatingActionButton = {
                if (isAdmin && currentTab != BulletinTab.ACCOUNT) {
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
                                text = { Text("Export") },
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
                                text = { Text(stringResource(R.string.action_add_member)) },
                                modifier = Modifier.testTag("admin_fab_members")
                            )
                        }
                    } else {
                        val (fabLabel, onFabClick) = when (currentTab) {
                            BulletinTab.NEWS -> Pair(stringResource(R.string.action_add_news)) {
                                editingNews = null
                                showNewsEditor = true
                            }
                            BulletinTab.SCHEDULES -> Pair(stringResource(R.string.action_add_schedule)) {
                                editingSchedule = null
                                showScheduleEditor = true
                            }
                            BulletinTab.GALLERY -> Pair(stringResource(R.string.action_add_photo)) {
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
            ) {
                if (useNavRail) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                        header = {
                            Surface(
                                shape = CircleShape,
                                shadowElevation = 2.dp,
                                modifier = Modifier
                                    .padding(top = 12.dp, bottom = 8.dp)
                                    .size(44.dp)
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.app_icon_buletin),
                                    contentDescription = stringResource(R.string.app_name),
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.clip(CircleShape)
                                )
                            }
                        },
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        Spacer(modifier = Modifier.height(8.dp))
                        visibleTabs.forEach { tab ->
                            val selected = currentTab == tab
                            val label = stringResource(tab.labelRes)
                            NavigationRailItem(
                                selected = selected,
                                onClick = { currentTab = tab },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = label
                                    )
                                },
                                label = { Text(label) },
                                modifier = Modifier.testTag("rail_tab_${tab.route}")
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    when (currentTab) {
                        BulletinTab.NEWS -> {
                            NewsScreen(
                                newsState = newsState,
                                searchQuery = newsSearchQuery,
                                selectedCategory = selectedNewsCategory,
                                bookmarkedIds = bookmarkedNewsIds,
                                isAdmin = isAdmin,
                                isExpandedScreen = isExpandedScreen,
                                focusedNewsId = focusedNewsId,
                                onConsumeFocusedNews = { focusedNewsId = null },
                                onSearchQueryChange = viewModel::setNewsSearchQuery,
                                onSelectCategory = viewModel::selectNewsCategory,
                                onToggleBookmark = { article ->
                                    viewModel.toggleBookmark(article.id, "NEWS", article.title)
                                },
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
                                isExpandedScreen = isExpandedScreen,
                                focusedScheduleId = focusedScheduleId,
                                onConsumeFocusedSchedule = { focusedScheduleId = null },
                                onSearchQueryChange = viewModel::setScheduleSearchQuery,
                                onSelectCategory = viewModel::selectScheduleCategory,
                                onToggleBookmark = { schedule ->
                                    viewModel.toggleBookmark(schedule.id, "SCHEDULE", schedule.title)
                                },
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
                                isAdmin = isAdmin,
                                onSelectCategory = viewModel::selectGalleryCategory,
                                onToggleLike = { photo ->
                                    viewModel.togglePhotoLike(photo.id)
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
                            if (!isGuest) {
                                MembersScreen(
                                    membersState = membersState,
                                    searchQuery = memberSearchQuery,
                                    isAdmin = isAdmin,
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
                                bookmarks = bookmarks,
                                isAdmin = isAdmin,
                                adminMembers = adminMembers,
                                newsCount = (newsState as? UiState.Success)?.data?.size ?: 0,
                                schedulesCount = (schedulesState as? UiState.Success)?.data?.size ?: 0,
                                galleryCount = (galleryState as? UiState.Success)?.data?.size ?: 0,
                                membersCount = (membersState as? UiState.Success)?.data?.size ?: 0,
                                isBusy = isBusy,
                                onSaveProfile = viewModel::saveProfile,
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
                                onUidCopied = {},
                                onNotificationPrefsUpdated = { newsEnabled, schedulesEnabled ->
                                    scope.launch {
                                        BulletinNotificationManager.fetchAndSyncFcmToken(context) { token, _, _ ->
                                            viewModel.syncFcmToken(token, newsEnabled, schedulesEnabled)
                                        }
                                        snackbarHostState.showSnackbar("Preferensi notifikasi FCM diperbarui.")
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
            defaultAuthorName = userProfile?.displayName ?: currentUser.displayName ?: "Sekretariat SIB Toburon",
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
            onSave = { title, desc, cat, date, time, loc, speaker, order, recurring, published ->
                viewModel.saveSchedule(
                    existingId = editingSchedule?.id,
                    title = title,
                    description = desc,
                    category = cat,
                    dateLabel = date,
                    timeLabel = time,
                    location = loc,
                    speaker = speaker,
                    sortOrder = order,
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
            defaultPhotographer = userProfile?.displayName ?: currentUser.displayName ?: "Tim Multimedia SIB Toburon",
            isBusy = isBusy,
            onDismiss = { showPhotoEditor = false },
            onSave = { title, caption, cat, date, preset, url, b64, photographer, published ->
                viewModel.saveGalleryPhoto(
                    existingId = editingPhoto?.id,
                    title = title,
                    caption = caption,
                    category = cat,
                    eventDate = date,
                    coverPreset = preset,
                    imageUrl = url,
                    imageBase64 = b64,
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
            isBusy = isBusy,
            onDismiss = { showMemberEditor = false },
            onSave = { fullName, gender, birthDateIso, baptismStatus, denomination ->
                viewModel.saveChurchMember(
                    existingId = editingMember?.id,
                    fullName = fullName,
                    gender = gender,
                    birthDateIso = birthDateIso,
                    baptismStatus = baptismStatus,
                    denomination = denomination,
                    onComplete = { showMemberEditor = false }
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
}
