package com.jgreubel.dataversioning.manualsnapshots

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import

@DataJpaTest
@Import(PostSnapshotService::class, CommentSnapshotService::class, UserService::class)
class PostCommentTest {

    @Autowired
    lateinit var postSnapshotService: PostSnapshotService

    @Autowired
    lateinit var postIdentifierRepository: PostIdentifierRepository

    @Autowired
    lateinit var postSnapshotRepository: PostSnapshotRepository

    @Autowired
    lateinit var commentSnapshotService: CommentSnapshotService

    @Autowired
    lateinit var commentIdentifierRepository: CommentIdentifierRepository

    @Autowired
    lateinit var commentSnapshotRepository: CommentSnapshotRepository

    @Test
    fun `post comment journey`() {
        val post = postSnapshotService.create("My first post")

        assertThat(postIdentifierRepository.count()).isEqualTo(1)
        assertThat(postSnapshotRepository.count()).isEqualTo(1)

        val updatedPost = postSnapshotService.edit(post.id, "My first post, with an edit")

        assertThat(postIdentifierRepository.count()).isEqualTo(1)
        assertThat(postSnapshotRepository.count()).isEqualTo(2)

        postSnapshotService.edit(updatedPost.id, "My first post, with a second edit")

        assertThat(postIdentifierRepository.count()).isEqualTo(1)
        assertThat(postSnapshotRepository.count()).isEqualTo(3)

        val comment = commentSnapshotService.create("Your post stinks!")
        postSnapshotService.addComment(updatedPost.id, comment)

        assertThat(postIdentifierRepository.count()).isEqualTo(1)
        assertThat(postSnapshotRepository.count()).isEqualTo(4)

        assertThat(commentIdentifierRepository.count()).isEqualTo(1)
        assertThat(commentSnapshotRepository.count()).isEqualTo(1)

        commentSnapshotService.edit(comment.id, "Woops, I meant your post rocks!")

        assertThat(postIdentifierRepository.count()).isEqualTo(1)
        assertThat(postSnapshotRepository.count()).isEqualTo(4)

        assertThat(commentIdentifierRepository.count()).isEqualTo(1)
        assertThat(commentSnapshotRepository.count()).isEqualTo(2)

        val foundPost = postSnapshotService.findOne(updatedPost.id)

        assertThat(foundPost.comments.size).isEqualTo(1)
        assertThat(foundPost.comments[0].content).isEqualTo("Woops, I meant your post rocks!")

        postSnapshotService.delete(foundPost.id)

        assertThat(postIdentifierRepository.count()).isEqualTo(1)
        assertThat(postSnapshotRepository.count()).isEqualTo(5)

        assertThat(commentIdentifierRepository.count()).isEqualTo(1)
        assertThat(commentSnapshotRepository.count()).isEqualTo(2)
    }
}