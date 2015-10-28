package org.hibernate.test.bytecode.enhancement.join;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;

import org.hibernate.test.bytecode.enhancement.AbstractEnhancerTestTask;

public abstract class AbstractHHH3949TestTask extends AbstractEnhancerTestTask {

	public Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {Person.class, Vehicle.class};
	}

	public void prepare() {
		Configuration cfg = new Configuration();
//		cfg.setProperty( Environment.ENABLE_LAZY_LOAD_NO_TRANS, "true" );
		cfg.setProperty( Environment.ENABLE_LAZY_LOAD_NO_TRANS, "false" );
		cfg.setProperty( Environment.USE_SECOND_LEVEL_CACHE, "false" );
		super.prepare( cfg );

		Session session = getFactory().openSession();
		Transaction tx = session.beginTransaction();

		// it is important that the data associations remain as follows:
		//		* Johnny <-> Volkswagen Golf
		//		* Ricky <-> Subaru Impreza
		//		* Rosy -> none
		//		* none <- Renault Truck
		//
		// see #shouldHaveVehicle and #shouldHaveDriver

		Person person1 = new Person( "Johnny" );
		Person person2 = new Person( "Ricky" );
		Person person3 = new Person( "Rosy" );
		session.save( person1 );
		session.save( person2 );
		session.save( person3 );

		Vehicle vehicle1 = new Vehicle( "Volkswagen Golf" );
		vehicle1.setDriver( person1 );
		session.save( vehicle1 );

		Vehicle vehicle2 = new Vehicle( "Subaru Impreza" );
		vehicle2.setDriver( person2 );
		person2.setVehicle( vehicle2 );
		session.save( vehicle2 );

		Vehicle vehicle3 = new Vehicle( "Renault Truck" );

		session.save( vehicle3 );

		tx.commit();
		session.close();
	}

	protected boolean shouldHaveVehicle(Person person) {
		return "Johnny".equals( person.getName() )
				|| "Ricky".equals( person.getName() );
	}

	protected boolean shouldHaveDriver(Vehicle vehicle) {
		return "Volkswagen Golf".equals( vehicle.getName() )
				|| "Subaru Impreza".equals( vehicle.getName() );
	}

	protected void cleanup() {
	}

}
