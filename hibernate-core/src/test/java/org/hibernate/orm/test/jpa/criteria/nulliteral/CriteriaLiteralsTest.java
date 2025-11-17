/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.criteria.nulliteral;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.ListJoin;
import jakarta.persistence.criteria.Root;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.exception.SQLGrammarException;
import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.Setting;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Vlad Mihalcea
 */
@RequiresDialect(H2Dialect.class)
@Jpa(
		annotatedClasses = {CriteriaLiteralsTest.Book.class, CriteriaLiteralsTest.Author.class},
		integrationSettings = @Setting(name = AvailableSettings.DIALECT_NATIVE_PARAM_MARKERS, value = "false"),
		useCollectingStatementInspector = true
)
public class CriteriaLiteralsTest {

	private SQLStatementInspector sqlStatementInspector;

	@BeforeEach
	public void init(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			Book book = new Book();
			book.name = "Java Persistence with Hibernate";

			Author author1 = new Author();
			author1.name = "Christian Bauer";

			Author author2 = new Author();
			author2.name = "Gavin Ling";

			book.authors.add( author1 );
			book.authors.add( author2 );
			entityManager.persist( book );
		} );
		scope.inTransaction( entityManager ->
			assertThrows(
					SQLGrammarException.class,
					() -> entityManager.createNativeQuery("SELECT REPEAT('abc' || ' ', 1000000000000) FROM MY_ENTITY" )
							.getSingleResult(),
					"Should have thrown exception!"
			)
		);

		sqlStatementInspector = scope.getCollectingStatementInspector();
		sqlStatementInspector.clear();
	}

	@AfterEach
	public void cleanupTestData(EntityManagerFactoryScope scope) {
		scope.getEntityManagerFactory().getSchemaManager().truncate();
	}

	@Test
	public void testLiteralsInWhereClause(EntityManagerFactoryScope scope) {

		scope.inTransaction( entityManager -> {
			final CriteriaBuilder cb = entityManager.getCriteriaBuilder();
			final CriteriaQuery<Tuple> query = cb.createQuery( Tuple.class );
			final Root<Book> entity = query.from( Book.class );
			query.where( cb.equal(
					entity.get( "name" ),
					cb.literal(
							"( SELECT REPEAT('abc' || ' ', 10000000000 FROM MY_ENTITY )" )
			) );

			query.multiselect(
					cb.literal( "abc" ),
					entity.get( "name" )
			);

			sqlStatementInspector.clear();
			List<Tuple> tuples = entityManager.createQuery( query )
					.getResultList();

			assertEquals( 1, sqlStatementInspector.getSqlQueries().size() );
			sqlStatementInspector.assertExecuted("select 'abc',b1_0.name from Book b1_0 where b1_0.name='( SELECT REPEAT(''abc'' || '' '', 10000000000 FROM MY_ENTITY )'");
			assertTrue( tuples.isEmpty() );
		} );
	}

	@Test
	public void testNumericLiteralsInWhereClause(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager ->
			testNumericLiterals(
				entityManager,
				"select 'abc',b1_0.name from Book b1_0 where b1_0.id=1"
			)
		);
	}

	@Test
	public void testNumericLiteralsInWhereClauseUsingBindParameters(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager ->
			testNumericLiterals(
				entityManager,
				"select 'abc',b1_0.name from Book b1_0 where b1_0.id=1"
			)
		);
	}

	private void testNumericLiterals(EntityManager entityManager, String expectedSQL) {
		final CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		final CriteriaQuery<Tuple> query = cb.createQuery( Tuple.class );
		final Root<Book> entity = query.from( Book.class );

		query.where( cb.equal(
				entity.get( "id" ),
				cb.literal( 1 )
		) );

		query.multiselect(
				cb.literal( "abc" ),
				entity.get( "name" )
		);

		sqlStatementInspector.clear();

		List<Tuple> tuples = entityManager.createQuery( query ).getResultList();
		assertEquals( 1, tuples.size() );

		sqlStatementInspector.assertExecuted( expectedSQL );
	}

	@Test
	public void testCriteriaParameters(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			final CriteriaBuilder cb = entityManager.getCriteriaBuilder();
			CriteriaQuery<String> query = cb.createQuery( String.class );
			Root<Book> root = query.from( Book.class );
			ListJoin<Book, Author> authors = root.joinList( "authors" );

			query.where( cb.equal(
					root.get( "name" ),
					"( SELECT REPEAT('abc' || ' ', 10000000000 FROM MY_ENTITY )"
			), cb.equal( authors.index(), 0 ) )
					.select( authors.get( "name" ) );

			sqlStatementInspector.clear();

			entityManager.createQuery( query ).getResultList();
			assertEquals( 1, sqlStatementInspector.getSqlQueries().size() );

			sqlStatementInspector.assertExecuted( "select a1_0.name from Book b1_0 join Author a1_0 on b1_0.id=a1_0.book_id where b1_0.name=? and a1_0.index_id=?" );
		} );
	}

	@Test
	public void testLiteralsInSelectClause(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			final CriteriaBuilder cb = entityManager.getCriteriaBuilder();
			CriteriaQuery<String> query = cb.createQuery( String.class );
			Root<Book> root = query.from( Book.class );
			root.joinList( "authors" );

			query.where( cb.equal(
					root.get( "name" ),
					"Java Persistence with Hibernate"
			))
			.select( cb.literal( "( SELECT REPEAT('abc' || ' ', 10000000000 FROM MY_ENTITY )" ) );

			entityManager.createQuery( query ).getResultList();
		} );
	}

	@Entity(name = "Book")
	public static class Book {

		@Id
		@GeneratedValue
		private Integer id;

		private String name;

		@OneToMany(mappedBy = "book", cascade = CascadeType.ALL)
		@OrderColumn(name = "index_id")
		private List<Author> authors = new ArrayList<>();
	}

	@Entity(name = "Author")
	public static class Author {

		@Id
		@GeneratedValue
		private Integer id;

		private String name;

		@ManyToOne
		private Book book;
	}
}
