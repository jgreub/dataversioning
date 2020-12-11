package com.jgreubel.dataversioning.envers

import org.hibernate.envers.Audited
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