package com.jgreubel.dataversioning.envers

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class PostCommentTest {

    @Autowired
    private lateinit var postRepository: PostRepository

    @Autowired
    private lateinit var commentRepository: CommentRepository

    @Test
    fun `post comment journey`() {
        val post = Post(0, "content", listOf())
        val savedPost = postRepository.save(post)
        val updatedPost = savedPost.copy(content = "new content")
        val savedUpdatedPost = postRepository.save(updatedPost)

        val comment = Comment(0, "this is my comment")
        val savedComment = commentRepository.save(comment)

        val postWitComment = savedUpdatedPost.copy(comments = listOf(savedComment))
        val savedPostWithComment = postRepository.save(postWitComment)

        val updatedComment = savedComment.copy(content = "new comment content")
        val savedUpdatedComment = commentRepository.save(updatedComment)

        val postRevisions = postRepository.findRevisions(savedPost.id)
        println("Number of post revisions: ${postRevisions.content.size}")
        println("Post revision [0]: ${postRevisions.content[0]}")
        println("Post revision [1]: ${postRevisions.content[1]}")
        println("Post revision [2]: ${postRevisions.content[2]}")

        val commentRevisions = commentRepository.findRevisions(savedComment.id)
        println("Number of comment revisions: ${commentRevisions.content.size}")
        println("Comment revision [0]: ${commentRevisions.content[0]}")
        println("Comment revision [1]: ${commentRevisions.content[1]}")

        val fetchedPost = postRepository.findById(savedPost.id)
        println("Fetched Post: $fetchedPost")
    }
}