package org.hibernate.test.usercollection.parameterized;

import junit.framework.Test;

import org.hibernate.junit.functional.FunctionalTestCase;
import org.hibernate.junit.functional.FunctionalTestClassTestSuite;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.Hibernate;

/**
 * Tes for parameterized user collection types.
 *
 * @author Holger Brands
 * @author Steve Ebersole
 */
public class ParameterizedUserCollectionTypeTest extends FunctionalTestCase {
	public ParameterizedUserCollectionTypeTest(String string) {
		super( string );
	}

	public static Test suite() {
		return new FunctionalTestClassTestSuite( ParameterizedUserCollectionTypeTest.class );
	}

	public String[] getMappings() {
		return new String[] { "usercollection/parameterized/Mapping.hbm.xml" };
	}

	public void testBasicOperation() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		Entity entity = new Entity( "tester" );
		entity.getValues().add( "value-1" );
		s.persist( entity );
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		entity = ( Entity ) s.get( Entity.class, "tester" );
		assertTrue( Hibernate.isInitialized( entity.getValues() ) );
		assertEquals( 1, entity.getValues().size() );
        assertEquals( "Hello", ( ( DefaultableList ) entity.getValues() ).getDefaultValue() );
		s.delete( entity );
		t.commit();
		s.close();
	}
}
