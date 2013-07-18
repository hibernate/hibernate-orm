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
import org.junit.Test;

import org.hibernate.Session;
import org.hibernate.dialect.SybaseASE15Dialect;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Tests eager materialization and mutation of long strings.
 *
 * @author Steve Ebersole
 */
@SuppressWarnings( {"UnusedDeclaration"})
public abstract class LongStringTest extends BaseCoreFunctionalTestCase {
	private static final int LONG_STRING_SIZE = 10000;

	@Test
	public void testBoundedLongStringAccess() {
		String original = buildRecursively( LONG_STRING_SIZE, 'x' );
		String changed = buildRecursively( LONG_STRING_SIZE, 'y' );
		String empty = "";

		Session s = openSession();
		s.beginTransaction();
		LongStringHolder entity = new LongStringHolder();
		s.save( entity );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		entity = ( LongStringHolder ) s.get( LongStringHolder.class, entity.getId() );
		assertNull( entity.getLongString() );
		entity.setLongString( original );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		entity = ( LongStringHolder ) s.get( LongStringHolder.class, entity.getId() );
		assertEquals( LONG_STRING_SIZE, entity.getLongString().length() );
		assertEquals( original, entity.getLongString() );
		entity.setLongString( changed );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		entity = ( LongStringHolder ) s.get( LongStringHolder.class, entity.getId() );
		assertEquals( LONG_STRING_SIZE, entity.getLongString().length() );
		assertEquals( changed, entity.getLongString() );
		entity.setLongString( null );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		entity = ( LongStringHolder ) s.get( LongStringHolder.class, entity.getId() );
		assertNull( entity.getLongString() );
		entity.setLongString( empty );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		entity = ( LongStringHolder ) s.get( LongStringHolder.class, entity.getId() );
		if ( entity.getLongString() != null ) {
            if(getDialect() instanceof SybaseASE15Dialect){
                //Sybase uses a single blank to denote an empty string (this is by design). So, when inserting an empty string '', it is interpreted as single blank ' '.
                assertEquals( empty.length(), entity.getLongString().trim().length() );
                assertEquals( empty, entity.getLongString().trim() );
            }else{
			    assertEquals( empty.length(), entity.getLongString().length() );
                assertEquals( empty, entity.getLongString() );
            }
		}
		s.delete( entity );
		s.getTransaction().commit();
		s.close();
	}

	private String buildRecursively(int size, char baseChar) {
		StringBuilder buff = new StringBuilder();
		for( int i = 0; i < size; i++ ) {
			buff.append( baseChar );
		}
		return buff.toString();
	}
}
