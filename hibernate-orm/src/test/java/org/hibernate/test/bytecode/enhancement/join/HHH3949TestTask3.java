package org.hibernate.test.bytecode.enhancement.join;

import java.util.List;

import org.hibernate.FetchMode;
import org.hibernate.Session;

import org.junit.Assert;

public class HHH3949TestTask3 extends AbstractHHH3949TestTask {

	@SuppressWarnings("unchecked")
	public void execute() {
		Session session = getFactory().openSession();
		session.getTransaction().begin();
		List<Person> persons = (List<Person>) session.createCriteria( Person.class )
				.setFetchMode( "vehicle", FetchMode.JOIN )
				.list();
		for ( Person person : persons ) {
			if ( shouldHaveVehicle( person ) ) {
				Assert.assertNotNull( person.getVehicle() );
				Assert.assertNotNull( person.getVehicle().getDriver() );
			}
		}
		session.getTransaction().commit();
		session.close();
	}
}
