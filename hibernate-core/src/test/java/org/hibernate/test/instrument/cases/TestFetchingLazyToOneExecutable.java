/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.instrument.cases;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.bytecode.spi.InstrumentedClassLoader;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
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
		BootstrapServiceRegistryBuilder bsrb = new BootstrapServiceRegistryBuilder();
		// make sure we pick up the TCCL, and make sure its the isolated CL...
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		if ( classLoader == null ) {
			throw new RuntimeException( "Isolated ClassLoader not yet set as TCCL" );
		}
		if ( !InstrumentedClassLoader.class.isInstance( classLoader ) ) {
			throw new RuntimeException( "Isolated ClassLoader not yet set as TCCL" );
		}
		bsrb.applyClassLoader( classLoader );

		serviceRegistry = new StandardServiceRegistryBuilder( bsrb.build() )
				.applySetting( Environment.HBM2DDL_AUTO, "create-drop" )
				.applySetting( AvailableSettings.USE_SECOND_LEVEL_CACHE, "false" )
				.build();

		MetadataSources metadataSources = new MetadataSources( serviceRegistry );
		metadataSources.addAnnotatedClass( Person.class );
		metadataSources.addAnnotatedClass( Passport.class );

		factory = metadataSources.buildMetadata().buildSessionFactory();

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
			StandardServiceRegistryBuilder.destroy( serviceRegistry );
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
