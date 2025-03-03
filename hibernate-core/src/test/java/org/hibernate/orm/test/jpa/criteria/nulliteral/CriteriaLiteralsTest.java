/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.criteria.nulliteral;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
import org.hibernate.orm.test.jpa.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.jdbc.SQLStatementInterceptor;
import org.junit.Before;
import org.junit.Test;

import static java.lang.Boolean.FALSE;
import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Vlad Mihalcea
 */
@RequiresDialect(H2Dialect.class)
public class CriteriaLiteralsTest extends BaseEntityManagerFunctionalTestCase {

	private SQLStatementInterceptor sqlStatementInterceptor;

	@Override
	protected void addConfigOptions(Map options) {
		sqlStatementInterceptor = new SQLStatementInterceptor( options );
		options.put( AvailableSettings.DIALECT_NATIVE_PARAM_MARKERS, FALSE );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Book.class, Author.class };
	}

	@Before
	public void init() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			Book book = new Book();
			book.name = "Java Persistence with Hibernate";

			Author author1 = new Author();
			author1.name = "Christian Bauer";

			Author author2 = new Author();
			author1.name = "Gavin Ling";

			book.authors.add( author1 );
			book.authors.add( author2 );
			entityManager.persist( book );
		} );
		try {
			doInJPA( this::entityManagerFactory, entityManager -> {
				entityManager
						.createNativeQuery(
								"SELECT REPEAT('abc' || ' ', 1000000000000) FROM MY_ENTITY" )
						.getSingleResult();
			} );
			fail( "Should have thrown exception!" );
		}
		catch ( Exception expected ) {
			assertEquals(
					SQLGrammarException.class,
					expected.getClass()
			);
		}
	}

	@Test
	public void testLiteralsInWhereClause() throws Exception {

		doInJPA( this::entityManagerFactory, entityManager -> {

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

			sqlStatementInterceptor.clear();
			List<Tuple> tuples = entityManager.createQuery( query )
					.getResultList();

			assertEquals(
					1,
					sqlStatementInterceptor.getSqlQueries().size()
			);
			sqlStatementInterceptor.assertExecuted("select 'abc',b1_0.name from Book b1_0 where b1_0.name='( SELECT REPEAT(''abc'' || '' '', 10000000000 FROM MY_ENTITY )'");
			assertTrue( tuples.isEmpty() );
		} );
	}

	@Test
	public void testNumericLiteralsInWhereClause() throws Exception {
		doInJPA( this::entityManagerFactory, entityManager -> {
			testNumericLiterals(
				entityManager,
				"select 'abc',b1_0.name from Book b1_0 where b1_0.id=1"
			);
		} );
	}

	@Test
	public void testNumericLiteralsInWhereClauseUsingBindParameters() throws Exception {
		doInJPA( this::entityManagerFactory, entityManager -> {
			testNumericLiterals(
				entityManager,
				"select 'abc',b1_0.name from Book b1_0 where b1_0.id=1"
			);
		} );
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

		sqlStatementInterceptor.clear();

		List<Tuple> tuples = entityManager.createQuery( query ).getResultList();
		assertEquals( 1, tuples.size() );

		sqlStatementInterceptor.assertExecuted( expectedSQL );
	}

	@Test
	public void testCriteriaParameters() throws Exception {
		doInJPA( this::entityManagerFactory, entityManager -> {
			final CriteriaBuilder cb = entityManager.getCriteriaBuilder();
			CriteriaQuery<String> query = cb.createQuery( String.class );
			Root<Book> root = query.from( Book.class );
			ListJoin<Book, Author> authors = root.joinList( "authors" );

			query.where( cb.equal(
					root.get( "name" ),
					"( SELECT REPEAT('abc' || ' ', 10000000000 FROM MY_ENTITY )"
			), cb.equal( authors.index(), 0 ) )
					.select( authors.get( "name" ) );

			sqlStatementInterceptor.clear();
			entityManager.createQuery( query ).getResultList();
			assertEquals(
					1,
					sqlStatementInterceptor.getSqlQueries().size()
			);
			sqlStatementInterceptor.assertExecuted( "select a1_0.name from Book b1_0 join Author a1_0 on b1_0.id=a1_0.book_id where b1_0.name=? and a1_0.index_id=?" );
		} );
	}

	@Test
	public void testLiteralsInSelectClause() throws Exception {
		doInJPA( this::entityManagerFactory, entityManager -> {
			final CriteriaBuilder cb = entityManager.getCriteriaBuilder();
			CriteriaQuery<String> query = cb.createQuery( String.class );
			Root<Book> root = query.from( Book.class );
			ListJoin<Book, Author> authors = root.joinList( "authors" );

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
