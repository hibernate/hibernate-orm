/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
