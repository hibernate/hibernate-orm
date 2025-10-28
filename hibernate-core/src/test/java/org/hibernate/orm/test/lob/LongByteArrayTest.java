/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.lob;

import org.hibernate.dialect.SybaseASEDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;


/**
 * Tests eager materialization and mutation of long byte arrays.
 *
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@SessionFactory
public abstract class LongByteArrayTest {
	private static final int ARRAY_SIZE = 10000;

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	public void testBoundedLongByteArrayAccess(SessionFactoryScope scope) {
		byte[] original = buildRecursively( ARRAY_SIZE, true );
		byte[] changed = buildRecursively( ARRAY_SIZE, false );
		byte[] empty = new byte[] {};

		Long id = scope.fromTransaction(session -> {
			var entity = new LongByteArrayHolder();
			session.persist( entity );
			return entity.getId();
		} );

		scope.inTransaction(session -> {
			var entity = session.find( LongByteArrayHolder.class, id );
			assertNull( entity.getLongByteArray() );
			entity.setLongByteArray( original );
		} );

		scope.inTransaction(session -> {
			var entity = session.find( LongByteArrayHolder.class, id );
			Assertions.assertEquals( ARRAY_SIZE, entity.getLongByteArray().length );
			assertEquals( original, entity.getLongByteArray() );
			entity.setLongByteArray( changed );
		} );

		scope.inTransaction(session -> {
			var entity = session.get( LongByteArrayHolder.class, id );
			Assertions.assertEquals( ARRAY_SIZE, entity.getLongByteArray().length );
			assertEquals( changed, entity.getLongByteArray() );
			entity.setLongByteArray( null );
		} );

		scope.inTransaction(session -> {
			var entity = session.find( LongByteArrayHolder.class, id );
			assertNull( entity.getLongByteArray() );
			entity.setLongByteArray( empty );
		} );
	}

	@Test
	@SkipForDialect(dialectClass = SybaseASEDialect.class, reason = "Sybase returns byte[]{0}")
	public void testEmptyArray(SessionFactoryScope scope) {
		byte[] empty = new byte[] {};

		var longByteArrayHolder = new LongByteArrayHolder();
		scope.inTransaction(session -> {
			longByteArrayHolder.setLongByteArray( empty );
			session.persist( longByteArrayHolder );
		} );

		scope.inTransaction(session -> {
			var entity = session.find( LongByteArrayHolder.class, longByteArrayHolder.getId() );
			if ( entity.getLongByteArray() != null ) {
				Assertions.assertEquals( empty.length, entity.getLongByteArray().length );
				assertEquals( empty, entity.getLongByteArray() );
			}
		} );
	}

	@Test
	public void testSaving(SessionFactoryScope scope) {
		byte[] value = buildRecursively( ARRAY_SIZE, true );

		var id = scope.fromTransaction(session -> {
			var entity = new LongByteArrayHolder();
			entity.setLongByteArray( value );
			session.persist( entity );
			return entity.getId();
		} );

		scope.inTransaction(session -> {
			var entity = session.get( LongByteArrayHolder.class, id );
			Assertions.assertEquals( ARRAY_SIZE, entity.getLongByteArray().length );
			assertEquals( value, entity.getLongByteArray() );
			session.remove( entity );
		} );
	}

	private byte[] buildRecursively(int size, boolean on) {
		byte[] data = new byte[size];
		data[0] = mask( on );
		for ( int i = 0; i < size; i++ ) {
			data[i] = mask( on );
			on = !on;
		}
		return data;
	}

	private byte mask(boolean on) {
		return on ? (byte) 1 : (byte) 0;
	}

	public static void assertEquals(byte[] val1, byte[] val2) {
		if ( !Arrays.equals( val1, val2 ) ) {
			fail( "byte arrays did not match" );
		}
	}
}
