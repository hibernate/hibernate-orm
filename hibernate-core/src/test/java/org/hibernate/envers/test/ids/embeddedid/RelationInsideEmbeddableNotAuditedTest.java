/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.ids.embeddedid;

import java.io.Serializable;
import java.util.List;

import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import org.hibernate.envers.Audited;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.junit.jupiter.api.Disabled;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.hamcrest.CollectionMatchers;
import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

/**
 * Test an audited entity with an embeddable composite key that has an association
 * to a non-audited entity type.
 *
 * @author Chris Cranford
 */
@TestForIssue(jiraKey = "HHH-12498")
@Disabled("NYI - SingularPersistentAttributeEntity#visitJdbcTypes")
public class RelationInsideEmbeddableNotAuditedTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	private Integer authorId;
	private BookId bookId1;
	private BookId bookId2;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[]{ Book.class, Author.class };
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		inTransactions(
				// Revision 1, persist author and book
				entityManager -> {
					final Author author = new Author();
					author.setName( "Stephen King" );
					entityManager.persist( author );
					authorId = author.getId();

					final Book book = new Book();
					book.setId( new BookId() );
					book.getId().setId( 1 );
					book.getId().setAuthor( author );
					book.setName( "Gunslinger" );
					book.setEdition( 1 );
					entityManager.persist( book );
					this.bookId1 = book.getId();
				},

				// Revision 2, persist new book
				entityManager -> {
					final Author author = entityManager.find( Author.class, authorId );

					final Book book = new Book();
					book.setId( new BookId() );
					book.getId().setId( 2 );
					book.getId().setAuthor( author );
					book.setName( "Gunslinger" );
					book.setEdition( 2 );
					entityManager.persist( book );
					this.bookId2 = book.getId();
				},

				// Modify books
				entityManager -> {
					final Book book1 = entityManager.find( Book.class, bookId1 );
					book1.setName( "Gunslinger: Dark Tower" );
					entityManager.merge( book1 );
				},

				entityManager -> {
					final Book book2 = entityManager.find( Book.class, bookId2 );
					book2.setName( "Gunslinger: Dark Tower" );
					entityManager.merge( book2 );
				},

				//! Delete books
				entityManager -> {
					final Book book1 = entityManager.find( Book.class, bookId1 );
					entityManager.remove( book1 );

					final Book book2 = entityManager.find( Book.class, bookId2 );
					entityManager.remove( book2 );
				}
		);
	}

	@DynamicTest
	public void tesRevisionCounts() {
		assertThat( getAuditReader().getRevisions( Book.class, bookId1 ), contains( 1, 3, 5 ) );
		assertThat( getAuditReader().getRevisions( Book.class, bookId2 ), contains( 2, 4, 5 ) );
	}

	@DynamicTest
	public void testRevisionHistoryBook1() {
		final Book rev1 = getAuditReader().find( Book.class, bookId1, 1 );
		assertThat( rev1.getId().getAuthor(), notNullValue() );

		final Book rev3 = getAuditReader().find( Book.class, bookId1, 3 );
		assertThat( rev3.getId().getAuthor(), notNullValue() );

		final Book rev5 = getAuditReader().find( Book.class, bookId1, 5 );
		assertThat( rev5, nullValue() );
	}

	@DynamicTest
	public void testRevisionHistoryBook2() {
		final Book rev2 = getAuditReader().find( Book.class, bookId2, 2 );
		assertThat( rev2.getId().getAuthor(), notNullValue() );

		final Book rev4 = getAuditReader().find( Book.class, bookId2, 4 );
		assertThat( rev4.getId().getAuthor(), notNullValue() );

		final Book rev5 = getAuditReader().find( Book.class, bookId2, 5 );
		assertThat( rev5, nullValue() );
	}

	@DynamicTest
	public void testSelectDeletedEntitiesBook1() {
		final List books = getAuditReader().createQuery()
				.forRevisionsOfEntity( Book.class, true, true )
				.add( AuditEntity.id().eq( bookId1 ) )
				.add( AuditEntity.revisionType().eq( RevisionType.DEL ) )
				.getResultList();

		assertThat( books, CollectionMatchers.isNotEmpty() );

		final Book book = (Book) books.get( 0 );
		assertThat( book.getId(), notNullValue() );
		assertThat( book.getId().getAuthor(), notNullValue() );
		assertThat( book.getId().getAuthor().getId(), equalTo( authorId ) );
	}

	@DynamicTest
	public void testSelectDeletedEntitiesBook2() {
		final List books = getAuditReader().createQuery()
				.forRevisionsOfEntity( Book.class, true, true )
				.add( AuditEntity.id().eq( bookId2 ) )
				.add( AuditEntity.revisionType().eq( RevisionType.DEL ) )
				.getResultList();

		assertThat( books, CollectionMatchers.isNotEmpty() );

		final Book book = (Book) books.get( 0 );
		assertThat( book.getId(), notNullValue() );
		assertThat( book.getId().getAuthor(), notNullValue() );
		assertThat( book.getId().getAuthor().getId(), equalTo( authorId ) );
	}

	@Audited
	@Entity(name = "Book")
	public static class Book {
		@EmbeddedId
		private BookId id;
		private String name;
		private Integer edition;

		public BookId getId() {
			return id;
		}

		public void setId(BookId id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Integer getEdition() {
			return edition;
		}

		public void setEdition(Integer edition) {
			this.edition = edition;
		}
	}

	@Embeddable
	public static class BookId implements Serializable {
		private Integer id;

		@ManyToOne(fetch = FetchType.LAZY)
		private Author author;

		BookId() {

		}

		BookId(Integer id, Author author) {
			this.id = id;
			this.author = author;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public Author getAuthor() {
			return author;
		}

		public void setAuthor(Author author) {
			this.author = author;
		}
	}

	@Entity(name = "Author")
	public static class Author {
		@Id
		@GeneratedValue
		private Integer id;
		private String name;

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
