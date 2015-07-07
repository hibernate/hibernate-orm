package org.hibernate.test.bytecode.enhancement.join;

import java.util.List;

import org.hibernate.Session;

import org.junit.Assert;

public class HHH3949TestTask2 extends AbstractHHH3949TestTask {

	public void execute() {
		performQueryAndVerifyResults( "from Vehicle v fetch all properties left join fetch v.driver" );
		performQueryAndVerifyResults( "from Vehicle v left join fetch v.driver" );
	}

	@SuppressWarnings("unchecked")
	public void performQueryAndVerifyResults(String query) {
		// 1) open session
		Session session = getFactory().openSession();
		// 2) perform the query
		List<Vehicle> vehicles = (List<Vehicle>) session.createQuery( query ).list();
		// 3) close the session : this ensures that no more queries and/or data loading happen
		session.close();

		// 4) verify the results
		for ( Vehicle vehicle : vehicles ) {
			if ( vehicle.getId() < 3 ) {
				Assert.assertNotNull( vehicle.getDriver() );
				Assert.assertNotNull( vehicle.getDriver().getVehicle() );
			}
		}
	}
}
