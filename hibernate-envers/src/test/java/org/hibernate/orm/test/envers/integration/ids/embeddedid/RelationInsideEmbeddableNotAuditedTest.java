/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.ids.embeddedid;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

import org.hibernate.envers.Audited;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test an audited entity with an embeddable composite key that has an association
 * to a non-audited entity type.
 *
 * @author Chris Cranford
 */
@JiraKey(value = "HHH-12498")
@EnversTest
@Jpa(annotatedClasses = {RelationInsideEmbeddableNotAuditedTest.Book.class,
						RelationInsideEmbeddableNotAuditedTest.Author.class})
public class RelationInsideEmbeddableNotAuditedTest {
	private Integer authorId;
	private BookId bookId1;
	private BookId bookId2;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		// Revision 1, persist author and book
		scope.inTransaction( entityManager -> {
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
		scope.inTransaction( entityManager -> {
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
		scope.inTransaction( entityManager -> {
			final Book book1 = entityManager.find( Book.class, bookId1 );
			book1.setName( "Gunslinger: Dark Tower" );
			entityManager.merge( book1 );
		} );

		scope.inTransaction( entityManager -> {
			final Book book2 = entityManager.find( Book.class, bookId2 );
			book2.setName( "Gunslinger: Dark Tower" );
			entityManager.merge( book2 );
		} );

		//! Delete books
		scope.inTransaction( entityManager -> {
			final Book book1 = entityManager.find( Book.class, bookId1 );
			entityManager.remove( book1 );

			final Book book2 = entityManager.find( Book.class, bookId2 );
			entityManager.remove( book2 );
		} );
	}

	@Test
	public void tesRevisionCounts(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			assertEquals( Arrays.asList( 1, 3, 5 ),
					AuditReaderFactory.get( em ).getRevisions( Book.class, bookId1 ) );
			assertEquals( Arrays.asList( 2, 4, 5 ),
					AuditReaderFactory.get( em ).getRevisions( Book.class, bookId2 ) );
		} );
	}

	@Test
	public void testRevisionHistoryBook1(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );

			final Book rev1 = auditReader.find( Book.class, bookId1, 1 );
			assertNotNull( rev1.getId().getAuthor() );

			final Book rev3 = auditReader.find( Book.class, bookId1, 3 );
			assertNotNull( rev3.getId().getAuthor() );

			final Book rev5 = auditReader.find( Book.class, bookId1, 5 );
			assertNull( rev5 );
		} );
	}

	@Test
	public void testRevisionHistoryBook2(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );

			final Book rev2 = auditReader.find( Book.class, bookId2, 2 );
			assertNotNull( rev2.getId().getAuthor() );

			final Book rev4 = auditReader.find( Book.class, bookId2, 4 );
			assertNotNull( rev4.getId().getAuthor() );

			final Book rev5 = auditReader.find( Book.class, bookId2, 5 );
			assertNull( rev5 );
		} );
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testSelectDeletedEntitiesBook1(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			List<Book> books = (List<Book>) AuditReaderFactory.get( em ).createQuery()
					.forRevisionsOfEntity( Book.class, true, true )
					.add( AuditEntity.id().eq( bookId1 ) )
					.add( AuditEntity.revisionType().eq( RevisionType.DEL ) )
					.getResultList();

			assertTrue( !books.isEmpty() );

			final Book book = books.get( 0 );
			assertNotNull( book.getId() );
			assertNotNull( book.getId().getAuthor() );
			assertEquals( authorId, book.getId().getAuthor().getId() );
		} );
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testSelectDeletedEntitiesBook2(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			List<Book> books = (List<Book>) AuditReaderFactory.get( em ).createQuery()
					.forRevisionsOfEntity( Book.class, true, true )
					.add( AuditEntity.id().eq( bookId2 ) )
					.add( AuditEntity.revisionType().eq( RevisionType.DEL ) )
					.getResultList();

			assertTrue( !books.isEmpty() );

			final Book book = books.get( 0 );
			assertNotNull( book.getId() );
			assertNotNull( book.getId().getAuthor() );
			assertEquals( authorId, book.getId().getAuthor().getId() );
		} );
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
