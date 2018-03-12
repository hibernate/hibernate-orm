/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.insertordering;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.hibernate.cfg.Environment;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;

/**
 * @author Andrea Boriero
 */
@TestForIssue(jiraKey = "HHH-12380")
public class InsertOrderingHasParentTest extends BaseNonConfigCoreFunctionalTestCase {

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] { Author.class, Book.class, Comment.class };
	}

	@Override
	protected void addSettings(Map settings) {
		settings.put( Environment.ORDER_INSERTS, "true" );
	}

	@Test
	public void testInsert() {

		doInHibernate( this::sessionFactory, session -> {
			Book book = new Book();
			book.setComment( new Comment( "first comment" ) );
			book.setComments( Arrays.asList( new Comment( "second comment" ) ) );

			Author author = new Author();
			author.setBook( book );

			session.persist( author );
		} );

	}

	@Entity(name = "Author")
	public static class Author {
		@Id
		@GeneratedValue
		Long id;

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

	@Entity(name = "Comment") @Table(name = "book_comment")
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
