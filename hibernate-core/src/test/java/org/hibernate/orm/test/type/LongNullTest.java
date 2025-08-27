/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.type;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.type.SqlTypes;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

@DomainModel(annotatedClasses = LongNullTest.Foo.class)
@SessionFactory
public class LongNullTest {

	@Entity
	public static final class Foo {
		@Id
		@GeneratedValue
		private Long id;

		@JdbcTypeCode(SqlTypes.LONG32NVARCHAR)
		@Column
		private String field = null;

		@JdbcTypeCode(SqlTypes.LONG32VARCHAR)
		@Column
		private String nfield = null;

		@JdbcTypeCode(SqlTypes.LONG32VARBINARY)
		@Column
		private byte[] bfield = null;
	}

	@Test
	public void testNull(SessionFactoryScope scope) {
		final var expected = scope.fromTransaction( s -> {
			final var foo = new Foo();
			s.persist( foo );
			return foo;
		} );
		final var actual = scope.fromSession( s -> s.find( Foo.class, expected.id ) );
		assertEquals( expected.field, actual.field );
		assertEquals( expected.nfield, actual.nfield );
		assertArrayEquals( expected.bfield, actual.bfield );
	}

	@Test
	public void testNonNull(SessionFactoryScope scope) {
		final var expected = scope.fromTransaction( s -> {
			final var foo = new Foo();
			foo.bfield = "ABC".getBytes();
			foo.field = "DEF";
			foo.nfield = "GHI";
			s.persist( foo );
			return foo;
		} );
		final var actual = scope.fromSession( s -> s.find( Foo.class, expected.id ) );
		assertEquals( expected.field, actual.field );
		assertEquals( expected.nfield, actual.nfield );
		assertArrayEquals( expected.bfield, actual.bfield );
	}
}
