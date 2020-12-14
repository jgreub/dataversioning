package com.jgreubel.dataversioning.envers

import org.hibernate.envers.Audited
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.history.RevisionRepository
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.Id

@Entity
@Audited
data class Post(
    @Id
    @GeneratedValue
    val id: Long,
    val content: String
)

interface PostRepository: JpaRepository<Post, Long>, RevisionRepository<Post, Long, Int>