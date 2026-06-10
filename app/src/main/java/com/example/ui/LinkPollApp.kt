package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LinkPollApp(viewModel: LinkPollViewModel) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    
    val activeRoute by viewModel.activeRoute.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val unreadNotifications by viewModel.unreadNotificationsCount.collectAsStateWithLifecycle()
    val isCreatePollShowing by viewModel.isCreatePollDialogShowing.collectAsStateWithLifecycle()
    val toastMessage by viewModel.toastMessage.collectAsStateWithLifecycle()

    var showNotificationsPanel by remember { mutableStateOf(false) }

    // Intercept hardware/system back button safely to handle Route stack
    BackHandler(enabled = activeRoute != "home") {
        viewModel.goBack()
    }

    // Trigger toast alerts
    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            // Display alert instantly
            viewModel.clearToast()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = PanelWhite,
                modifier = Modifier.width(280.dp)
            ) {
                Spacer(modifier = Modifier.height(24.dp))
                // Sidebar Header
                Row(
                    modifier = Modifier
                        .padding(horizontal = 24.dp, vertical = 8.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(InkDark),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("LP", color = LimeAccent, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                    Spacer(modifier = Modifier.width(11.dp))
                    Text("LinkPoll", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                }
                
                Spacer(modifier = Modifier.height(16.dp))

                // Navigation Items
                val navItems = listOf(
                    Triple("home", Icons.Default.Home, "Home"),
                    Triple("explore", Icons.Default.Search, "Explore"),
                    Triple("following", Icons.Default.Person, "Following"),
                    Triple("mypolls", Icons.Default.Star, "My Polls"),
                    Triple("settings", Icons.Default.Settings, "Settings")
                )

                navItems.forEach { (route, icon, label) ->
                    NavigationDrawerItem(
                        icon = { Icon(icon, contentDescription = label, tint = InkDark) },
                        label = { Text(label, color = InkDark, fontWeight = FontWeight.Bold) },
                        selected = activeRoute == route,
                        onClick = {
                            coroutineScope.launch { drawerState.close() }
                            viewModel.navigateTo(route)
                        },
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = LimeAccent,
                            unselectedContainerColor = Color.Transparent
                        ),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // User account chip bottom sidebar
                currentUser?.let { me ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(ChipBg)
                            .clickable {
                                coroutineScope.launch { drawerState.close() }
                                viewModel.navigateToUserProfile(me.username)
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        UserAvatar(
                            name = me.name,
                            avatarUrl = me.avatar,
                            colorHex = me.color,
                            size = 36
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = me.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "@${me.username}",
                                fontSize = 12.sp,
                                color = TextMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                // Top Header bar
                CenterAlignedTopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { viewModel.navigateTo("home") }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(9.dp))
                                    .background(InkDark),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("LP", color = LimeAccent, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "LinkPoll",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                fontSize = 19.sp,
                                color = InkDark
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            coroutineScope.launch {
                                if (drawerState.isClosed) drawerState.open() else drawerState.close()
                            }
                        }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu", tint = InkDark)
                        }
                    },
                    actions = {
                        // Action buttons
                        IconButton(onClick = { showNotificationsPanel = true }) {
                            Box {
                                Icon(Icons.Default.Notifications, contentDescription = "Notifications", tint = InkDark)
                                if (unreadNotifications > 0) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(RedAlert)
                                            .align(Alignment.TopEnd)
                                    )
                                }
                            }
                        }
                        
                        FloatingActionButton(
                            onClick = { viewModel.setCreatePollDialogVisible(true) },
                            containerColor = LimeAccent,
                            elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp),
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .size(36.dp),
                            shape = CircleShape
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Post Poll", tint = InkDark, modifier = Modifier.size(18.dp))
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = WarmBg
                    )
                )
            },
            containerColor = WarmBg
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Route Routing Router
                when (activeRoute) {
                    "home" -> FeedScreen(viewModel)
                    "explore" -> ExploreScreen(viewModel)
                    "following" -> FollowingScreen(viewModel)
                    "mypolls" -> MyPollsScreen(viewModel)
                    "settings" -> SettingsScreen(viewModel)
                    "detail" -> PollDetailScreen(viewModel)
                    "profile" -> ProfileScreen(viewModel)
                    else -> FeedScreen(viewModel)
                }

                // Toast Alert Popup Banner
                toastMessage?.let { msg ->
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 32.dp, start = 16.dp, end = 16.dp)
                            .clip(RoundedCornerShape(99.dp))
                            .background(InkDark)
                            .padding(horizontal = 20.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = msg,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Custom Poll Dialog
                if (isCreatePollShowing) {
                    CreatePollDialog(
                        onDismiss = { viewModel.setCreatePollDialogVisible(false) },
                        onSubmit = { q, opts, cat, dur, mult, anon ->
                            viewModel.createPoll(q, opts, cat, dur, mult, anon)
                        }
                    )
                }

                // In-App Notifications Panel Drawer
                if (showNotificationsPanel) {
                    NotificationsPanel(
                        viewModel = viewModel,
                        onDismiss = { showNotificationsPanel = false }
                    )
                }
            }
        }
    }
}

