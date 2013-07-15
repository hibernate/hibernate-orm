/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008-2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.test.lob;

import junit.framework.AssertionFailedError;
import org.junit.Assert;
import org.junit.Test;

import org.hibernate.Session;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertNull;

/**
 * Tests eager materialization and mutation of long byte arrays.
 *
 * @author Steve Ebersole
 */
public abstract class LongByteArrayTest extends BaseCoreFunctionalTestCase {
	private static final int ARRAY_SIZE = 10000;

	@Test
	public void testBoundedLongByteArrayAccess() {
		byte[] original = buildRecursively( ARRAY_SIZE, true );
		byte[] changed = buildRecursively( ARRAY_SIZE, false );
		byte[] empty = new byte[] {};

		Session s = openSession();
		s.beginTransaction();
		LongByteArrayHolder entity = new LongByteArrayHolder();
		s.save( entity );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		entity = ( LongByteArrayHolder ) s.get( LongByteArrayHolder.class, entity.getId() );
		assertNull( entity.getLongByteArray() );
		entity.setLongByteArray( original );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		entity = ( LongByteArrayHolder ) s.get( LongByteArrayHolder.class, entity.getId() );
		Assert.assertEquals( ARRAY_SIZE, entity.getLongByteArray().length );
		assertEquals( original, entity.getLongByteArray() );
		entity.setLongByteArray( changed );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		entity = ( LongByteArrayHolder ) s.get( LongByteArrayHolder.class, entity.getId() );
		Assert.assertEquals( ARRAY_SIZE, entity.getLongByteArray().length );
		assertEquals( changed, entity.getLongByteArray() );
		entity.setLongByteArray( null );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		entity = ( LongByteArrayHolder ) s.get( LongByteArrayHolder.class, entity.getId() );
		assertNull( entity.getLongByteArray() );
		entity.setLongByteArray( empty );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		entity = ( LongByteArrayHolder ) s.get( LongByteArrayHolder.class, entity.getId() );
		if ( entity.getLongByteArray() != null ) {
			Assert.assertEquals( empty.length, entity.getLongByteArray().length );
			assertEquals( empty, entity.getLongByteArray() );
		}
		s.delete( entity );
		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void testSaving() {
		byte[] value = buildRecursively( ARRAY_SIZE, true );

		Session s = openSession();
		s.beginTransaction();
		LongByteArrayHolder entity = new LongByteArrayHolder();
		entity.setLongByteArray( value );
		s.persist( entity );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		entity = ( LongByteArrayHolder ) s.get( LongByteArrayHolder.class, entity.getId() );
		Assert.assertEquals( ARRAY_SIZE, entity.getLongByteArray().length );
		assertEquals( value, entity.getLongByteArray() );
		s.delete( entity );
		s.getTransaction().commit();
		s.close();
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
		return on ? ( byte ) 1 : ( byte ) 0;
	}

	public static void assertEquals(byte[] val1, byte[] val2) {
		if ( !ArrayHelper.isEquals( val1, val2 ) ) {
			throw new AssertionFailedError( "byte arrays did not match" );
		}
	}
}
