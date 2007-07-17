package org.hibernate.test.lob;

import java.sql.Clob;

import junit.framework.Test;

import org.hibernate.Hibernate;
import org.hibernate.LockMode;
import org.hibernate.Session;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.junit.functional.DatabaseSpecificFunctionalTestCase;
import org.hibernate.junit.functional.FunctionalTestClassTestSuite;

/**
 * Test various access scenarios for eager and lazy materialization
 * of CLOB data, as well as bounded and unbounded materialization
 * and mutation.
 *
 * @author Steve Ebersole
 */
public class ClobTest extends DatabaseSpecificFunctionalTestCase {
	private static final int CLOB_SIZE = 10000;

	public ClobTest(String name) {
		super( name );
	}

	public String[] getMappings() {
		return new String[] { "lob/LobMappings.hbm.xml" };
	}

	public static Test suite() {
		return new FunctionalTestClassTestSuite( ClobTest.class );
	}

	public boolean appliesTo(Dialect dialect) {
		if ( ! dialect.supportsExpectedLobUsagePattern() ) {
			reportSkip( "database/driver does not support expected LOB usage pattern", "LOB support" );
			return false;
		}
		return true;
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
		String original = buildRecursively( CLOB_SIZE, 'x' );
		String changed = buildRecursively( CLOB_SIZE, 'y' );

		Session s = openSession();
		s.beginTransaction();
		LobHolder entity = new LobHolder();
		entity.setClobLocator( Hibernate.createClob( original ) );
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
		entity.setClobLocator( Hibernate.createClob( changed ) );
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
		if ( ! supportsUnboundedLobLocatorMaterialization() ) {
			return;
		}

		// Note: unbounded mutation of the underlying lob data is completely
		// unsupported; most databases would not allow such a construct anyway.
		// Thus here we are only testing materialization...

		String original = buildRecursively( CLOB_SIZE, 'x' );

		Session s = openSession();
		s.beginTransaction();
		LobHolder entity = new LobHolder();
		entity.setClobLocator( Hibernate.createClob( original ) );
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

	private String extractData(Clob clob) throws Throwable {
		if ( getDialect() instanceof H2Dialect ) {
			return clob.getSubString( 1, ( int ) clob.length() );
		}
		else {
			char[] data = new char[ (int) clob.length() ];
			clob.getCharacterStream().read( data );
			return new String( data );
		}
	}


	private String buildRecursively(int size, char baseChar) {
		StringBuffer buff = new StringBuffer();
		for( int i = 0; i < size; i++ ) {
			buff.append( baseChar );
		}
		return buff.toString();
	}
}
