package org.hibernate.test.generated;

import junit.framework.Test;

import org.hibernate.junit.functional.DatabaseSpecificFunctionalTestCase;
import org.hibernate.junit.functional.FunctionalTestClassTestSuite;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.Oracle9Dialect;
import org.hibernate.Session;

/**
 * {@inheritDoc}
 *
 * @author Steve Ebersole
 */
public class PartiallyGeneratedComponentTest extends DatabaseSpecificFunctionalTestCase {
	public PartiallyGeneratedComponentTest(String string) {
		super( string );
	}

	public String[] getMappings() {
		return new String[] { "generated/ComponentOwner.hbm.xml" };
	}

	public static Test suite() {
		return new FunctionalTestClassTestSuite( PartiallyGeneratedComponentTest.class );
	}

	public boolean appliesTo(Dialect dialect) {
		return dialect instanceof Oracle9Dialect;
	}

	public void testPartialComponentGeneration() {
		ComponentOwner owner = new ComponentOwner( "initial" );
		Session s = openSession();
		s.beginTransaction();
		s.save( owner );
		s.getTransaction().commit();
		s.close();

		assertNotNull( "expecting insert value generation", owner.getComponent() );
		int previousValue = owner.getComponent().getGenerated();
		assertFalse( "expecting insert value generation", 0 == previousValue );

		s = openSession();
		s.beginTransaction();
		owner = ( ComponentOwner ) s.get( ComponentOwner.class, owner.getId() );
		assertEquals( "expecting insert value generation", previousValue, owner.getComponent().getGenerated() );
		owner.setName( "subsequent" );
		s.getTransaction().commit();
		s.close();

		assertNotNull( owner.getComponent() );
		previousValue = owner.getComponent().getGenerated();

		s = openSession();
		s.beginTransaction();
		owner = ( ComponentOwner ) s.get( ComponentOwner.class, owner.getId() );
		assertEquals( "expecting update value generation", previousValue, owner.getComponent().getGenerated() );
		s.delete( owner );
		s.getTransaction().commit();
		s.close();
	}
}
