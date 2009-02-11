// $Id: SerializableTypeTest.java 10977 2006-12-12 23:28:04Z steve.ebersole@jboss.com $
package org.hibernate.test.lob;

import junit.framework.Test;

import org.hibernate.Session;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.SybaseDialect;
import org.hibernate.dialect.Sybase11Dialect;
import org.hibernate.dialect.SybaseASE15Dialect;
import org.hibernate.dialect.SybaseAnywhereDialect;
import org.hibernate.junit.functional.FunctionalTestCase;
import org.hibernate.junit.functional.FunctionalTestClassTestSuite;

/**
 * Tests of {@link org.hibernate.type.SerializableType}
 * 
 * @author Steve Ebersole
 */
public class SerializableTypeTest extends FunctionalTestCase {

	public SerializableTypeTest(String testName) {
		super( testName );
	}

	public String[] getMappings() {
		return new String[] { "lob/LobMappings.hbm.xml" };
	}

	public static Test suite() {
		return new FunctionalTestClassTestSuite( SerializableTypeTest.class );
	}


	public String getCacheConcurrencyStrategy() {
		return null;
	}

	public void testNewSerializableType() {
		// Sybase dialects do not support ResultSet.getBlob(String)
		if ( getDialect() instanceof SybaseDialect || getDialect() instanceof Sybase11Dialect || getDialect() instanceof SybaseASE15Dialect || getDialect() instanceof SybaseAnywhereDialect ) {
			return;
		}

		final String payloadText = "Initial payload";

		Session s = openSession();
		s.beginTransaction();
		LobHolder holder = new LobHolder();
		holder.setSerialData( new SerializableData( payloadText ) );
		s.save( holder );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		holder = ( LobHolder ) s.get( LobHolder.class, holder.getId() );
		SerializableData serialData = ( SerializableData ) holder.getSerialData();
		assertEquals( payloadText, serialData.getPayload() );
		s.delete( holder );
		s.getTransaction().commit();
		s.close();
	}


}
