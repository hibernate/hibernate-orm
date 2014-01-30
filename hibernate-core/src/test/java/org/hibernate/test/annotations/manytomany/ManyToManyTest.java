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
package org.hibernate.test.annotations.manytomany;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import org.hibernate.Hibernate;
import org.hibernate.JDBCException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Restrictions;
import org.hibernate.testing.FailureExpectedWithNewMetamodel;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Many to many tests
 *
 * @author Emmanuel Bernard
 */
@SuppressWarnings("unchecked")
@FailureExpectedWithNewMetamodel
public class ManyToManyTest extends BaseCoreFunctionalTestCase {
	@Test
	public void testDefault() throws Exception {
		Session s;
		Transaction tx;
		s = openSession();
		tx = s.beginTransaction();
		Store fnac = new Store();
		fnac.setName( "Fnac" );
		KnownClient emmanuel = new KnownClient();
		emmanuel.setName( "Emmanuel" );
		emmanuel.setStores( new HashSet<Store>() );
		fnac.setCustomers( new HashSet<KnownClient>() );
		fnac.getCustomers().add( emmanuel );
		emmanuel.getStores().add( fnac );
		fnac.setImplantedIn( new HashSet<City>() );
		City paris = new City();
		fnac.getImplantedIn().add( paris );
		paris.setName( "Paris" );
		s.persist( fnac );
		tx.commit();
		s.close();

		s = openSession();
		tx = s.beginTransaction();
		Store store;
		KnownClient knownClient;
		City city;
		store = (Store) s.get( Store.class, fnac.getId() );
		assertNotNull( store );
		assertNotNull( store.getCustomers() );
		assertEquals( 1, store.getCustomers().size() );
		knownClient = store.getCustomers().iterator().next();
		assertEquals( emmanuel.getName(), knownClient.getName() );
		assertNotNull( store.getImplantedIn() );
		assertEquals( 1, store.getImplantedIn().size() );
		city = store.getImplantedIn().iterator().next();
		assertEquals( paris.getName(), city.getName() );
		tx.commit();
		s.close();

		s = openSession();
		tx = s.beginTransaction();
		knownClient = (KnownClient) s.get( KnownClient.class, emmanuel.getId() );
		assertNotNull( knownClient );
		assertNotNull( knownClient.getStores() );
		assertEquals( 1, knownClient.getStores().size() );
		store = knownClient.getStores().iterator().next();
		assertEquals( fnac.getName(), store.getName() );
		tx.commit();
		s.close();
	}

	@Test
	public void testCanUseCriteriaQuery() throws Exception {
		Session s;
		Transaction tx;
		s = openSession();
		tx = s.beginTransaction();
		Store fnac = new Store();
		fnac.setName( "Fnac" );
		Supplier emi = new Supplier();
		emi.setName( "Emmanuel" );
		emi.setSuppStores( new HashSet<Store>() );
		fnac.setSuppliers( new HashSet<Supplier>() );
		fnac.getSuppliers().add( emi );
		emi.getSuppStores().add( fnac );
		s.persist( fnac );
		tx.commit();
		s.close();

		s = openSession();
		tx = s.beginTransaction();
		List result = s.createCriteria( Supplier.class ).createAlias( "suppStores", "s" ).add(
				Restrictions.eq( "s.name", "Fnac" ) ).list();
		assertEquals( 1, result.size() );
		tx.commit();
		s.close();
	}

