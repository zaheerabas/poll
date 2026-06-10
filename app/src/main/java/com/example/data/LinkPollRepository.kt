package com.example.data

import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.util.UUID

class LinkPollRepository(private val pollDao: PollDao) {

    // Helper to generate custom short code for sharing
    private fun generateShortCode(): String {
        return UUID.randomUUID().toString().substring(0, 7)
    }

    // Reactively observe the currently logged-in user
    val currentUserSession: Flow<UserEntity?> = pollDao.observeAllUsers().map { list ->
        list.find { it.isMe }
    }

    // Reactive list of all followed usernames
    private val followedUsernamesFlow: Flow<Set<String>> = pollDao.observeAllFollows().map { list ->
        list.map { it.username }.toSet()
    }

    // Assemble profile presentation objects
    val allUserProfilesFlow: Flow<List<UserProfile>> = combine(
        pollDao.observeAllUsers(),
        followedUsernamesFlow,
        pollDao.observeAllPolls()
    ) { users, followedSet, polls ->
        users.map { u ->
            UserProfile(
                username = u.username,
                name = u.name,
                bio = u.bio,
                location = u.location,
                website = u.website,
                color = u.color,
                avatar = u.avatar,
                followers = u.followers + if (followedSet.contains(u.username) && !u.isMe) 1 else 0,
                following = u.following,
                pollCount = polls.count { it.authorUsername == u.username },
                isMe = u.isMe,
                isFollowing = followedSet.contains(u.username)
            )
        }
    }

    // Reactive list of all comments for presentation
    val allCommentsFlow: Flow<List<Comment>> = combine(
        pollDao.observeAllComments(),
        pollDao.observeAllCommentLikes(),
        currentUserSession
    ) { comments, commentLikes, me ->
        val myUsername = me?.username ?: ""
        comments.map { c ->
            val likesCount = commentLikes.count { it.commentId == c.id }
            val likedByMe = commentLikes.any { it.commentId == c.id && it.username == myUsername }
            Comment(
                id = c.id,
                pollCode = c.pollCode,
                name = c.name,
                username = c.username,
                color = c.color,
                avatar = c.avatar,
                body = c.body,
                createdAt = c.createdAt,
                likes = likesCount,
                likedByMe = likedByMe,
                isMine = c.username == myUsername
            )
        }
    }

    // Reactive notifications flow with unread support
    val allNotificationsFlow: Flow<List<NotificationEntity>> = pollDao.observeAllNotifications()

    // Highly reactive master stream assembling full dynamic Poll states (List-based combine)
    val allPollsFlow: Flow<List<Poll>> = combine(
        listOf(
            pollDao.observeAllPolls(),
            allUserProfilesFlow,
            pollDao.observeAllOptions(),
            pollDao.observeAllVotes(),
            pollDao.observeAllLikes(),
            pollDao.observeAllBookmarks(),
            pollDao.observeAllReactions(),
            currentUserSession
        )
    ) { array ->
        @Suppress("UNCHECKED_CAST")
        val polls = array[0] as List<PollEntity>
        @Suppress("UNCHECKED_CAST")
        val users = array[1] as List<UserProfile>
        @Suppress("UNCHECKED_CAST")
        val options = array[2] as List<PollOptionEntity>
        @Suppress("UNCHECKED_CAST")
        val votes = array[3] as List<PollVoteEntity>
        @Suppress("UNCHECKED_CAST")
        val likes = array[4] as List<PollLikeEntity>
        @Suppress("UNCHECKED_CAST")
        val bookmarks = array[5] as List<PollBookmarkEntity>
        @Suppress("UNCHECKED_CAST")
        val reactions = array[6] as List<PollReactionEntity>
        val me = array[7] as UserEntity?

        val myUsername = me?.username ?: ""

        polls.map { p ->
            val authorProfile = users.find { it.username == p.authorUsername } ?: UserProfile(
                username = p.authorUsername,
                name = "Deleted User",
                bio = "",
                location = "",
                website = "",
                color = "#76715f",
                avatar = "",
                followers = 0,
                following = 0,
                pollCount = 0,
                isMe = false,
                isFollowing = false
            )

            val pOptions = options.filter { it.pollCode == p.code }.map { o ->
                PollOption(id = o.id, label = o.label, votes = o.votes)
            }

            val pVotes = votes.filter { it.pollCode == p.code }
            val totalVotesCount = pOptions.sumOf { it.votes }
            val myPollVotes = pVotes.filter { it.username == myUsername }.map { it.optionId }
            val hasVotedState = myPollVotes.isNotEmpty()

            val pLikes = likes.filter { it.pollCode == p.code }
            val isLikedByMe = pLikes.any { it.username == myUsername }

            val pBookmarks = bookmarks.filter { it.pollCode == p.code }
            val isBookmarkedByMe = pBookmarks.any { it.username == myUsername }

            val pReactions = reactions.filter { it.pollCode == p.code }
            val reactionsMap = pReactions.groupBy { it.emoji }.mapValues { it.value.size }
            val myReactionChoice = pReactions.find { it.username == myUsername }?.emoji

            val shareUrlAddress = "https://linkpoll.aistudio/p/${p.code}"

            Poll(
                code = p.code,
                question = p.question,
                category = p.category,
                multiple = p.multiple,
                anonymous = p.anonymous,
                durationH = p.durationH,
                createdAt = p.createdAt,
                author = authorProfile,
                options = pOptions,
                totalVotes = totalVotesCount,
                hasVoted = hasVotedState,
                myVotes = myPollVotes,
                likes = p.likes + pLikes.size,
                liked = isLikedByMe,
                bookmarked = isBookmarkedByMe,
                comments = p.comments,
                shares = p.shares,
                reactions = reactionsMap,
                myReaction = myReactionChoice,
                shareUrl = shareUrlAddress
            )
        }
    }

