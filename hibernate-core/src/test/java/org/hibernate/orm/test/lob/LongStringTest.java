/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.lob;


import org.hibernate.dialect.SybaseASEDialect;

import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;


/**
 * Tests eager materialization and mutation of long strings.
 *
 * @author Steve Ebersole
 */
@SuppressWarnings("unused")
@SessionFactory
public abstract class LongStringTest {
	private static final int LONG_STRING_SIZE = 10000;

	@Test
	public void testBoundedLongStringAccess(SessionFactoryScope scope) {
		String original = buildRecursively( LONG_STRING_SIZE, 'x' );
		String changed = buildRecursively( LONG_STRING_SIZE, 'y' );
		String empty = "";

		Long id = scope.fromTransaction(
				session -> {
					LongStringHolder entity = new LongStringHolder();
					session.persist( entity );
					return entity.getId();
				}
		);

		scope.inTransaction(
				session -> {
					LongStringHolder entity = session.get( LongStringHolder.class, id );
					assertNull( entity.getLongString() );
					entity.setLongString( original );
				}
		);

		scope.inTransaction(
				session -> {
					LongStringHolder entity = session.get( LongStringHolder.class, id );
					assertEquals( LONG_STRING_SIZE, entity.getLongString().length() );
					assertEquals( original, entity.getLongString() );
					entity.setLongString( changed );
				}
		);

		scope.inTransaction(
				session -> {
					LongStringHolder entity = session.get( LongStringHolder.class, id );
					assertEquals( LONG_STRING_SIZE, entity.getLongString().length() );
					assertEquals( changed, entity.getLongString() );
					entity.setLongString( null );
				}
		);

		scope.inTransaction(
				session -> {
					LongStringHolder entity = session.get( LongStringHolder.class, id );
					assertNull( entity.getLongString() );
					entity.setLongString( empty );
				}
		);

		scope.inTransaction(
				session -> {
					LongStringHolder entity = session.get( LongStringHolder.class, id );
					if ( entity.getLongString() != null ) {
						if ( scope.getSessionFactory().getJdbcServices().getDialect() instanceof SybaseASEDialect ) {
							//Sybase uses a single blank to denote an empty string (this is by design). So, when inserting an empty string '', it is interpreted as single blank ' '.
							assertEquals( empty.length(), entity.getLongString().trim().length() );
							assertEquals( empty, entity.getLongString().trim() );
						}
						else {
							assertEquals( empty.length(), entity.getLongString().length() );
							assertEquals( empty, entity.getLongString() );
						}
					}
					session.remove( entity );
				}
		);
	}

	private String buildRecursively(int size, char baseChar) {
		StringBuilder buff = new StringBuilder();
		for ( int i = 0; i < size; i++ ) {
			buff.append( baseChar );
		}
		return buff.toString();
	}
}
