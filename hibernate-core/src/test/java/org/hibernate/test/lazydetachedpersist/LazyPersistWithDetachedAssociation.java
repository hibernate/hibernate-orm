package org.hibernate.test.lazydetachedpersist;

import org.hibernate.Session;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertNotNull;

public class LazyPersistWithDetachedAssociation extends BaseCoreFunctionalTestCase {

	@Before
	public void setUpData() {
		Session s = openSession();
		s.beginTransaction();
		
        Address address = new Address();
        address.setId(1L);
        address.setContent("21 Jump St");
		s.persist( address );

		s.getTransaction().commit();
		s.close();
	}

	@After
	public void cleanUpData() {
		Session s = openSession();
		s.beginTransaction();
		s.delete( s.get( Address.class, 1L ) );
		s.delete( s.get( Person.class, 1L ) );
		s.getTransaction().commit();
		s.close();
	}

	@Test
	@TestForIssue( jiraKey = "HHH-3846" )
	public void testDetachedAssociationOnPersisting() {
		sessionFactory().getStatistics().clear();

		Session s = openSession();
		s.beginTransaction();
		// first load the address
        Address loadedAddress = (Address) session.load(Address.class, 1L);
        
		assertNotNull( loadedAddress );

		s.getTransaction().commit();
		s.close();
		
		s = openSession();
		s.beginTransaction();
		
		Address reloadedAddr = (Address) s.get(Address.class, 1L);
		
		Person person = new Person();
        person.setId(1L);
        person.setName("Johnny Depp");
        person.setAddress(loadedAddress);
        
        s.persist(person); 
		s.getTransaction().commit();
		s.close();
	}

	@Override
	protected void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.setProperty( Environment.ENABLE_LAZY_LOAD_NO_TRANS, "false" );
		cfg.setProperty( Environment.GENERATE_STATISTICS, "true" );
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				Address.class,
				Person.class,
		};
	}

}
