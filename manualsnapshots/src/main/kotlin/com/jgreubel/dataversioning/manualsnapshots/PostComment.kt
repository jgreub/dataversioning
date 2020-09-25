package com.jgreubel.dataversioning.manualsnapshots

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Component
import java.time.Instant
import javax.persistence.*

/****************/
/** Post Stuff **/
/****************/

// *** Business Layer *** //

data class Post(
    val id: Long,
    val content: String,
    val comments: List<Comment>
)

interface PostRepository {
    fun findOne(id: Long): Post
    fun create(content: String): Post
    fun edit(id: Long, newContent: String): Post
    fun addComment(id: Long, comment: Comment): Post
    fun delete(id: Long)
}

// *** Data Layer *** //

@Entity
data class PostIdentifier(
    @Id
    @GeneratedValue
    val id: Long
)

@Entity
data class PostSnapshot(
    @Id
    @GeneratedValue
    val versionId: Long,
    val content: String,
    @ManyToMany
    val commentIdentifiers: List<CommentIdentifier>,

    /** Audit Properties **/
    @ManyToOne(optional = false)
    val postIdentifier: PostIdentifier,
    val createdDateTime: Instant,
    val createdBy: String,
    val deleted: Boolean
)

interface PostIdentifierRepository : JpaRepository<PostIdentifier, Long>
interface PostSnapshotRepository : JpaRepository<PostSnapshot, Long> {
    fun findTop1ByPostIdentifierIdAndDeletedFalseOrderByCreatedDateTimeDesc(id: Long): PostSnapshot?
}

@Component
class PostSnapshotService(
    private val postIdentifierRepository: PostIdentifierRepository,
    private val postSnapshotRepository: PostSnapshotRepository,
    private val commentSnapshotService: CommentSnapshotService,
    private val userService: UserService
): PostRepository {

    override fun findOne(id: Long): Post {
        val latestVersion = postSnapshotRepository.findTop1ByPostIdentifierIdAndDeletedFalseOrderByCreatedDateTimeDesc(id)
            ?: throw RuntimeException("Not found")

        return convertToPost(latestVersion)
    }

    override fun create(content: String): Post {
        val postIdentifier = postIdentifierRepository.save(
            PostIdentifier(id = 0)
        )

        val postVersion = postSnapshotRepository.save(
            PostSnapshot(
                versionId = 0,
                content = content,
                commentIdentifiers = listOf(),
                /** Audit Properties **/
                postIdentifier = postIdentifier,
                createdDateTime = Instant.now(),
                createdBy = userService.getCurrentUsername(),
                deleted = false
            )
        )

        return convertToPost(postVersion)
    }

    override fun edit(id: Long, newContent: String): Post {
        val latestVersion = postSnapshotRepository.findTop1ByPostIdentifierIdAndDeletedFalseOrderByCreatedDateTimeDesc(id)
            ?: throw RuntimeException("Not found")

        val newVersion = postSnapshotRepository.save(
            PostSnapshot(
                versionId = 0,
                content = newContent,
                // JPA Weirdness - Notice the .toList() required for Hibernate
                commentIdentifiers = latestVersion.commentIdentifiers.toList(),
                /** Audit Properties **/
                postIdentifier = latestVersion.postIdentifier,
                createdDateTime = Instant.now(),
                createdBy = userService.getCurrentUsername(),
                deleted = false
            )
        )

        return convertToPost(newVersion)
    }

    override fun addComment(id: Long, comment: Comment): Post {
        val latestVersion = postSnapshotRepository.findTop1ByPostIdentifierIdAndDeletedFalseOrderByCreatedDateTimeDesc(id)
            ?: throw RuntimeException("Not found")

        val newCommentIdentifiers =
            latestVersion.commentIdentifiers + CommentIdentifier(id = comment.id)

        val newVersion = postSnapshotRepository.save(
            PostSnapshot(
                versionId = 0,
                content = latestVersion.content,
                commentIdentifiers = newCommentIdentifiers,
                /** Audit Properties **/
                postIdentifier = latestVersion.postIdentifier,
                createdDateTime = Instant.now(),
                createdBy = userService.getCurrentUsername(),
                deleted = false
            )
        )

        return convertToPost(newVersion)
    }

    override fun delete(id: Long) {
        val latestVersion = postSnapshotRepository.findTop1ByPostIdentifierIdAndDeletedFalseOrderByCreatedDateTimeDesc(id)
            ?: throw RuntimeException("Not found")

        postSnapshotRepository.save(
            PostSnapshot(
                versionId = 0,
                content = latestVersion.content,
                // JPA Weirdness - Notice the .toList() required for Hibernate
                commentIdentifiers = latestVersion.commentIdentifiers.toList(),
                /** Audit Properties **/
                postIdentifier = latestVersion.postIdentifier,
                createdDateTime = Instant.now(),
                createdBy = userService.getCurrentUsername(),
                deleted = true
            )
        )
    }

    // Helpers

    private fun convertToPost(postSnapshot: PostSnapshot): Post {
        return Post(
            id = postSnapshot.postIdentifier.id,
            content = postSnapshot.content,
            comments = postSnapshot.commentIdentifiers.map {
                commentSnapshotService.findOne(it.id)
            }
        )
    }

}

