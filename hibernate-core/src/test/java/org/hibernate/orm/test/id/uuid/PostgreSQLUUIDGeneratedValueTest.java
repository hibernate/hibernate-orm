/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.id.uuid;

import java.util.List;
import java.util.UUID;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.query.NativeQuery;
import org.hibernate.type.StandardBasicTypes;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

/**
 * @author Vlad Mihalcea
 */
@RequiresDialect(value = PostgreSQLDialect.class, version = 940)
@DomainModel(annotatedClasses = { PostgreSQLUUIDGeneratedValueTest.Book.class })
@SessionFactory
public class PostgreSQLUUIDGeneratedValueTest {

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

	@Entity(name = "Book")
	static class Book {

		@Id
		@GeneratedValue
		UUID id;

		String title;

		String author;

	}
}
