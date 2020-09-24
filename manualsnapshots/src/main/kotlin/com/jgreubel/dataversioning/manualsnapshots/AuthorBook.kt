package com.jgreubel.dataversioning.manualsnapshots

import org.springframework.data.jpa.repository.JpaRepository
import java.awt.print.Book
import java.time.Instant
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.ManyToMany

//@Entity
//data class Author(
//    @Id
//    @GeneratedValue
//    val id: Long,
//    val name: String,
//    @ManyToMany
//    val books: List<Book>,
//
//    /** Audit Properties **/
//    val createdDateTime: Instant,
//    val createdBy: String
//)
//
//interface AuthorRepository : JpaRepository<Author, Long>
//
//@Entity
//data class Book(
//    @Id
//    @GeneratedValue
//    val id: Long,
//    val name: String,
//    @ManyToMany
//    val authors: List<Author>,
//
//    /** Audit Properties **/
//    val createdDateTime: Instant,
//    val createdBy: String
//)
//
//interface BookRepository : JpaRepository<Book, Long>