/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.annotations.manytomany;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.hibernate.Hibernate;
import org.hibernate.Transaction;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit5.SessionFactoryBasedFunctionalTest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Many to many tests
 *
 * @author Emmanuel Bernard
 */
@SuppressWarnings("unchecked")
public class ManyToManyTest extends SessionFactoryBasedFunctionalTest {

	@Test
	public void testDefault() {
		Store fnac = new Store();
		fnac.setName( "Fnac" );

		KnownClient emmanuel = new KnownClient();
		emmanuel.setName( "Emmanuel" );
		emmanuel.setStores( new HashSet<>() );

		fnac.setCustomers( new HashSet<>() );
		fnac.getCustomers().add( emmanuel );
		emmanuel.getStores().add( fnac );
		fnac.setImplantedIn( new HashSet<>() );
		City paris = new City();
		fnac.getImplantedIn().add( paris );
		paris.setName( "Paris" );

		inTransaction(
				session -> {
					session.persist( fnac );
				}
		);

		inTransaction(
				session -> {
					Store store = session.get( Store.class, fnac.getId() );
					assertNotNull( store );
					assertNotNull( store.getCustomers() );
					assertEquals( 1, store.getCustomers().size() );
					KnownClient knownClient = store.getCustomers().iterator().next();
					assertEquals( emmanuel.getName(), knownClient.getName() );
					assertNotNull( store.getImplantedIn() );
					assertEquals( 1, store.getImplantedIn().size() );
					City city = store.getImplantedIn().iterator().next();
					assertEquals( paris.getName(), city.getName() );
				}
		);

		inTransaction(
				session -> {
					KnownClient knownClient = session.get( KnownClient.class, emmanuel.getId() );
					assertNotNull( knownClient );
					assertNotNull( knownClient.getStores() );
					assertEquals( 1, knownClient.getStores().size() );
					Store store = knownClient.getStores().iterator().next();
					assertEquals( fnac.getName(), store.getName() );
				}
		);
	}

	@Test
	@Disabled("Criteria not yet implemented")
	public void testCanUseCriteriaQuery() {
		Store fnac = new Store();
		fnac.setName( "Fnac" );
		Supplier emi = new Supplier();
		emi.setName( "Emmanuel" );
		emi.setSuppStores( new HashSet<>() );
		fnac.setSuppliers( new HashSet<>() );
		fnac.getSuppliers().add( emi );
		emi.getSuppStores().add( fnac );

		inTransaction(
				session -> {
					session.persist( fnac );
				}
		);

		inTransaction(
				session -> {
//					List result = session.createCriteria( Supplier.class ).createAlias( "suppStores", "s" ).add(
//							Restrictions.eq( "s.name", "Fnac" ) ).list();
//					assertEquals( 1, result.size() );
				}
		);
	}

	@Test
	public void testMappedBy() {
		Store fnac = new Store();
		fnac.setName( "Fnac" );
		Supplier emi = new Supplier();
		emi.setName( "Emmanuel" );
		emi.setSuppStores( new HashSet<>() );
		fnac.setSuppliers( new HashSet<>() );
		fnac.getSuppliers().add( emi );
		emi.getSuppStores().add( fnac );

		inTransaction(
				session -> {
					session.persist( fnac );
				}
		);

		inTransaction(
				session -> {
					Store store = session.get( Store.class, fnac.getId() );
					assertNotNull( store );
					assertNotNull( store.getSuppliers() );
					assertEquals( 1, store.getSuppliers().size() );
					Supplier supplier = store.getSuppliers().iterator().next();
					assertEquals( emi.getName(), supplier.getName() );
				}
		);

		inTransaction(
				session -> {
					Supplier supplier = session.get( Supplier.class, emi.getId() );
					assertNotNull( supplier );
					assertNotNull( supplier.getSuppStores() );
					assertEquals( 1, supplier.getSuppStores().size() );
					Store store = supplier.getSuppStores().iterator().next();
					assertEquals( fnac.getName(), store.getName() );
				}
		);
	}

