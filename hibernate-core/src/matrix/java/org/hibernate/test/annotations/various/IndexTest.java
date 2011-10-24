/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.test.annotations.various;

import java.util.Date;

import org.junit.Test;

import org.hibernate.Session;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

/**
 * @author Emmanuel Bernard
 */
public class IndexTest extends BaseCoreFunctionalTestCase {
	@Test
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

	@Test
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

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[]{
				Conductor.class,
				Vehicule.class,
				ProfessionalAgreement.class,
				Truck.class
		};
	}
}
