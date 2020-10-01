package com.jgreubel.dataversioning.manualsnapshots

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
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

data class PostVersion(
    val postId: Long,
    val versionId: Long,
    val content: String,
    val comments: List<CommentVersion>,
    val createdDateTime: Instant,
    val createdBy: String,
    val deleted: Boolean
)

interface PostRepository {
    fun findOne(id: Long): Post
    fun findAll(): List<Post>
    fun create(content: String): Post
    fun edit(id: Long, newContent: String): Post
    fun addComment(id: Long, comment: Comment): Post
    fun delete(id: Long)
    fun findOneVersion(versionId: Long): PostVersion
    fun findAllVersions(id: Long): List<PostVersion>
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
    val commentSnapshots: List<CommentSnapshot>,

    /** Version & Audit Properties **/
    @ManyToOne(optional = false)
    val postIdentifier: PostIdentifier,
    val createdDateTime: Instant,
    val validUntil: Instant?,
    val createdBy: String,
    val deleted: Boolean
)

interface PostIdentifierRepository : JpaRepository<PostIdentifier, Long>
interface PostSnapshotRepository : JpaRepository<PostSnapshot, Long> {
    fun findByPostIdentifierIdAndValidUntilNullAndDeletedFalseOrderByCreatedDateTimeDesc(id: Long): PostSnapshot?
    fun findAllByValidUntilNullAndDeletedFalseOrderByCreatedDateTimeDesc(): List<PostSnapshot>
    fun findAllByPostIdentifierIdOrderByCreatedDateTimeAsc(id: Long): List<PostSnapshot>
}

