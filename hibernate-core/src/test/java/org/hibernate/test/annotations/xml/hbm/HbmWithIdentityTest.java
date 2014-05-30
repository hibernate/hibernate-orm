/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
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
 */
package org.hibernate.test.annotations.xml.hbm;

import org.hibernate.Session;
import org.hibernate.testing.DialectChecks;
import org.hibernate.testing.RequiresDialectFeature;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

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
