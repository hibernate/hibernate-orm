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
package org.hibernate.test.annotations.entity;

import org.junit.Test;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

/**
 * @author Emmanuel Bernard
 */
public class PropertyDefaultMappingsTest extends BaseCoreFunctionalTestCase {
	@Test
	public void testSerializableObject() throws Exception {
		Session s;
		Transaction tx;
		s = openSession();
		tx = s.beginTransaction();
		Country c = new Country();
		c.setName( "France" );
		Address a = new Address();
		a.setCity( "Paris" );
		a.setCountry( c );
		s.persist( a );
		tx.commit();
		s.close();

		s = openSession();
		tx = s.beginTransaction();
		Address reloadedAddress = (Address) s.get( Address.class, a.getId() );
		assertNotNull( reloadedAddress );
		assertNotNull( reloadedAddress.getCountry() );
		assertEquals( a.getCountry().getName(), reloadedAddress.getCountry().getName() );
		tx.rollback();
		s.close();
	}

	@Test
	public void testTransientField() throws Exception {
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		WashingMachine wm = new WashingMachine();
		wm.setActive( true );
		s.persist( wm );
		tx.commit();
		s.clear();
		tx = s.beginTransaction();
		wm = (WashingMachine) s.get( WashingMachine.class, wm.getId() );
		assertFalse( "transient should not be persistent", wm.isActive() );
		s.delete( wm );
		tx.commit();
		s.close();
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[]{
				Address.class,
				WashingMachine.class
		};
	}
}
