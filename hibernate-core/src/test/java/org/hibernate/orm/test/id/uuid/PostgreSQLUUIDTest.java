/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.id.uuid;

import java.util.List;
import java.util.UUID;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Query;

import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.query.NativeQuery;
import org.hibernate.type.StandardBasicTypes;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

/**
 * @author Vlad Mihalcea
 */
@RequiresDialect(value = PostgreSQLDialect.class)
@DomainModel(annotatedClasses = { PostgreSQLUUIDTest.Book.class })
@SessionFactory
public class PostgreSQLUUIDTest {

	private UUID id;

	@BeforeEach
	void setUp(SessionFactoryScope scope) {
		id = scope.fromTransaction( session -> {
			final Book book = new Book();
			book.title = "High-Performance Java Persistence";
			book.author = "Vlad Mihalcea";
			session.persist( book );
			return book.id;
		} );
		assertThat( id, notNullValue() );
	}

	@AfterEach
	void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testJPQL(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final List<UUID> books = session.createQuery(
					"select b.id " +
							"from Book b " +
							"where b.id = :id", UUID.class)
					.setParameter( "id", id )
					.getResultList();
			assertThat( books, hasSize( 1 ) );
		} );
	}

	@Test
	public void testNativeSQL(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final List<?> books = session.createNativeQuery(
					"select b.id as id " +
							"from Book b " +
							"where b.id = :id")
					.setParameter( "id", id )
					.unwrap( NativeQuery.class )
					.addScalar( "id", StandardBasicTypes.UUID )
					.getResultList();
			assertThat( books, hasSize( 1 ) );
		} );
	}

	@Test
	@JiraKey( value = "HHH-14358" )
	public void testUUIDNullBinding(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Query query = session.createQuery( "SELECT b FROM Book b WHERE :id is null or b.id = :id", Book.class );
					query.setParameter("id", null);
					List<?> results = Assertions.assertDoesNotThrow( query::getResultList,
									"Should not throw a PSQLException of type \"could not determine data type of parameter\" " );
					Assertions.assertEquals( 1, results.size() );
				}
		);
	}

	@Entity(name = "Book")
	static class Book {

		@Id
		@GeneratedValue
		UUID id;

		String title;

		String author;

	}
}
