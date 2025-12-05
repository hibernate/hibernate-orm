/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.lob;

import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.dialect.SybaseDialect;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * Tests eager materialization and mutation of long strings.
 *
 * @author Steve Ebersole
 */
@RequiresDialect(SQLServerDialect.class)
@RequiresDialect(SybaseDialect.class)
@DomainModel(
		annotatedPackageNames = "org.hibernate.orm.test.annotations.lob",
		annotatedClasses = {
				LongStringHolder.class
		}
)
@SessionFactory
public class TextTest {

	private static final int LONG_STRING_SIZE = 10000;

	@Test
	public void testBoundedLongStringAccess(SessionFactoryScope scope) {
		String original = buildRecursively( LONG_STRING_SIZE, 'x' );
		String changed = buildRecursively( LONG_STRING_SIZE, 'y' );

		LongStringHolder e = new LongStringHolder();
		scope.inTransaction(
				session ->
						session.persist( e )
		);

		scope.inTransaction(
				session -> {
					LongStringHolder entity = session.find( LongStringHolder.class, e.getId() );
					assertThat( entity.getLongString() ).isNull();
					assertThat( entity.getName() ).isNull();
					assertThat( entity.getWhatEver() ).isNull();
					entity.setLongString( original );
					entity.setName( original.toCharArray() );
					entity.setWhatEver( wrapPrimitive( original.toCharArray() ) );
				}
		);


		scope.inTransaction(
				session -> {
					LongStringHolder entity = session.find( LongStringHolder.class, e.getId() );
					assertThat( entity.getLongString().length() ).isEqualTo( LONG_STRING_SIZE );
					assertThat( entity.getLongString() ).isEqualTo( original );
					assertThat( entity.getName() ).isNotNull();
					assertThat( entity.getName().length ).isEqualTo( LONG_STRING_SIZE );
					assertThat( entity.getName() ).isEqualTo( original.toCharArray() );
					assertThat( entity.getWhatEver() ).isNotNull();
					assertThat( entity.getWhatEver().length ).isEqualTo( LONG_STRING_SIZE );
					assertThat( unwrapNonPrimitive( entity.getWhatEver() ) ).isEqualTo( original.toCharArray() );
					entity.setLongString( changed );
					entity.setName( changed.toCharArray() );
					entity.setWhatEver( wrapPrimitive( changed.toCharArray() ) );
				}
		);

		scope.inTransaction(
				session -> {
					LongStringHolder entity = session.find( LongStringHolder.class, e.getId() );
					assertThat( entity.getLongString().length() ).isEqualTo( LONG_STRING_SIZE );
					assertThat( entity.getLongString() ).isEqualTo( changed );
					assertThat( entity.getName() ).isNotNull();
					assertThat( entity.getName().length ).isEqualTo( LONG_STRING_SIZE );
					assertThat( entity.getName() ).isEqualTo( changed.toCharArray() );
					assertThat( entity.getWhatEver() ).isNotNull();
					assertThat( entity.getWhatEver().length ).isEqualTo( LONG_STRING_SIZE );
					assertThat( unwrapNonPrimitive( entity.getWhatEver() ) ).isEqualTo( changed.toCharArray() );
					entity.setLongString( null );
					entity.setName( null );
					entity.setWhatEver( null );
				}
		);

		scope.inTransaction(
				session -> {
					LongStringHolder entity = session.find( LongStringHolder.class, e.getId() );
					assertThat( entity.getLongString() ).isNull();
					assertThat( entity.getName() ).isNull();
					assertThat( entity.getWhatEver() ).isNull();
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

	private Character[] wrapPrimitive(char[] bytes) {
		int length = bytes.length;
		Character[] result = new Character[length];
		for ( int index = 0; index < length; index++ ) {
			result[index] = bytes[index];
		}
		return result;
	}

	private char[] unwrapNonPrimitive(Character[] bytes) {
		int length = bytes.length;
		char[] result = new char[length];
		for ( int i = 0; i < length; i++ ) {
			result[i] = bytes[i];
		}
		return result;
	}
}
