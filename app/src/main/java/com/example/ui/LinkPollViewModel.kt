package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class LinkPollViewModel(application: Application) : AndroidViewModel(application) {

    private val database = PollDatabase.getDatabase(application)
    private val repository = LinkPollRepository(database.pollDao())

    // --- Core Database Streams ---
    val currentUser: StateFlow<UserEntity?> = repository.currentUserSession
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val allUserProfiles: StateFlow<List<UserProfile>> = repository.allUserProfilesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPolls: StateFlow<List<Poll>> = repository.allPollsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allComments: StateFlow<List<Comment>> = repository.allCommentsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notifications: StateFlow<List<NotificationEntity>> = repository.allNotificationsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unreadNotificationsCount: StateFlow<Int> = notifications
        .map { list -> list.count { !it.read } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // --- UI Control States ---
    private val _feedSelectedTab = MutableStateFlow("foryou")
    val feedSelectedTab: StateFlow<String> = _feedSelectedTab.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _activeRoute = MutableStateFlow("home")
    val activeRoute: StateFlow<String> = _activeRoute.asStateFlow()

    private val _activePollDetailCode = MutableStateFlow<String?>(null)
    val activePollDetailCode: StateFlow<String?> = _activePollDetailCode.asStateFlow()

    private val _activeUserProfileUsername = MutableStateFlow<String?>(null)
    val activeUserProfileUsername: StateFlow<String?> = _activeUserProfileUsername.asStateFlow()

    private val _isCreatePollDialogShowing = MutableStateFlow(false)
    val isCreatePollDialogShowing: StateFlow<Boolean> = _isCreatePollDialogShowing.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    // Route history for clean back-press navigation
    private val routeStack = mutableListOf<String>()

    init {
        viewModelScope.launch {
            repository.prePopulateIfEmpty()
        }
    }

    // --- Navigation ---
    fun navigateTo(route: String) {
        if (route != _activeRoute.value) {
            routeStack.add(_activeRoute.value)
            _activeRoute.value = route
        }
    }

    fun navigateToPollDetail(code: String) {
        _activePollDetailCode.value = code
        navigateTo("detail")
    }

    fun navigateToUserProfile(username: String) {
        _activeUserProfileUsername.value = username
        navigateTo("profile")
    }

    fun goBack(): Boolean {
        if (routeStack.isNotEmpty()) {
            val prev = routeStack.removeAt(routeStack.size - 1)
            _activeRoute.value = prev
            return true
        }
        return false
    }

    // --- Setters & Dialog Controls ---
    fun selectFeedTab(tab: String) {
        _feedSelectedTab.value = tab
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setCreatePollDialogVisible(visible: Boolean) {
        _isCreatePollDialogShowing.value = visible
    }

    fun showToast(msg: String) {
        _toastMessage.value = msg
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    // --- Action Implementations ---

    fun register(name: String, username: String) {
        viewModelScope.launch {
            try {
                repository.registerUser(name, username)
                showToast("Welcome aboard! 🎉")
            } catch (e: Exception) {
                showToast(e.message ?: "Could not register")
            }
        }
    }

    fun updateProfile(
        name: String,
        username: String,
        bio: String,
        location: String,
        website: String,
        color: String,
        avatar: String
    ) {
        viewModelScope.launch {
            try {
                val updated = repository.updateUserProfile(name, username, bio, location, website, color, avatar)
                if (updated != null) {
                    showToast("Profile updated successfully!")
                } else {
                    showToast("Failed to update profile")
                }
            } catch (e: Exception) {
                showToast(e.message ?: "Error updating profile")
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.signOut()
            showToast("Signed out.")
            _activeRoute.value = "home"
        }
    }

    fun createPoll(
        question: String,
        options: List<String>,
        category: String,
        durationH: Int?,
        multiple: Boolean,
        anonymous: Boolean
    ) {
        viewModelScope.launch {
            if (question.trim().length < 3) {
                showToast("Write a longer question!")
                return@launch
            }
            val validOptions = options.map { it.trim() }.filter { it.isNotBlank() }
            if (validOptions.size < 2) {
                showToast("Add at least 2 options!")
                return@launch
            }
            try {
                val poll = repository.createPoll(question, validOptions, category, durationH, multiple, anonymous)
                if (poll != null) {
                    showToast("Poll posted! 🚀")
                    setCreatePollDialogVisible(false)
                    navigateToPollDetail(poll.code)
                } else {
                    showToast("Must be signed in to post a poll!")
                }
            } catch (e: Exception) {
                showToast(e.message ?: "Error creating poll")
            }
        }
    }

    fun vote(pollCode: String, optionId: Long) {
        viewModelScope.launch {
            val user = currentUser.value
            if (user == null) {
                showToast("Register an account to vote! 👥")
                navigateTo("settings")
                return@launch
            }
            val success = repository.vote(pollCode, optionId)
            if (success) {
                showToast("Vote counted! 📊")
            } else {
                showToast("Failed to vote")
            }
        }
    }

    fun toggleLike(pollCode: String) {
        viewModelScope.launch {
            val me = currentUser.value
            if (me == null) {
                showToast("Join LinkPoll to like! ❤️")
                navigateTo("settings")
                return@launch
            }
            val target = allPolls.value.find { it.code == pollCode } ?: return@launch
            if (target.liked) {
                repository.removeLike(pollCode, me.username)
            } else {
                repository.addLike(pollCode, me.username)
            }
        }
    }

    fun toggleBookmark(pollCode: String) {
        viewModelScope.launch {
            val me = currentUser.value
            if (me == null) {
                showToast("Join LinkPoll to bookmark! 🔖")
                navigateTo("settings")
                return@launch
            }
            val target = allPolls.value.find { it.code == pollCode } ?: return@launch
            if (target.bookmarked) {
                repository.removeBookmark(pollCode, me.username)
                showToast("Removed from bookmarks")
            } else {
                repository.addBookmark(pollCode, me.username)
                showToast("Saved to bookmarks!")
            }
        }
    }

    fun toggleReaction(pollCode: String, emoji: String) {
        viewModelScope.launch {
            val me = currentUser.value
            if (me == null) {
                showToast("Join LinkPoll to react! ✨")
                navigateTo("settings")
                return@launch
            }
            val target = allPolls.value.find { it.code == pollCode } ?: return@launch
            if (target.myReaction == emoji) {
                repository.removeReaction(pollCode, me.username)
            } else {
                repository.addReaction(pollCode, emoji, me.username)
            }
        }
    }

    fun postComment(pollCode: String, body: String) {
        viewModelScope.launch {
            if (body.trim().isBlank()) return@launch
            val user = currentUser.value
            if (user == null) {
                showToast("Join LinkPoll to post comments!")
                navigateTo("settings")
                return@launch
            }
            try {
                val c = repository.addComment(pollCode, body)
                if (c != null) {
                    showToast("Comment posted!")
                }
            } catch (e: Exception) {
                showToast(e.message ?: "Error commenting")
            }
        }
    }

    fun deleteComment(commentId: Long, pollCode: String) {
        viewModelScope.launch {
            try {
                repository.deleteComment(commentId, pollCode)
                showToast("Comment deleted.")
            } catch (e: Exception) {
                showToast(e.message ?: "Failed to delete comment")
            }
        }
    }

    fun toggleCommentLike(commentId: Long) {
        viewModelScope.launch {
            val me = currentUser.value
            if (me == null) {
                showToast("Join LinkPoll to like comments!")
                navigateTo("settings")
                return@launch
            }
            val activeComments = allComments.value
            val target = activeComments.find { it.id == commentId } ?: return@launch
            if (target.likedByMe) {
                repository.removeCommentLike(commentId, me.username)
            } else {
                repository.addCommentLike(commentId, me.username)
            }
        }
    }

    fun toggleFollowUser(targetUsername: String) {
        viewModelScope.launch {
            val me = currentUser.value
            if (me == null) {
                showToast("Sign in to follow creators! 👥")
                navigateTo("settings")
                return@launch
            }
            if (targetUsername == me.username) {
                showToast("You can't follow yourself!")
                return@launch
            }
            val targetProfile = allUserProfiles.value.find { it.username == targetUsername } ?: return@launch
            if (targetProfile.isFollowing) {
                repository.unfollowUser(targetUsername)
                showToast("Unfollowed @$targetUsername")
            } else {
                repository.followUser(targetUsername, me.username)
                showToast("Following @$targetUsername!")
            }
        }
    }

    fun markNotificationsRead() {
        viewModelScope.launch {
            repository.clearAllNotifications()
        }
    }
}
