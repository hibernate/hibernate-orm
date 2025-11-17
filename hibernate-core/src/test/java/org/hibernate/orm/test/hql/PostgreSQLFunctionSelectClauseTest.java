/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.hql;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Tuple;
import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.FunctionContributor;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.testing.orm.junit.BootstrapServiceRegistry;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.type.StandardBasicTypes;
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
@DomainModel(annotatedClasses = PostgreSQLFunctionSelectClauseTest.Book.class)
@SessionFactory
public class PostgreSQLFunctionSelectClauseTest {
	@BeforeEach
	void setUp(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> session.doWork( (connection) -> {
			try(Statement statement = connection.createStatement()) {
				statement.executeUpdate(
						"CREATE OR REPLACE FUNCTION apply_vat(integer) RETURNS integer " +
						"   AS 'select cast(($1 * 1.2) as integer);' " +
						"   LANGUAGE SQL " +
						"   IMMUTABLE " +
						"   RETURNS NULL ON NULL INPUT;"
				);
			}
		} ) );

		factoryScope.inTransaction( (session) -> {
			var book = new Book();
			book.setIsbn("978-9730228236");
			book.setTitle("High-Performance Java Persistence");
			book.setAuthor("Vlad Mihalcea");
			book.setPriceCents(4500);
			session.persist(book);
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
	public void testHibernateSelectClauseFunction(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (entityManager) -> {
			//tag::hql-user-defined-function-postgresql-select-clause-example[]
			var books = entityManager.createQuery(
							"select b.title as title, apply_vat(b.priceCents) as price " +
							"from Book b " +
							"where b.author = :author ", Tuple.class)
					.setParameter("author", "Vlad Mihalcea")
					.getResultList();
			//end::hql-user-defined-function-postgresql-select-clause-example[]

			Assertions.assertEquals( 1, books.size() );

			Tuple book = books.get(0);
			Assertions.assertEquals( "High-Performance Java Persistence", book.get("title") );
			Assertions.assertEquals( 4500 * 1.2, ((Number) book.get("price")).intValue() );
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

	public static class FunctionContributorImpl implements FunctionContributor {
		@Override
		public void contributeFunctions(FunctionContributions functionContributions) {
			//tag::hql-user-defined-functions-register-metadata-builder-example[]
			functionContributions.getFunctionRegistry().namedDescriptorBuilder( "apply_vat" )
					.setInvariantType( functionContributions.getTypeConfiguration().getBasicTypeRegistry().resolve( StandardBasicTypes.INTEGER ) )
					.setArgumentsValidator( StandardArgumentsValidators.of( Integer.class ) )
					.register();
			//end::hql-user-defined-functions-register-metadata-builder-example[]
		}
	}
}