	@Test
	@Disabled("Inheritance support not yet implemented")
	public void testBasic() {
		final Employer employer = new Employer();
		final Employee employee = new Employee();
		inTransaction(
				session -> {
					session.persist( employee );
					Set erColl = new HashSet();
					Collection eeColl = new ArrayList();
					erColl.add( employee );
					eeColl.add( employer );
					employer.setEmployees( erColl );
					employee.setEmployers( eeColl );
				}
		);

		inTransaction(
				session -> {
					Employer er = session.load( Employer.class, employer.getId() );
					assertNotNull( er );
					assertNotNull( er.getEmployees() );
					assertEquals( 1, er.getEmployees().size() );
					Employee eeFromDb = (Employee) er.getEmployees().iterator().next();
					assertEquals( employee.getId(), eeFromDb.getId() );
				}
		);

		inSession(
				session -> {
					Transaction tx = session.beginTransaction();
					try {
						Employee ee = session.get( Employee.class, employee.getId() );
						assertNotNull( ee );
						assertFalse( Hibernate.isInitialized( ee.getEmployers() ), "ManyToMany mappedBy lazyness" );
						tx.commit();
					}
					catch (Exception e) {
						if ( tx.isActive() ) {
							tx.rollback();
						}
						throw e;
					}
					assertFalse( Hibernate.isInitialized( employee.getEmployers() ), "ManyToMany mappedBy lazyness" );
				}
		);

		inTransaction(
				session -> {
					Employee ee = session.get( Employee.class, employee.getId() );
					assertNotNull( ee );
					Employer er = ee.getEmployers().iterator().next();
					assertTrue( Hibernate.isInitialized( er ), "second join non lazy" );
					session.delete( er );
					session.delete( ee );
				}
		);
	}

	@Test
	@Disabled("Inheritance support not yet implemented")
	public void testOrderByEmployee() {
		inTransaction(
				session -> {
					Employer employer = new Employer();
					Employee employee1 = new Employee();
					employee1.setName( "Emmanuel" );
					Employee employee2 = new Employee();
					employee2.setName( "Alice" );

					session.persist( employee1 );
					session.persist( employee2 );

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

					session.flush();
					session.clear();

					employer = session.get( Employer.class, employer.getId() );
					assertNotNull( employer );
					assertNotNull( employer.getEmployees() );
					assertEquals( 2, employer.getEmployees().size() );
					Employee eeFromDb = (Employee) employer.getEmployees().iterator().next();
					assertEquals( employee2.getName(), eeFromDb.getName() );
				}
		);
	}

	// HHH-4394
	@Test
	@Disabled("Inheritance support not yet implemented")
	public void testOrderByContractor() {
		inTransaction(
				session -> {
					// create some test entities
					Employer employer = new Employer();
					Contractor contractor1 = new Contractor();
					contractor1.setName( "Emmanuel" );
					contractor1.setHourlyRate( 100.0f );
					Contractor contractor2 = new Contractor();
					contractor2.setName( "Hardy" );
					contractor2.setHourlyRate( 99.99f );
					session.persist( contractor1 );
					session.persist( contractor2 );

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

					session.flush();
					session.clear();

					// assertions
					employer = session.get( Employer.class, employer.getId() );
					assertNotNull( employer );
					assertNotNull( employer.getContractors() );
					assertEquals( 2, employer.getContractors().size() );
					Contractor firstContractorFromDb = (Contractor) employer.getContractors().iterator().next();
					assertEquals( contractor2.getName(), firstContractorFromDb.getName() );
				}
		);
	}

