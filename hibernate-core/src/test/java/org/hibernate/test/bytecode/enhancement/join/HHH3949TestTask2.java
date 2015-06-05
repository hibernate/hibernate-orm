package org.hibernate.test.bytecode.enhancement.join;

import java.util.List;

import org.hibernate.Session;

import org.junit.Assert;

public class HHH3949TestTask2 extends AbstractHHH3949TestTask {

	@SuppressWarnings("unchecked")
	public void execute() {
		Session session = getFactory().openSession();
		List<Vehicle> vehicles = (List<Vehicle>) session.createQuery( "from Vehicle v left join fetch v.driver" )
				.list();
		for ( Vehicle vehicle : vehicles ) {
			if ( vehicle.getId() < 3 ) {
				Assert.assertNotNull( vehicle.getDriver() );
				Assert.assertNotNull( vehicle.getDriver().getVehicle() );
			}
		}
		session.close();
	}
}
