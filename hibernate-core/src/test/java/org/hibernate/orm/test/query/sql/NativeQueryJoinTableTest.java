/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.sql;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.query.NativeQuery;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Marco Belladelli
 */
@DomainModel( annotatedClasses = {
		NativeQueryJoinTableTest.Shelf.class, NativeQueryJoinTableTest.Book.class,
} )
@SessionFactory
@Jira( "https://hibernate.atlassian.net/browse/HHH-18494" )
public class NativeQueryJoinTableTest {
	private static final String SHELF_ID = "shelf1";
	private static final String FILE_ID = "file1";

	@Test
	public void testTypedQuery(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final NativeQuery<Book> query = session.createNativeQuery(
					"select book.*, book_1_.shelfid from BOOK_T book, SHELF_BOOK book_1_ where book.fileid = book_1_.fileid",
					Book.class
			);
			final Book retrievedBook = query.getSingleResult();
			assertEquals( FILE_ID, retrievedBook.getFileId() );
			assertEquals( "Birdwatchers Guide to Dodos", retrievedBook.getTitle() );
			assertEquals( "nonfiction", retrievedBook.getShelf().getArea() );
			assertEquals( 3, retrievedBook.getShelf().getShelfNumber() );
			assertEquals( SHELF_ID, retrievedBook.getShelf().getShelfid() );
			assertEquals( 5, retrievedBook.getShelf().getPosition() );
		} );
	}

	@Test
	public void testAddEntity(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final NativeQuery query = session.createNativeQuery(
					"select {book.*}, book_1_.shelfid from BOOK_T book, SHELF_BOOK book_1_ where book.fileid = book_1_.fileid"
			);
			query.addEntity( "book", Book.class );
			final Book retrievedBook = (Book) query.getSingleResult();
			assertEquals( FILE_ID, retrievedBook.getFileId() );
			assertEquals( "Birdwatchers Guide to Dodos", retrievedBook.getTitle() );
			assertEquals( "nonfiction", retrievedBook.getShelf().getArea() );
			assertEquals( 3, retrievedBook.getShelf().getShelfNumber() );
			assertEquals( SHELF_ID, retrievedBook.getShelf().getShelfid() );
			assertEquals( 5, retrievedBook.getShelf().getPosition() );
		} );
	}

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Shelf shelf = new Shelf();
			shelf.setShelfid( SHELF_ID );
			shelf.setArea( "nonfiction" );
			shelf.setPosition( Integer.valueOf( 5 ) );
			shelf.setShelfNumber( Integer.valueOf( 3 ) );
			shelf.setBooks( new HashSet<>() );
			session.persist( shelf );
			final Book book = new Book( FILE_ID );
			book.setTitle( "Birdwatchers Guide to Dodos" );
			book.setShelf( shelf );
			session.persist( book );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncateMappedObjects();
	}

	@Entity( name = "Shelf" )
	@Table( name = "SHELF" )
	public static class Shelf {
		@Id
		private String shelfid;

		private String area;

		private Integer shelfNumber;

		private Integer position;

		@OneToMany
		@JoinTable( name = "SHELF_BOOK", joinColumns = @JoinColumn( name = "shelfid" ), inverseJoinColumns = @JoinColumn( name = "fileid" ) )
		private Set<Book> books;

		public Shelf() {
		}

		public String getShelfid() {
			return shelfid;
		}

		public void setShelfid(String shelfid) {
			this.shelfid = shelfid;
		}

		public String getArea() {
			return area;
		}

		public void setArea(String area) {
			this.area = area;
		}

		public Integer getShelfNumber() {
			return shelfNumber;
		}

		public void setShelfNumber(Integer shelfNumber) {
			this.shelfNumber = shelfNumber;
		}

		public Integer getPosition() {
			return position;
		}

		public void setPosition(Integer position) {
			this.position = position;
		}

		public Set<Book> getBooks() {
			return books;
		}

		public void setBooks(Set<Book> books) {
			this.books = books;
		}
	}

	;

	@Entity( name = "Book" )
	@Table( name = "BOOK_T" )
	public static class Book {
		@Id
		private String fileid;

		private String title;

		@ManyToOne( optional = false, fetch = FetchType.EAGER )
		@JoinTable( name = "SHELF_BOOK" )
		@JoinColumn( name = "shelfid" )
		private Shelf shelf;

		public Book() {
		}

		public Book(final String fileid) {
			this.fileid = fileid;
		}

		public String getFileId() {
			return fileid;
		}

		public void setFileId(final String fileid) {
			this.fileid = fileid;
		}

		public String getTitle() {
			return title;
		}

		public void setTitle(final String title) {
			this.title = title;
		}

		public Shelf getShelf() {
			return shelf;
		}

		public void setShelf(Shelf shelf) {
			this.shelf = shelf;
		}
	}
}
