/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.locking;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.Hibernate;
import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static jakarta.persistence.LockModeType.PESSIMISTIC_FORCE_INCREMENT;
import static jakarta.persistence.LockModeType.PESSIMISTIC_WRITE;
import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("JUnitMalformedDeclaration")
@DomainModel( annotatedClasses = {
		LockExistingBytecodeProxyTest.Book.class,
		LockExistingBytecodeProxyTest.Page.class
})
@SessionFactory
@BytecodeEnhanced
@Jira( "https://hibernate.atlassian.net/browse/HHH-17828" )
public class LockExistingBytecodeProxyTest {

	@Test
	public void testFindAndLockAfterFind(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Page page = session.find( Page.class, 1 );
			assertThat( Hibernate.isInitialized( page.book ) ).isFalse();

			final Book book = session.find( Book.class, 1, PESSIMISTIC_WRITE );
			assertThat( book ).isSameAs( page.book );
			assertThat( session.getLockMode( book ) ).isEqualTo( PESSIMISTIC_WRITE );
			assertThat( Hibernate.isInitialized( book ) ).isTrue();
		} );
	}

	@Test
	public void testLockAfterFind(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Page page = session.find( Page.class, 1 );
			assertThat( Hibernate.isInitialized( page.book ) ).isFalse();

			session.lock( page.book, PESSIMISTIC_FORCE_INCREMENT );
			assertThat( session.getLockMode( page.book ) ).isEqualTo( PESSIMISTIC_FORCE_INCREMENT );
			assertThat( Hibernate.isInitialized( page.book ) ).isTrue();
		} );
	}

	@BeforeEach
	public void createTestData(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Book book = new Book( 1, "My Story" );
			final Page page = new Page( 1, book );
			session.persist( book );
			session.persist( page );
		} );
	}

	@AfterEach
	public void dropTestData(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Entity(name="Book")
	@Table(name="books")
	@SuppressWarnings({"FieldCanBeLocal", "unused"})
	public static class Book {
		@Id
		private Integer id;
		private String name;
		@Version
		private Long version;

		public Book() {
		}

		public Book(Integer id, String name) {
			this.id = id;
			this.name = name;
		}
	}

	@Entity(name="Page")
	@Table(name="pages")
	@SuppressWarnings({"FieldCanBeLocal", "unused"})
	public static class Page {
		@Id
		private Integer id;
		@ManyToOne( fetch = FetchType.LAZY )
		private Book book;

		public Page() {
		}

		public Page(Integer id, Book book) {
			this.id = id;
			this.book = book;
		}
	}
}
