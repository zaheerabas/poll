package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val username: String,
    val name: String,
    val bio: String = "",
    val location: String = "",
    val website: String = "",
    val color: String = "#15140f",
    val avatar: String = "",
    val followers: Int = 0,
    val following: Int = 0,
    val pollCount: Int = 0,
    val isMe: Boolean = false
)

@Entity(tableName = "follows")
data class FollowEntity(
    @PrimaryKey val username: String
)

@Entity(tableName = "polls")
data class PollEntity(
    @PrimaryKey val code: String,
    val question: String,
    val category: String,
    val multiple: Boolean = false,
    val anonymous: Boolean = false,
    val durationH: Int? = null, // null for no limit
    val createdAt: Long = System.currentTimeMillis(),
    val authorUsername: String,
    val likes: Int = 0,
    val comments: Int = 0,
    val shares: Int = 0
)

@Entity(tableName = "poll_options")
data class PollOptionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val pollCode: String,
    val label: String,
    val votes: Int = 0
)

@Entity(tableName = "poll_votes")
data class PollVoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val pollCode: String,
    val optionId: Long,
    val username: String
)

@Entity(tableName = "poll_likes")
data class PollLikeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val pollCode: String,
    val username: String
)

@Entity(tableName = "poll_bookmarks")
data class PollBookmarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val pollCode: String,
    val username: String
)

@Entity(tableName = "poll_reactions")
data class PollReactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val pollCode: String,
    val emoji: String,
    val username: String
)

@Entity(tableName = "comments")
data class CommentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val pollCode: String,
    val name: String,
    val username: String,
    val color: String,
    val avatar: String,
    val body: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "comment_likes")
data class CommentLikeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val commentId: Long,
    val username: String
)

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String, // "vote", "like", "comment", "follow"
    val fromName: String,
    val fromUsername: String,
    val fromColor: String,
    val fromAvatar: String,
    val pollCode: String? = null,
    val pollQuestion: String? = null,
    val read: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

// UI Presentation Models assembled from entities on-the-fly
data class UserProfile(
    val username: String,
    val name: String,
    val bio: String,
    val location: String,
    val website: String,
    val color: String,
    val avatar: String,
    val followers: Int,
    val following: Int,
    val pollCount: Int,
    val isMe: Boolean,
    val isFollowing: Boolean = false
)

data class PollOption(
    val id: Long,
    val label: String,
    val votes: Int
)

data class Poll(
    val code: String,
    val question: String,
    val category: String,
    val multiple: Boolean,
    val anonymous: Boolean,
    val durationH: Int?,
    val createdAt: Long,
    val author: UserProfile,
    val options: List<PollOption>,
    val totalVotes: Int,
    val hasVoted: Boolean,
    val myVotes: List<Long>,
    val likes: Int,
    val liked: Boolean,
    val bookmarked: Boolean,
    val comments: Int,
    val shares: Int,
    val reactions: Map<String, Int>, // emoji -> count
    val myReaction: String?,
    val shareUrl: String
)

data class Comment(
    val id: Long,
    val pollCode: String,
    val name: String,
    val username: String,
    val color: String,
    val avatar: String,
    val body: String,
    val createdAt: Long,
    val likes: Int,
    val likedByMe: Boolean,
    val isMine: Boolean
)
