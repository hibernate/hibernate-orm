/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.length;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

@SessionFactory
@DomainModel(annotatedClasses = WithLongByteArrays.class)
public class BinaryLengthTest {
	@Test
	public void testLength(SessionFactoryScope scope) {
		WithLongByteArrays arrays = new WithLongByteArrays();
		arrays.longish = "hello world ".repeat(2500).getBytes();
		arrays.long16 = "hello world ".repeat(2700).getBytes();
		arrays.long32 = "hello world ".repeat(20000).getBytes();
		// This is an important test that it's possible to handle
		// very long binary types in a way that's portable across
		// both Postgres (bytea) and other databases (blob)
		arrays.lob = "hello world ".repeat(40000).getBytes();
		scope.inTransaction(s -> s.persist(arrays));
		scope.inTransaction(s -> {
			WithLongByteArrays arrs = s.find(WithLongByteArrays.class, arrays.id);
			assertArrayEquals(arrs.longish, arrays.longish);
			assertArrayEquals(arrs.long16, arrays.long16);
			assertArrayEquals(arrs.long32, arrays.long32);
			assertArrayEquals(arrs.lob, arrays.lob);
		});
	}
}
