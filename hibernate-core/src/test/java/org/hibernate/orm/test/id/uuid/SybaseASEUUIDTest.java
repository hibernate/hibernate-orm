/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.id.uuid;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.SybaseASEDialect;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Jan Schatteman
 */
@RequiresDialect(value = SybaseASEDialect.class)
@DomainModel(annotatedClasses = { SybaseASEUUIDTest.Book.class })
@SessionFactory
public class SybaseASEUUIDTest {

	private static final UUID uuid = UUID.fromString("53886a8a-7082-4879-b430-25cb94415b00");

	@BeforeEach
	void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Book book = new Book(uuid, "John Doe");
			session.persist( book );
		} );
	}

	@AfterEach
	void tearDown(SessionFactoryScope scope) {
		scope.inTransaction(
			session -> session.createMutationQuery( "delete from Book" ).executeUpdate()
		);
	}

	@Test
	@JiraKey( value = "HHH-17246" )
	public void testTrailingZeroByteTruncation(SessionFactoryScope scope) {
		scope.inSession(
			session -> assertEquals( 15, session.createNativeQuery("select id from Book", byte[].class).getSingleResult().length )
		);
		scope.inTransaction(
				session -> {
					Book b = session.createQuery( "from Book", Book.class ).getSingleResult();
					assertEquals(uuid, b.id);
				}
		);
	}

	@Entity(name = "Book")
	static class Book {
		@Id
		// The purpose is to effectively provoke the trailing 0 bytes truncation
		@JdbcType( SybaseUuidAsVarbinaryJdbcType.class )
		UUID id;

		String author;

		public Book() {
		}

		public Book(UUID id, String author) {
			this.id = id;
			this.author = author;
		}
	}

}