    // --- Action Handlers ---

    suspend fun registerUser(name: String, usernameInput: String): UserEntity {
        val sanitized = usernameInput.trim().lowercase().replace("@", "")
        val username = if (sanitized.isEmpty()) "user_${System.currentTimeMillis() % 10000}" else sanitized
        
        // Remove prior 'isMe' status
        val currentMe = pollDao.getMe()
        if (currentMe != null) {
            pollDao.insertUser(currentMe.copy(isMe = false))
        }

        val newUser = UserEntity(
            username = username,
            name = name.ifBlank { "Anonymous Bear" },
            isMe = true,
            color = "#15140f"
        )
        pollDao.insertUser(newUser)
        return newUser
    }

    suspend fun updateUserProfile(
        name: String,
        username: String,
        bio: String,
        location: String,
        website: String,
        color: String,
        avatar: String
    ): UserEntity? {
        val currentMe = pollDao.getMe() ?: return null
        
        // If username changes, make sure it's valid, otherwise keep previous
        val originalUsername = currentMe.username
        val newUsername = username.trim().lowercase().replace("@", "")
        
        val updatedUser = currentMe.copy(
            name = name.ifBlank { currentMe.name },
            username = if (newUsername.isNotBlank()) newUsername else originalUsername,
            bio = bio,
            location = location,
            website = website,
            color = color,
            avatar = avatar
        )

        pollDao.insertUser(updatedUser)
        return updatedUser
    }

    suspend fun signOut() {
        val currentMe = pollDao.getMe()
        if (currentMe != null) {
            pollDao.insertUser(currentMe.copy(isMe = false))
        }
    }

    suspend fun createPoll(
        question: String,
        options: List<String>,
        category: String,
        durationH: Int?,
        multiple: Boolean,
        anonymous: Boolean
    ): PollEntity? {
        val me = pollDao.getMe() ?: return null
        val code = generateShortCode()
        
        val poll = PollEntity(
            code = code,
            question = question.trim(),
            category = category,
            durationH = durationH,
            multiple = multiple,
            anonymous = anonymous,
            authorUsername = me.username
        )
        pollDao.insertPoll(poll)

        options.filter { it.isNotBlank() }.forEach { optLabel ->
            pollDao.insertOption(
                PollOptionEntity(
                    pollCode = code,
                    label = optLabel.trim(),
                    votes = 0
                )
            )
        }

        return poll
    }

