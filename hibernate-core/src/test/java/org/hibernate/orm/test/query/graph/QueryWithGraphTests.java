/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.graph;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import org.hibernate.Hibernate;
import org.hibernate.annotations.NamedEntityGraph;
import org.hibernate.graph.spi.RootGraphImplementor;
import org.hibernate.orm.test.query.resultmapping.dynamic.Book;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.FailureExpected;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Steve Ebersole
 */
@DomainModel(annotatedClasses = {Book.class, QueryWithGraphTests.Publisher.class})
@SessionFactory
@FailureExpected(reason = "Need https://github.com/jakartaee/persistence/pull/842")
public class QueryWithGraphTests {
	@BeforeEach
	void setUp(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			var book = new Book( 1, "The Two Towers", "987-654", LocalDate.now() );
			session.persist( book );

			var publisher = new Publisher( 1, "Me", List.of(book) );
			session.persist( publisher );
		} );
	}

	@AfterEach
	void tearDown(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	@Test
	void simpleTest(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			var results = session.createQuery( "from Publisher", Publisher.class ).list();
			assertThat( results ).hasSize( 1 );
			assertThat( Hibernate.isInitialized( results.get( 0 ).books ) ).isFalse();
		} );
		factoryScope.inTransaction( (session) -> {
			final RootGraphImplementor<Publisher> entityGraph = (RootGraphImplementor<Publisher>) session.getEntityGraph( "pub-with-books" );
			var results = session.createQuery( "from Publisher", entityGraph ).list();
			assertThat( results ).hasSize( 1 );
			assertThat( Hibernate.isInitialized( results.get( 0 ).books ) ).isTrue();
		} );
	}

	@Entity(name="Publisher")
	@Table(name="Publisher")
	@NamedEntityGraph(
			name = "pub-with-books",
			graph = "id, name, books"
	)
	public static class Publisher {
		@Id
		private Integer id;
		private String name;
		@OneToMany
		@JoinColumn(name = "book_fk")
		private List<Book> books;

		public Publisher() {
		}

		public Publisher(Integer id, String name, List<Book> books) {
			this.id = id;
			this.name = name;
			this.books = books;
		}
	}
}
