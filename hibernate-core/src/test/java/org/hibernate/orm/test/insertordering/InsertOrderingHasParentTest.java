/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.insertordering;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.jupiter.api.Test;

/**
 * @author Andrea Boriero
 */
@JiraKey(value = "HHH-12380")
public class InsertOrderingHasParentTest extends BaseInsertOrderingTest {

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] { Author.class, Book.class, Comment.class };
	}

	@Test
	public void testInsert() {

		sessionFactoryScope().inTransaction( session -> {
			Book book = new Book();
			book.setComment( new Comment( "first comment" ) );
			book.setComments( Arrays.asList( new Comment( "second comment" ) ) );

			Author author = new Author();
			author.setBook( book );

			session.persist( author );

			clearBatches();
		} );

		verifyContainsBatches(
				new Batch( "insert into book_comment (book_comment,id) values (?,?)", 2 ),
				new Batch( "insert into Book (comment_id,title,id) values (?,?,?)" ),
				new Batch( "insert into Author (book_id,name,id) values (?,?,?)" ),
				new Batch( "insert into Book_book_comment (Book_id,comments_id) values (?,?)" )
		);
	}

	@Entity(name = "Author")
	public static class Author {
		@Id
		@GeneratedValue
		Long id;

		String name;

		@OneToOne(cascade = CascadeType.ALL)
		Book book;

		public Book getBook() {
			return book;
		}

		public void setBook(Book book) {
			this.book = book;
		}
	}

	@Entity(name = "Book")
	public static class Book {
		@Id
		@GeneratedValue
		Long id;

		String title;

		@OneToOne(cascade = CascadeType.ALL)
		Comment comment;

		@ManyToMany(cascade = CascadeType.ALL)
		List<Comment> comments = new ArrayList<>();

		public Comment getComment() {
			return comment;
		}

		public void setComment(Comment comment) {
			this.comment = comment;
		}

		public List<Comment> getComments() {
			return comments;
		}

		public void setComments(List<Comment> comments) {
			this.comments = comments;
		}
	}

	@Entity(name = "Comment")
	@Table(name = "book_comment")
	public static class Comment {
		@Id
		@GeneratedValue
		Long id;

		@Column(name = "book_comment")
		String comment;

		public Comment() {
		}

		public Comment(String comment) {
			this.comment = comment;
		}

		public String getComment() {
			return comment;
		}

		public void setComment(String comment) {
			this.comment = comment;
		}
	}

}
