/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.xml.hbm;

import org.junit.Test;

import org.hibernate.Session;
import org.hibernate.testing.DialectChecks;
import org.hibernate.testing.RequiresDialectFeature;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

/**
 * @author Emmanuel Bernard
 */
@RequiresDialectFeature(DialectChecks.SupportsIdentityColumns.class)
public class HbmWithIdentityTest extends BaseCoreFunctionalTestCase {
	@Test
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

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] { Sky.class, ZImpl.class };
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
