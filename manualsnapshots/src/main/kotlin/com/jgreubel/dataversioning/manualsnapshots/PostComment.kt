package com.jgreubel.dataversioning.manualsnapshots

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Component
import java.time.Instant
import javax.persistence.*

/****************/
/** Post Stuff **/
/****************/

@Entity
data class PostIdentifier(
    @Id
    @GeneratedValue
    val id: Long
)

@Entity
data class PostVersion(
    @Id
    @GeneratedValue
    val versionId: Long,
    val content: String,
    @OneToMany //TODO: Hmm... Maybe make it a ManyToMany?
    val commentIdentifiers: List<CommentIdentifier>,

    /** Audit Properties **/
    @ManyToOne(optional = false)
    val postIdentifier: PostIdentifier,
    val createdDateTime: Instant,
    val createdBy: String,
    val deleted: Boolean
)

interface PostIdentifierRepository : JpaRepository<PostIdentifier, Long>
interface PostVersionRepository : JpaRepository<PostVersion, Long> {
    fun findTop1ByPostIdentifierIdAndDeletedFalseOrderByCreatedDateTimeDesc(id: Long): PostVersion?
}

// Would be in business layer
data class Post(
    val id: Long,
    val content: String,
    val comments: List<Comment>
)

@Component
class PostService(
    private val postIdentifierRepository: PostIdentifierRepository,
    private val postVersionRepository: PostVersionRepository,
    private val commentService: CommentService,
    private val userService: UserService
) {

    fun findOne(id: Long): Post {
        val latestVersion = postVersionRepository.findTop1ByPostIdentifierIdAndDeletedFalseOrderByCreatedDateTimeDesc(id)
            ?: throw RuntimeException("Not found")

        return convertToPost(latestVersion)
    }

    fun create(content: String): Post {
        val postIdentifier = postIdentifierRepository.save(
            PostIdentifier(id = 0)
        )

        val postVersion = postVersionRepository.save(
            PostVersion(
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

    fun edit(id: Long, newContent: String): Post {
        val latestVersion = postVersionRepository.findTop1ByPostIdentifierIdAndDeletedFalseOrderByCreatedDateTimeDesc(id)
            ?: throw RuntimeException("Not found")

        val newVersion = postVersionRepository.save(
            PostVersion(
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

    fun addComment(id: Long, comment: Comment): Post { // TODO: What should API be?
        val latestVersion = postVersionRepository.findTop1ByPostIdentifierIdAndDeletedFalseOrderByCreatedDateTimeDesc(id)
            ?: throw RuntimeException("Not found")

        val newCommentIdentifiers =
            latestVersion.commentIdentifiers + CommentIdentifier(id = comment.id)

        val newVersion = postVersionRepository.save(
            PostVersion(
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

    fun delete(id: Long) {
        val latestVersion = postVersionRepository.findTop1ByPostIdentifierIdAndDeletedFalseOrderByCreatedDateTimeDesc(id)
            ?: throw RuntimeException("Not found")

        postVersionRepository.save(
            PostVersion(
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

    private fun convertToPost(postVersion: PostVersion): Post {
        return Post(
            id = postVersion.postIdentifier.id,
            content = postVersion.content,
            comments = postVersion.commentIdentifiers.map {
                commentService.findOne(it.id)
            }
        )
    }

}

/*******************/
/** Comment Stuff **/
/*******************/

@Entity
data class CommentIdentifier(
    @Id
    @GeneratedValue
    val id: Long
)

@Entity
data class CommentVersion(
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
interface CommentVersionRepository : JpaRepository<CommentVersion, Long> {
    fun findTop1ByCommentIdentifierIdAndDeletedFalseOrderByCreatedDateTimeDesc(id: Long): CommentVersion?
}

// Would be in business layer
data class Comment(
    val id: Long,
    val content: String
)

@Component
class CommentService(
    private val commentIdentifierRepository: CommentIdentifierRepository,
    private val commentVersionRepository: CommentVersionRepository,
    private val userService: UserService
) {

    fun findOne(id: Long): Comment {
        val latestVersion = commentVersionRepository.findTop1ByCommentIdentifierIdAndDeletedFalseOrderByCreatedDateTimeDesc(id)
            ?: throw RuntimeException("Not found")

        return convertToComment(latestVersion)
    }

    fun create(content: String): Comment {
        val commentIdentifier = commentIdentifierRepository.save(
            CommentIdentifier(id = 0)
        )

        val commentVersion = commentVersionRepository.save(
            CommentVersion(
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

    // Helpers

    private fun convertToComment(commentVersion: CommentVersion): Comment {
        return Comment(
            id = commentVersion.commentIdentifier.id,
            content = commentVersion.content
        )
    }

}