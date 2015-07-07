package org.hibernate.test.bytecode.enhancement.join;

import java.util.List;

import org.hibernate.Hibernate;
import org.hibernate.Session;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class HHH3949TestTask1 extends AbstractHHH3949TestTask {

	public void execute() {
		// verify the work around query
		performQueryAndVerifyResults( "from Person p fetch all properties left join fetch p.vehicle" );
		performQueryAndVerifyResults( "from Person p left join fetch p.vehicle" );
	}

	@SuppressWarnings("unchecked")
	private void performQueryAndVerifyResults(String query) {
		// 1) open session
		Session session = getFactory().openSession();
		// 2) perform the query
		List<Person> persons = (List<Person>) session.createQuery( query ).list();
		// 3) close the session : this ensures that no more queries and/or data loading happen
		session.close();

		// 4) verify the results
		for ( Person person : persons ) {
			assertTrue( Hibernate.isInitialized( person ) );
			if ( person.getId() < 3 ) {
				assertNotNull( person.getVehicle() );
				assertTrue( Hibernate.isInitialized( person.getVehicle() ) );
				assertNotNull( person.getVehicle().getDriver() );
			}
		}
	}
}
