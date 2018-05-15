/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.ids.embeddedid;

import java.io.Serializable;
import java.util.Arrays;
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
import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.Priority;
import org.junit.Test;

import org.hibernate.testing.TestForIssue;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Test an audited entity with an embeddable composite key that has an association
 * to a non-audited entity type.
 *
 * @author Chris Cranford
 */
@TestForIssue(jiraKey = "HHH-12498")
public class RelationInsideEmbeddableNotAuditedTest extends BaseEnversJPAFunctionalTestCase {
	private Integer authorId;
	private BookId bookId1;
	private BookId bookId2;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[]{ Book.class, Author.class };
	}

	@Test
	@Priority(10)
	public void initData() {
		// Revision 1, persist author and book
		doInJPA( this::entityManagerFactory, entityManager -> {
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
		} );

		// Revision 2, persist new book
		doInJPA( this::entityManagerFactory, entityManager -> {
			final Author author = entityManager.find( Author.class, authorId );

			final Book book = new Book();
			book.setId( new BookId() );
			book.getId().setId( 2 );
			book.getId().setAuthor( author );
			book.setName( "Gunslinger" );
			book.setEdition( 2 );
			entityManager.persist( book );
			this.bookId2 = book.getId();
		} );

		// Modify books
		doInJPA( this::entityManagerFactory, entityManager -> {
			final Book book1 = entityManager.find( Book.class, bookId1 );
			book1.setName( "Gunslinger: Dark Tower" );
			entityManager.merge( book1 );
		} );

		doInJPA( this::entityManagerFactory, entityManager -> {
			final Book book2 = entityManager.find( Book.class, bookId2 );
			book2.setName( "Gunslinger: Dark Tower" );
			entityManager.merge( book2 );
		} );

		//! Delete books
		doInJPA( this::entityManagerFactory, entityManager -> {
			final Book book1 = entityManager.find( Book.class, bookId1 );
			entityManager.remove( book1 );

			final Book book2 = entityManager.find( Book.class, bookId2 );
			entityManager.remove( book2 );
		} );
	}

	@Test
	public void tesRevisionCounts() {
		assertEquals( Arrays.asList( 1, 3, 5 ), getAuditReader().getRevisions( Book.class, bookId1 ) );
		assertEquals( Arrays.asList( 2, 4, 5 ), getAuditReader().getRevisions( Book.class, bookId2 ) );
	}

	@Test
	public void testRevisionHistoryBook1() {
		final Book rev1 = getAuditReader().find( Book.class, bookId1, 1 );
		assertNotNull( rev1.getId().getAuthor() );

		final Book rev3 = getAuditReader().find( Book.class, bookId1, 3 );
		assertNotNull( rev3.getId().getAuthor() );

		final Book rev5 = getAuditReader().find( Book.class, bookId1, 5 );
		assertNull( rev5 );
	}

	@Test
	public void testRevisionHistoryBook2() {
		final Book rev2 = getAuditReader().find( Book.class, bookId2, 2 );
		assertNotNull( rev2.getId().getAuthor() );

		final Book rev4 = getAuditReader().find( Book.class, bookId2, 4 );
		assertNotNull( rev4.getId().getAuthor() );

		final Book rev5 = getAuditReader().find( Book.class, bookId2, 5 );
		assertNull( rev5 );
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testSelectDeletedEntitiesBook1() {
		List<Book> books = (List<Book>) getAuditReader().createQuery()
				.forRevisionsOfEntity( Book.class, true, true )
				.add( AuditEntity.id().eq( bookId1 ) )
				.add( AuditEntity.revisionType().eq( RevisionType.DEL ) )
				.getResultList();

		assertTrue( !books.isEmpty() );

		final Book book = books.get( 0 );
		assertNotNull( book.getId() );
		assertNotNull( book.getId().getAuthor() );
		assertEquals( authorId, book.getId().getAuthor().getId() );
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testSelectDeletedEntitiesBook2() {
		List<Book> books = (List<Book>) getAuditReader().createQuery()
				.forRevisionsOfEntity( Book.class, true, true )
				.add( AuditEntity.id().eq( bookId2 ) )
				.add( AuditEntity.revisionType().eq( RevisionType.DEL ) )
				.getResultList();

		assertTrue( !books.isEmpty() );

		final Book book = books.get( 0 );
		assertNotNull( book.getId() );
		assertNotNull( book.getId().getAuthor() );
		assertEquals( authorId, book.getId().getAuthor().getId() );
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
