package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.NewsComment
import com.example.data.UiState
import com.example.ui.i18n.LocalAppLanguage
import com.example.ui.i18n.tr

private data class NewsCommentThread(
    val parent: NewsComment,
    val replies: List<NewsComment>
)

@Composable
fun NewsCommentsSection(
    commentsState: UiState<List<NewsComment>>,
    currentUserId: String?,
    isGuest: Boolean,
    isAdmin: Boolean,
    onReplyClick: (CommentReplyTarget) -> Unit,
    onDeleteComment: (NewsComment) -> Unit,
    modifier: Modifier = Modifier
) {
    val comments = (commentsState as? UiState.Success)?.data ?: emptyList()

    val threads = remember(comments) {
        val byId = comments.associateBy { it.id }
        val topLevel = comments
            .filter { it.parentCommentId.isNullOrBlank() || !byId.containsKey(it.parentCommentId) }
            .sortedByDescending { it.createdAt?.seconds ?: Long.MAX_VALUE }
        val repliesByParent = comments
            .filter { !it.parentCommentId.isNullOrBlank() && byId.containsKey(it.parentCommentId) }
            .groupBy { it.parentCommentId!! }

        topLevel.map { parent ->
            val replies = repliesByParent[parent.id]
                .orEmpty()
                .sortedBy { it.createdAt?.seconds ?: Long.MAX_VALUE }
            NewsCommentThread(parent = parent, replies = replies)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("news_comments_section")
    ) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(modifier = Modifier.height(18.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.ChatBubbleOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = tr(
                    "Komen & Balasan (${comments.size})",
                    "Comments & Replies (${comments.size})"
                ),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        when (commentsState) {
            is UiState.Loading -> {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp)
                ) {
                    CircularProgressIndicator(
                        strokeWidth = 2.5.dp,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            is UiState.Error -> {
                Text(
                    text = commentsState.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            is UiState.Success -> {
                if (threads.isEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = tr(
                                "Belum ada komen lagi. Kongsikan pandangan atau doa anda mengenai berita ini!",
                                "No comments yet. Share your thoughts or blessings on this news post!"
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        threads.forEach { thread ->
                            val parent = thread.parent
                            NewsCommentCard(
                                comment = parent,
                                isReply = false,
                                canReply = !isGuest,
                                canDelete = !isGuest && (isAdmin || parent.authorId == currentUserId),
                                onReply = {
                                    onReplyClick(
                                        CommentReplyTarget(
                                            rootParentCommentId = parent.id,
                                            replyToAuthorName = parent.authorName
                                        )
                                    )
                                },
                                onDelete = { onDeleteComment(parent) }
                            )

                            if (thread.replies.isNotEmpty()) {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 24.dp)
                                ) {
                                    thread.replies.forEach { reply ->
                                        Row(modifier = Modifier.fillMaxWidth()) {
                                            Box(
                                                modifier = Modifier
                                                    .width(2.5.dp)
                                                    .height(48.dp)
                                                    .padding(top = 6.dp)
                                                    .background(
                                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                                                        RoundedCornerShape(2.dp)
                                                    )
                                            )
                                            Spacer(Modifier.width(10.dp))
                                            NewsCommentCard(
                                                comment = reply,
                                                isReply = true,
                                                canReply = !isGuest,
                                                canDelete = !isGuest && (isAdmin || reply.authorId == currentUserId),
                                                onReply = {
                                                    onReplyClick(
                                                        CommentReplyTarget(
                                                            rootParentCommentId = parent.id,
                                                            replyToAuthorName = reply.authorName
                                                        )
                                                    )
                                                },
                                                onDelete = { onDeleteComment(reply) },
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NewsCommentCard(
    comment: NewsComment,
    isReply: Boolean,
    canReply: Boolean,
    canDelete: Boolean,
    onReply: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lang = LocalAppLanguage.current
    val resolvedName = comment.authorName.trim().ifBlank { "Jemaat SIB Toburon" }
    val initials = resolvedName
        .split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercaseChar().toString() }
        .ifBlank { "ST" }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isReply) {
            MaterialTheme.colorScheme.surfaceContainerLow
        } else {
            MaterialTheme.colorScheme.surfaceContainerLowest
        },
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (isReply) 0.55f else 0.75f)
        ),
        modifier = modifier
            .fillMaxWidth()
            .testTag("news_comment_item_${comment.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (isReply) {
                        MaterialTheme.colorScheme.secondaryContainer
                    } else {
                        MaterialTheme.colorScheme.primaryContainer
                    },
                    modifier = Modifier.size(if (isReply) 28.dp else 32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = initials,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isReply) {
                                MaterialTheme.colorScheme.onSecondaryContainer
                            } else {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            }
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = resolvedName,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (comment.createdAt != null) {
                        Text(
                            text = formatIndonesianTimestamp(comment.createdAt, lang),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (canDelete) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("delete_news_comment_${comment.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = tr("Padam komen", "Delete comment"),
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(6.dp))

            if (!comment.replyToAuthorName.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                    modifier = Modifier.padding(bottom = 4.dp)
                ) {
                    Text(
                        text = "@${comment.replyToAuthorName}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Text(
                text = comment.text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (canReply) {
                Spacer(Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onReply)
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                        .testTag("reply_news_comment_${comment.id}")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Reply,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = tr("Balas", "Reply"),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun NewsCommentComposerBar(
    commentText: String,
    onCommentTextChange: (String) -> Unit,
    replyTarget: CommentReplyTarget?,
    onClearReplyTarget: () -> Unit,
    isGuest: Boolean,
    isPosting: Boolean,
    onSendClick: () -> Unit,
    inputFocusRequester: FocusRequester? = null,
    isInline: Boolean = false,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        tonalElevation = if (isInline) 1.dp else 3.dp,
        shape = if (isInline) RoundedCornerShape(16.dp) else RoundedCornerShape(0.dp),
        border = if (isInline) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) else null,
        modifier = modifier
            .fillMaxWidth()
            .then(if (isInline) Modifier else Modifier.imePadding())
            .testTag("news_comment_composer_bar")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            if (!isInline) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }

            if (isGuest) {
                Text(
                    text = tr(
                        "Log masuk dengan Google di tab Akaun untuk menulis komen atau membalas.",
                        "Sign in with Google in the Account tab to comment or reply."
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
                )
            } else {
                if (replyTarget != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f))
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                            .testTag("news_reply_banner")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Reply,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = tr(
                                    "Membalas ${replyTarget.replyToAuthorName}",
                                    "Replying to ${replyTarget.replyToAuthorName}"
                                ),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(
                            onClick = onClearReplyTarget,
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("news_cancel_reply_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = tr("Batal balasan", "Cancel reply"),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    OutlinedTextField(
                        value = commentText,
                        onValueChange = { if (it.length <= 500) onCommentTextChange(it) },
                        placeholder = {
                            Text(
                                text = if (replyTarget != null) {
                                    tr("Tulis balasan anda…", "Write your reply…")
                                } else {
                                    tr("Tulis komen pada berita ini…", "Write a comment on this news post…")
                                },
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        maxLines = 3,
                        shape = RoundedCornerShape(22.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .then(
                                if (inputFocusRequester != null) {
                                    Modifier.focusRequester(inputFocusRequester)
                                } else {
                                    Modifier
                                }
                            )
                            .testTag("news_comment_input")
                    )

                    val canSend = commentText.trim().isNotEmpty() && !isPosting
                    Surface(
                        shape = CircleShape,
                        color = if (canSend) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        }
                    ) {
                        IconButton(
                            onClick = onSendClick,
                            enabled = canSend,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("news_comment_send_button")
                        ) {
                            if (isPosting) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(20.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = tr("Hantar komen", "Send comment"),
                                    tint = if (canSend) {
                                        MaterialTheme.colorScheme.onPrimary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                    },
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