	@Test
	public void testDefaultCompositePk() throws Exception {
		Session s;
		Transaction tx;

		s = openSession();
		tx = s.beginTransaction();
		CatPk catPk = new CatPk();
		catPk.setName( "Minou" );
		catPk.setThoroughbred( "Persan" );
		Cat cat = new Cat();
		cat.setId( catPk );
		cat.setAge( 32 );
		Woman woman = new Woman();
		WomanPk womanPk = new WomanPk();
		womanPk.setFirstName( "Emma" );
		womanPk.setLastName( "Peel" );
		woman.setId( womanPk );
		woman.setCats( new HashSet<Cat>() );
		woman.getCats().add( cat );
		cat.setHumanContacts( new HashSet<Woman>() );
		cat.getHumanContacts().add( woman );
		s.persist( woman );
		s.persist( cat );
		tx.commit();
		s.close();

		s = openSession();
		tx = s.beginTransaction();
		Cat sameCat = (Cat) s.get( Cat.class, cat.getId() );
		assertNotNull( sameCat );
		assertNotNull( sameCat.getHumanContacts() );
		assertEquals( 1, sameCat.getHumanContacts().size() );
		Woman sameWoman = sameCat.getHumanContacts().iterator().next();
		assertEquals( sameWoman.getId().getLastName(), woman.getId().getLastName() );
		tx.commit();
		s.close();

		s = openSession();
		tx = s.beginTransaction();
		sameWoman = (Woman) s.get( Woman.class, woman.getId() );
		assertNotNull( sameWoman );
		assertNotNull( sameWoman.getCats() );
		assertEquals( 1, sameWoman.getCats().size() );
		sameCat = sameWoman.getCats().iterator().next();
		assertEquals( cat.getAge(), sameCat.getAge() );
		tx.commit();
		s.close();
	}

	@Test
	public void testMappedBy() throws Exception {
		Session s;
		Transaction tx;
		s = openSession();
		tx = s.beginTransaction();
		Store fnac = new Store();
		fnac.setName( "Fnac" );
		Supplier emi = new Supplier();
		emi.setName( "Emmanuel" );
		emi.setSuppStores( new HashSet<Store>() );
		fnac.setSuppliers( new HashSet<Supplier>() );
		fnac.getSuppliers().add( emi );
		emi.getSuppStores().add( fnac );
		s.persist( fnac );
		tx.commit();
		s.close();

		s = openSession();
		tx = s.beginTransaction();
		Store store;
		Supplier supplier;
		store = (Store) s.get( Store.class, fnac.getId() );
		assertNotNull( store );
		assertNotNull( store.getSuppliers() );
		assertEquals( 1, store.getSuppliers().size() );
		supplier = store.getSuppliers().iterator().next();
		assertEquals( emi.getName(), supplier.getName() );
		tx.commit();
		s.close();

		s = openSession();
		tx = s.beginTransaction();
		supplier = (Supplier) s.get( Supplier.class, emi.getId() );
		assertNotNull( supplier );
		assertNotNull( supplier.getSuppStores() );
		assertEquals( 1, supplier.getSuppStores().size() );
		store = supplier.getSuppStores().iterator().next();
		assertEquals( fnac.getName(), store.getName() );
		tx.commit();
		s.close();
	}

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
	public void testSelf() throws Exception {
		Session s;
		Transaction tx;
		s = openSession();
		tx = s.beginTransaction();
		Friend f = new Friend();
		Friend sndF = new Friend();
		f.setName( "Starsky" );
		sndF.setName( "Hutch" );
		Set frnds = new HashSet();
		frnds.add( sndF );
		f.setFriends( frnds );
		//Starsky is a friend of Hutch but hutch is not
		s.persist( f );
		tx.commit();
		s.close();

		s = openSession();
		tx = s.beginTransaction();
		f = (Friend) s.load( Friend.class, f.getId() );
		assertNotNull( f );
		assertNotNull( f.getFriends() );
		assertEquals( 1, f.getFriends().size() );
		Friend fromDb2ndFrnd = f.getFriends().iterator().next();
		assertEquals( fromDb2ndFrnd.getId(), sndF.getId() );
		assertEquals( 0, fromDb2ndFrnd.getFriends().size() );
		tx.commit();
		s.close();
	}