	@Test
	@Disabled("Inheritance support not yet implemented")
	public void testRemoveInBetween() {
		Employer employer = new Employer();
		Employee employee = new Employee();
		Employee employee2 = new Employee();

		inTransaction(
				session -> {

					session.persist( employee );
					session.persist( employee2 );
					Set erColl = new HashSet();
					Collection eeColl = new ArrayList();
					erColl.add( employee );
					erColl.add( employee2 );
					eeColl.add( employer );
					employer.setEmployees( erColl );
					employee.setEmployers( eeColl );
				}
		);

		inTransaction(
				session -> {
					Employer er = session.load( Employer.class, employer.getId() );
					assertNotNull( er );
					assertNotNull( er.getEmployees() );
					assertEquals( 2, er.getEmployees().size() );
					Iterator iterator = er.getEmployees().iterator();
					Employee eeFromDb = (Employee) iterator.next();
					if ( eeFromDb.getId().equals( employee.getId() ) ) {
						eeFromDb = (Employee) iterator.next();
					}
					assertEquals( employee2.getId(), eeFromDb.getId() );
					er.getEmployees().remove( eeFromDb );
					eeFromDb.getEmployers().remove( er );
				}
		);

		inSession(
				session -> {
					Employee ee;
					Transaction tx = session.beginTransaction();
					try {
						ee = session.get( Employee.class, employee.getId() );
						assertNotNull( ee );
						assertFalse( Hibernate.isInitialized( ee.getEmployers() ), "ManyToMany mappedBy lazyness" );
						tx.commit();
					}
					catch (Exception e) {
						if ( tx.isActive() ) {
							tx.rollback();
						}
						throw e;
					}
					assertFalse( Hibernate.isInitialized( ee.getEmployers() ), "ManyToMany mappedBy lazyness" );
				}
		);

		inTransaction(
				session -> {
					Employee ee = session.get( Employee.class, employee.getId() );
					assertNotNull( ee );
					Employer er = ee.getEmployers().iterator().next();
					assertTrue( Hibernate.isInitialized( er ), "second join non lazy" );
					assertEquals( 1, er.getEmployees().size() );
					session.delete( er );
					session.delete( ee );
				}
		);
	}

	@Test
	@Disabled("Inheritance support not yet implemented")
	public void testJoinedSubclassManyToMany() {
		Zone a = new Zone();
		InspectorPrefixes inspectorPrefixesp = new InspectorPrefixes( "dgi" );
		inTransaction(
				session -> {
					session.save( a );
					session.save( inspectorPrefixesp );
					inspectorPrefixesp.getAreas().add( a );
				}
		);

		inTransaction(
				session -> {
					InspectorPrefixes ip = session.get( InspectorPrefixes.class, inspectorPrefixesp.getId() );
					assertNotNull( ip );
					assertEquals( 1, ip.getAreas().size() );
					assertEquals( a.getId(), ip.getAreas().get( 0 ).getId() );
					session.delete( ip );
					session.delete( ip.getAreas().get( 0 ) );
				}
		);
	}

	@Test
	@Disabled("Inheritance support not yet implemented")
	public void testJoinedSubclassManyToManyWithNonPkReference() {
		Zone a = new Zone();
		InspectorPrefixes inspectorPrefixes = new InspectorPrefixes( "dgi" );
		inspectorPrefixes.setName( "Inspector" );

		inTransaction(
				session -> {
					session.save( a );
					session.save( inspectorPrefixes );
					inspectorPrefixes.getDesertedAreas().add( a );
				}
		);

		inTransaction(
				session -> {
					InspectorPrefixes ip = session.get(
							InspectorPrefixes.class,
							inspectorPrefixes.getId()
					);
					assertNotNull( ip );
					assertEquals( 1, ip.getDesertedAreas().size() );
					assertEquals( a.getId(), ip.getDesertedAreas().get( 0 ).getId() );
					session.delete( ip );
					session.delete( ip.getDesertedAreas().get( 0 ) );
				}
		);
	}

