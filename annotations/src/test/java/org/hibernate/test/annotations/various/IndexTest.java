//$Id$
package org.hibernate.test.annotations.various;

import java.util.Date;

import org.hibernate.Session;
import org.hibernate.test.annotations.TestCase;

/**
 * @author Emmanuel Bernard
 */
public class IndexTest extends TestCase {
	public void testIndexManyToOne() throws Exception {
		//TODO find a way to test indexes???
		Session s = openSession();
		s.getTransaction().begin();
		Conductor emmanuel = new Conductor();
		emmanuel.setName( "Emmanuel" );
		s.persist( emmanuel );
		Vehicule tank = new Vehicule();
		tank.setCurrentConductor( emmanuel );
		tank.setRegistrationNumber( "324VX43" );
		s.persist( tank );
		s.flush();
		s.delete( tank );
		s.delete( emmanuel );
		s.getTransaction().rollback();
		s.close();
	}

	public void testIndexAndJoined() throws Exception {
		Session s = openSession();
		s.getTransaction().begin();
		Conductor cond = new Conductor();
		cond.setName( "Bob" );
		s.persist( cond );
		ProfessionalAgreement agreement = new ProfessionalAgreement();
		agreement.setExpirationDate( new Date() );
		s.persist( agreement );
		Truck truck = new Truck();
		truck.setAgreement( agreement );
		truck.setWeight( 20 );
		truck.setRegistrationNumber( "2003424" );
		truck.setYear( 2005 );
		truck.setCurrentConductor( cond );
		s.persist( truck );
		s.flush();
		s.delete( truck );
		s.delete( agreement );
		s.delete( cond );
		s.getTransaction().rollback();
		s.close();
	}

	protected Class[] getAnnotatedClasses() {
		return new Class[]{
				Conductor.class,
				Vehicule.class,
				ProfessionalAgreement.class,
				Truck.class
		};
	}
}
