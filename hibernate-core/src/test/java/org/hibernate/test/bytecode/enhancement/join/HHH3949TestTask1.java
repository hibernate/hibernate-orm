package org.hibernate.test.bytecode.enhancement.join;

import java.util.List;

import org.hibernate.Session;

import org.junit.Assert;

public class HHH3949TestTask1 extends AbstractHHH3949TestTask {

	@SuppressWarnings("unchecked")
	public void execute() {
		Session session = getFactory().openSession();
		List<Person> persons = (List<Person>) session.createQuery( "from Person p left join fetch p.vehicle" ).list();
		for ( Person person : persons ) {
			if ( person.getId() < 3 ) {
				Assert.assertNotNull( person.getVehicle() );
				Assert.assertNotNull( person.getVehicle().getDriver() );
			}
		}
		session.close();
	}
}
