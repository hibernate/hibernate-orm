/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.onetoone;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

import org.hibernate.Hibernate;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Andrea Boriero
 */
@DomainModel(
		annotatedClasses = {
				OneToOneLazyTest.Title.class,
				OneToOneLazyTest.Book.class
		}
)
@SessionFactory
public class OneToOneLazyTest {

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				sesison -> {
					Title title = new Title( 1L );
					Book book = new Book( 2L, title );

					sesison.persist( title );
					sesison.persist( book );
				}
		);
	}

	@Test
	public void testLazyLoading(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Book book = session.find( Book.class, 2L );
					Title title = book.getTitle();
					assertThat( Hibernate.isInitialized( title ), is( false ) );
					assertThat( title, notNullValue() );
					assertThat( title.getId(), is( 1L ) );
				}
		);

		scope.inTransaction(
				session -> {
					Title title = session.find( Title.class, 1L );
					Book book = title.getBook();
					assertThat( Hibernate.isInitialized( book ), is( true ) );
					assertThat( book, notNullValue() );
					assertThat( book.getId(), is( 2L ) );
				}
		);
	}

	@Entity(name = "Book")
	public static class Book {

		@Id
		private Long id;

		@OneToOne(fetch = FetchType.LAZY)
		private Title title;

		public Book() {
		}

		public Book(Long id, Title title) {
			this.id = id;
			this.title = title;
			title.setBook( this );
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Title getTitle() {
			return title;
		}
	}

	@Entity(name = "Title")
	public static class Title {

		@Id
		private Long id;

		@OneToOne(fetch = FetchType.LAZY, mappedBy = "title")
		private Book book;

		public Title() {
		}

		public Title(Long id) {
			this.id = id;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Book getBook() {
			return book;
		}

		public void setBook(Book book) {
			this.book = book;
		}
	}
}