@Component
class PostSnapshotService(
    private val postIdentifierRepository: PostIdentifierRepository,
    private val postSnapshotRepository: PostSnapshotRepository,
    private val commentSnapshotService: CommentSnapshotService,
    private val userService: UserService
): PostRepository {

    override fun findOne(id: Long): Post {
        val latestVersion = postSnapshotRepository.findByPostIdentifierIdAndValidUntilNullAndDeletedFalseOrderByCreatedDateTimeDesc(id)
            ?: throw RuntimeException("Not found")

        return convertToPost(latestVersion)
    }

    override fun findAll(): List<Post> {
        val latestVersions = postSnapshotRepository.findAllByValidUntilNullAndDeletedFalseOrderByCreatedDateTimeDesc()

        return latestVersions.map { convertToPost(it) }
    }

    @Transactional
    override fun create(content: String): Post {
        val postIdentifier = postIdentifierRepository.save(
            PostIdentifier(id = 0)
        )

        val postVersion = postSnapshotRepository.save(
            PostSnapshot(
                versionId = 0,
                content = content,
                commentSnapshots = listOf(),
                /** Version & Audit Properties **/
                postIdentifier = postIdentifier,
                createdDateTime = Instant.now(),
                validUntil = null,
                createdBy = userService.getCurrentUsername(),
                deleted = false
            )
        )

        return convertToPost(postVersion)
    }

    @Transactional
    override fun edit(id: Long, newContent: String): Post {
        val latestVersion = postSnapshotRepository.findByPostIdentifierIdAndValidUntilNullAndDeletedFalseOrderByCreatedDateTimeDesc(id)
            ?: throw RuntimeException("Not found")

        retireVersion(latestVersion)

        val newVersion = postSnapshotRepository.save(
            PostSnapshot(
                versionId = 0,
                content = newContent,
                // JPA Weirdness - Notice the .toList() required for Hibernate
                commentSnapshots = latestVersion.commentSnapshots.toList(),
                /** Version & Audit Properties **/
                postIdentifier = latestVersion.postIdentifier,
                createdDateTime = Instant.now(),
                validUntil = null,
                createdBy = userService.getCurrentUsername(),
                deleted = false
            )
        )

        return convertToPost(newVersion)
    }

    @Transactional
    override fun addComment(id: Long, comment: Comment): Post {
        val latestVersion = postSnapshotRepository.findByPostIdentifierIdAndValidUntilNullAndDeletedFalseOrderByCreatedDateTimeDesc(id)
            ?: throw RuntimeException("Not found")

        retireVersion(latestVersion)

        val commentSnapshot = commentSnapshotService.getCurrentSnapshot(comment.id)

        val newCommentSnapshots =
            latestVersion.commentSnapshots + commentSnapshot

        val newVersion = postSnapshotRepository.save(
            PostSnapshot(
                versionId = 0,
                content = latestVersion.content,
                commentSnapshots = newCommentSnapshots,
                /** Version & Audit Properties **/
                postIdentifier = latestVersion.postIdentifier,
                createdDateTime = Instant.now(),
                validUntil = null,
                createdBy = userService.getCurrentUsername(),
                deleted = false
            )
        )

        return convertToPost(newVersion)
    }

    @Transactional
    override fun delete(id: Long) {
        val latestVersion = postSnapshotRepository.findByPostIdentifierIdAndValidUntilNullAndDeletedFalseOrderByCreatedDateTimeDesc(id)
            ?: throw RuntimeException("Not found")

        retireVersion(latestVersion)

        postSnapshotRepository.save(
            PostSnapshot(
                versionId = 0,
                content = latestVersion.content,
                // JPA Weirdness - Notice the .toList() required for Hibernate
                commentSnapshots = latestVersion.commentSnapshots.toList(),
                /** Version & Audit Properties **/
                postIdentifier = latestVersion.postIdentifier,
                createdDateTime = Instant.now(),
                validUntil = null,
                createdBy = userService.getCurrentUsername(),
                deleted = true
            )
        )
    }

    override fun findOneVersion(versionId: Long): PostVersion {
        val version = postSnapshotRepository.findById(versionId).orElse(null)
            ?: throw RuntimeException("Not found")

        return convertToPostVersion(version)
    }

    override fun findAllVersions(id: Long): List<PostVersion> {
        val versions = postSnapshotRepository.findAllByPostIdentifierIdOrderByCreatedDateTimeAsc(id)

        return versions.map { convertToPostVersion(it) }
    }

    // Helpers

    private fun retireVersion(version: PostSnapshot) {
        postSnapshotRepository.save(
            version.copy(
                validUntil = Instant.now()
            )
        )
    }

    private fun convertToPost(postSnapshot: PostSnapshot): Post {
        return Post(
            id = postSnapshot.postIdentifier.id,
            content = postSnapshot.content,
            comments = postSnapshot.commentSnapshots.map {
                commentSnapshotService.findOne(it.commentIdentifier.id)
            }
        )
    }

    private fun convertToPostVersion(postSnapshot: PostSnapshot): PostVersion {
        return PostVersion(
            postId = postSnapshot.postIdentifier.id,
            versionId = postSnapshot.versionId,
            content = postSnapshot.content,
            comments = postSnapshot.commentSnapshots.map {
                commentSnapshotService.findOneVersion(it.versionId)
            },
            createdDateTime = postSnapshot.createdDateTime,
            createdBy = postSnapshot.createdBy,
            deleted = postSnapshot.deleted
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

data class CommentVersion(
    val commentId: Long,
    val versionId: Long,
    val content: String,
    val createdDateTime: Instant,
    val createdBy: String,
    val deleted: Boolean
)

interface CommentRepository {
    fun findOne(id: Long): Comment
    fun findAll(): List<Comment>
    fun create(content: String): Comment
    fun edit(id: Long, newContent: String): Comment
    fun delete(id: Long)
    fun findOneVersion(versionId: Long): CommentVersion
    fun findAllVersions(id: Long): List<CommentVersion>
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

    /** Version & Audit Properties **/
    @ManyToOne(optional = false)
    val commentIdentifier: CommentIdentifier,
    val createdDateTime: Instant,
    val validUntil: Instant?,
    val createdBy: String,
    val deleted: Boolean
)

interface CommentIdentifierRepository : JpaRepository<CommentIdentifier, Long>
interface CommentSnapshotRepository : JpaRepository<CommentSnapshot, Long> {
    fun findByCommentIdentifierIdAndValidUntilNullAndDeletedFalseOrderByCreatedDateTimeDesc(id: Long): CommentSnapshot?
    fun findAllByValidUntilNullAndDeletedFalseOrderByCreatedDateTimeDesc(): List<CommentSnapshot>
    fun findAllByCommentIdentifierIdOrderByCreatedDateTimeAsc(id: Long): List<CommentSnapshot>
}

@Component
class CommentSnapshotService(
    private val commentIdentifierRepository: CommentIdentifierRepository,
    private val commentSnapshotRepository: CommentSnapshotRepository,
    private val userService: UserService
): CommentRepository {

    override fun findOne(id: Long): Comment {
        val latestVersion = commentSnapshotRepository.findByCommentIdentifierIdAndValidUntilNullAndDeletedFalseOrderByCreatedDateTimeDesc(id)
            ?: throw RuntimeException("Not found")

        return convertToComment(latestVersion)
    }

    override fun findAll(): List<Comment> {
        val latestVersions = commentSnapshotRepository.findAllByValidUntilNullAndDeletedFalseOrderByCreatedDateTimeDesc()

        return latestVersions.map { convertToComment(it) }
    }

    @Transactional
    override fun create(content: String): Comment {
        val commentIdentifier = commentIdentifierRepository.save(
            CommentIdentifier(id = 0)
        )

        val commentVersion = commentSnapshotRepository.save(
            CommentSnapshot(
                versionId = 0,
                content = content,
                /** Version & Audit Properties **/
                commentIdentifier = commentIdentifier,
                createdDateTime = Instant.now(),
                validUntil = null,
                createdBy = userService.getCurrentUsername(),
                deleted = false
            )
        )

        return convertToComment(commentVersion)
    }

    @Transactional
    override fun edit(id: Long, newContent: String): Comment {
        val latestVersion = commentSnapshotRepository.findByCommentIdentifierIdAndValidUntilNullAndDeletedFalseOrderByCreatedDateTimeDesc(id)
            ?: throw RuntimeException("Not found")

        retireVersion(latestVersion)

        val newVersion = commentSnapshotRepository.save(
            CommentSnapshot(
                versionId = 0,
                content = newContent,
                /** Version & Audit Properties **/
                commentIdentifier = latestVersion.commentIdentifier,
                createdDateTime = Instant.now(),
                validUntil = null,
                createdBy = userService.getCurrentUsername(),
                deleted = false
            )
        )

        return convertToComment(newVersion)
    }

    @Transactional
    override fun delete(id: Long) {
        val latestVersion = commentSnapshotRepository.findByCommentIdentifierIdAndValidUntilNullAndDeletedFalseOrderByCreatedDateTimeDesc(id)
            ?: throw RuntimeException("Not found")

        retireVersion(latestVersion)

        commentSnapshotRepository.save(
            CommentSnapshot(
                versionId = 0,
                content = latestVersion.content,
                /** Version & Audit Properties **/
                commentIdentifier = latestVersion.commentIdentifier,
                createdDateTime = Instant.now(),
                validUntil = null,
                createdBy = userService.getCurrentUsername(),
                deleted = true
            )
        )
    }

    override fun findOneVersion(versionId: Long): CommentVersion {
        val version = commentSnapshotRepository.findById(versionId).orElse(null)
            ?: throw RuntimeException("Not found")

        return convertToCommentVersion(version)
    }

    override fun findAllVersions(id: Long): List<CommentVersion> {
        val versions = commentSnapshotRepository.findAllByCommentIdentifierIdOrderByCreatedDateTimeAsc(id)

        return versions.map { convertToCommentVersion(it) }
    }

    // Data Layer Only Methods

    fun getCurrentSnapshot(id: Long): CommentSnapshot {
        return commentSnapshotRepository.findByCommentIdentifierIdAndValidUntilNullAndDeletedFalseOrderByCreatedDateTimeDesc(id)
            ?: throw RuntimeException("Not found")
    }

    // Helpers

    private fun retireVersion(version: CommentSnapshot) {
        commentSnapshotRepository.save(
            version.copy(
                validUntil = Instant.now()
            )
        )
    }

    private fun convertToComment(commentSnapshot: CommentSnapshot): Comment {
        return Comment(
            id = commentSnapshot.commentIdentifier.id,
            content = commentSnapshot.content
        )
    }

    private fun convertToCommentVersion(commentSnapshot: CommentSnapshot): CommentVersion {
        return CommentVersion(
            commentId = commentSnapshot.commentIdentifier.id,
            versionId = commentSnapshot.versionId,
            content = commentSnapshot.content,
            createdDateTime = commentSnapshot.createdDateTime,
            createdBy = commentSnapshot.createdBy,
            deleted = commentSnapshot.deleted
        )
    }

}