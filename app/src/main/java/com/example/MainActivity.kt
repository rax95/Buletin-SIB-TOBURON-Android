package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.outlined.CalendarMonth
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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import com.example.data.ChurchSchedule
import com.example.data.GalleryPhoto
import com.example.data.NewsArticle
import com.example.data.UiState
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.signOutUser
import com.example.ui.components.AdminMemberAddDialog
import com.example.ui.components.AdminNewsEditorDialog
import com.example.ui.components.AdminPhotoEditorDialog
import com.example.ui.components.AdminScheduleEditorDialog
import com.example.ui.screens.GalleryScreen
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

private enum class BulletinTab(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    NEWS("news", R.string.nav_news, Icons.Filled.Newspaper, Icons.Outlined.Newspaper),
    SCHEDULES("schedules", R.string.nav_schedules, Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth),
    GALLERY("gallery", R.string.nav_gallery, Icons.Filled.PhotoLibrary, Icons.Outlined.PhotoLibrary),
    ACCOUNT("account", R.string.nav_account, Icons.Filled.Person, Icons.Outlined.Person)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BuletinSibTheme {
                AppContent()
            }
        }
    }
}

@Composable
fun AppContent() {
    var currentUser by remember { mutableStateOf<FirebaseUser?>(Firebase.auth.currentUser) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose { Firebase.auth.removeAuthStateListener(listener) }
    }

    val user = currentUser
    if (user == null) {
        AuthScreen(
            onAuthSuccess = {
                currentUser = Firebase.auth.currentUser
            }
        )
    } else {
        // Key by uid so account switches cleanly tear down and recreate ViewModel & snapshot listeners
        key(user.uid) {
            BuletinMainShell(
                currentUser = user,
                onSignedOut = { currentUser = null }
            )
        }
    }
}

@Composable
private fun BuletinMainShell(
    currentUser: FirebaseUser,
    onSignedOut: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val viewModel: BulletinViewModel = viewModel(
        key = currentUser.uid,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                BulletinViewModel(
                    repository = BulletinRepository(db),
                    currentUserId = currentUser.uid,
                    userEmail = currentUser.email,
                    userDisplayName = currentUser.displayName,
                    emailVerified = currentUser.isEmailVerified
                )
            }
        }
    )

    val newsState by viewModel.newsState.collectAsStateWithLifecycle()
    val schedulesState by viewModel.schedulesState.collectAsStateWithLifecycle()
    val galleryState by viewModel.galleryState.collectAsStateWithLifecycle()
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

    var currentTab by rememberSaveable { mutableStateOf(BulletinTab.NEWS) }
    var focusedNewsId by remember { mutableStateOf<String?>(null) }
    var focusedScheduleId by remember { mutableStateOf<String?>(null) }

    // Admin Editor Modal States
    var showNewsEditor by remember { mutableStateOf(false) }
    var editingNews by remember { mutableStateOf<NewsArticle?>(null) }
    var showScheduleEditor by remember { mutableStateOf(false) }
    var editingSchedule by remember { mutableStateOf<ChurchSchedule?>(null) }
    var showPhotoEditor by remember { mutableStateOf(false) }
    var editingPhoto by remember { mutableStateOf<GalleryPhoto?>(null) }
    var showAddAdminDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(viewModel) {
        viewModel.snackbarMessage.collect { msg ->
            snackbarHostState.showSnackbar(msg)
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
                        BulletinTab.entries.forEach { tab ->
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
                        BulletinTab.ACCOUNT -> Pair("", {})
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
                        BulletinTab.entries.forEach { tab ->
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
                                onSeedStarter = viewModel::seedStarterData
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
                                onSeedStarter = viewModel::seedStarterData
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
                                onSeedStarter = viewModel::seedStarterData
                            )
                        }

                        BulletinTab.ACCOUNT -> {
                            ProfileAdminScreen(
                                uid = currentUser.uid,
                                email = currentUser.email,
                                googleDisplayName = currentUser.displayName,
                                userProfile = userProfile,
                                bookmarks = bookmarks,
                                isAdmin = isAdmin,
                                adminMembers = adminMembers,
                                newsCount = (newsState as? UiState.Success)?.data?.size ?: 0,
                                schedulesCount = (schedulesState as? UiState.Success)?.data?.size ?: 0,
                                galleryCount = (galleryState as? UiState.Success)?.data?.size ?: 0,
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
                                onSeedStarterClick = viewModel::seedStarterData,
                                onOpenAddAdminDialog = { showAddAdminDialog = true },
                                onRemoveAdminMember = viewModel::removeAdminMember,
                                onUidCopied = {
                                    // Instant confirmation
                                },
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
    if (showNewsEditor) {
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

    if (showScheduleEditor) {
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

    if (showPhotoEditor) {
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

    if (showAddAdminDialog) {
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
