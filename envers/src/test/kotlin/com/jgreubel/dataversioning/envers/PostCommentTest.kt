package com.jgreubel.dataversioning.envers

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.data.envers.repository.support.EnversRevisionRepositoryFactoryBean
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import javax.persistence.EntityManager

@DataJpaTest
class PostCommentTest {

    @Autowired
    private lateinit var postRepository: PostRepository

    @Test
    fun `post comment journey`() {
        val post = Post(0, "content")
        val savedPost = postRepository.save(post)
        val updatedPost = savedPost.copy(content = "new content")
        val savedUpdatedPost = postRepository.save(updatedPost)

        val revisions = postRepository.findRevisions(savedPost.id)
        println("Number of revisions: ${revisions.content.size}")
        println("Revision[0]: ${revisions.content[0]}")
        println("Revision[1]: ${revisions.content[1]}")
    }
}