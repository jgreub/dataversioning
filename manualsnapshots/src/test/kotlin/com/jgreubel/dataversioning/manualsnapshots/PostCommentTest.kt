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
    lateinit var postRepository: PostRepository

    @Autowired
    lateinit var commentRepository: CommentRepository

    @Test
    fun `post comment journey`() {
        val post = postRepository.create("My first post")
        postRepository.edit(post.id, "My first post with an edit")
        postRepository.edit(post.id, "My first post with a second edit")
        val comment = commentRepository.create("Your post stinks!")
        postRepository.addComment(post.id, comment)
        commentRepository.edit(comment.id, "Woops, I meant your post rocks!")

        val allPosts = postRepository.findAll()
        assertThat(allPosts.size).isEqualTo(1)

        val postVersions = postRepository.findAllVersions(post.id)
        assertThat(postVersions.size).isEqualTo(4)

        val allComments = commentRepository.findAll()
        assertThat(allComments.size).isEqualTo(1)

        val commentVersions = commentRepository.findAllVersions(comment.id)
        assertThat(commentVersions.size).isEqualTo(2)
    }
}