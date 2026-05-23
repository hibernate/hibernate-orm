/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.graph;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityGraph;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import org.hibernate.Hibernate;
import org.hibernate.annotations.NamedEntityGraph;
import org.hibernate.graph.spi.RootGraphImplementor;
import org.hibernate.orm.test.query.resultmapping.dynamic.Book;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * @author Steve Ebersole
 */
@DomainModel(annotatedClasses = {Book.class, QueryWithGraphTests.Publisher.class})
@SessionFactory
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

	@Test
	void namedQueryAppliesEntityGraph(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			var results = session.createNamedQuery( "Publisher.findAllWithGraph", Publisher.class ).list();
			assertThat( results ).hasSize( 1 );
			assertThat( Hibernate.isInitialized( results.get( 0 ).books ) ).isTrue();
		} );
	}

	@Test
	void getEntityGraphReturnsMutableCopy(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			final EntityGraph<Publisher> entityGraph = session.getEntityGraph( Publisher.class, "pub-base" );
			assertThat( entityGraph.getName() ).isEqualTo( "pub-base" );
			assertThat( entityGraph.hasAttributeNode( "books" ) ).isFalse();

			entityGraph.addAttributeNode( "books" );
			assertThat( entityGraph.hasAttributeNode( "books" ) ).isTrue();

			final EntityGraph<Publisher> entityGraphAgain = session.getEntityGraph( Publisher.class, "pub-base" );
			assertThat( entityGraphAgain.getName() ).isEqualTo( "pub-base" );
			assertThat( entityGraphAgain.hasAttributeNode( "books" ) ).isFalse();
		} );
	}

	@Test
	void withEntityGraphAppliesEntityGraph(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			final EntityGraph<Publisher> entityGraph = session.getEntityGraph( Publisher.class, "pub-with-books" );
			final var query = session.createQuery( "from Publisher" );
			final var graphedQuery = query.withEntityGraph( entityGraph );
			assertThat( graphedQuery ).isNotSameAs( query );

			final var plainResults = query.asSelectionQuery(Publisher.class).getResultList();
			assertThat( plainResults ).hasSize( 1 );
			assertThat( Hibernate.isInitialized( plainResults.get( 0 ).books ) ).isFalse();
		} );
		factoryScope.inTransaction( (session) -> {
			final EntityGraph<Publisher> entityGraph = session.getEntityGraph( Publisher.class, "pub-with-books" );
			var results = session.createQuery( "from Publisher" )
					.withEntityGraph( entityGraph )
					.getResultList();
			assertThat( results ).hasSize( 1 );
			assertThat( Hibernate.isInitialized( results.get( 0 ).books ) ).isTrue();
		} );
	}

	@Test
	void ofTypeReturnsCloneWithCopiedParameters(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			final var query = session.createQuery( "select p.name from Publisher p where p.id = :id" );
			query.setParameter( "id", 1 );

			final var typedQuery = query.ofType( String.class );
			assertThat( typedQuery ).isNotSameAs( query );
			assertThat( typedQuery.getSingleResult() ).isEqualTo( "Me" );
		} );
	}

	@Test
	void resultSetMappingIsRejectedForJpaQuery(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> assertThatIllegalStateException()
				.isThrownBy( () -> session.createQuery( "from Publisher" )
						.withResultSetMapping( jakarta.persistence.sql.ResultSetMapping.entity( Publisher.class ) ) ) );
	}

	@Entity(name="Publisher")
	@Table(name="Publisher")
	@NamedQuery(
			name = "Publisher.findAllWithGraph",
			query = "from Publisher",
			resultClass = Publisher.class,
			entityGraph = "pub-with-books"
	)
	@NamedEntityGraph(
			name = "pub-with-books",
			graph = "id, name, books"
	)
	@NamedEntityGraph(
			name = "pub-base",
			graph = "id, name"
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
