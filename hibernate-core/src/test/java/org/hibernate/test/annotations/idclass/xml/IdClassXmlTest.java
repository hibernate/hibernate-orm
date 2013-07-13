/*
 * JBoss, Home of Professional Open Source
 * Copyright 2005, JBoss Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
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