	@Test
	public void testCompositePk() throws Exception {
		Session s;
		Transaction tx;

		ManPk m1pk = new ManPk();
		m1pk.setElder( true );
		m1pk.setFirstName( "Lucky" );
		m1pk.setLastName( "Luke" );
		ManPk m2pk = new ManPk();
		m2pk.setElder( false );
		m2pk.setFirstName( "Joe" );
		m2pk.setLastName( "Dalton" );

		Man m1 = new Man();
		m1.setId( m1pk );
		m1.setCarName( "Jolly Jumper" );
		Man m2 = new Man();
		m2.setId( m2pk );

		WomanPk w1pk = new WomanPk();
		w1pk.setFirstName( "Ma" );
		w1pk.setLastName( "Dalton" );
		WomanPk w2pk = new WomanPk();
		w2pk.setFirstName( "Carla" );
		w2pk.setLastName( "Bruni" );

		Woman w1 = new Woman();
		w1.setId( w1pk );
		Woman w2 = new Woman();
		w2.setId( w2pk );

		Set<Woman> womens = new HashSet<Woman>();
		womens.add( w1 );
		m1.setWomens( womens );
		Set<Woman> womens2 = new HashSet<Woman>();
		womens2.add( w1 );
		womens2.add( w2 );
		m2.setWomens( womens2 );

		Set<Man> mens = new HashSet<Man>();
		mens.add( m1 );
		mens.add( m2 );
		w1.setMens( mens );
		Set<Man> mens2 = new HashSet<Man>();
		mens2.add( m2 );
		w2.setMens( mens2 );

		s = openSession();
		tx = s.beginTransaction();
		s.persist( m1 );
		s.persist( m2 );
		tx.commit();
		s.close();

		s = openSession();
		tx = s.beginTransaction();
		m1 = (Man) s.load( Man.class, m1pk );
		assertFalse( m1.getWomens().isEmpty() );
		assertEquals( 1, m1.getWomens().size() );
		w1 = (Woman) s.load( Woman.class, w1pk );
		assertFalse( w1.getMens().isEmpty() );
		assertEquals( 2, w1.getMens().size() );

		tx.commit();
		s.close();
	}

	@Test
	public void testAssociationTableUniqueConstraints() throws Exception {
		Session s = openSession();
		Permission readAccess = new Permission();
		readAccess.setPermission( "read" );
		readAccess.setExpirationDate( new Date() );
		Collection<Permission> coll = new ArrayList<Permission>( 2 );
		coll.add( readAccess );
		coll.add( readAccess );
		Group group = new Group();
		group.setId( new Integer( 1 ) );
		group.setPermissions( coll );
		s.getTransaction().begin();
		try {
			s.persist( group );
			s.getTransaction().commit();
			fail( "Unique constraints not applied on association table" );
		}
		catch (JDBCException e) {
			//success
			s.getTransaction().rollback();
		}
		finally {
			s.close();
		}
	}

	@Test
	public void testAssociationTableAndOrderBy() throws Exception {
		Session s = openSession();
		s.enableFilter( "Groupfilter" );
		Permission readAccess = new Permission();
		readAccess.setPermission( "read" );
		readAccess.setExpirationDate( new Date() );
		Permission writeAccess = new Permission();
		writeAccess.setPermission( "write" );
		writeAccess.setExpirationDate( new Date( new Date().getTime() - 10*60*1000 ) );
		Collection<Permission> coll = new ArrayList<Permission>( 2 );
		coll.add( readAccess );
		coll.add( writeAccess );
		Group group = new Group();
		group.setId( new Integer( 1 ) );
		group.setPermissions( coll );
		s.getTransaction().begin();
		s.persist( group );
		s.flush();
		s.clear();
		group = (Group) s.get( Group.class, group.getId() );
		s.createQuery( "select g from Group g join fetch g.permissions").list();
		assertEquals( "write", group.getPermissions().iterator().next().getPermission() );
		s.getTransaction().rollback();
		s.close();
	}

