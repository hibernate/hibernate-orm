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
		cfg.setProperty( Environment.ENABLE_LAZY_LOAD_NO_TRANS, "true" );
		cfg.setProperty( Environment.USE_SECOND_LEVEL_CACHE, "false" );
		super.prepare( cfg );

		Session session = getFactory().openSession();
		Transaction tx = session.beginTransaction();

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

	protected void cleanup() {
	}

}
