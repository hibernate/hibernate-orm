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
package org.hibernate.test.abstractembeddedcomponents.propertyref;

import org.hibernate.Session;
import org.hibernate.Transaction;

import org.hibernate.testing.FailureExpectedWithNewMetamodel;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;

/**
 * @author Steve Ebersole
 */
@FailureExpectedWithNewMetamodel( jiraKey = "HHH-7242", message = "property-ref" )
public class AbstractComponentPropertyRefTest extends BaseCoreFunctionalTestCase {
	public String[] getMappings() {
		return new String[] { "abstractembeddedcomponents/propertyref/Mappings.hbm.xml" };
	}

	@Test
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
