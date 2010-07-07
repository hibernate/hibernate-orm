package org.hibernate.test.entityname;

import org.hibernate.Session;
import org.hibernate.testing.junit.functional.FunctionalTestCase;

/**
 * 
 * @author stliu
 *
 */
public class EntityNameFromSubClassTest extends FunctionalTestCase {

	public EntityNameFromSubClassTest(String string) {
		super(string);
	}

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

	public String[] getMappings() {
		return new String[] { "entityname/Vehicle.hbm.xml" };
	}

}