@Composable
fun FeedScreen(viewModel: LinkPollViewModel) {
    val selectedTab by viewModel.feedSelectedTab.collectAsStateWithLifecycle()
    val allPolls by viewModel.allPolls.collectAsStateWithLifecycle()
    val profiles by viewModel.allUserProfiles.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()

    // Calculate database stats
    val totalUsers = profiles.size
    val totalVotes = allPolls.sumOf { p -> p.options.sumOf { it.votes } }
    val activePollsCount = allPolls.size

    val filteredPolls = remember(allPolls, selectedTab, currentUser) {
        when (selectedTab) {
            "foryou" -> allPolls // Feed ranking recommendations
            "following" -> allPolls.filter { it.author.isFollowing }
            "trending" -> allPolls.sortedByDescending { it.likes + it.totalVotes }
            "new" -> allPolls.sortedByDescending { it.createdAt }
            "bookmarks" -> allPolls.filter { it.bookmarked }
            else -> allPolls
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Aesthetic Scroll Tab indicator
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .background(WarmBg)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val tabs = listOf(
                "foryou" to "For You",
                "following" to "Following",
                "trending" to "Trending",
                "new" to "New",
                "bookmarks" to "Bookmarks"
            )
            items(tabs) { (key, title) ->
                val active = selectedTab == key
                Column(
                    modifier = Modifier
                        .clickable { viewModel.selectFeedTab(key) }
                        .padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = title,
                        fontFamily = FontFamily.Serif,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 16.sp,
                        color = if (active) InkDark else SoftGray
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Box(
                        modifier = Modifier
                            .height(2.5.dp)
                            .width(18.dp)
                            .clip(CircleShape)
                            .background(if (active) LimeAccent else Color.Transparent)
                    )
                }
            }
        }

        // Stats Box Rows
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(18.dp))
                .border(1.dp, LineMuted, RoundedCornerShape(18.dp))
                .background(PanelWhite),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatsBlock(value = formatStatsCount(totalVotes), label = "Votes today")
            Box(modifier = Modifier
                .height(40.dp)
                .width(1.dp)
                .background(LineMuted))
            StatsBlock(value = activePollsCount.toString(), label = "Active polls")
            Box(modifier = Modifier
                .height(40.dp)
                .width(1.dp)
                .background(LineMuted))
            StatsBlock(value = totalUsers.toString(), label = "Members")
        }

        // Poll list display
        if (filteredPolls.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📊", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = if (selectedTab == "bookmarks") "No bookmarks yet." else "Be the first to start a discussion!",
                        color = TextMuted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                items(filteredPolls, key = { it.code }) { poll ->
                    PollCard(poll = poll, viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun ExploreScreen(viewModel: LinkPollViewModel) {
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val allPolls by viewModel.allPolls.collectAsStateWithLifecycle()
    val allProfiles by viewModel.allUserProfiles.collectAsStateWithLifecycle()

    val categories = listOf("Technology", "Food", "Lifestyle", "Sports", "Politics", "Entertainment", "Business", "Science", "Other")

    val (matchedPolls, matchedUsers) = remember(searchQuery, allPolls, allProfiles) {
        val q = searchQuery.trim().lowercase()
        if (q.isBlank()) {
            Pair(emptyList(), emptyList())
        } else {
            val polls = allPolls.filter { 
                it.question.lowercase().contains(q) || 
                it.category.lowercase().contains(q) 
            }
            val users = allProfiles.filter { 
                it.name.lowercase().contains(q) || 
                it.username.lowercase().contains(q) 
            }
            Pair(polls, users)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Search text outline input
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.updateSearchQuery(it) },
            placeholder = { Text("Search polls, keywords, users…", color = SoftGray) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "SearchIcon") },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = LimeDark,
                unfocusedBorderColor = LineMuted,
                unfocusedContainerColor = PanelWhite,
                focusedContainerColor = PanelWhite
            ),
            shape = RoundedCornerShape(99.dp),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (searchQuery.isBlank()) {
            Text(
                "Browse categories",
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Black,
                fontSize = 18.sp,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            // Categories pill list
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { cat ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(99.dp))
                            .border(1.dp, LineMuted, RoundedCornerShape(99.dp))
                            .background(PanelWhite)
                            .clickable { viewModel.updateSearchQuery(cat) }
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                    ) {
                        Text(cat, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = InkDark)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text(
                "Trending Tags",
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Black,
                fontSize = 18.sp,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            // Dynamic Topics computed from SQLite categories
            val tags = categories.shuffled().take(4)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .border(1.dp, LineMuted, RoundedCornerShape(18.dp))
                    .background(PanelWhite)
                    .padding(8.dp)
            ) {
                tags.forEachIndexed { idx, t ->
                    val num = allPolls.count { it.category == t }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.updateSearchQuery(t) }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${idx + 1}",
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            color = SoftGray,
                            modifier = Modifier.width(24.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text("#$t", fontWeight = FontWeight.Bold, fontSize = 14.5.sp)
                            Text("$num active discussions", fontSize = 12.sp, color = TextMuted)
                        }
                        Text("↗", fontWeight = FontWeight.Bold, color = GreenSuccess)
                    }
                    if (idx < tags.size - 1) {
                        Box(modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(LineLight))
                    }
                }
            }
        } else {
            // Search Results screen
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (matchedUsers.isNotEmpty()) {
                    item {
                        Text(
                            "People",
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp
                        )
                    }
                    items(matchedUsers) { user ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .border(1.dp, LineMuted, RoundedCornerShape(14.dp))
                                .background(PanelWhite)
                                .clickable { viewModel.navigateToUserProfile(user.username) }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            UserAvatar(user.name, user.avatar, user.color, size = 38)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(user.name, fontWeight = FontWeight.Bold, fontSize = 14.5.sp)
                                Text("@${user.username}", fontSize = 12.sp, color = TextMuted)
                            }
                        }
                    }
                }

                item {
                    Text(
                        "Polls",
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                if (matchedPolls.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No matching polls found", color = TextMuted)
                        }
                    }
                } else {
                    items(matchedPolls) { poll ->
                        PollCard(poll = poll, viewModel = viewModel)
                    }
                }
            }
        }
    }
}

