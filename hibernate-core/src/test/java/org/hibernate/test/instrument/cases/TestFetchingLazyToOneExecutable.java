/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat Inc. or third-party contributors as
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
package org.hibernate.test.instrument.cases;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.service.ServiceRegistry;

import org.hibernate.testing.ServiceRegistryBuilder;
import org.hibernate.test.instrument.domain.Passport;
import org.hibernate.test.instrument.domain.Person;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * @author Steve Ebersole
 */
public class TestFetchingLazyToOneExecutable implements Executable {
	private ServiceRegistry serviceRegistry;
	private SessionFactory factory;

	@Override
	public void execute() throws Exception {
		doBaselineAssertions();

		doFetchNonMappedBySideAssertions();
		doFetchMappedBySideAssertions();
	}

	private void doBaselineAssertions() {
		{
			// First, load from the non-owning side by id.  Person#passport should be uninitialized
			Session s = factory.openSession();
			s.beginTransaction();
			Person person = (Person) s.get( Person.class, 1 );
			assertTrue( Hibernate.isInitialized( person ) );
			assertFalse( Hibernate.isPropertyInitialized( person, "passport" ) );
			assertNotNull( person.getPassport() );
			s.getTransaction().commit();
			s.close();
		}

		{
			// Then, load from the owning side by id.  Passport#person should be uninitialized
			Session s = factory.openSession();
			s.beginTransaction();
			Passport passport = (Passport) s.get( Passport.class, 1 );
			assertTrue( Hibernate.isInitialized( passport ) );
			assertFalse( Hibernate.isPropertyInitialized( passport, "person" ) );
			assertNotNull( passport.getPerson() );
			s.getTransaction().commit();
			s.close();
		}
	}

	private void doFetchNonMappedBySideAssertions() {
		// try to eagerly fetch the association from the owning (non-mappedBy) side
		Session s = factory.openSession();
		s.beginTransaction();
// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
// the whole question here is design, and whether the `fetch all properties` should be needed
//		Passport p = (Passport) s.createQuery( "select p from Passport p join fetch p.person" ).uniqueResult();
// versus:
// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		Passport p = (Passport) s.createQuery( "select p from Passport p fetch all properties join fetch p.person" ).uniqueResult();
		assertTrue( Hibernate.isInitialized( p ) );
		assertTrue(
				"Assertion that the eager fetch of non-mappedBy association (Passport#person) was performed properly",
				Hibernate.isPropertyInitialized( p, "person" )
		);
		assertNotNull( p.getPerson() );
		assertTrue( Hibernate.isInitialized( p.getPerson() ) );
		assertSame( p, p.getPerson().getPassport() );
		s.getTransaction().commit();
		s.close();

	}

	private void doFetchMappedBySideAssertions() {
		// try to eagerly fetch the association from the non-owning (mappedBy) side
		Session s = factory.openSession();
		s.beginTransaction();
// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
// the whole question here is design, and whether the `fetch all properties` should be needed
//		Person p  = (Person) s.createQuery( "select p from Person p join fetch p.passport" ).uniqueResult();
// versus:
// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		Person p  = (Person) s.createQuery( "select p from Person p fetch all properties join fetch p.passport" ).uniqueResult();
		assertTrue( Hibernate.isInitialized( p ) );
		assertTrue(
				"Assertion that the eager fetch of mappedBy association (Person#passport) was performed properly",
				Hibernate.isPropertyInitialized( p, "passport" )
		);
		assertNotNull( p.getPassport() );
		assertTrue( Hibernate.isInitialized( p.getPassport() ) );
		assertSame( p, p.getPassport().getPerson() );
		s.getTransaction().commit();
		s.close();
	}

	@Override
	public final void prepare() {
		Configuration cfg = new Configuration()
				.setProperty( AvailableSettings.HBM2DDL_AUTO, "create-drop" )
				.setProperty( AvailableSettings.USE_SECOND_LEVEL_CACHE, "false" );
		cfg.addAnnotatedClass( Person.class );
		cfg.addAnnotatedClass( Passport.class );
		serviceRegistry = ServiceRegistryBuilder.buildServiceRegistry( cfg.getProperties() );
		factory = cfg.buildSessionFactory( serviceRegistry );

		createData();
	}

	private void createData() {
		Person steve = new Person( "Steve" );
		Passport passport = new Passport( steve, "123456789", "Acme Emirates" );

		Session s = factory.openSession();
		s.beginTransaction();
		s.save( steve );
		s.save( passport );
		s.getTransaction().commit();
		s.close();
	}

	@Override
	public final void complete() {
		try {
			cleanupData();
		}
		finally {
			factory.close();
			factory = null;
			if ( serviceRegistry != null ) {
				ServiceRegistryBuilder.destroy( serviceRegistry );
				serviceRegistry = null;
			}
		}
	}

	private void cleanupData() {
		Session s = factory.openSession();
		s.beginTransaction();
		s.createQuery( "delete Passport" ).executeUpdate();
		s.createQuery( "delete Person" ).executeUpdate();
		s.getTransaction().commit();
		s.close();
	}

}
