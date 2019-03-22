/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.update;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import org.hibernate.annotations.SelectBeforeUpdate;
import org.hibernate.envers.Audited;
import org.hibernate.envers.test.EnversSessionFactoryBasedFunctionalTest;
import org.junit.jupiter.api.Disabled;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.hamcrest.CollectionMatchers;
import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Chris Cranford
 */
@TestForIssue(jiraKey = "HHH-11056")
@Disabled("NYI - @SelectBeforeUpdate annotation.")
public class SelectBeforeUpdateTest extends EnversSessionFactoryBasedFunctionalTest {

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class<?>[] { Book.class, Author.class };
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		final Author author1 = new Author( 1, "Author1" );
		final Author author2 = new Author( 2, "Author2" );
		final Author author3 = new Author( 3, "Author3" );
		final Author author4 = new Author( 4, "Author4" );

		final Book book1 = new Book( 1, "Book1", author1 );
		final Book book2 = new Book( 2, "Book2", author2 );
		final Book book3 = new Book( 3, "Book3", author3 );
		final Book book4 = new Book( 4, "Book4", author4 );

		// Revision 1 - Insert new entities
		inTransaction(
				session -> {
					session.save( author1 );
					session.save( book1 );
				}
		);

		// No revision - Update detached with no changes
		inTransaction(
				session -> {
					session.update( book1 );
				}
		);

		// Revision 2 - Insert new entities
		inTransaction(
				session -> {
					session.save( author2 );
					session.save( book2 );
				}
		);

		// Revision 3 - Update detached with changes
		inTransaction(
				session -> {
					book2.setName( "Book2Updated" );
					session.update( book2 );
				}
		);

		// Revision 4 - Insert new entities
		inTransaction(
				session -> {
					session.save( author3 );
					session.save( book3 );
				}
		);

		// No revision - Update detached with no changes
		inTransaction(
				session -> {
					session.update( book3 );
				}
		);

		// Revision 5 - Update detached with changes
		inTransaction(
				session -> {
					book3.setName( "Book3Updated" );
					session.update( book3 );
				}
		);

		// Revision 6 - Insert new entities
		inTransaction(
				session -> {
					session.save( author4 );
					session.save( book4 );
				}
		);

		// Revision 7 - Update detached with changes
		inTransaction(
				session -> {
					book4.setName( "Book4Updated" );
					session.update( book4 );
				}
		);

		// No revision - Update detached with no changes
		inTransaction(
				session -> {
					session.update( book4 );
				}
		);
	}

	@DynamicTest
	public void testRevisionCountsUpdateDetachedUnchanged() {
		assertThat( getAuditReader().getRevisions( Author.class, 1 ), CollectionMatchers.hasSize( 1 ) );
		assertThat( getAuditReader().getRevisions( Book.class, 1 ), CollectionMatchers.hasSize( 1 ) );
	}

	@DynamicTest
	public void testRevisionCountsUpdateDetachedChanged() {
		assertThat( getAuditReader().getRevisions( Author.class, 2 ), CollectionMatchers.hasSize( 1 ) );
		assertThat( getAuditReader().getRevisions( Book.class, 2 ), CollectionMatchers.hasSize( 2 ) );
	}

	@DynamicTest
	public void testRevisionCountsUpdateDetachedUnchangedAndChanged() {
		assertThat( getAuditReader().getRevisions( Author.class, 3 ), CollectionMatchers.hasSize( 1 ) );
		assertThat( getAuditReader().getRevisions( Book.class, 3 ), CollectionMatchers.hasSize( 2 ) );
	}

	@DynamicTest
	public void testRevisionCountsUpdateDetachedChangedAndUnchanged() {
		assertThat( getAuditReader().getRevisions( Author.class, 4 ), CollectionMatchers.hasSize( 1 ) );
		assertThat( getAuditReader().getRevisions( Book.class, 4 ), CollectionMatchers.hasSize( 2 ) );
	}

	@Entity(name = "Book")
	@SelectBeforeUpdate
	@Audited
	public static class Book {
		@Id
		private Integer id;
		private String name;
		@ManyToOne
		@JoinColumn(updatable = false)
		private Author author;

		Book() {

		}

		Book(Integer id, String name, Author author) {
			this.id = id;
			this.name = name;
			this.author = author;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Author getAuthor() {
			return author;
		}

		public void setAuthor(Author author) {
			this.author = author;
		}
	}

	@Entity(name = "Author")
	@Audited
	public static class Author {
		@Id
		private Integer id;
		private String name;

		Author() {

		}

		Author(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}
}