    suspend fun vote(pollCode: String, optionId: Long): Boolean {
        val me = pollDao.getMe() ?: return false
        val poll = pollDao.getPoll(pollCode) ?: return false
        
        val vote = PollVoteEntity(
            pollCode = pollCode,
            optionId = optionId,
            username = me.username
        )
        pollDao.insertVote(vote)
        pollDao.incrementOptionVote(optionId)

        // Increment poll comments or similar or trigger notify if author is not me
        if (poll.authorUsername != me.username && !poll.anonymous) {
            pollDao.insertNotification(
                NotificationEntity(
                    type = "vote",
                    fromName = me.name,
                    fromUsername = me.username,
                    fromColor = me.color,
                    fromAvatar = me.avatar,
                    pollCode = poll.code,
                    pollQuestion = poll.question
                )
            )
        }

        return true
    }

    suspend fun addLike(pollCode: String, username: String) {
        pollDao.insertLike(PollLikeEntity(pollCode = pollCode, username = username))
        // Trigger notification
        val poll = pollDao.getPoll(pollCode)
        if (poll != null && poll.authorUsername != username) {
            val user = pollDao.getUser(username)
            if (user != null) {
                pollDao.insertNotification(
                    NotificationEntity(
                        type = "like",
                        fromName = user.name,
                        fromUsername = user.username,
                        fromColor = user.color,
                        fromAvatar = user.avatar,
                        pollCode = poll.code,
                        pollQuestion = poll.question
                    )
                )
            }
        }
    }

    suspend fun removeLike(pollCode: String, username: String) {
        pollDao.deleteLike(pollCode, username)
    }

    suspend fun addBookmark(pollCode: String, username: String) {
        pollDao.insertBookmark(PollBookmarkEntity(pollCode = pollCode, username = username))
    }

    suspend fun removeBookmark(pollCode: String, username: String) {
        pollDao.deleteBookmark(pollCode, username)
    }

    suspend fun addReaction(pollCode: String, emoji: String, username: String) {
        // Clear past reaction of user for this poll to stick to one reaction
        pollDao.deleteReaction(pollCode, username) 
        pollDao.insertReaction(PollReactionEntity(pollCode = pollCode, emoji = emoji, username = username))
    }

    suspend fun removeReaction(pollCode: String, username: String) {
        pollDao.deleteReaction(pollCode, username)
    }

    suspend fun addComment(pollCode: String, body: String): CommentEntity? {
        val me = pollDao.getMe() ?: return null
        val poll = pollDao.getPoll(pollCode) ?: return null

        val comment = CommentEntity(
            pollCode = pollCode,
            name = me.name,
            username = me.username,
            color = me.color,
            avatar = me.avatar,
            body = body.trim()
        )
        pollDao.insertComment(comment)

        // Increment author count
        pollDao.updatePoll(poll.copy(comments = poll.comments + 1))

        // Notify author
        if (poll.authorUsername != me.username) {
            pollDao.insertNotification(
                NotificationEntity(
                    type = "comment",
                    fromName = me.name,
                    fromUsername = me.username,
                    fromColor = me.color,
                    fromAvatar = me.avatar,
                    pollCode = poll.code,
                    pollQuestion = poll.question
                )
            )
        }

        return comment
    }

    suspend fun deleteComment(commentId: Long, pollCode: String) {
        pollDao.deleteComment(commentId)
        val poll = pollDao.getPoll(pollCode)
        if (poll != null && poll.comments > 0) {
            pollDao.updatePoll(poll.copy(comments = poll.comments - 1))
        }
    }

    suspend fun addCommentLike(commentId: Long, username: String) {
        pollDao.insertCommentLike(CommentLikeEntity(commentId = commentId, username = username))
    }

    suspend fun removeCommentLike(commentId: Long, username: String) {
        pollDao.deleteCommentLike(commentId, username)
    }

    suspend fun followUser(targetUsername: String, myUsername: String) {
        pollDao.insertFollow(FollowEntity(username = targetUsername))
        
        // Notify user of follower
        val me = pollDao.getUser(myUsername)
        if (me != null) {
            pollDao.insertNotification(
                NotificationEntity(
                    type = "follow",
                    fromName = me.name,
                    fromUsername = me.username,
                    fromColor = me.color,
                    fromAvatar = me.avatar,
                    pollCode = null,
                    pollQuestion = null
                )
            )
        }
    }

