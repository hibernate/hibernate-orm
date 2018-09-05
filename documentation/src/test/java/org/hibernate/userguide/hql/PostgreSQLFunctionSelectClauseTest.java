/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.userguide.hql;

import java.sql.Statement;
import java.util.List;
import java.util.Map;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Tuple;

import org.hibernate.Session;
import org.hibernate.boot.MetadataBuilder;
import org.hibernate.boot.spi.MetadataBuilderContributor;
import org.hibernate.dialect.PostgreSQL82Dialect;
import org.hibernate.dialect.function.StandardSQLFunction;
import org.hibernate.jpa.boot.internal.EntityManagerFactoryBuilderImpl;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.hibernate.type.StandardBasicTypes;

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
public class PostgreSQLFunctionSelectClauseTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
			Book.class
		};
	}

	@Override
	protected void addMappings(Map settings) {
		//tag::hql-user-defined-functions-register-metadata-builder-example[]
		settings.put( "hibernate.metadata_builder_contributor",
			(MetadataBuilderContributor) metadataBuilder ->
				metadataBuilder.applySqlFunction(
					"apply_vat",
					new StandardSQLFunction(
						"apply_vat",
						StandardBasicTypes.INTEGER
					)
				)
		);
		//end::hql-user-defined-functions-register-metadata-builder-example[]
	}

	@Override
	protected void afterEntityManagerFactoryBuilt() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			entityManager.unwrap( Session.class ).doWork(
				connection -> {
					try(Statement statement = connection.createStatement()) {
						statement.executeUpdate(
							"CREATE OR REPLACE FUNCTION apply_vat(integer) RETURNS integer " +
							"   AS 'select cast(($1 * 1.2) as integer);' " +
							"   LANGUAGE SQL " +
							"   IMMUTABLE " +
							"   RETURNS NULL ON NULL INPUT;"
						);
					}
				}
			);
		});

		doInJPA( this::entityManagerFactory, entityManager -> {
			Book book = new Book();

			book.setIsbn( "978-9730228236" );
			book.setTitle( "High-Performance Java Persistence" );
			book.setAuthor( "Vlad Mihalcea" );
			book.setPriceCents( 4500 );

			entityManager.persist( book );
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
	public void testHibernateSelectClauseFunction() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			//tag::hql-user-defined-function-postgresql-select-clause-example[]
			List<Tuple> books = entityManager.createQuery(
				"select b.title as title, apply_vat(b.priceCents) as price " +
				"from Book b " +
				"where b.author = :author ", Tuple.class )
			.setParameter( "author", "Vlad Mihalcea" )
			.getResultList();

			assertEquals( 1, books.size() );

			Tuple book = books.get( 0 );
			assertEquals( "High-Performance Java Persistence", book.get( "title" ) );
			assertEquals( 5400, ((Number) book.get( "price" )).intValue() );

			//end::hql-user-defined-function-postgresql-select-clause-example[]
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
