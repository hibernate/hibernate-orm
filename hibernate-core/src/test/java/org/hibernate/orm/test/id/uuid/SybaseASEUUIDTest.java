/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.id.uuid;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.dialect.SybaseASEDialect;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.util.uuid.SafeRandomUUIDGenerator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Jan Schatteman
 */
@RequiresDialect(value = SybaseASEDialect.class)
@DomainModel(annotatedClasses = { SybaseASEUUIDTest.Book.class })
@SessionFactory
public class SybaseASEUUIDTest {

	private UUID uuid;

	@BeforeEach
	void setUp(SessionFactoryScope scope) {
		this.uuid = scope.fromTransaction( session -> {
			UUID uuid = UUID.randomUUID();
			// Create a UUID with trailing zeros
			while ( SafeRandomUUIDGenerator.isSafeUUID(uuid) ) {
				uuid = UUID.randomUUID();
			}
			final Book book = new Book(uuid, "John Doe");
			session.persist( book );
			return uuid;
		} );
	}

	@AfterEach
	void tearDown(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createMutationQuery( "delete from Book" ).executeUpdate();
				}
		);
	}

	@Test
	@JiraKey( value = "" )
	public void testTrailingZeroByteTruncation(SessionFactoryScope scope) {
		scope.inSession(
				session -> {
					session.doWork(
							connection -> {
								Statement st = connection.createStatement();
								ResultSet rs = st.executeQuery( "select id from Book" );
								rs.next();
								byte[] barr = rs.getBytes( 1 );
								assertEquals( 15, barr.length );
							}
					);
				}
		);
		scope.inTransaction(
				session -> {
					Book b = session.createQuery( "from Book", Book.class ).getSingleResult();
					UUID uuid = b.id;
					ByteBuffer bb = ByteBuffer.allocate( 8 );
					bb.putLong( uuid.getLeastSignificantBits() );
					byte[] arr = bb.array();
					assertEquals(8, arr.length);
					assertEquals(this.uuid, uuid);
				}
		);
	}

	@Entity(name = "Book")
	static class Book {
		@Id
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
