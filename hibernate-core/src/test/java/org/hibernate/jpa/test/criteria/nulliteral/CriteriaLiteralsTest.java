/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.criteria.nulliteral;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderColumn;
import javax.persistence.Tuple;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.ListJoin;
import javax.persistence.criteria.Root;

import org.hibernate.dialect.H2Dialect;
import org.hibernate.exception.SQLGrammarException;
import org.hibernate.hql.internal.ast.QuerySyntaxException;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.test.util.jdbc.PreparedStatementSpyConnectionProvider;
import org.junit.Before;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Vlad Mihalcea
 */
@RequiresDialect(H2Dialect.class)
public class CriteriaLiteralsTest extends BaseEntityManagerFunctionalTestCase {
	
	private PreparedStatementSpyConnectionProvider connectionProvider = new PreparedStatementSpyConnectionProvider();

	@Override
	protected Map getConfig() {
		Map config = super.getConfig();
		config.put(
				org.hibernate.cfg.AvailableSettings.CONNECTION_PROVIDER,
				connectionProvider
		);
		return config;
	}

	@Override
	public void releaseResources() {
		super.releaseResources();
		connectionProvider.stop();
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
					expected.getCause().getClass()
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

			connectionProvider.clear();
			List<Tuple> tuples = entityManager.createQuery( query )
					.getResultList();

			assertEquals(
					1,
					connectionProvider.getPreparedStatements().size()
			);
			assertNotNull( connectionProvider.getPreparedStatement(
					"select 'abc' as col_0_0_, criteriali0_.name as col_1_0_ from Book criteriali0_ where criteriali0_.name=?" ) );
			assertTrue( tuples.isEmpty() );
		} );
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

			connectionProvider.clear();
			entityManager.createQuery( query ).getResultList();
			assertEquals(
					1,
					connectionProvider.getPreparedStatements().size()
			);
			assertNotNull( connectionProvider.getPreparedStatement(
					"select authors1_.name as col_0_0_ from Book criteriali0_ inner join Author authors1_ on criteriali0_.id=authors1_.book_id where criteriali0_.name=? and authors1_.index_id=0" )
			);
		} );
	}

	@Test
	public void testLiteralsInSelectClause() throws Exception {
		try {
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
		catch ( Exception expected ) {
			assertEquals(
					QuerySyntaxException.class,
					expected.getCause().getClass()
			);
		}
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
