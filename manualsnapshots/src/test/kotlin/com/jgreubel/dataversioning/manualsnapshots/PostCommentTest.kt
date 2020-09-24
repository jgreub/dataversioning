package com.jgreubel.dataversioning.manualsnapshots

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.jdbc.core.JdbcTemplate

@DataJpaTest
@Import(PostService::class, CommentService::class, UserService::class)
class PostCommentTest {

    @Autowired
    lateinit var postService: PostService

    @Autowired
    lateinit var postIdentifierRepository: PostIdentifierRepository

    @Autowired
    lateinit var postVersionRepository: PostVersionRepository

    @Autowired
    lateinit var commentService: CommentService

    @Autowired
    lateinit var commentIdentifierRepository: CommentIdentifierRepository

    @Autowired
    lateinit var commentVersionRepository: CommentVersionRepository

    @Autowired
    lateinit var jdbcTemplate: JdbcTemplate

    @Test
    fun `post comment journey`() {
        val post = postService.create("My first post")

        assertThat(postIdentifierRepository.count()).isEqualTo(1)
        assertThat(postVersionRepository.count()).isEqualTo(1)

        val updatedPost =
            postService.edit(post.id, "My first post, with an edit")

        assertThat(postIdentifierRepository.count()).isEqualTo(1)
        assertThat(postVersionRepository.count()).isEqualTo(2)

        postService.edit(updatedPost.id, "My first post, with a second edit")

        assertThat(postIdentifierRepository.count()).isEqualTo(1)
        assertThat(postVersionRepository.count()).isEqualTo(3)

        val latestVersion =
            postVersionRepository.findTop1ByPostIdentifierIdAndDeletedFalseOrderByCreatedDateTimeDesc(
                post.id
            )

        assertThat(latestVersion?.versionId).isEqualTo(4L) // Off by one for some reason...


    }
}