	@Test
	public void testAssociationTableAndOrderByWithSet() throws Exception {
		Session s = openSession();
		s.enableFilter( "Groupfilter" );

		Permission readAccess = new Permission();
		readAccess.setPermission( "read" );
		readAccess.setExpirationDate( new Date() );
		
		Permission writeAccess = new Permission();
		writeAccess.setPermission( "write" );
		writeAccess.setExpirationDate( new Date( new Date().getTime() - 10*60*1000 ) );
		
		Permission executeAccess = new Permission();
		executeAccess.setPermission( "execute" );
		executeAccess.setExpirationDate( new Date( new Date().getTime() - 5*60*1000 ) );
		
		Set<Permission> coll = new HashSet<Permission>( 3 );
		coll.add( readAccess );
		coll.add( writeAccess );
		coll.add( executeAccess );

		GroupWithSet group = new GroupWithSet();
		group.setId( new Integer( 1 ) );
		group.setPermissions( coll );
		s.getTransaction().begin();
		s.persist( group );
		s.flush();
		s.clear();

		group = (GroupWithSet) s.get( GroupWithSet.class, group.getId() );
		s.createQuery( "select g from Group g join fetch g.permissions").list();
		Iterator<Permission> permIter = group.getPermissions().iterator();
		assertEquals( "write", permIter.next().getPermission() );
		assertEquals( "execute", permIter.next().getPermission() );
		assertEquals( "read", permIter.next().getPermission() );
		s.getTransaction().rollback();
		s.close();
	}
	
	@Test
	public void testJoinedSubclassManyToMany() throws Exception {
		Session s = openSession();
		Zone a = new Zone();
		InspectorPrefixes ip = new InspectorPrefixes( "dgi" );
		Transaction tx = s.beginTransaction();
		s.save( a );
		s.save( ip );
		ip.getAreas().add( a );
		tx.commit();
		s.close();
		s = openSession();
		tx = s.beginTransaction();
		ip = (InspectorPrefixes) s.get( InspectorPrefixes.class, ip.getId() );
		assertNotNull( ip );
		assertEquals( 1, ip.getAreas().size() );
		assertEquals( a.getId(), ip.getAreas().get( 0 ).getId() );
		s.delete( ip );
		s.delete( ip.getAreas().get( 0 ) );
		tx.commit();
		s.close();
	}

	@Test
	public void testJoinedSubclassManyToManyWithNonPkReference() throws Exception {
		Session s = openSession();
		Zone a = new Zone();
		InspectorPrefixes ip = new InspectorPrefixes( "dgi" );
		ip.setName( "Inspector" );
		Transaction tx = s.beginTransaction();
		s.save( a );
		s.save( ip );
		ip.getDesertedAreas().add( a );
		tx.commit();
		s.close();
		s = openSession();
		tx = s.beginTransaction();
		ip = (InspectorPrefixes) s.get( InspectorPrefixes.class, ip.getId() );
		assertNotNull( ip );
		assertEquals( 1, ip.getDesertedAreas().size() );
		assertEquals( a.getId(), ip.getDesertedAreas().get( 0 ).getId() );
		s.delete( ip );
		s.delete( ip.getDesertedAreas().get( 0 ) );
		tx.commit();
		s.close();
	}

	@Test
	public void testReferencedColumnNameToSuperclass() throws Exception {
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		BuildingCompany comp = new BuildingCompany();
		comp.setFoundedIn( new Date() );
		comp.setName( "Builder century corp.");
		s.persist( comp );
		Building building = new Building();
		building.setCompany( comp );
		s.persist( building );
		s.flush();
		s.clear();
		building = (Building) s.get( Building.class, building.getId() );
		assertEquals( comp.getName(), building.getCompany().getName() );
		tx.rollback();
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
				Friend.class,
				Employer.class,
				Employee.class,
				ContactInfo.class,
				JobInfo.class,
				Contractor.class,
				Man.class,
				Woman.class,
				Store.class,
				KnownClient.class,
				Supplier.class,
				City.class,
				Cat.class,
				CatPk.class,
				Group.class,
				GroupWithSet.class,
				Permission.class,
				Zone.class,
				Inspector.class,
				InspectorPrefixes.class,
				BuildingCompany.class,
				Building.class,
				PhoneNumber.class,
				ProgramManager.class
		};
	}

}