/*******************/
/** Comment Stuff **/
/*******************/

// *** Business Layer *** //

data class Comment(
    val id: Long,
    val content: String
)

interface CommentRepository {
    fun findOne(id: Long): Comment
    fun create(content: String): Comment
    fun edit(id: Long, newContent: String): Comment
    fun delete(id: Long)
}

// *** Data Layer *** //

@Entity
data class CommentIdentifier(
    @Id
    @GeneratedValue
    val id: Long
)

@Entity
data class CommentSnapshot(
    @Id
    @GeneratedValue
    val versionId: Long,
    val content: String,

    /** Audit Properties **/
    @ManyToOne(optional = false)
    val commentIdentifier: CommentIdentifier,
    val createdDateTime: Instant,
    val createdBy: String,
    val deleted: Boolean
)

interface CommentIdentifierRepository : JpaRepository<CommentIdentifier, Long>
interface CommentSnapshotRepository : JpaRepository<CommentSnapshot, Long> {
    fun findTop1ByCommentIdentifierIdAndDeletedFalseOrderByCreatedDateTimeDesc(id: Long): CommentSnapshot?
}

@Component
class CommentSnapshotService(
    private val commentIdentifierRepository: CommentIdentifierRepository,
    private val commentSnapshotRepository: CommentSnapshotRepository,
    private val userService: UserService
): CommentRepository {

    override fun findOne(id: Long): Comment {
        val latestVersion = commentSnapshotRepository.findTop1ByCommentIdentifierIdAndDeletedFalseOrderByCreatedDateTimeDesc(id)
            ?: throw RuntimeException("Not found")

        return convertToComment(latestVersion)
    }

    override fun create(content: String): Comment {
        val commentIdentifier = commentIdentifierRepository.save(
            CommentIdentifier(id = 0)
        )

        val commentVersion = commentSnapshotRepository.save(
            CommentSnapshot(
                versionId = 0,
                content = content,
                /** Audit Properties **/
                commentIdentifier = commentIdentifier,
                createdDateTime = Instant.now(),
                createdBy = userService.getCurrentUsername(),
                deleted = false
            )
        )

        return convertToComment(commentVersion)
    }

    override fun edit(id: Long, newContent: String): Comment {
        val latestVersion = commentSnapshotRepository.findTop1ByCommentIdentifierIdAndDeletedFalseOrderByCreatedDateTimeDesc(id)
            ?: throw RuntimeException("Not found")

        val newVersion = commentSnapshotRepository.save(
            CommentSnapshot(
                versionId = 0,
                content = newContent,
                /** Audit Properties **/
                commentIdentifier = latestVersion.commentIdentifier,
                createdDateTime = Instant.now(),
                createdBy = userService.getCurrentUsername(),
                deleted = false
            )
        )

        return convertToComment(newVersion)
    }

    override fun delete(id: Long) {
        val latestVersion = commentSnapshotRepository.findTop1ByCommentIdentifierIdAndDeletedFalseOrderByCreatedDateTimeDesc(id)
            ?: throw RuntimeException("Not found")

        commentSnapshotRepository.save(
            CommentSnapshot(
                versionId = 0,
                content = latestVersion.content,
                /** Audit Properties **/
                commentIdentifier = latestVersion.commentIdentifier,
                createdDateTime = Instant.now(),
                createdBy = userService.getCurrentUsername(),
                deleted = true
            )
        )
    }

    // Helpers

    private fun convertToComment(commentSnapshot: CommentSnapshot): Comment {
        return Comment(
            id = commentSnapshot.commentIdentifier.id,
            content = commentSnapshot.content
        )
    }

}