/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.entityname;
import org.junit.Test;

import org.hibernate.Session;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;

/**
 * @author stliu
 */
public class EntityNameFromSubClassTest extends BaseCoreFunctionalTestCase {

	@Override
	protected String getBaseForMappings() {
		return "org/hibernate/orm/test/";
	}

	@Override
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
		s.persist(stliu);
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
