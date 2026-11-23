package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.GalleryComment
import com.example.data.UiState
import com.example.ui.i18n.LocalAppLanguage
import com.example.ui.i18n.tr

data class CommentReplyTarget(
    val rootParentCommentId: String,
    val replyToAuthorName: String
)

private data class CommentThread(
    val parent: GalleryComment,
    val replies: List<GalleryComment>
)

@Composable
fun GalleryCommentsSection(
    commentsState: UiState<List<GalleryComment>>,
    currentUserId: String?,
    isGuest: Boolean,
    isAdmin: Boolean,
    onReplyClick: (CommentReplyTarget) -> Unit,
    onDeleteComment: (GalleryComment) -> Unit,
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
            CommentThread(parent = parent, replies = replies)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("gallery_comments_section")
    ) {
        HorizontalDivider(color = Color.White.copy(alpha = 0.14f))
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.ChatBubbleOutline,
                contentDescription = null,
                tint = Color(0xFFF2B86B),
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = tr(
                    "Komen & Balasan (${comments.size})",
                    "Comments & Replies (${comments.size})"
                ),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
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
                        color = Color(0xFFF2B86B),
                        strokeWidth = 2.5.dp,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            is UiState.Error -> {
                Text(
                    text = commentsState.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFFF8A80)
                )
            }

            is UiState.Success -> {
                if (threads.isEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color.White.copy(alpha = 0.06f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = tr(
                                "Belum ada komen lagi. Kongsikan ucapan atau kata semangat anda di bawah!",
                                "No comments yet. Share a blessing or message below!"
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.68f),
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        threads.forEach { thread ->
                            val parent = thread.parent
                            GalleryCommentCard(
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
                                                    .width(2.dp)
                                                    .height(48.dp)
                                                    .padding(top = 6.dp)
                                                    .background(
                                                        Color(0x66F2B86B),
                                                        RoundedCornerShape(2.dp)
                                                    )
                                            )
                                            Spacer(Modifier.width(10.dp))
                                            GalleryCommentCard(
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
private fun GalleryCommentCard(
    comment: GalleryComment,
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
        color = if (isReply) Color.White.copy(alpha = 0.055f) else Color.White.copy(alpha = 0.085f),
        modifier = modifier
            .fillMaxWidth()
            .testTag("gallery_comment_item_${comment.id}")
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
                    color = if (isReply) Color(0x44F2B86B) else Color(0xFF1B4332),
                    modifier = Modifier.size(if (isReply) 28.dp else 32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = initials,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isReply) Color(0xFFF2B86B) else Color.White
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = resolvedName,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (comment.createdAt != null) {
                        Text(
                            text = formatIndonesianTimestamp(comment.createdAt, lang),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.56f)
                        )
                    }
                }

                if (canDelete) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("delete_comment_${comment.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = tr("Padam komen", "Delete comment"),
                            tint = Color(0xFFFF8A80),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(6.dp))

            if (!comment.replyToAuthorName.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0x28F2B86B),
                    modifier = Modifier.padding(bottom = 4.dp)
                ) {
                    Text(
                        text = "@${comment.replyToAuthorName}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFF2B86B),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Text(
                text = comment.text,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.92f)
            )

            if (canReply) {
                Spacer(Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onReply)
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                        .testTag("reply_comment_${comment.id}")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Reply,
                        contentDescription = null,
                        tint = Color(0xFFF2B86B),
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = tr("Balas", "Reply"),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFF2B86B)
                    )
                }
            }
        }
    }
}

@Composable
fun GalleryCommentComposerBar(
    commentText: String,
    onCommentTextChange: (String) -> Unit,
    replyTarget: CommentReplyTarget?,
    onClearReplyTarget: () -> Unit,
    isGuest: Boolean,
    isPosting: Boolean,
    onSendClick: () -> Unit,
    inputFocusRequester: FocusRequester? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color(0xFF0E1816),
        tonalElevation = 4.dp,
        modifier = modifier
            .fillMaxWidth()
            .testTag("gallery_comment_composer_bar")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            HorizontalDivider(color = Color.White.copy(alpha = 0.14f))

            if (isGuest) {
                Text(
                    text = tr(
                        "Log masuk dengan Google di tab Akaun untuk menulis komen atau membalas.",
                        "Sign in with Google in the Account tab to comment or reply."
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.72f),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
                )
            } else {
                if (replyTarget != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0x26F2B86B))
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                            .testTag("gallery_reply_banner")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Reply,
                                contentDescription = null,
                                tint = Color(0xFFF2B86B),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = tr(
                                    "Membalas ${replyTarget.replyToAuthorName}",
                                    "Replying to ${replyTarget.replyToAuthorName}"
                                ),
                                style = MaterialTheme.typography.labelMedium,
                                color = Color(0xFFF2B86B),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(
                            onClick = onClearReplyTarget,
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("gallery_cancel_reply_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = tr("Batal balasan", "Cancel reply"),
                                tint = Color.White.copy(alpha = 0.85f),
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
                                    tr("Tulis komen pada siaran ini…", "Write a comment on this post…")
                                },
                                color = Color.White.copy(alpha = 0.52f),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        maxLines = 3,
                        shape = RoundedCornerShape(22.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFFF2B86B),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.24f),
                            focusedContainerColor = Color.White.copy(alpha = 0.08f),
                            unfocusedContainerColor = Color.White.copy(alpha = 0.06f),
                            cursorColor = Color(0xFFF2B86B)
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
                            .testTag("gallery_comment_input")
                    )

                    val canSend = commentText.trim().isNotEmpty() && !isPosting
                    Surface(
                        shape = CircleShape,
                        color = if (canSend) Color(0xFFF2B86B) else Color.White.copy(alpha = 0.14f)
                    ) {
                        IconButton(
                            onClick = onSendClick,
                            enabled = canSend,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("gallery_comment_send_button")
                        ) {
                            if (isPosting) {
                                CircularProgressIndicator(
                                    color = Color(0xFF0E1816),
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(20.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = tr("Hantar komen", "Send comment"),
                                    tint = if (canSend) Color(0xFF0E1816) else Color.White.copy(alpha = 0.45f),
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
