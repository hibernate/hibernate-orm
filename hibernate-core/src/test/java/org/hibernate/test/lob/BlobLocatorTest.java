//$Id: $
/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
 *
 */
package org.hibernate.test.lob;
import java.sql.Blob;
import junit.framework.AssertionFailedError;
import junit.framework.Test;
import org.hibernate.Hibernate;
import org.hibernate.LockMode;
import org.hibernate.Session;
import org.hibernate.dialect.Dialect;
import org.hibernate.testing.junit.functional.DatabaseSpecificFunctionalTestCase;
import org.hibernate.testing.junit.functional.FunctionalTestClassTestSuite;
import org.hibernate.util.ArrayHelper;

/**
 * Tests lazy materialization of data mapped by
 * {@link org.hibernate.type.BlobType}, as well as bounded and unbounded
 * materialization and mutation.
 *
 * @author Steve Ebersole
 */
public class BlobLocatorTest extends DatabaseSpecificFunctionalTestCase {
	private static final int BLOB_SIZE = 10000;

	public BlobLocatorTest(String name) {
		super( name );
	}

	public String[] getMappings() {
		return new String[] { "lob/LobMappings.hbm.xml" };
	}

	public static Test suite() {
		return new FunctionalTestClassTestSuite( BlobLocatorTest.class );
	}

	public boolean appliesTo(Dialect dialect) {
		if ( ! dialect.supportsExpectedLobUsagePattern() ) {
			reportSkip( "database/driver does not support expected LOB usage pattern", "LOB support" );
			return false;
		}
		return true;
	}

	public void testBoundedBlobLocatorAccess() throws Throwable {
		byte[] original = buildRecursively( BLOB_SIZE, true );
		byte[] changed = buildRecursively( BLOB_SIZE, false );

		Session s = openSession();
		s.beginTransaction();
		LobHolder entity = new LobHolder();
		entity.setBlobLocator( s.getLobHelper().createBlob( original ) );
		s.save( entity );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		entity = ( LobHolder ) s.get( LobHolder.class, entity.getId() );
		assertEquals( BLOB_SIZE, entity.getBlobLocator().length() );
		assertEquals( original, extractData( entity.getBlobLocator() ) );
		s.getTransaction().commit();
		s.close();

		// test mutation via setting the new clob data...
		if ( supportsLobValueChangePropogation() ) {
			s = openSession();
			s.beginTransaction();
			entity = ( LobHolder ) s.get( LobHolder.class, entity.getId(), LockMode.UPGRADE );
			entity.getBlobLocator().truncate( 1 );
			entity.getBlobLocator().setBytes( 1, changed );
			s.getTransaction().commit();
			s.close();

			s = openSession();
			s.beginTransaction();
			entity = ( LobHolder ) s.get( LobHolder.class, entity.getId(), LockMode.UPGRADE );
			assertNotNull( entity.getBlobLocator() );
			assertEquals( BLOB_SIZE, entity.getBlobLocator().length() );
			assertEquals( changed, extractData( entity.getBlobLocator() ) );
			entity.getBlobLocator().truncate( 1 );
			entity.getBlobLocator().setBytes( 1, original );
			s.getTransaction().commit();
			s.close();
		}

		// test mutation via supplying a new clob locator instance...
		s = openSession();
		s.beginTransaction();
		entity = ( LobHolder ) s.get( LobHolder.class, entity.getId(), LockMode.UPGRADE );
		assertNotNull( entity.getBlobLocator() );
		assertEquals( BLOB_SIZE, entity.getBlobLocator().length() );
		assertEquals( original, extractData( entity.getBlobLocator() ) );
		entity.setBlobLocator( s.getLobHelper().createBlob( changed ) );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		entity = ( LobHolder ) s.get( LobHolder.class, entity.getId() );
		assertEquals( BLOB_SIZE, entity.getBlobLocator().length() );
		assertEquals( changed, extractData( entity.getBlobLocator() ) );
		s.delete( entity );
		s.getTransaction().commit();
		s.close();

	}

	public void testUnboundedBlobLocatorAccess() throws Throwable {
		if ( ! supportsUnboundedLobLocatorMaterialization() ) {
			return;
		}

		// Note: unbounded mutation of the underlying lob data is completely
		// unsupported; most databases would not allow such a construct anyway.
		// Thus here we are only testing materialization...

		byte[] original = buildRecursively( BLOB_SIZE, true );

		Session s = openSession();
		s.beginTransaction();
		LobHolder entity = new LobHolder();
		entity.setBlobLocator( Hibernate.getLobCreator( s ).createBlob( original ) );
		s.save( entity );
		s.getTransaction().commit();
		s.close();

		// load the entity with the clob locator, and close the session/transaction;
		// at that point it is unbounded...
		s = openSession();
		s.beginTransaction();
		entity = ( LobHolder ) s.get( LobHolder.class, entity.getId() );
		s.getTransaction().commit();
		s.close();

		assertEquals( BLOB_SIZE, entity.getBlobLocator().length() );
		assertEquals( original, extractData( entity.getBlobLocator() ) );

		s = openSession();
		s.beginTransaction();
		s.delete( entity );
		s.getTransaction().commit();
		s.close();
	}

	private byte[] extractData(Blob blob) throws Throwable {
		return blob.getBytes( 1, ( int ) blob.length() );
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