	@Test
	@Disabled("Inheritance support not yet implemented")
	public void testReferencedColumnNameToSuperclass() {
		inTransaction(
				session -> {
					BuildingCompany comp = new BuildingCompany();
					comp.setFoundedIn( new Date() );
					comp.setName( "Builder century corp." );
					session.persist( comp );
					Building building = new Building();
					building.setCompany( comp );
					session.persist( building );
					session.flush();
					session.clear();
					building = session.get( Building.class, building.getId() );
					assertEquals( comp.getName(), building.getCompany().getName() );
				}
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-4685")
	@Disabled("Inheritance support not yet implemented")
	public void testManyToManyEmbeddableBiDirectionalDotNotationInMappedBy() {
		// Section 11.1.25
		// The ManyToMany annotation may be used within an embeddable class contained within an entity class to specify a
		// relationship to a collection of entities[101]. If the relationship is bidirectional and the entity containing
		// the embeddable class is the owner of the relationship, the non-owning side must use the mappedBy element of the
		// ManyToMany annotation to specify the relationship field or property of the embeddable class. The dot (".")
		// notation syntax must be used in the mappedBy element to indicate the relationship attribute within the embedded
		// attribute. The value of each identifier used with the dot notation is the name of the respective embedded field
		// or property.
		Employee employee = new Employee();
		inTransaction(
				session -> {
					employee.setName( "Sharon" );
					List<PhoneNumber> phoneNumbers = new ArrayList<>();
					Collection<Employee> employees = new ArrayList<>();
					employees.add( employee );
					ContactInfo contactInfo = new ContactInfo();
					PhoneNumber number = new PhoneNumber();
					number.setEmployees( employees );
					phoneNumbers.add( number );
					contactInfo.setPhoneNumbers( phoneNumbers );
					employee.setContactInfo( contactInfo );
					session.persist( employee );
				}
		);

		inTransaction(
				session -> {
					Employee e = session.get( employee.getClass(), employee.getId() );
					// follow both directions of many to many association
					assertEquals(
							"same employee",
							e.getName(),
							e.getContactInfo().getPhoneNumbers().get( 0 ).getEmployees().iterator().next().getName()
					);
				} );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-4685")
	@Disabled("Inheritance support not yet implemented")
	public void testOneToManyEmbeddableBiDirectionalDotNotationInMappedBy() {
		// Section 11.1.26
		// The ManyToOne annotation may be used within an embeddable class to specify a relationship from the embeddable
		// class to an entity class. If the relationship is bidirectional, the non-owning OneToMany entity side must use the
		// mappedBy element of the OneToMany annotation to specify the relationship field or property of the embeddable field
		// or property on the owning side of the relationship. The dot (".") notation syntax must be used in the mappedBy
		// element to indicate the relationship attribute within the embedded attribute. The value of each identifier used
		// with the dot notation is the name of the respective embedded field or property.
		Employee employee = new Employee();
		inTransaction(
				session -> {
					JobInfo job = new JobInfo();
					job.setJobDescription( "Sushi Chef" );
					ProgramManager pm = new ProgramManager();
					Collection<Employee> employees = new ArrayList<>();
					employees.add( employee );
					pm.setManages( employees );
					job.setPm( pm );
					employee.setJobInfo( job );
					session.persist( employee );
				}
		);

		inTransaction(
				session -> {
					Employee e = session.get( employee.getClass(), employee.getId() );
					assertEquals(
							"same job in both directions",
							e.getJobInfo().getJobDescription(),
							e.getJobInfo().getPm().getManages().iterator().next().getJobInfo().getJobDescription()
					);
				}
		);
	}

	@Override
	protected boolean isCleanupTestDataRequired() {
		return false;
	}

	@Override
	protected void cleanupTestData() {
		inTransaction(
				session -> {
					List<Store> stores = session.createQuery( "from Store" ).list();
					stores.forEach( store -> {
						store.getCustomers().forEach( customer -> session.delete( customer ) );
						store.getSuppliers().forEach( supplier -> session.delete( supplier ) );
						store.getImplantedIn().forEach( city -> session.delete( city ) );
						session.delete( store );
					} );
				}
		);
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
//				Employer.class,
//				Employee.class,
//				Contractor.class,
				Store.class,
				KnownClient.class,
				Supplier.class,
				City.class,
//				Zone.class,
//				Inspector.class,
//				InspectorPrefixes.class,
//				BuildingCompany.class,
//				Building.class,
//				PhoneNumber.class,
//				ProgramManager.class
		};
	}

}
