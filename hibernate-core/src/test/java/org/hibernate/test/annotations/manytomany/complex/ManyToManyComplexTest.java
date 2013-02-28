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
package org.hibernate.test.annotations.manytomany.complex;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.testing.FailureExpectedWithNewMetamodel;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Many to many tests
 *
 * @author Emmanuel Bernard
 */
@SuppressWarnings("unchecked")
public class ManyToManyComplexTest
 extends BaseCoreFunctionalTestCase {

	@Test
	public void testBasic() throws Exception {
		Session s;
		Transaction tx;
		s = openSession();
		tx = s.beginTransaction();
		Employer er = new Employer();
		Employee ee = new Employee();
		s.persist( ee );
		Set erColl = new HashSet();
		Collection eeColl = new ArrayList();
		erColl.add( ee );
		eeColl.add( er );
		er.setEmployees( erColl );
		ee.setEmployers( eeColl );
		//s.persist(ee);
		tx.commit();
		s.close();

		s = openSession();
		tx = s.beginTransaction();
		er = (Employer) s.load( Employer.class, er.getId() );
		assertNotNull( er );
		assertNotNull( er.getEmployees() );
		assertEquals( 1, er.getEmployees().size() );
		Employee eeFromDb = (Employee) er.getEmployees().iterator().next();
		assertEquals( ee.getId(), eeFromDb.getId() );
		tx.commit();
		s.close();

		s = openSession();
		tx = s.beginTransaction();
		ee = (Employee) s.get( Employee.class, ee.getId() );
		assertNotNull( ee );
		assertFalse( "ManyToMany mappedBy lazyness", Hibernate.isInitialized( ee.getEmployers() ) );
		tx.commit();
		assertFalse( "ManyToMany mappedBy lazyness", Hibernate.isInitialized( ee.getEmployers() ) );
		s.close();

		s = openSession();
		tx = s.beginTransaction();
		ee = (Employee) s.get( Employee.class, ee.getId() );
		assertNotNull( ee );
		er = ee.getEmployers().iterator().next();
		assertTrue( "second join non lazy", Hibernate.isInitialized( er ) );
		s.delete( er );
		s.delete( ee );
		tx.commit();
		s.close();
	}

	@Test
	public void testOrderByEmployee() throws Exception {
		Session s;
		Transaction tx;
		s = openSession();
		tx = s.beginTransaction();
		Employer employer = new Employer();
		Employee employee1 = new Employee();
		employee1.setName( "Emmanuel" );
		Employee employee2 = new Employee();
		employee2.setName( "Alice" );
		s.persist( employee1 );
		s.persist( employee2 );
		Set erColl = new HashSet();
		Collection eeColl = new ArrayList();
		Collection eeColl2 = new ArrayList();
		erColl.add( employee1 );
		erColl.add( employee2 );
		eeColl.add( employer );
		eeColl2.add( employer );
		employer.setEmployees( erColl );
		employee1.setEmployers( eeColl );
		employee2.setEmployers( eeColl2 );

		s.flush();
		s.clear();

		employer = (Employer) s.get( Employer.class, employer.getId() );
		assertNotNull( employer );
		assertNotNull( employer.getEmployees() );
		assertEquals( 2, employer.getEmployees().size() );
		Employee eeFromDb = (Employee) employer.getEmployees().iterator().next();
		assertEquals( employee2.getName(), eeFromDb.getName() );
		tx.rollback();
		s.close();
	}
	
	// HHH-4394
	@Test
	public void testOrderByContractor() throws Exception {
		Session s;
		Transaction tx;
		s = openSession();
		tx = s.beginTransaction();

		// create some test entities
		Employer employer = new Employer();
		Contractor contractor1 = new Contractor();
		contractor1.setName( "Emmanuel" );
		contractor1.setHourlyRate(100.0f);
		Contractor contractor2 = new Contractor();
		contractor2.setName( "Hardy" );
		contractor2.setHourlyRate(99.99f);
		s.persist( contractor1 );
		s.persist( contractor2 );

		// add contractors to employer
		List setOfContractors = new ArrayList();
		setOfContractors.add( contractor1 );
		setOfContractors.add( contractor2 );
		employer.setContractors( setOfContractors );

		// add employer to contractors
		Collection employerListContractor1 = new ArrayList();
		employerListContractor1.add( employer );
		contractor1.setEmployers( employerListContractor1 );

		Collection employerListContractor2 = new ArrayList();
		employerListContractor2.add( employer );
		contractor2.setEmployers( employerListContractor2 );

		s.flush();
		s.clear();

		// assertions
		employer = (Employer) s.get( Employer.class, employer.getId() );
		assertNotNull( employer );
		assertNotNull( employer.getContractors() );
		assertEquals( 2, employer.getContractors().size() );
		Contractor firstContractorFromDb = (Contractor) employer.getContractors().iterator().next();
		assertEquals( contractor2.getName(), firstContractorFromDb.getName() );
		tx.rollback();
		s.close();
	}
	
	@Test
	public void testRemoveInBetween() throws Exception {
		Session s;
		Transaction tx;
		s = openSession();
		tx = s.beginTransaction();
		Employer er = new Employer();
		Employee ee = new Employee();
		Employee ee2 = new Employee();
		s.persist( ee );
		s.persist( ee2 );
		Set erColl = new HashSet();
		Collection eeColl = new ArrayList();
		erColl.add( ee );
		erColl.add( ee2 );
		eeColl.add( er );
		er.setEmployees( erColl );
		ee.setEmployers( eeColl );
		//s.persist(ee);
		tx.commit();
		s.close();

		s = openSession();
		tx = s.beginTransaction();
		er = (Employer) s.load( Employer.class, er.getId() );
		assertNotNull( er );
		assertNotNull( er.getEmployees() );
		assertEquals( 2, er.getEmployees().size() );
		Iterator iterator = er.getEmployees().iterator();
		Employee eeFromDb = (Employee) iterator.next();
		if ( eeFromDb.getId().equals( ee.getId() ) ) {
			eeFromDb = (Employee) iterator.next();
		}
		assertEquals( ee2.getId(), eeFromDb.getId() );
		er.getEmployees().remove( eeFromDb );
		eeFromDb.getEmployers().remove( er );
		tx.commit();
		s.close();

		s = openSession();
		tx = s.beginTransaction();
		ee = (Employee) s.get( Employee.class, ee.getId() );
		assertNotNull( ee );
		assertFalse( "ManyToMany mappedBy lazyness", Hibernate.isInitialized( ee.getEmployers() ) );
		tx.commit();
		assertFalse( "ManyToMany mappedBy lazyness", Hibernate.isInitialized( ee.getEmployers() ) );
		s.close();

		s = openSession();
		tx = s.beginTransaction();
		ee = (Employee) s.get( Employee.class, ee.getId() );
		assertNotNull( ee );
		er = ee.getEmployers().iterator().next();
		assertTrue( "second join non lazy", Hibernate.isInitialized( er ) );
		assertEquals( 1, er.getEmployees().size() );
		s.delete( er );
		s.delete( ee );
		tx.commit();
		s.close();
	}

	@Test
	@TestForIssue( jiraKey = "HHH-4685" )
	public void testManyToManyEmbeddableBiDirectionalDotNotationInMappedBy() throws Exception {
		// Section 11.1.25
		// The ManyToMany annotation may be used within an embeddable class contained within an entity class to specify a
		// relationship to a collection of entities[101]. If the relationship is bidirectional and the entity containing
		// the embeddable class is the owner of the relationship, the non-owning side must use the mappedBy element of the
		// ManyToMany annotation to specify the relationship field or property of the embeddable class. The dot (".")
		// notation syntax must be used in the mappedBy element to indicate the relationship attribute within the embedded
		// attribute. The value of each identifier used with the dot notation is the name of the respective embedded field
		// or property.
		Session s;
		s = openSession();
		s.getTransaction().begin();
		Employee e = new Employee();
		e.setName( "Sharon" );
		List<PhoneNumber> phoneNumbers = new ArrayList<PhoneNumber>();
		Collection<Employee> employees = new ArrayList<Employee>();
		employees.add( e );
		ContactInfo contactInfo = new ContactInfo();
		PhoneNumber number = new PhoneNumber();
		number.setEmployees( employees );
		phoneNumbers.add( number );
		contactInfo.setPhoneNumbers( phoneNumbers );
		e.setContactInfo( contactInfo );
		s.persist( e );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.getTransaction().begin();
		e = (Employee)s.get( e.getClass(),e.getId() );
		// follow both directions of many to many association
		assertEquals("same employee", e.getName(), e.getContactInfo().getPhoneNumbers().get(0).getEmployees().iterator().next().getName());
		s.getTransaction().commit();

		s.close();
	}

	@Test
	@TestForIssue( jiraKey = "HHH-4685" )
	public void testOneToManyEmbeddableBiDirectionalDotNotationInMappedBy() throws Exception {
		// Section 11.1.26
		// The ManyToOne annotation may be used within an embeddable class to specify a relationship from the embeddable
		// class to an entity class. If the relationship is bidirectional, the non-owning OneToMany entity side must use the
		// mappedBy element of the OneToMany annotation to specify the relationship field or property of the embeddable field
		// or property on the owning side of the relationship. The dot (".") notation syntax must be used in the mappedBy
		// element to indicate the relationship attribute within the embedded attribute. The value of each identifier used
		// with the dot notation is the name of the respective embedded field or property.
		Session s;
		s = openSession();
		s.getTransaction().begin();
		Employee e = new Employee();
		JobInfo job = new JobInfo();
		job.setJobDescription( "Sushi Chef" );
		ProgramManager pm = new ProgramManager();
		Collection<Employee> employees = new ArrayList<Employee>();
		employees.add(e);
		pm.setManages( employees );
		job.setPm(pm);
		e.setJobInfo( job );
		s.persist( e );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.getTransaction().begin();
		e = (Employee) s.get( e.getClass(), e.getId() );
		assertEquals( "same job in both directions",
			e.getJobInfo().getJobDescription(),
			e.getJobInfo().getPm().getManages().iterator().next().getJobInfo().getJobDescription()  );
		s.getTransaction().commit();
		s.close();
	}

	@Override
    protected Class[] getAnnotatedClasses() {
		return new Class[]{
				Employer.class,
				Employee.class,
				Contractor.class,
				PhoneNumber.class,
				ProgramManager.class
		};
	}

}