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
import java.sql.SQLException;
import java.io.IOException;

import junit.framework.AssertionFailedError;

import org.hibernate.LockMode;
import org.hibernate.Session;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.Dialect;
import org.hibernate.junit.functional.DatabaseSpecificFunctionalTestCase;
import org.hibernate.util.ArrayHelper;

/**
 * Test various access scenarios for eager and lazy materialization
 * of BLOB data, as well as bounded and unbounded materialization
 * and mutation.
 *
 * @author Steve Ebersole
 */
public abstract class AbstractBlobTest extends DatabaseSpecificFunctionalTestCase {
	public static final int BLOB_SIZE = 10000;

	public AbstractBlobTest(String name) {
		super( name );
	}

	public String[] getMappings() {
		return new String[] { "lob/LobMappings.hbm.xml" };
	}

	public boolean appliesTo(Dialect dialect) {
		if ( ! dialect.supportsExpectedLobUsagePattern() ) {
			reportSkip( "database/driver does not support expected LOB usage pattern", "LOB support" );
			return false;
		}
		return true;
	}

	protected abstract Blob createBlobLocator(Session s, byte[] bytes);

	protected abstract Blob createBlobLocatorFromStream(Session s, byte[] bytes) throws IOException;

	protected abstract Blob createBlobLocatorFromStreamUsingLength(Session s, byte[] bytes) throws IOException ;

	protected boolean skipLobLocatorTests()  throws SQLException {
		return false;
	}

	public void configure(Configuration cfg) {
		super.configure( cfg );
		// set connection pool size to 0 so that tests will always use a clean connection;
		// this ensures that the test does not use LOB data left on the connection from
		// prior to being returned to the connection pool.
		cfg.setProperty( Environment.POOL_SIZE, "0" );
		// ConnectionReleaseMode.AFTER_TRANSACTION is the default for non-JTA connection.
		// The Connection used for the unit tests is non-JTA. This setting for
		// Environment.RELEASE_CONNECTIONS is just to make it explicit for these tests.
		cfg.setProperty( Environment.RELEASE_CONNECTIONS, "after_transaction" );
	}

	public void testBoundedMaterializedBlobAccess() {
		byte[] original = buildRecursively( BLOB_SIZE, true );
		byte[] changed = buildRecursively( BLOB_SIZE, false );

		Session s = openSession();
		s.beginTransaction();
		LobHolder entity = new LobHolder();
		entity.setMaterializedBlob( original );
		s.save( entity );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		entity = ( LobHolder ) s.get( LobHolder.class, entity.getId() );
		assertEquals( BLOB_SIZE, entity.getMaterializedBlob().length );
		assertEquals( original, entity.getMaterializedBlob() );
		entity.setMaterializedBlob( changed );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		entity = ( LobHolder ) s.get( LobHolder.class, entity.getId() );
		assertEquals( BLOB_SIZE, entity.getMaterializedBlob().length );
		assertEquals( changed, entity.getMaterializedBlob() );
		s.delete( entity );
		s.getTransaction().commit();
		s.close();
	}

