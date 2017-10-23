/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.lob;

import java.sql.Clob;

import org.hibernate.LockOptions;
import org.hibernate.Session;
import org.hibernate.dialect.SybaseASE157Dialect;
import org.hibernate.dialect.TeradataDialect;
import org.hibernate.type.descriptor.java.DataHelper;

import org.hibernate.testing.DialectChecks;
import org.hibernate.testing.RequiresDialectFeature;
import org.hibernate.testing.SkipForDialect;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Tests lazy materialization of data mapped by
 * {@link org.hibernate.type.ClobType} as well as bounded and unbounded
 * materialization and mutation.
 *
 * @author Steve Ebersole
 */
@RequiresDialectFeature(
		value = DialectChecks.SupportsExpectedLobUsagePattern.class,
		comment = "database/driver does not support expected LOB usage pattern"
)
public class ClobLocatorTest extends BaseCoreFunctionalTestCase {
	private static final int CLOB_SIZE = 10000;

	public String[] getMappings() {
		return new String[] { "lob/LobMappings.hbm.xml" };
	}

	@Test
	@SkipForDialect(
			value = TeradataDialect.class,
			jiraKey = "HHH-6637",
			comment = "Teradata requires locator to be used in same session where it was created/retrieved"
	)
	public void testBoundedClobLocatorAccess() throws Throwable {
		String original = buildString( CLOB_SIZE, 'x' );
		String changed = buildString( CLOB_SIZE, 'y' );
		String empty = "";

		Session s = openSession();
		s.beginTransaction();
		LobHolder entity = new LobHolder();
		entity.setClobLocator( s.getLobHelper().createClob( original ) );
		s.save( entity );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		entity = s.get( LobHolder.class, entity.getId() );
		assertEquals( CLOB_SIZE, entity.getClobLocator().length() );
		assertEquals( original, extractData( entity.getClobLocator() ) );
		s.getTransaction().commit();
		s.close();

		// test mutation via setting the new clob data...
		if ( getDialect().supportsLobValueChangePropogation() ) {
			s = openSession();
			s.beginTransaction();
			entity = ( LobHolder ) s.byId( LobHolder.class ).with( LockOptions.UPGRADE ).load( entity.getId() );
			entity.getClobLocator().truncate( 1 );
			entity.getClobLocator().setString( 1, changed );
			s.getTransaction().commit();
			s.close();

			s = openSession();
			s.beginTransaction();
			entity = ( LobHolder ) s.byId( LobHolder.class ).with( LockOptions.UPGRADE ).load( entity.getId() );
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
		entity = ( LobHolder ) s.byId( LobHolder.class ).with( LockOptions.UPGRADE ).load( entity.getId() );
		assertNotNull( entity.getClobLocator() );
		assertEquals( CLOB_SIZE, entity.getClobLocator().length() );
		assertEquals( original, extractData( entity.getClobLocator() ) );
		entity.setClobLocator( s.getLobHelper().createClob( changed ) );
		s.getTransaction().commit();
		s.close();

		// test empty clob
		if ( !(getDialect() instanceof SybaseASE157Dialect) ) { // Skip for Sybase. HHH-6425
			s = openSession();
			s.beginTransaction();
			entity = s.get( LobHolder.class, entity.getId() );
			assertEquals( CLOB_SIZE, entity.getClobLocator().length() );
			assertEquals( changed, extractData( entity.getClobLocator() ) );
			entity.setClobLocator( s.getLobHelper().createClob( empty ) );
			s.getTransaction().commit();
			s.close();

			s = openSession();
			s.beginTransaction();
			entity = s.get( LobHolder.class, entity.getId() );
			if ( entity.getClobLocator() != null) {
				assertEquals( empty.length(), entity.getClobLocator().length() );
				assertEquals( empty, extractData( entity.getClobLocator() ) );
			}
			s.delete( entity );
			s.getTransaction().commit();
			s.close();
		}

	}

	@Test
	@RequiresDialectFeature(
			value = DialectChecks.SupportsUnboundedLobLocatorMaterializationCheck.class,
			comment = "database/driver does not support materializing a LOB locator outside the owning transaction"
	)
	public void testUnboundedClobLocatorAccess() throws Throwable {
		// Note: unbounded mutation of the underlying lob data is completely
		// unsupported; most databases would not allow such a construct anyway.
		// Thus here we are only testing materialization...

		String original = buildString( CLOB_SIZE, 'x' );

		Session s = openSession();
		s.beginTransaction();
		LobHolder entity = new LobHolder();
		entity.setClobLocator( s.getLobHelper().createClob( original ) );
		s.save( entity );
		s.getTransaction().commit();
		s.close();

		// load the entity with the clob locator, and close the session/transaction;
		// at that point it is unbounded...
		s = openSession();
		s.beginTransaction();
		entity = s.get( LobHolder.class, entity.getId() );
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

	public static String extractData(Clob clob) throws Exception {
		return DataHelper.extractString( clob.getCharacterStream() );
	}

	public static String buildString(int size, char baseChar) {
		StringBuilder buff = new StringBuilder();
		for( int i = 0; i < size; i++ ) {
			buff.append( baseChar );
		}
		return buff.toString();
	}
}
