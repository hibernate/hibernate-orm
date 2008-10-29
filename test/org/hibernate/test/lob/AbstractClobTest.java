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

import java.sql.SQLException;
import java.sql.Clob;
import java.io.IOException;

import org.hibernate.LockMode;
import org.hibernate.Session;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.junit.functional.DatabaseSpecificFunctionalTestCase;

/**
 * Test various access scenarios for eager and lazy materialization
 * of CLOB data, as well as bounded and unbounded materialization
 * and mutation.
 *
 * @author Steve Ebersole
 */
public abstract class AbstractClobTest extends DatabaseSpecificFunctionalTestCase {
	public static final int CLOB_SIZE = 10000;

	public AbstractClobTest(String name) {
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

	protected abstract Clob createClobLocator(Session s, String str);

	protected abstract Clob createClobLocatorFromStreamUsingLength(Session s, String str ) throws IOException ;

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

	public void testBoundedMaterializedClobAccess() {
		String original = buildRecursively( CLOB_SIZE, 'x' );
		String changed = buildRecursively( CLOB_SIZE, 'y' );

		Session s = openSession();
		s.beginTransaction();
		LobHolder entity = new LobHolder();
		entity.setMaterializedClob( original );
		s.save( entity );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		entity = ( LobHolder ) s.get( LobHolder.class, entity.getId() );
		assertEquals( CLOB_SIZE, entity.getMaterializedClob().length() );
		assertEquals( original, entity.getMaterializedClob() );
		entity.setMaterializedClob( changed );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		entity = ( LobHolder ) s.get( LobHolder.class, entity.getId() );
		assertEquals( CLOB_SIZE, entity.getMaterializedClob().length() );
		assertEquals( changed, entity.getMaterializedClob() );
		s.delete( entity );
		s.getTransaction().commit();
		s.close();
	}

	public void testBoundedClobLocatorAccess() throws Throwable {
		if ( skipLobLocatorTests() ) {
			return;
		}

		String original = buildRecursively( CLOB_SIZE, 'x');
		String changed = buildRecursively( CLOB_SIZE, 'y' );

		Session s = openSession();
		s.beginTransaction();
		LobHolder entity = new LobHolder();
		entity.setClobLocator( createClobLocator( s, original ) );
		s.save( entity );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		entity = ( LobHolder ) s.get( LobHolder.class, entity.getId() );
		assertEquals( CLOB_SIZE, entity.getClobLocator().length() );
		assertEquals( original, extractData( entity.getClobLocator() ) );
		s.getTransaction().commit();
		s.close();

		// test mutation via setting the new clob data...
		if ( supportsLobValueChangePropogation() ) {
			s = openSession();
			s.beginTransaction();
			entity = ( LobHolder ) s.get( LobHolder.class, entity.getId(), LockMode.UPGRADE );
			entity.getClobLocator().truncate( 1 );
			entity.getClobLocator().setString( 1, changed );
			s.getTransaction().commit();
			s.close();

			s = openSession();
			s.beginTransaction();
			entity = ( LobHolder ) s.get( LobHolder.class, entity.getId(), LockMode.UPGRADE );
			assertNotNull( entity.getClobLocator() );
			assertEquals( CLOB_SIZE, entity.getClobLocator().length() );
			assertEquals( changed, extractData( entity.getClobLocator() ) );
			entity.getClobLocator().truncate( 1 );
			entity.getClobLocator().setString( 1, original );
			s.getTransaction().commit();
			s.close();
		}

		// test mutation via supplying a new clob locator instance...
		s = openSession();
		s.beginTransaction();
		entity = ( LobHolder ) s.get( LobHolder.class, entity.getId(), LockMode.UPGRADE );
		assertNotNull( entity.getClobLocator() );
		assertEquals( CLOB_SIZE, entity.getClobLocator().length() );
		assertEquals( original, extractData( entity.getClobLocator() ) );
		entity.setClobLocator( createClobLocator( s, changed ) );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		entity = ( LobHolder ) s.get( LobHolder.class, entity.getId() );
		assertEquals( CLOB_SIZE, entity.getClobLocator().length() );
		assertEquals( changed, extractData( entity.getClobLocator() ) );
		s.delete( entity );
		s.getTransaction().commit();
		s.close();
	}

	public void testBoundedClobLocatorAccessFromStreamUsingLength() throws Throwable {
		if ( skipLobLocatorTests() ) {
			return;
		}

		String original = buildRecursively( CLOB_SIZE, 'x');
		String changed = buildRecursively( CLOB_SIZE, 'y' );

		Session s = openSession();
		s.beginTransaction();
		LobHolder entity = new LobHolder();
		entity.setClobLocator( createClobLocatorFromStreamUsingLength( s, original ) );
		s.save( entity );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		entity = ( LobHolder ) s.get( LobHolder.class, entity.getId() );
		assertEquals( CLOB_SIZE, entity.getClobLocator().length() );
		assertEquals( original, extractData( entity.getClobLocator() ) );
		s.getTransaction().commit();
		s.close();

		// test mutation via setting the new clob data...
		if ( supportsLobValueChangePropogation() ) {
			s = openSession();
			s.beginTransaction();
			entity = ( LobHolder ) s.get( LobHolder.class, entity.getId(), LockMode.UPGRADE );
			entity.getClobLocator().truncate( 1 );
			entity.getClobLocator().setString( 1, changed );
			s.getTransaction().commit();
			s.close();

			s = openSession();
			s.beginTransaction();
			entity = ( LobHolder ) s.get( LobHolder.class, entity.getId(), LockMode.UPGRADE );
			assertNotNull( entity.getClobLocator() );
			assertEquals( CLOB_SIZE, entity.getClobLocator().length() );
			assertEquals( changed, extractData( entity.getClobLocator() ) );
			entity.getClobLocator().truncate( 1 );
			entity.getClobLocator().setString( 1, original );
			s.getTransaction().commit();
			s.close();
		}

		// test mutation via supplying a new clob locator instance...
		s = openSession();
		s.beginTransaction();
		entity = ( LobHolder ) s.get( LobHolder.class, entity.getId(), LockMode.UPGRADE );
		assertNotNull( entity.getClobLocator() );
		assertEquals( CLOB_SIZE, entity.getClobLocator().length() );
		assertEquals( original, extractData( entity.getClobLocator() ) );
		entity.setClobLocator( createClobLocatorFromStreamUsingLength( s, changed ) );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		entity = ( LobHolder ) s.get( LobHolder.class, entity.getId() );
		assertEquals( CLOB_SIZE, entity.getClobLocator().length() );
		assertEquals( changed, extractData( entity.getClobLocator() ) );
		s.delete( entity );
		s.getTransaction().commit();
		s.close();
	}

	public void testUnboundedClobLocatorAccess() throws Throwable {
		if ( skipLobLocatorTests() || ! supportsUnboundedLobLocatorMaterialization() ) {
			return;
		}

		// Note: unbounded mutation of the underlying lob data is completely
		// unsupported; most databases would not allow such a construct anyway.
		// Thus here we are only testing materialization...

		String original = buildRecursively( CLOB_SIZE, 'x' );

		Session s = openSession();
		s.beginTransaction();
		LobHolder entity = new LobHolder();
		entity.setClobLocator( createClobLocator( s, original ) );
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

		assertEquals( CLOB_SIZE, entity.getClobLocator().length() );
		assertEquals( original, extractData( entity.getClobLocator() ) );

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
		String str = buildRecursively( CLOB_SIZE, 'x' );
		Clob clob = createClobLocator( s, str );
		assertEquals( CLOB_SIZE, clob.length() );
		assertEquals( str, extractData( clob ) );
		s.close();
	}

	public void testCreateAndAccessLobLocatorInTransaction() throws Throwable {
		if ( skipLobLocatorTests() || ! supportsUnboundedLobLocatorMaterialization() ) {
			return;
		}
		Session s = openSession();
		s.getTransaction().begin();
		String str = buildRecursively( CLOB_SIZE, 'x' );
		Clob clob = createClobLocator( s, str );
		assertEquals( CLOB_SIZE, clob.length() );
		assertEquals( str, extractData( clob ) );
		s.getTransaction().commit();
		s.close();
	}

	public void testCreateLobLocatorInTransactionAccessOutOfTransaction() throws Throwable {
		if ( skipLobLocatorTests() || ! supportsUnboundedLobLocatorMaterialization() ) {
			return;
		}
		Session s = openSession();
		s.getTransaction().begin();
		String str = buildRecursively( CLOB_SIZE, 'x' );
		Clob clob = createClobLocator( s, str );
		s.getTransaction().commit();
		assertEquals( CLOB_SIZE, clob.length() );
		assertEquals( str, extractData( clob ) );
		s.close();
	}

	public void testCreateLobLocatorInTransactionAccessInNextTransaction() throws Throwable {
		if ( skipLobLocatorTests() || ! supportsUnboundedLobLocatorMaterialization() ) {
			return;
		}
		Session s = openSession();
		s.getTransaction().begin();
		String str = buildRecursively( CLOB_SIZE, 'x' );
		Clob clob = createClobLocator( s, str );
		s.getTransaction().commit();
		s.getTransaction().begin();
		assertEquals( CLOB_SIZE, clob.length() );
		assertEquals( str, extractData( clob ) );
		s.getTransaction().commit();
		s.close();
	}

	public void testCreateLobLocatorInTransactionAccessAfterSessionClose() throws Throwable {
		if ( skipLobLocatorTests() || ! supportsUnboundedLobLocatorMaterialization() ) {
			return;
		}
		Session s = openSession();
		s.getTransaction().begin();
		String str = buildRecursively( CLOB_SIZE, 'x' );
		Clob clob = createClobLocator( s, str );
		s.getTransaction().commit();
		s.close();
		assertEquals( CLOB_SIZE, clob.length() );
		assertEquals( str, extractData( clob ) );
	}

	public void testCreateLobLocatorInTransactionAccessInNextSession() throws Throwable {
		if ( skipLobLocatorTests() || ! supportsUnboundedLobLocatorMaterialization() ) {
			return;
		}
		Session s = openSession();
		s.getTransaction().begin();
		String str = buildRecursively( CLOB_SIZE, 'x' );
		Clob clob = createClobLocator( s, str );
		s.getTransaction().commit();
		s.close();
		s = openSession();
		assertEquals( CLOB_SIZE, clob.length() );
		assertEquals( str, extractData( clob ) );
		s.close();
	}

	public void testCreateLobLocatorInTransactionAccessInNextSessionTransaction() throws Throwable {
		if ( skipLobLocatorTests() || ! supportsUnboundedLobLocatorMaterialization() ) {
			return;
		}
		Session s = openSession();
		s.getTransaction().begin();
		String str = buildRecursively( CLOB_SIZE, 'x' );
		Clob clob = createClobLocator( s, str );
		s.getTransaction().commit();
		s.close();
		s = openSession();
		s.getTransaction().begin();
		assertEquals( CLOB_SIZE, clob.length() );
		assertEquals( str, extractData( clob ) );
		s.getTransaction().commit();
		s.close();
	}

	protected String extractData(Clob clob) throws Throwable {
		if ( getDialect() instanceof H2Dialect ) {
			return clob.getSubString( 1, ( int ) clob.length() );
		}
		else {
			char[] data = new char[ (int) clob.length() ];
			clob.getCharacterStream().read( data );
			return new String( data );
		}
	}

	protected String buildRecursively(int size, char baseChar) {
		StringBuffer buff = new StringBuffer();
		for( int i = 0; i < size; i++ ) {
			buff.append( baseChar );
		}
		return buff.toString();
	}
}