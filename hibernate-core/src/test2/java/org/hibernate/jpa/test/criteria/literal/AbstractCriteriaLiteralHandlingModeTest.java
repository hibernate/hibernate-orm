/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.criteria.literal;

import java.util.List;
import java.util.Map;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Tuple;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.hibernate.dialect.H2Dialect;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.jdbc.SQLStatementInterceptor;
import org.hibernate.test.util.jdbc.PreparedStatementSpyConnectionProvider;
import org.junit.Before;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.hibernate.testing.transaction.TransactionUtil.setJdbcTimeout;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Vlad Mihalcea
 */
public abstract class AbstractCriteriaLiteralHandlingModeTest extends BaseEntityManagerFunctionalTestCase {

	private SQLStatementInterceptor sqlStatementInterceptor;

	@Override
	protected void addConfigOptions(Map options) {
		sqlStatementInterceptor = new SQLStatementInterceptor( options );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
			Book.class
		};
	}

	@Before
	public void init() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			Book book = new Book();
			book.id = 1;
			book.name = bookName();

			entityManager.persist( book );
		} );
	}

	@Test
	public void testLiteralHandlingMode() throws Exception {
		doInJPA( this::entityManagerFactory, entityManager -> {
			final CriteriaBuilder cb = entityManager.getCriteriaBuilder();

			final CriteriaQuery<Tuple> query = cb.createQuery( Tuple.class );

			final Root<Book> entity = query.from( Book.class );
			query.where(
				cb.and(
					cb.equal(
							entity.get( "id" ),
							cb.literal( 1 )
					),
					cb.equal(
							entity.get( "name" ),
							cb.literal( bookName() )
					)
				)
			);

			query.multiselect(
					cb.literal( "abc" ),
					entity.get( "name" )
			);

			sqlStatementInterceptor.clear();

			List<Tuple> tuples = entityManager.createQuery( query ).getResultList();
			assertEquals( 1, tuples.size() );

			sqlStatementInterceptor.assertExecuted( expectedSQL() );
		} );
	}

	protected abstract String expectedSQL();

	@Entity(name = "Book")
	public static class Book {

		@Id
		private Integer id;

		private String name;
	}

	protected String bookName() {
		return "Vlad's High-Performance Java Persistence";
	}
}
