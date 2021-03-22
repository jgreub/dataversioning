package com.jgreubel.dataversioning.envers

import org.hibernate.envers.Audited
import org.hibernate.envers.RevisionEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.history.RevisionRepository
import javax.persistence.*

@Entity
@Audited
data class Post(
    @Id
    @GeneratedValue
    val id: Long,
    val content: String,
    @OneToMany(fetch = FetchType.EAGER)
    val comments: List<Comment>
)

interface PostRepository: JpaRepository<Post, Long>, RevisionRepository<Post, Long, Int>

@Entity
@Audited
data class Comment(
    @Id
    @GeneratedValue
    val id: Long,
    val content: String
)

interface CommentRepository: JpaRepository<Comment, Long>, RevisionRepository<Comment, Long, Int>

//TODO: https://blog.frankel.ch/spring-data-spring-security-and-envers-integration/