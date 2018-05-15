/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.idclass.xml;

import org.junit.Test;

import org.hibernate.testing.FailureExpected;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

/**
 * A test for HHH-4282
 *
 * @author Hardy Ferentschik
 */
@FailureExpected( jiraKey = "HHH-4282" )
public class IdClassXmlTest extends BaseCoreFunctionalTestCase {
	@Test
	public void testEntityMappingPropertiesAreNotIgnored() {
		throw new RuntimeException();
//		Session s = openSession();
//		Transaction tx = s.beginTransaction();
//
//		HabitatSpeciesLink link = new HabitatSpeciesLink();
//		link.setHabitatId( 1l );
//		link.setSpeciesId( 1l );
//		s.persist( link );
//
//		Query q = s.getNamedQuery( "testQuery" );
//		assertEquals( 1, q.list().size() );
//
//		tx.rollback();
//		s.close();
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				HabitatSpeciesLink.class
		};
	}

	@Override
	protected String[] getXmlFiles() {
		return new String[] {
				"org/hibernate/test/annotations/idclass/xml/HabitatSpeciesLink.xml"
		};
	}
}