	public void testBoundedBlobLocatorAccess() throws Throwable {
		if ( skipLobLocatorTests() ) {
			return;
		}

		byte[] original = buildRecursively( BLOB_SIZE, true );
		byte[] changed = buildRecursively( BLOB_SIZE, false );

		Session s = openSession();
		s.beginTransaction();
		LobHolder entity = new LobHolder();
		entity.setBlobLocator( createBlobLocator( s, original ) );
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

		// test mutation via setting the new blob data...
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

		// test mutation via supplying a new blob locator instance...
		s = openSession();
		s.beginTransaction();
		entity = ( LobHolder ) s.get( LobHolder.class, entity.getId(), LockMode.UPGRADE );
		assertNotNull( entity.getBlobLocator() );
		assertEquals( BLOB_SIZE, entity.getBlobLocator().length() );
		assertEquals( original, extractData( entity.getBlobLocator() ) );
		entity.setBlobLocator( createBlobLocator( s, changed ) );
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

	public void testBoundedBlobLocatorAccessFromStream() throws Throwable {
		if ( skipLobLocatorTests() ) {
			return;
		}

		byte[] original = buildRecursively( BLOB_SIZE, true );
		byte[] changed = buildRecursively( BLOB_SIZE, false );

		Session s = openSession();
		s.beginTransaction();
		LobHolder entity = new LobHolder();
		entity.setBlobLocator( createBlobLocatorFromStream( s, original ) );
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

		// test mutation via setting the new blob data...
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

		// test mutation via supplying a new blob locator instance...
		s = openSession();
		s.beginTransaction();
		entity = ( LobHolder ) s.get( LobHolder.class, entity.getId(), LockMode.UPGRADE );
		assertNotNull( entity.getBlobLocator() );
		assertEquals( BLOB_SIZE, entity.getBlobLocator().length() );
		assertEquals( original, extractData( entity.getBlobLocator() ) );
		entity.setBlobLocator( createBlobLocatorFromStream( s, changed ) );
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

	public void testBoundedBlobLocatorAccessFromStreamUsingLength() throws Throwable {
		if ( skipLobLocatorTests() ) {
			return;
		}

		byte[] original = buildRecursively( BLOB_SIZE, true );
		byte[] changed = buildRecursively( BLOB_SIZE, false );

		Session s = openSession();
		s.beginTransaction();
		LobHolder entity = new LobHolder();
		entity.setBlobLocator( createBlobLocatorFromStreamUsingLength( s, original ) );
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

		// test mutation via setting the new blob data...
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

		// test mutation via supplying a new blob locator instance...
		s = openSession();
		s.beginTransaction();
		entity = ( LobHolder ) s.get( LobHolder.class, entity.getId(), LockMode.UPGRADE );
		assertNotNull( entity.getBlobLocator() );
		assertEquals( BLOB_SIZE, entity.getBlobLocator().length() );
		assertEquals( original, extractData( entity.getBlobLocator() ) );
		entity.setBlobLocator( createBlobLocatorFromStreamUsingLength( s, changed ) );
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
		if ( skipLobLocatorTests() || ! supportsUnboundedLobLocatorMaterialization() ) {
			return;
		}

		// Note: unbounded mutation of the underlying lob data is completely
		// unsupported; most databases would not allow such a construct anyway.
		// Thus here we are only testing materialization...

		byte[] original = buildRecursively( BLOB_SIZE, true );

		Session s = openSession();
		s.beginTransaction();
		LobHolder entity = new LobHolder();
		entity.setBlobLocator( createBlobLocator( s, original ) );
		s.save( entity );
		s.getTransaction().commit();
		s.close();

		// load the entity with the blob locator, and close the session/transaction;
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

	public void testCreateAndAccessLobLocatorInSessionNoTransaction() throws Throwable {
		if ( skipLobLocatorTests() || ! supportsUnboundedLobLocatorMaterialization() ) {
			return;
		}
		Session s = openSession();
		byte[] bytes = buildRecursively( BLOB_SIZE, true );
		Blob blob = createBlobLocator( s, bytes );
		assertEquals( BLOB_SIZE, blob.length() );
		assertEquals( bytes, extractData( blob ) );
		s.close();
	}

	public void testCreateAndAccessLobLocatorInTransaction() throws Throwable {
		if ( skipLobLocatorTests() || ! supportsUnboundedLobLocatorMaterialization() ) {
			return;
		}
		Session s = openSession();
		s.getTransaction().begin();
		byte bytes[] = buildRecursively( BLOB_SIZE, true );
		Blob blob = createBlobLocator( s, bytes );
		assertEquals( BLOB_SIZE, blob.length() );
		assertEquals( bytes, extractData( blob ) );
		s.getTransaction().commit();
		s.close();
	}

	public void testCreateLobLocatorInTransactionAccessOutOfTransaction() throws Throwable {
		if ( skipLobLocatorTests() || ! supportsUnboundedLobLocatorMaterialization() ) {
			return;
		}
		Session s = openSession();
		s.getTransaction().begin();
		byte bytes[] = buildRecursively( BLOB_SIZE, true );
		Blob blob = createBlobLocator( s, bytes );
		s.getTransaction().commit();
		assertEquals( BLOB_SIZE, blob.length() );
		assertEquals( bytes, extractData( blob ) );
		s.close();
	}

	public void testCreateLobLocatorInTransactionAccessInNextTransaction() throws Throwable {
		if ( skipLobLocatorTests() || ! supportsUnboundedLobLocatorMaterialization() ) {
			return;
		}
		Session s = openSession();
		s.getTransaction().begin();
		byte bytes[] = buildRecursively( BLOB_SIZE, true );
		Blob blob = createBlobLocator( s, bytes );
		s.getTransaction().commit();
		s.getTransaction().begin();
		assertEquals( BLOB_SIZE, blob.length() );
		assertEquals( bytes, extractData( blob ) );
		s.getTransaction().commit();
		s.close();
	}

	public void testCreateLobLocatorInTransactionAccessAfterSessionClose() throws Throwable {
		if ( skipLobLocatorTests() || ! supportsUnboundedLobLocatorMaterialization() ) {
			return;
		}
		Session s = openSession();
		s.getTransaction().begin();
		byte bytes[] = buildRecursively( BLOB_SIZE, true );
		Blob blob = createBlobLocator( s, bytes );
		s.getTransaction().commit();
		s.close();
		assertEquals( BLOB_SIZE, blob.length() );
		assertEquals( bytes, extractData( blob ) );
	}

	public void testCreateLobLocatorInTransactionAccessInNextSession() throws Throwable {
		if ( skipLobLocatorTests() || ! supportsUnboundedLobLocatorMaterialization() ) {
			return;
		}
		Session s = openSession();
		s.getTransaction().begin();
		byte bytes[] = buildRecursively( BLOB_SIZE, true );
		Blob blob = createBlobLocator( s, bytes );
		s.getTransaction().commit();
		s.close();
		s = openSession();
		assertEquals( BLOB_SIZE, blob.length() );
		assertEquals( bytes, extractData( blob ) );
		s.close();
	}

	public void testCreateLobLocatorInTransactionAccessInNextSessionTransaction() throws Throwable {
		if ( skipLobLocatorTests() || ! supportsUnboundedLobLocatorMaterialization() ) {
			return;
		}
		Session s = openSession();
		s.getTransaction().begin();
		byte bytes[] = buildRecursively( BLOB_SIZE, true );
		Blob blob = createBlobLocator( s, bytes );
		s.getTransaction().commit();
		s.close();
		s = openSession();
		s.getTransaction().begin();
		assertEquals( BLOB_SIZE, blob.length() );
		assertEquals( bytes, extractData( blob ) );
		s.getTransaction().commit();
		s.close();
	}
	
	protected byte[] extractData(Blob blob) throws Throwable {
		byte bytesRead[] = new byte[ ( int ) blob.length() ];
		blob.getBinaryStream().read( bytesRead );
		return bytesRead;
	}

	protected byte[] buildRecursively(int size, boolean on) {
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