@Composable
fun FollowingScreen(viewModel: LinkPollViewModel) {
    val allPolls by viewModel.allPolls.collectAsStateWithLifecycle()
    val followingPolls = remember(allPolls) { allPolls.filter { it.author.isFollowing } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            "Following Feed",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (followingPolls.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("👥", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        "Follow more creators on LinkPoll to construct a personalized feed stream!",
                        color = TextMuted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(followingPolls) { poll ->
                    PollCard(poll = poll, viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun MyPollsScreen(viewModel: LinkPollViewModel) {
    val allPolls by viewModel.allPolls.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()

    val myPolls = remember(allPolls, currentUser) {
        val cu = currentUser
        if (cu == null) emptyList() else allPolls.filter { it.author.username == cu.username }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            "My Discussions",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (currentUser == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🔒", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Register an account to publish and monitor your polls!", color = TextMuted)
                }
            }
        } else if (myPolls.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📊", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("You haven't initiated any discussions yet.", color = TextMuted)
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { viewModel.setCreatePollDialogVisible(true) },
                        colors = ButtonDefaults.buttonColors(containerColor = LimeAccent, contentColor = InkDark)
                    ) {
                        Text("Create a Poll")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(myPolls) { poll ->
                    PollCard(poll = poll, viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(viewModel: LinkPollViewModel) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()

    var nameVal by remember { mutableStateOf("") }
    var userVal by remember { mutableStateOf("") }
    var bioVal by remember { mutableStateOf("") }
    var locVal by remember { mutableStateOf("") }
    var webVal by remember { mutableStateOf("") }
    var colVal by remember { mutableStateOf("#15140f") }
    var avVal by remember { mutableStateOf("") }

    // Synchronize inputs when profile loads
    LaunchedEffect(currentUser) {
        currentUser?.let { me ->
            nameVal = me.name
            userVal = me.username
            bioVal = me.bio
            locVal = me.location
            webVal = me.website
            colVal = me.color
            avVal = me.avatar
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 40.dp)
    ) {
        item {
            Text(
                "Profile Setup",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black
            )
        }

        if (currentUser == null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = PanelWhite),
                    border = borderFromMuted()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            "Join LinkPoll",
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "Create an active identity to publish polls, vote, make inline comments, and react!",
                            color = TextMuted,
                            fontSize = 13.5.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text("FULL NAME", style = MaterialTheme.typography.labelLarge, color = TextMuted)
                        OutlinedTextField(
                            value = nameVal,
                            onValueChange = { nameVal = it },
                            placeholder = { Text("e.g. John Doe") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text("USERNAME", style = MaterialTheme.typography.labelLarge, color = TextMuted)
                        OutlinedTextField(
                            value = userVal,
                            onValueChange = { userVal = it },
                            placeholder = { Text("e.g. johndoe (omitting @)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                if (nameVal.isNotBlank() && userVal.isNotBlank()) {
                                    viewModel.register(nameVal, userVal)
                                } else {
                                    viewModel.showToast("Inputs must not be blank!")
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = LimeAccent, contentColor = InkDark),
                            shape = RoundedCornerShape(99.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Create Account", fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
        } else {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = PanelWhite),
                    border = borderFromMuted()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            UserAvatar(name = nameVal, avatarUrl = avVal, colorHex = colVal, size = 56)
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text("Edit Profile Information", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text("Matches layout updates in real-time.", fontSize = 12.sp, color = TextMuted)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text("FULL NAME", style = MaterialTheme.typography.labelLarge, color = TextMuted)
                        OutlinedTextField(
                            value = nameVal,
                            onValueChange = { nameVal = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text("BIO DESCRIPTION", style = MaterialTheme.typography.labelLarge, color = TextMuted)
                        OutlinedTextField(
                            value = bioVal,
                            onValueChange = { bioVal = it },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 3
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("LOCATION", style = MaterialTheme.typography.labelLarge, color = TextMuted)
                                OutlinedTextField(
                                    value = locVal,
                                    onValueChange = { locVal = it },
                                    singleLine = true
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("WEBSITE", style = MaterialTheme.typography.labelLarge, color = TextMuted)
                                OutlinedTextField(
                                    value = webVal,
                                    onValueChange = { webVal = it },
                                    singleLine = true
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text("AVATAR IMAGE URL", style = MaterialTheme.typography.labelLarge, color = TextMuted)
                        OutlinedTextField(
                            value = avVal,
                            onValueChange = { avVal = it },
                            placeholder = { Text("Paste valid HTTP path") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Text("PROFILE SWATCH ACCENT", style = MaterialTheme.typography.labelLarge, color = TextMuted)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val colors = listOf("#15140f", "#1f4d35", "#3c2a4d", "#3a1d1d", "#2e2416", "#142036")
                            colors.forEach { col ->
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(android.graphics.Color.parseColor(col)))
                                        .border(
                                            2.dp,
                                            if (colVal.lowercase() == col.lowercase()) LimeAccent else Color.Transparent,
                                            CircleShape
                                        )
                                        .clickable { colVal = col }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = {
                                viewModel.updateProfile(nameVal, userVal, bioVal, locVal, webVal, colVal, avVal)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = InkDark, contentColor = Color.White),
                            shape = RoundedCornerShape(99.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Save Changes", fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedButton(
                            onClick = { viewModel.logout() },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = RedAlert),
                            shape = RoundedCornerShape(99.dp),
                            border = BorderStroke(1.dp, RedAlert),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Sign Out Account")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PollDetailScreen(viewModel: LinkPollViewModel) {
    val activeCode by viewModel.activePollDetailCode.collectAsStateWithLifecycle()
    val allPolls by viewModel.allPolls.collectAsStateWithLifecycle()
    val allComments by viewModel.allComments.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()

    val poll = remember(activeCode, allPolls) { allPolls.find { it.code == activeCode } }
    val comments = remember(activeCode, allComments) { allComments.filter { it.pollCode == activeCode } }

    var commentText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            IconButton(onClick = { viewModel.goBack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = InkDark)
            }
            Text("Back to stream", color = InkDark, fontWeight = FontWeight.Bold)
        }

        if (poll == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Discussion not found or deleted.", color = TextMuted)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    PollCard(poll = poll, viewModel = viewModel)
                }

                item {
                    Text(
                        "${comments.size} comments",
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                // Inline writing block
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = PanelWhite),
                        border = borderFromMuted()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            UserAvatar(
                                name = currentUser?.name ?: "?",
                                avatarUrl = currentUser?.avatar ?: "",
                                colorHex = currentUser?.color ?: "#15140f",
                                size = 32
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            OutlinedTextField(
                                value = commentText,
                                onValueChange = { commentText = it },
                                placeholder = { Text("Write inline thoughts…", fontSize = 13.5.sp) },
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedBorderColor = Color.Transparent,
                                    focusedBorderColor = Color.Transparent,
                                    unfocusedContainerColor = ChipBg,
                                    focusedContainerColor = ChipBg
                                ),
                                shape = RoundedCornerShape(12.dp),
                                maxLines = 2
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            IconButton(
                                onClick = {
                                    if (commentText.isNotBlank()) {
                                        viewModel.postComment(poll.code, commentText)
                                        commentText = ""
                                    }
                                },
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(InkDark)
                                    .size(36.dp)
                            ) {
                                Icon(Icons.Default.Send, contentDescription = "Send", tint = LimeAccent, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }

                if (comments.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = PanelWhite),
                            border = borderFromMuted()
                        ) {
                            Text(
                                "No active discussion. Start the thread below!",
                                color = TextMuted,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(24.dp).fillMaxWidth(),
                                fontSize = 13.sp
                            )
                        }
                    }
                } else {
                    items(comments) { comment ->
                        CommentItem(comment = comment, viewModel = viewModel)
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileScreen(viewModel: LinkPollViewModel) {
    val activeUsername by viewModel.activeUserProfileUsername.collectAsStateWithLifecycle()
    val allProfiles by viewModel.allUserProfiles.collectAsStateWithLifecycle()
    val allPolls by viewModel.allPolls.collectAsStateWithLifecycle()

    val profile = remember(activeUsername, allProfiles) { allProfiles.find { it.username == activeUsername } }
    val profilePolls = remember(activeUsername, allPolls) { allPolls.filter { it.author.username == activeUsername } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            IconButton(onClick = { viewModel.goBack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = InkDark)
            }
            Text("Back", color = InkDark, fontWeight = FontWeight.Bold)
        }

        if (profile == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Profile not found.", color = TextMuted)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = PanelWhite),
                        shape = RoundedCornerShape(18.dp),
                        border = borderFromMuted()
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                UserAvatar(profile.name, profile.avatar, profile.color, size = 64)
                                if (!profile.isMe) {
                                    Button(
                                        onClick = { viewModel.toggleFollowUser(profile.username) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (profile.isFollowing) PanelWhite else InkDark,
                                            contentColor = if (profile.isFollowing) InkDark else Color.White
                                        ),
                                        border = if (profile.isFollowing) BorderStroke(1.dp, LineMuted) else null,
                                        shape = RoundedCornerShape(99.dp)
                                    ) {
                                        Text(if (profile.isFollowing) "Following" else "Follow", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                                    }
                                } else {
                                    OutlinedButton(
                                        onClick = { viewModel.navigateTo("settings") },
                                        shape = RoundedCornerShape(99.dp),
                                        border = BorderStroke(1.dp, LineMuted)
                                    ) {
                                        Text("Edit Settings", color = InkDark)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                profile.name,
                                style = MaterialTheme.typography.displayLarge,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text("@${profile.username}", fontSize = 14.sp, color = TextMuted)

                            if (profile.bio.isNotBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(profile.bio, fontSize = 14.5.sp, color = InkDark)
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Meta rows layout
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (profile.location.isNotBlank()) {
                                    Icon(Icons.Default.Place, contentDescription = "Loc", tint = SoftGray, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(profile.location, fontSize = 12.5.sp, color = TextMuted)
                                    Spacer(modifier = Modifier.width(16.dp))
                                }
                                if (profile.website.isNotBlank()) {
                                    Icon(Icons.Default.Share, contentDescription = "Web", tint = SoftGray, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(profile.website, fontSize = 12.5.sp, color = Color(0xFF3B6FD6), fontWeight = FontWeight.Bold)
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Counter rows
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                ProfileStatLabel(count = profile.pollCount, label = "polls")
                                ProfileStatLabel(count = profile.followers, label = "followers")
                                ProfileStatLabel(count = profile.following, label = "following")
                            }
                        }
                    }
                }

                item {
                    Text(
                        "Discussions Initiated",
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                if (profilePolls.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No discussions started by @${profile.username}", color = TextMuted)
                        }
                    }
                } else {
                    items(profilePolls) { poll ->
                        PollCard(poll = poll, viewModel = viewModel)
                    }
                }
            }
        }
    }
}

// Subordinate Layout Component: Individual Poll Card
@Composable
fun PollCard(poll: Poll, viewModel: LinkPollViewModel) {
    val clipboard = LocalClipboardManager.current
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { viewModel.navigateToPollDetail(poll.code) },
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = PanelWhite),
        border = borderFromMuted()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { viewModel.navigateToUserProfile(poll.author.username) },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    UserAvatar(poll.author.name, poll.author.avatar, poll.author.color, size = 40)
                    Spacer(modifier = Modifier.width(11.dp))
                    Column {
                        Text(
                            poll.author.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("@${poll.author.username}", fontSize = 12.5.sp, color = TextMuted)
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(modifier = Modifier
                                .size(3.dp)
                                .clip(CircleShape)
                                .background(SoftGray))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(formatTimeAgo(poll.createdAt), fontSize = 12.sp, color = SoftGray)
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(99.dp))
                        .background(ChipBg)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(poll.category, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Bold Title serif
            Text(
                poll.question,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                lineHeight = 25.sp,
                color = InkDark
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Options List
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                poll.options.forEach { opt ->
                    val optionPct = if (poll.totalVotes > 0) {
                        (opt.votes.toFloat() / poll.totalVotes * 100).toInt()
                    } else 0

                    val isChosenByMe = poll.myVotes.contains(opt.id)

                    if (poll.hasVoted) {
                        // Locked progressive bar layout
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .clip(RoundedCornerShape(13.dp))
                                .border(
                                    1.6.dp,
                                    if (isChosenByMe) InkDark else LineMuted,
                                    RoundedCornerShape(13.dp)
                                )
                                .background(PanelWhite)
                        ) {
                            // Progress filled background width using absolute ratios
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(fraction = optionPct.toFloat() / 100f)
                                    .background(if (isChosenByMe) LimeAccent else LimeAccent.copy(alpha = 0.32f))
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    if (isChosenByMe) {
                                        Text("✓ ", fontWeight = FontWeight.Black, fontSize = 14.sp)
                                    }
                                    Text(
                                        opt.label,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Text("$optionPct%", fontFamily = FontFamily.Serif, fontWeight = FontWeight.Black, fontSize = 14.sp)
                            }
                        }
                    } else {
                        // Clicking casting layout
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .clip(RoundedCornerShape(13.dp))
                                .border(1.6.dp, LineMuted, RoundedCornerShape(13.dp))
                                .background(PanelWhite)
                                .clickable { viewModel.vote(poll.code, opt.id) }
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(opt.label, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Sub-status row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Pulse live bullet
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(if (poll.hasVoted) TextMuted else RedAlert)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (poll.hasVoted) "Finished" else "Live",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (poll.hasVoted) TextMuted else RedAlert
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(modifier = Modifier
                    .size(3.dp)
                    .clip(CircleShape)
                    .background(LineMuted))
                Spacer(modifier = Modifier.width(8.dp))
                Text("${poll.totalVotes} votes", fontSize = 12.sp, color = TextMuted)
                
                if (poll.multiple) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(modifier = Modifier
                        .size(3.dp)
                        .clip(CircleShape)
                        .background(LineMuted))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("multi-choice", fontSize = 11.5.sp, color = TextMuted)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(LineLight)
            )
            Spacer(modifier = Modifier.height(10.dp))

            // Social Controls row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                EngagementButton(
                    icon = Icons.Default.Favorite,
                    active = poll.liked,
                    label = poll.likes.toString(),
                    activeColor = HeartPink,
                    onClick = { viewModel.toggleLike(poll.code) }
                )

                EngagementButton(
                    icon = Icons.Default.MailOutline,
                    active = false,
                    label = poll.comments.toString(),
                    onClick = { viewModel.navigateToPollDetail(poll.code) }
                )

                EngagementButton(
                    icon = Icons.Default.Share,
                    active = false,
                    label = "",
                    onClick = {
                        clipboard.setText(AnnotatedString(poll.shareUrl))
                        viewModel.showToast("Link copied to clipboard! Share it anywhere.")
                    }
                )

                EngagementButton(
                    icon = Icons.Default.FavoriteBorder, // matches Bookmark
                    active = poll.bookmarked,
                    label = "",
                    activeColor = InkDark,
                    onClick = { viewModel.toggleBookmark(poll.code) }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Emoji quick reactions bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val emojiReactions = listOf("🔥", "🤔", "😂", "😮", "❤️", "👍")
                emojiReactions.forEach { emo ->
                    val ct = poll.reactions[emo] ?: 0
                    val active = poll.myReaction == emo
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .border(
                                1.5.dp,
                                if (active) LimeDark else LineMuted,
                                RoundedCornerShape(20.dp)
                            )
                            .background(if (active) LimeAccent.copy(alpha = 0.2f) else WarmBg)
                            .clickable { viewModel.toggleReaction(poll.code, emo) }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(emo, fontSize = 13.sp)
                            if (ct > 0) {
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    ct.toString(),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextMuted
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// Subordinate Layout Component: Inline Nested Comment Rows
@Composable
fun CommentItem(comment: Comment, viewModel: LinkPollViewModel) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        UserAvatar(comment.name, comment.avatar, comment.color, size = 30)
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    comment.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.5.sp,
                    modifier = Modifier.clickable { viewModel.navigateToUserProfile(comment.username) }
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("@${comment.username} • ${formatTimeAgo(comment.createdAt)}", fontSize = 11.5.sp, color = SoftGray)
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(comment.body, fontSize = 14.sp, color = InkDark)
            
            // Nested interactions likes & del
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable { viewModel.toggleCommentLike(comment.id) }
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Icon(
                        Icons.Default.Favorite,
                        contentDescription = "LikeCom",
                        tint = if (comment.likedByMe) HeartPink else SoftGray,
                        modifier = Modifier.size(13.dp)
                    )
                    if (comment.likes > 0) {
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(comment.likes.toString(), fontSize = 11.sp, color = TextMuted)
                    }
                }
                
                if (comment.isMine) {
                    Spacer(modifier = Modifier.width(16.dp))
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "DelCom",
                        tint = SoftGray,
                        modifier = Modifier
                            .size(13.dp)
                            .clickable { viewModel.deleteComment(comment.id, comment.pollCode) }
                    )
                }
            }
        }
    }
}

// Subordinate Layout Component: Notifications List Drawer Panel
@Composable
fun NotificationsPanel(viewModel: LinkPollViewModel, onDismiss: () -> Unit) {
    val list by viewModel.notifications.collectAsStateWithLifecycle()
    
    // Clear alerts automatically when viewed
    LaunchedEffect(Unit) {
        viewModel.markNotificationsRead()
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(480.dp),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = PanelWhite),
            border = borderFromMuted()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Notifications",
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "CloseAlerts")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (list.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("No notifications yet.", color = TextMuted)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(list) { alert ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (alert.read) Color.Transparent else LimeAccent.copy(alpha = 0.08f))
                                    .padding(8.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                UserAvatar(alert.fromName, alert.fromAvatar, alert.fromColor, size = 32)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = when (alert.type) {
                                            "vote" -> "${alert.fromName} cast a vote on your poll!"
                                            "like" -> "${alert.fromName} liked your discussion."
                                            "comment" -> "${alert.fromName} added a thought to your thread."
                                            "follow" -> "${alert.fromName} started following your handle!"
                                            else -> "${alert.fromName} performed an action."
                                        },
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    alert.pollQuestion?.let { q ->
                                        Text(
                                            text = q,
                                            fontSize = 11.5.sp,
                                            color = TextMuted,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            fontStyle = FontStyle.Italic,
                                            modifier = Modifier.padding(top = 2.dp)
                                        )
                                    }
                                    Text(formatTimeAgo(alert.createdAt), fontSize = 10.sp, color = SoftGray, modifier = Modifier.padding(top = 2.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Subordinate Layout Component: Custom Poll Creator Window overlay
@Composable
fun CreatePollDialog(
    onDismiss: () -> Unit,
    onSubmit: (question: String, options: List<String>, category: String, durationH: Int?, multiple: Boolean, anonymous: Boolean) -> Unit
) {
    var question by remember { mutableStateOf("") }
    var rawOptions = remember { mutableStateListOf("", "") }
    var selectedCategory by remember { mutableStateOf("Technology") }
    
    var enableMultiChoice by remember { mutableStateOf(false) }
    var enableAnonymity by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = PanelWhite),
            border = borderFromMuted()
        ) {
            LazyColumn(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Create a Poll",
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Black,
                            fontSize = 22.sp
                        )
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "CloseCreator")
                        }
                    }
                }

                item {
                    Text("YOUR QUESTION", style = MaterialTheme.typography.labelLarge, color = TextMuted)
                    OutlinedTextField(
                        value = question,
                        onValueChange = { question = it },
                        placeholder = { Text("What do you want to ask the world?", fontSize = 13.5.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LimeDark,
                            unfocusedBorderColor = LineMuted
                        ),
                        maxLines = 3
                    )
                }

                item {
                    Text("ANSWER OPTIONS (2-6)", style = MaterialTheme.typography.labelLarge, color = TextMuted)
                }

                items(rawOptions.size) { index ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("::", color = SoftGray, modifier = Modifier.width(18.dp))
                        OutlinedTextField(
                            value = rawOptions[index],
                            onValueChange = { rawOptions[index] = it },
                            placeholder = { Text("Option ${index + 1}", fontSize = 13.5.sp) },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = LimeDark,
                                unfocusedBorderColor = LineMuted
                            ),
                            singleLine = true
                        )
                        if (rawOptions.size > 2) {
                            IconButton(onClick = { rawOptions.removeAt(index) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Remove", tint = SoftGray)
                            }
                        }
                    }
                }

                if (rawOptions.size < 6) {
                    item {
                        OutlinedButton(
                            onClick = { rawOptions.add("") },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.5.dp, LineMuted),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("+ Add Option", color = InkDark, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                item {
                    Text("CATEGORY", style = MaterialTheme.typography.labelLarge, color = TextMuted)
                    
                    // Simple select list
                    val categories = listOf("Technology", "Food", "Lifestyle", "Sports", "Politics", "Entertainment", "Business")
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(categories) { cat ->
                            val selected = selectedCategory == cat
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(99.dp))
                                    .border(1.dp, if (selected) InkDark else LineMuted, RoundedCornerShape(99.dp))
                                    .background(if (selected) LimeAccent else PanelWhite)
                                    .clickable { selectedCategory = cat }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(cat, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = InkDark)
                            }
                        }
                    }
                }

                item {
                    Text("SETTINGS", style = MaterialTheme.typography.labelLarge, color = TextMuted)
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Allow multiple selections", fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
                        Switch(
                            checked = enableMultiChoice,
                            onCheckedChange = { enableMultiChoice = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = InkDark)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Anonymous voting results", fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
                        Switch(
                            checked = enableAnonymity,
                            onCheckedChange = { enableAnonymity = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = InkDark)
                        )
                    }
                }

                item {
                    Button(
                        onClick = {
                            onSubmit(question, rawOptions.toList(), selectedCategory, 24, enableMultiChoice, enableAnonymity)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = LimeAccent, contentColor = InkDark),
                        shape = RoundedCornerShape(99.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Publish Poll", fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}

// Subordinate Component helpers

@Composable
fun StatsBlock(value: String, label: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(12.dp)
    ) {
        Text(
            text = value,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Black,
            fontSize = 22.sp,
            color = InkDark
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = TextMuted
        )
    }
}

@Composable
fun ProfileStatLabel(count: Int, label: String) {
    Row {
        Text(text = "$count", fontWeight = FontWeight.Black, fontFamily = FontFamily.Serif, fontSize = 14.5.sp, color = InkDark)
        Spacer(modifier = Modifier.width(3.dp))
        Text(text = label, fontSize = 13.5.sp, color = TextMuted)
    }
}

@Composable
fun EngagementButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    active: Boolean,
    label: String,
    activeColor: Color = LimeAccent,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (active) activeColor else SoftGray,
            modifier = Modifier.size(18.dp)
        )
        if (label.isNotBlank() && label != "0") {
            Spacer(modifier = Modifier.width(4.dp))
            Text(label, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (active) activeColor else TextMuted)
        }
    }
}

@Composable
fun UserAvatar(name: String, avatarUrl: String, colorHex: String, size: Int = 34) {
    val sizeDp = size.dp
    val textInitials = if (name.isNotBlank()) name.take(2).uppercase() else "?"
    
    Box(
        modifier = Modifier
            .size(sizeDp)
            .clip(CircleShape)
            .background(Color(android.graphics.Color.parseColor(colorHex.ifBlank { "#15140f" }))),
        contentAlignment = Alignment.Center
    ) {
        if (avatarUrl.isNotBlank()) {
            AsyncImage(
                model = avatarUrl,
                contentDescription = "Avatar",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
            )
        } else {
            Text(
                text = textInitials,
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Serif,
                fontSize = (size / 2.3).sp
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable () -> Unit
) {
    androidx.compose.foundation.layout.FlowRow(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement,
        verticalArrangement = verticalArrangement
    ) {
        content()
    }
}

// Utility formatting functions

private fun formatTimeAgo(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    val seconds = diff / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24
    return when {
        seconds < 60 -> "Just now"
        minutes < 60 -> "${minutes}m ago"
        hours < 24 -> "${hours}h ago"
        else -> "${days}d ago"
    }
}

private fun formatStatsCount(count: Int): String {
    return when {
        count >= 1_000_000 -> String.format("%.1fM", count / 1_000_000f).replace(".0", "")
        count >= 1_000 -> String.format("%.1fK", count / 1_000f).replace(".0", "")
        else -> count.toString()
    }
}

fun borderFromMuted() = BorderStroke(1.dp, LineMuted)
