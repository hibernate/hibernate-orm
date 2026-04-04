/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query;

import org.hibernate.dialect.H2Dialect;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Reproducer for HHH-20261:
 * Poor error messages when mapping native-query results to
 * a custom class/record whose constructor does not match,
 * or when multiple constructors match.
 */
@DomainModel
@SessionFactory
@Jira("https://hibernate.atlassian.net/browse/HHH-20261")
public class NativeQueryConstructorErrorTest {

	@Test
	@RequiresDialect(H2Dialect.class)
	public void testConstructorMismatch(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Exception ex = assertThrows( Exception.class,
					() -> session.createNativeQuery( "select 'hello' as id, 'abc' as name", MiniInfo.class )
							.getSingleResult() );

			String msg = ex.getMessage();

			assertTrue( msg.contains(
					"Could not instantiate query result type - expected 'MiniInfo(Integer, int)' but found 'MiniInfo(String, String)' in 'org.hibernate.orm.test.query.NativeQueryConstructorErrorTest$MiniInfo' due to: argument type mismatch" ) );
		} );
	}

	@Test
	@RequiresDialect(H2Dialect.class)
	public void testMultipleMatchingConstructors(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Exception ex = assertThrows( Exception.class,
					() -> session.createNativeQuery( "select 1 as id, 'abc' as name", Ambiguous.class )
							.getSingleResult() );

			String msg = ex.getMessage();

			assertTrue( msg.contains( "Result class must have exactly one constructor with 2 parameters - found " ) );
			assertTrue( msg.contains( "Ambiguous(Object, Object)" ) );
			assertTrue( msg.contains( "Ambiguous(Number, CharSequence)" ) );

		} );
	}

	public record MiniInfo(Integer id, int age) {
	}

	public static class Ambiguous {
		public Ambiguous(Number id, CharSequence name) {
		}

		public Ambiguous(Object id, Object name) {
		}
	}
}
