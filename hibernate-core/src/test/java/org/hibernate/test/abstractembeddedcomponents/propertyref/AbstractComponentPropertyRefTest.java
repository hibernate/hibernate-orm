package org.hibernate.test.abstractembeddedcomponents.propertyref;
import junit.framework.Test;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.testing.junit.functional.FunctionalTestCase;
import org.hibernate.testing.junit.functional.FunctionalTestClassTestSuite;

/**
 * @author Steve Ebersole
 */
public class AbstractComponentPropertyRefTest extends FunctionalTestCase {
	public AbstractComponentPropertyRefTest(String name) {
		super( name );
	}

	public static Test suite() {
		return new FunctionalTestClassTestSuite( AbstractComponentPropertyRefTest.class );
	}

	public String[] getMappings() {
		return new String[] { "abstractembeddedcomponents/propertyref/Mappings.hbm.xml" };
	}

	public void testPropertiesRefCascades() {
		Session session = openSession();
		Transaction trans = session.beginTransaction();
		ServerImpl server = new ServerImpl();
		session.save( server );
		AddressImpl address = new AddressImpl();
		server.setAddress( address );
		address.setServer( server );
		session.flush();
		session.createQuery( "from Server s join fetch s.address" ).list();
		trans.commit();
		session.close();

		assertNotNull( server.getId() );
		assertNotNull( address.getId() );

		session = openSession();
		trans = session.beginTransaction();
		session.delete( address );
		session.delete( server );
		trans.commit();
		session.close();
	}
}
