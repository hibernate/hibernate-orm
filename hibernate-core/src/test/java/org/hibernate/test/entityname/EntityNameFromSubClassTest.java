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
package org.hibernate.test.entityname;
import org.junit.Test;

import org.hibernate.Session;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;

/**
 * @author stliu
 */
public class EntityNameFromSubClassTest extends BaseCoreFunctionalTestCase {
	public String[] getMappings() {
		return new String[] { "entityname/Vehicle.hbm.xml" };
	}

	@SuppressWarnings( {"unchecked"})
	@Test
	public void testEntityName() {
		Session s = openSession();
		s.beginTransaction();
		Person stliu = new Person();
		stliu.setName("stliu");
		Car golf = new Car();
		golf.setOwner("stliu");
		stliu.getCars().add(golf);
		s.save(stliu);
		s.getTransaction().commit();
		s.close();
		
		s=openSession();
		s.beginTransaction();
		Person p = (Person)s.get(Person.class, stliu.getId());
		assertEquals(1, p.getCars().size());
		assertEquals(Car.class, p.getCars().iterator().next().getClass());
		s.getTransaction().commit();
		s.close();
	}

}
