package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PollDao {

    // --- Users ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun getUser(username: String): UserEntity?

    @Query("SELECT * FROM users WHERE isMe = 1 LIMIT 1")
    suspend fun getMe(): UserEntity?

    @Query("SELECT * FROM users")
    fun observeAllUsers(): Flow<List<UserEntity>>

    // --- Follows ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFollow(follow: FollowEntity)

    @Query("DELETE FROM follows WHERE username = :username")
    suspend fun deleteFollow(username: String)

    @Query("SELECT * FROM follows")
    fun observeAllFollows(): Flow<List<FollowEntity>>

    // --- Polls ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPoll(poll: PollEntity)

    @Query("SELECT * FROM polls WHERE code = :code LIMIT 1")
    suspend fun getPoll(code: String): PollEntity?

    @Query("DELETE FROM polls WHERE code = :code")
    suspend fun deletePoll(code: String)

    @Query("SELECT * FROM polls ORDER BY createdAt DESC")
    fun observeAllPolls(): Flow<List<PollEntity>>

    @Update
    suspend fun updatePoll(poll: PollEntity)

    // --- Options ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOption(option: PollOptionEntity)

    @Query("SELECT * FROM poll_options")
    fun observeAllOptions(): Flow<List<PollOptionEntity>>

    @Query("UPDATE poll_options SET votes = votes + 1 WHERE id = :optionId")
    suspend fun incrementOptionVote(optionId: Long)

    // --- Votes ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVote(vote: PollVoteEntity)

    @Query("SELECT * FROM poll_votes")
    fun observeAllVotes(): Flow<List<PollVoteEntity>>

    // --- Likes ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLike(like: PollLikeEntity)

    @Query("DELETE FROM poll_likes WHERE pollCode = :pollCode AND username = :username")
    suspend fun deleteLike(pollCode: String, username: String)

    @Query("SELECT * FROM poll_likes")
    fun observeAllLikes(): Flow<List<PollLikeEntity>>

    // --- Bookmarks ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: PollBookmarkEntity)

    @Query("DELETE FROM poll_bookmarks WHERE pollCode = :pollCode AND username = :username")
    suspend fun deleteBookmark(pollCode: String, username: String)

    @Query("SELECT * FROM poll_bookmarks")
    fun observeAllBookmarks(): Flow<List<PollBookmarkEntity>>

    // --- Reactions ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReaction(reaction: PollReactionEntity)

    @Query("DELETE FROM poll_reactions WHERE pollCode = :pollCode AND username = :username")
    suspend fun deleteReaction(pollCode: String, username: String)

    @Query("SELECT * FROM poll_reactions")
    fun observeAllReactions(): Flow<List<PollReactionEntity>>

    // --- Comments ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComment(comment: CommentEntity)

    @Query("DELETE FROM comments WHERE id = :commentId")
    suspend fun deleteComment(commentId: Long)

    @Query("SELECT * FROM comments ORDER BY createdAt DESC")
    fun observeAllComments(): Flow<List<CommentEntity>>

    // --- Comment Likes ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCommentLike(like: CommentLikeEntity)

    @Query("DELETE FROM comment_likes WHERE commentId = :commentId AND username = :username")
    suspend fun deleteCommentLike(commentId: Long, username: String)

    @Query("SELECT * FROM comment_likes")
    fun observeAllCommentLikes(): Flow<List<CommentLikeEntity>>

    // --- Notifications ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationEntity)

    @Query("UPDATE notifications SET read = 1 WHERE read = 0")
    suspend fun markNotificationsAsRead()

    @Query("SELECT * FROM notifications ORDER BY createdAt DESC")
    fun observeAllNotifications(): Flow<List<NotificationEntity>>
}
