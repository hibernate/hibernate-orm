// $Id: SerializableTypeTest.java 10977 2006-12-12 23:28:04Z steve.ebersole@jboss.com $
package org.hibernate.test.lob;

import junit.framework.Test;

import org.hibernate.Session;
import org.hibernate.testing.junit.functional.FunctionalTestCase;
import org.hibernate.testing.junit.functional.FunctionalTestClassTestSuite;

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
		return new String[] { "lob/SerializableMappings.hbm.xml" };
	}

	public static Test suite() {
		return new FunctionalTestClassTestSuite( SerializableTypeTest.class );
	}


	public String getCacheConcurrencyStrategy() {
		return null;
	}

	public void testNewSerializableType() {
		final String initialPayloadText = "Initial payload";
		final String changedPayloadText = "Changed payload";

		Session s = openSession();
		s.beginTransaction();
		SerializableHolder holder = new SerializableHolder();
		s.save( holder );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		holder = ( SerializableHolder ) s.get( SerializableHolder.class, holder.getId() );
		assertNull( holder.getSerialData() );
		holder.setSerialData( new SerializableData( initialPayloadText ) );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		holder = ( SerializableHolder ) s.get( SerializableHolder.class, holder.getId() );
		SerializableData serialData = ( SerializableData ) holder.getSerialData();
		assertEquals( initialPayloadText, serialData.getPayload() );
		holder.setSerialData( new SerializableData( changedPayloadText ) );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		holder = ( SerializableHolder ) s.get( SerializableHolder.class, holder.getId() );
		serialData = ( SerializableData ) holder.getSerialData();
		assertEquals( changedPayloadText, serialData.getPayload() );
		holder.setSerialData( null );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		holder = ( SerializableHolder ) s.get( SerializableHolder.class, holder.getId() );
		assertNull( holder.getSerialData() );
		s.delete( holder );
		s.getTransaction().commit();
		s.close();
	}

}
