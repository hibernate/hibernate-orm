//$Id$
package org.hibernate.test.annotations.polymorphism;

import org.hibernate.test.annotations.TestCase;
import org.hibernate.Session;
import org.hibernate.Transaction;

/**
 * @author Emmanuel Bernard
 */
public class PolymorphismTest extends TestCase {

	public void testPolymorphism() throws Exception {
		Car car = new Car();
		car.setModel( "SUV" );
		SportCar car2 = new SportCar();
		car2.setModel( "350Z" );
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		tx.begin();
		s.persist( car );
		s.persist( car2 );
		s.flush();
		assertEquals( 2, s.createQuery( "select car from Car car").list().size() );
		assertEquals( 0, s.createQuery( "select count(m) from " + MovingThing.class.getName() + " m").list().size() );
		tx.rollback();
		s.close();

	}

	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				Car.class,
				SportCar.class
		};
	}
}
