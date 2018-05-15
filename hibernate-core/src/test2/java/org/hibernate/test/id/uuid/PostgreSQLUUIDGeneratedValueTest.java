/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.id.uuid;

import java.util.List;
import java.util.UUID;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.dialect.PostgreSQL82Dialect;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.hibernate.query.NativeQuery;
import org.hibernate.type.PostgresUUIDType;
import org.hibernate.type.UUIDBinaryType;

import org.hibernate.testing.RequiresDialect;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Vlad Mihalcea
 */
@RequiresDialect(PostgreSQL82Dialect.class)
public class PostgreSQLUUIDGeneratedValueTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
			Book.class
		};
	}

	private Book book;

	@Override
	protected void afterEntityManagerFactoryBuilt() {
		book = new Book();

		doInJPA( this::entityManagerFactory, entityManager -> {
			book.setTitle( "High-Performance Java Persistence" );
			book.setAuthor( "Vlad Mihalcea" );

			entityManager.persist( book );
		} );

		assertNotNull( book.getId() );
	}

	@Test
	public void testJPQL() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			List<UUID> books = entityManager.createQuery(
				"select b.id " +
				"from Book b " +
				"where b.id = :id")
			.setParameter( "id", book.id )
			.getResultList();

			assertEquals(1, books.size());
		} );
	}

	@Test
	public void testNativeSQL() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			List<UUID> books = entityManager.createNativeQuery(
				"select b.id as id " +
				"from Book b " +
				"where b.id = :id")
			.setParameter( "id", book.id )
			.unwrap( NativeQuery.class )
			.addScalar( "id", PostgresUUIDType.INSTANCE )
			.getResultList();

			assertEquals(1, books.size());
		} );
	}

	@Entity(name = "Book")
	public static class Book {

		@Id
		@GeneratedValue
		private UUID id;

		private String title;

		private String author;

		public UUID getId() {
			return id;
		}

		public void setId(UUID id) {
			this.id = id;
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
	}
}
