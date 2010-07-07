//$Id:HbmTest.java 9793 2006-04-26 02:20:18 -0400 (mer., 26 avr. 2006) epbernard $
package org.hibernate.test.annotations.xml.hbm;

import org.hibernate.Session;
import org.hibernate.testing.junit.DialectChecks;
import org.hibernate.testing.junit.RequiresDialectFeature;
import org.hibernate.test.annotations.TestCase;

/**
 * @author Emmanuel Bernard
 */
public class HbmWithIdentityTest extends TestCase {

	@RequiresDialectFeature(DialectChecks.SupportsIdentityColumns.class)
	public void testManyToOneAndInterface() throws Exception {
		Session s = openSession();
		s.getTransaction().begin();
		B b = new BImpl();
		b.setBId( 1 );
		s.persist( b );
		Z z = new ZImpl();
		z.setB( b );
		s.persist( z );
		s.flush();
		s.getTransaction().rollback();
		s.close();
	}

	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				Sky.class,
				ZImpl.class

		};
	}

	@Override
	protected String[] getXmlFiles() {
		return new String[] {
				"org/hibernate/test/annotations/xml/hbm/A.hbm.xml",
				"org/hibernate/test/annotations/xml/hbm/B.hbm.xml",
				"org/hibernate/test/annotations/xml/hbm/CloudType.hbm.xml"
		};
	}
}
