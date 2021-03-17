/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.userguide.hql;

import java.sql.Statement;
import java.util.List;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.Session;
import org.hibernate.dialect.PostgreSQL82Dialect;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.RequiresDialect;
import org.junit.After;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Vlad Mihalcea
 */
@RequiresDialect(PostgreSQL82Dialect.class)
public class PostgreSQLFunctionWhereClauseTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
			Book.class
		};
	}

	@Override
	protected void afterEntityManagerFactoryBuilt() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			entityManager.unwrap( Session.class ).doWork(
				connection -> {
					try(Statement statement = connection.createStatement()) {
						//tag::hql-user-defined-function-postgresql-example[]
						statement.executeUpdate(
							"CREATE OR REPLACE FUNCTION apply_vat(integer) RETURNS integer " +
							"   AS 'select cast(($1 * 1.2) as integer);' " +
							"   LANGUAGE SQL " +
							"   IMMUTABLE " +
							"   RETURNS NULL ON NULL INPUT;"
						);
						//end::hql-user-defined-function-postgresql-example[]
					}
				}
			);
		});

		doInJPA( this::entityManagerFactory, entityManager -> {
			//tag::hql-user-defined-function-postgresql-entity-example[]
			Book book = new Book();

			book.setIsbn( "978-9730228236" );
			book.setTitle( "High-Performance Java Persistence" );
			book.setAuthor( "Vlad Mihalcea" );
			book.setPriceCents( 4500 );

			entityManager.persist( book );
			//end::hql-user-defined-function-postgresql-entity-example[]
		});
	}

	@After
	public void destroy() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			entityManager.unwrap( Session.class ).doWork(
				connection -> {
					try(Statement statement = connection.createStatement()) {
						statement.executeUpdate(
							"DROP FUNCTION apply_vat(integer)"
						);
					}
				}
			);
		});
	}

	@Test
	public void testHibernatePassThroughFunction() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			//tag::hql-user-defined-function-postgresql-where-clause-example[]
			List<Book> books = entityManager.createQuery(
				"select b " +
				"from Book b " +
				"where apply_vat(b.priceCents) = :price ", Book.class )
			.setParameter( "price", 5400 )
			.getResultList();

			assertTrue( books
				.stream()
				.filter( book -> "High-Performance Java Persistence".equals( book.getTitle() ) )
				.findAny()
				.isPresent()
			);
			//end::hql-user-defined-function-postgresql-where-clause-example[]
		});
	}

	@Test
	public void testCustomFunction() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			//tag::hql-user-defined-function-postgresql-jpql-example[]
			List<Book> books = entityManager.createQuery(
				"select b " +
				"from Book b " +
				"where function('apply_vat', b.priceCents) = :price ", Book.class )
			.setParameter( "price", 5400 )
			.getResultList();

			assertTrue( books
				.stream()
				.filter( book -> "High-Performance Java Persistence".equals( book.getTitle() ) )
				.findAny()
				.isPresent()
			);
			//end::hql-user-defined-function-postgresql-jpql-example[]
		});
	}

	@Entity(name = "Book")
	public static class Book {

		@Id
		private String isbn;

		private String title;

		private String author;

		private Integer priceCents;

		public String getIsbn() {
			return isbn;
		}

		public void setIsbn(String isbn) {
			this.isbn = isbn;
		}

		public String getTitle() {
			return title;
		}

		public void setTitle(String title) {
			this.title = title;
		}

		public String getAuthor() {
			return author;
		}

		public void setAuthor(String author) {
			this.author = author;
		}

		public Integer getPriceCents() {
			return priceCents;
		}

		public void setPriceCents(Integer priceCents) {
			this.priceCents = priceCents;
		}
	}
}
