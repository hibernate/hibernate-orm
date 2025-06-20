/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.id.uuid;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.dialect.SybaseASEDialect;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.type.descriptor.java.UUIDJavaType;
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

	private static final UUID THE_UUID = UUID.fromString("53886a8a-7082-4879-b430-25cb94415b00");

	@BeforeEach
	void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createNativeQuery( "insert into book (id, author) values (?,?)" )
					.setParameter( 1, UUIDJavaType.ToBytesTransformer.INSTANCE.transform( THE_UUID ) )
					.setParameter( 2, "John Doe" )
					.executeUpdate();
		} );
	}

	@AfterEach
	void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	@JiraKey( value = "HHH-17246" )
	public void testTrailingZeroByteTruncation(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					// Assert that our assumption is correct i.e. Sybase truncates trailing zero bytes
					assertEquals( 15, session.createNativeQuery("select id from book", byte[].class).getSingleResult().length );
					Book b = session.createQuery( "from Book", Book.class ).getSingleResult();
					assertEquals( THE_UUID, b.id );
				}
		);
	}

	@Entity(name = "Book")
	@Table(name = "book")
	static class Book {
		@Id
		// The purpose is to effectively provoke the trailing 0 bytes truncation
		@Column(columnDefinition = "varbinary(16)")
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