    suspend fun unfollowUser(targetUsername: String) {
        pollDao.deleteFollow(targetUsername)
    }

    suspend fun clearAllNotifications() {
        pollDao.markNotificationsAsRead()
    }

    // --- Boot pre-population ---
    suspend fun prePopulateIfEmpty() {
        // Check if users exist in general
        val allUsers = pollDao.getUser("alex_dev")
        if (allUsers != null) {
            Log.d("LinkPollRepository", "Database already initialized.")
            return
        }

        Log.d("LinkPollRepository", "Pre-populating Database with default engaging data...")

        // 1. Create default creator accounts
        val alex = UserEntity(
            username = "alex_dev",
            name = "Alex Mercer",
            bio = "Kotlin Multiplatform nerd. Building clean stateful systems in Android ⚡",
            location = "Seattle, WA",
            website = "github.com/alexdev",
            color = "#1f4d35",
            followers = 1420,
            following = 412,
            pollCount = 5
        )
        val jen = UserEntity(
            username = "foodie_jen",
            name = "Jenna Cook",
            bio = "Food blogger, explorer, pizza critic 🍕 Let's argue about ingredients!",
            location = "Chicago, IL",
            website = "jencooks.com",
            color = "#3a1d1d",
            followers = 3405,
            following = 512,
            pollCount = 3
        )
        val prod = UserEntity(
            username = "productivity_guy",
            name = "Marcus Finch",
            bio = "Productivity writer. Flow-states, deep focus, and mechanical keyboards ⌨️",
            location = "London, UK",
            website = "finchflow.co",
            color = "#3c2a4d",
            followers = 920,
            following = 112,
            pollCount = 2
        )
        val sports = UserEntity(
            username = "sports_fan",
            name = "Danny G.",
            bio = "Premier league enthusiast, stats analyzer, and weekend striker ⚽",
            location = "Liverpool, UK",
            website = "linktr.ee/sportsdan",
            color = "#142036",
            followers = 480,
            following = 210,
            pollCount = 1
        )

        // Save mock authors
        pollDao.insertUser(alex)
        pollDao.insertUser(jen)
        pollDao.insertUser(prod)
        pollDao.insertUser(sports)

        // Create a default session account so user is logged in instantly
        val me = UserEntity(
            username = "opinion_hero",
            name = "Opinion Hero",
            bio = "Avid LinkPoll voter and discussion starter! Let the stats speak.",
            location = "Internet, Cloud",
            website = "linkpoll.aistudio",
            color = "#15140f",
            followers = 12,
            following = 8,
            isMe = true
        )
        pollDao.insertUser(me)

        // Add some default follows
        pollDao.insertFollow(FollowEntity("alex_dev"))
        pollDao.insertFollow(FollowEntity("foodie_jen"))

        // 2. Pre-populate Poll 1: Tech Stack
        val code1 = "tech26"
        val p1 = PollEntity(
            code = code1,
            question = "Which tech stack are you mastering in 2026? 🚀",
            category = "Technology",
            multiple = false,
            anonymous = false,
            durationH = 72,
            authorUsername = "alex_dev",
            likes = 82,
            comments = 2,
            shares = 18
        )
        pollDao.insertPoll(p1)
        pollDao.insertOption(PollOptionEntity(pollCode = code1, label = "Kotlin Multiplatform (KMP)", votes = 245))
        pollDao.insertOption(PollOptionEntity(pollCode = code1, label = "Jetpack Compose / SwiftUi", votes = 518))
        pollDao.insertOption(PollOptionEntity(pollCode = code1, label = "React / Solid.js / Next", votes = 391))
        pollDao.insertOption(PollOptionEntity(pollCode = code1, label = "Rust / Go / Spring Boot", votes = 142))

        // Prepopulate comments for Poll 1
        pollDao.insertComment(CommentEntity(pollCode = code1, name = "Jenna Cook", username = "foodie_jen", color = "#3a1d1d", avatar = "", body = "Kotlin Multiplatform is absolutely massive this year! Simple data layer sharing saves weeks.", createdAt = System.currentTimeMillis() - 7200000))
        pollDao.insertComment(CommentEntity(pollCode = code1, name = "Marcus Finch", username = "productivity_guy", color = "#3c2a4d", avatar = "", body = "Honestly, Rust for backend pipelines pairs crazy well with a declarative frontend. Speed is unmatched.", createdAt = System.currentTimeMillis() - 3600000))

        // Prepopulate Reactions for Poll 1
        pollDao.insertReaction(PollReactionEntity(pollCode = code1, emoji = "🔥", username = "foodie_jen"))
        pollDao.insertReaction(PollReactionEntity(pollCode = code1, emoji = "🔥", username = "productivity_guy"))

        // 3. Pre-populate Poll 2: Pineapple on pizza
        val code2 = "pizza26"
        val p2 = PollEntity(
            code = code2,
            question = "Is pineapple on pizza a culinary masterpiece or an absolute crime? 🍕",
            category = "Food",
            multiple = false,
            anonymous = true,
            durationH = 168,
            authorUsername = "foodie_jen",
            likes = 432,
            comments = 2,
            shares = 92
        )
        pollDao.insertPoll(p2)
        pollDao.insertOption(PollOptionEntity(pollCode = code2, label = "An absolute masterpiece! 😍", votes = 1251))
        pollDao.insertOption(PollOptionEntity(pollCode = code2, label = "A terrible crime! 🤮", votes = 1489))
        pollDao.insertOption(PollOptionEntity(pollCode = code2, label = "I eat it neutral but won't pay for it", votes = 281))

        pollDao.insertComment(CommentEntity(pollCode = code2, name = "Danny G.", username = "sports_fan", color = "#142036", avatar = "", body = "It's the sweetness combining with salty ham, chef's kiss! Jenna you need to try it with jalapeños too.", createdAt = System.currentTimeMillis() - 12000000))
        pollDao.insertComment(CommentEntity(pollCode = code2, name = "Alex Mercer", username = "alex_dev", color = "#1f4d35", avatar = "", body = "As long as it compiles in the oven I eat it.", createdAt = System.currentTimeMillis() - 6000000))

        // Reactions
        pollDao.insertReaction(PollReactionEntity(pollCode = code2, emoji = "😂", username = "alex_dev"))

        // 4. Pre-populate Poll 3: Work Focus Hours
        val code3 = "focus26"
        val p3 = PollEntity(
            code = code3,
            question = "How many hours of focused deep work do you get done organically daily?",
            category = "Lifestyle",
            multiple = false,
            anonymous = false,
            durationH = 24,
            authorUsername = "productivity_guy",
            likes = 120,
            comments = 1,
            shares = 12
        )
        pollDao.insertPoll(p3)
        pollDao.insertOption(PollOptionEntity(pollCode = code3, label = "1 - 3 hours (standard)", votes = 345))
        pollDao.insertOption(PollOptionEntity(pollCode = code3, label = "3 - 5 hours (productive)", votes = 512))
        pollDao.insertOption(PollOptionEntity(pollCode = code3, label = "5 - 7 hours (grind)", votes = 191))
        pollDao.insertOption(PollOptionEntity(pollCode = code3, label = "8+ hours (transcendence)", votes = 48))

        pollDao.insertComment(CommentEntity(pollCode = code3, name = "Alex Mercer", username = "alex_dev", color = "#1f4d35", avatar = "", body = "Five focused hours is already a massive victory, real screens-off focus is demanding.", createdAt = System.currentTimeMillis() - 5000000))

        // Assemble initial welcome notification for the current user mock session
        pollDao.insertNotification(
            NotificationEntity(
                type = "follow",
                fromName = "Alex Mercer",
                fromUsername = "alex_dev",
                fromColor = "#1f4d35",
                fromAvatar = "",
                createdAt = System.currentTimeMillis() - 10000
            )
        )
        pollDao.insertNotification(
            NotificationEntity(
                type = "like",
                fromName = "Jenna Cook",
                fromUsername = "foodie_jen",
                fromColor = "#3a1d1d",
                fromAvatar = "",
                pollCode = "tech26",
                pollQuestion = "Which tech stack are you mastering in 2026? 🚀",
                createdAt = System.currentTimeMillis() - 50000
            )
        )
    }
}
