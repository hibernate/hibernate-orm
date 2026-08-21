/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.hql;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.boot.model.FunctionContributor;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.testing.orm.junit.BootstrapServiceRegistry;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Statement;

/**
 * @author Vlad Mihalcea
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@RequiresDialect(PostgreSQLDialect.class)
@BootstrapServiceRegistry( javaServices = @BootstrapServiceRegistry.JavaService(
		role = FunctionContributor.class,
		impl = PostgreSQLFunctionSelectClauseTest.FunctionContributorImpl.class
) )
@DomainModel( annotatedClasses = PostgreSQLFunctionWhereClauseTest.Book.class )
@SessionFactory
public class PostgreSQLFunctionWhereClauseTest {
	@BeforeEach
	void setUp(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (entityManager) -> entityManager.doWork( (connection) -> {
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
		} ) );

		factoryScope.inTransaction( (entityManager) -> {
			//tag::hql-user-defined-function-postgresql-entity-example[]
			Book book = new Book();

			book.setIsbn("978-9730228236");
			book.setTitle("High-Performance Java Persistence");
			book.setAuthor("Vlad Mihalcea");
			book.setPriceCents(4500);

			entityManager.persist(book);
			//end::hql-user-defined-function-postgresql-entity-example[]
		} );
	}

	@AfterEach
	public void tearDown(SessionFactoryScope factoryScope) {
			factoryScope.inTransaction( (session) -> session.doWork( (connection) -> {
			try(Statement statement = connection.createStatement()) {
				statement.executeUpdate(
						"DROP FUNCTION apply_vat(integer)"
				);
			}
		} ) );

		factoryScope.dropData();
	}

	@Test
	public void testHibernatePassThroughFunction(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (entityManager) -> {
			//tag::hql-user-defined-function-postgresql-where-clause-example[]
			var books = entityManager.createQuery(
							"select b " +
							"from Book b " +
							"where apply_vat(b.priceCents) = :price ", Book.class)
					.setParameter("price", 5400)
					.getResultList();

			Assertions.assertTrue( books
					.stream()
					.anyMatch( book -> "High-Performance Java Persistence".equals(book.getTitle())) );
			//end::hql-user-defined-function-postgresql-where-clause-example[]
		} );
	}

	@Test
	public void testCustomFunction(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (entityManager) -> {
			//tag::hql-user-defined-function-postgresql-jpql-example[]
			var books = entityManager.createQuery(
							"select b " +
							"from Book b " +
							"where function('apply_vat', b.priceCents) = :price ", Book.class)
					.setParameter("price", 5400)
					.getResultList();

			Assertions.assertTrue( books
					.stream()
					.anyMatch( book -> "High-Performance Java Persistence".equals(book.getTitle())) );
			//end::hql-user-defined-function-postgresql-jpql-example[]
		} );
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
