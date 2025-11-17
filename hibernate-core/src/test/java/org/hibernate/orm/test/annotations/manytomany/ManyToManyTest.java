/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.manytomany;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Root;
import org.hibernate.Hibernate;
import org.hibernate.boot.model.naming.ImplicitNamingStrategyLegacyJpaImpl;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.metamodel.CollectionClassification;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SettingProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * Many to many tests
 *
 * @author Emmanuel Bernard
 */
@SuppressWarnings("unchecked")
@DomainModel(
		annotatedClasses = {
				Friend.class,
				Employer.class,
				Employee.class,
				Contractor.class,
				Man.class,
				Woman.class,
				Store.class,
				KnownClient.class,
				Supplier.class,
				City.class,
				Cat.class,
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
		}
)
@SessionFactory
@ServiceRegistry(
		settingProviders = {
				@SettingProvider(
						settingName = AvailableSettings.DEFAULT_LIST_SEMANTICS,
						provider = ManyToManyTest.ListSemanticProvider.class
				),
				@SettingProvider(
						settingName = AvailableSettings.IMPLICIT_NAMING_STRATEGY,
						provider = ManyToManyTest.ImplicitNamingStrategyProvider.class
				)
		}
)
public class ManyToManyTest {

	public static class ListSemanticProvider implements SettingProvider.Provider<CollectionClassification> {
		@Override
		public CollectionClassification getSetting() {
			return CollectionClassification.BAG;
		}
	}

	public static class ImplicitNamingStrategyProvider
			implements SettingProvider.Provider<ImplicitNamingStrategyLegacyJpaImpl> {
		@Override
		public ImplicitNamingStrategyLegacyJpaImpl getSetting() {
			return ImplicitNamingStrategyLegacyJpaImpl.INSTANCE;
		}
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncateMappedObjects();
	}

	@Test
	public void testDefault(SessionFactoryScope scope) {
		Store fnac = new Store();
		KnownClient emmanuel = new KnownClient();
		City paris = new City();

		scope.inTransaction(
				session -> {
					fnac.setName( "Fnac" );
					emmanuel.setName( "Emmanuel" );
					emmanuel.setStores( new HashSet<>() );
					fnac.setCustomers( new HashSet<>() );
					fnac.getCustomers().add( emmanuel );
					emmanuel.getStores().add( fnac );
					fnac.setImplantedIn( new HashSet<>() );
					fnac.getImplantedIn().add( paris );
					paris.setName( "Paris" );
					session.persist( fnac );
				}
		);

		scope.inTransaction(
				session -> {
					Store store = session.find( Store.class, fnac.getId() );
					assertThat( store ).isNotNull();
					assertThat( store.getCustomers() ).isNotNull();
					assertThat( store.getCustomers().size() ).isEqualTo( 1 );
					KnownClient knownClient = store.getCustomers().iterator().next();
					assertThat( knownClient.getName() ).isEqualTo( emmanuel.getName() );
					assertThat( store.getImplantedIn() ).isNotNull();
					assertThat( store.getImplantedIn().size() ).isEqualTo( 1 );
					City city = store.getImplantedIn().iterator().next();
					assertThat( city.getName() ).isEqualTo( paris.getName() );
				}
		);

		scope.inTransaction(
				session -> {
					KnownClient knownClient = session.find( KnownClient.class, emmanuel.getId() );
					assertThat( knownClient ).isNotNull();
					assertThat( knownClient.getStores() ).isNotNull();
					assertThat( knownClient.getStores().size() ).isEqualTo( 1 );
					Store store = knownClient.getStores().iterator().next();
					assertThat( store.getName() ).isEqualTo( fnac.getName() );
				}
		);
	}

	@Test
	public void testCanUseCriteriaQuery(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			Store fnac = new Store();
			fnac.setName( "Fnac" );
			Supplier emi = new Supplier();
			emi.setName( "Emmanuel" );
			emi.setSuppStores( new HashSet<>() );
			fnac.setSuppliers( new HashSet<>() );
			fnac.getSuppliers().add( emi );
			emi.getSuppStores().add( fnac );
			s.persist( fnac );
		} );

		scope.inTransaction( s -> {
			CriteriaBuilder criteriaBuilder = s.getCriteriaBuilder();
			CriteriaQuery<Supplier> criteria = criteriaBuilder.createQuery( Supplier.class );
			Root<Supplier> root = criteria.from( Supplier.class );
			Join<Object, Object> suppStores = root.join( "suppStores", JoinType.INNER );
			criteria.where( criteriaBuilder.equal( suppStores.get( "name" ), "Fnac" ) );
			List<Supplier> result = s.createQuery( criteria ).list();

//			List result = s.createCriteria( Supplier.class ).createAlias( "suppStores", "s" ).add(
//			Restrictions.eq( "s.name", "Fnac" ) ).list();
			assertThat( result.size() ).isEqualTo( 1 );
		} );
	}

	@Test
	public void testDefaultCompositePk(SessionFactoryScope scope) {
		Cat cat = new Cat();
		Woman woman = new Woman();
		scope.inTransaction(
				session -> {
					CatPk catPk = new CatPk();
					catPk.setName( "Minou" );
					catPk.setThoroughbred( "Persan" );
					cat.setId( catPk );
					cat.setAge( 32 );
					WomanPk womanPk = new WomanPk();
					womanPk.setFirstName( "Emma" );
					womanPk.setLastName( "Peel" );
					woman.setId( womanPk );
					woman.setCats( new HashSet<>() );
					woman.getCats().add( cat );
					cat.setHumanContacts( new HashSet<>() );
					cat.getHumanContacts().add( woman );
					session.persist( woman );
					session.persist( cat );
				}
		);

		scope.inTransaction(
				session -> {
					Cat sameCat = session.find( Cat.class, cat.getId() );
					assertThat( sameCat ).isNotNull();
					assertThat( sameCat.getHumanContacts() ).isNotNull();
					assertThat( sameCat.getHumanContacts().size() ).isEqualTo( 1 );
					Woman sameWoman = sameCat.getHumanContacts().iterator().next();
					assertThat( sameWoman.getId().getLastName() ).isEqualTo( woman.getId().getLastName() );
				}
		);

		scope.inTransaction(
				session -> {
					Woman sameWoman = session.find( Woman.class, woman.getId() );
					assertThat( sameWoman ).isNotNull();
					assertThat( sameWoman.getCats() ).isNotNull();
					assertThat( sameWoman.getCats().size() ).isEqualTo( 1 );
					Cat sameCat = sameWoman.getCats().iterator().next();
					assertThat( sameCat.getAge() ).isEqualTo( cat.getAge() );
				}
		);
	}

	@Test
	public void testMappedBy(SessionFactoryScope scope) {
		Store fnac = new Store();
		Supplier emi = new Supplier();
		scope.inTransaction(
				session -> {
					fnac.setName( "Fnac" );
					emi.setName( "Emmanuel" );
					emi.setSuppStores( new HashSet<>() );
					fnac.setSuppliers( new HashSet<>() );
					fnac.getSuppliers().add( emi );
					emi.getSuppStores().add( fnac );
					session.persist( fnac );
				}
		);

		scope.inTransaction(
				session -> {
					Store store = session.find( Store.class, fnac.getId() );
					assertThat( store ).isNotNull();
					assertThat( store.getSuppliers() ).isNotNull();
					assertThat( store.getSuppliers().size() ).isEqualTo( 1 );
					Supplier supplier = store.getSuppliers().iterator().next();
					assertThat( supplier.getName() ).isEqualTo( emi.getName() );
				}
		);

		scope.inTransaction(
				session -> {
					Supplier supplier = session.find( Supplier.class, emi.getId() );
					assertThat( supplier ).isNotNull();
					assertThat( supplier.getSuppStores() ).isNotNull();
					assertThat( supplier.getSuppStores().size() ).isEqualTo( 1 );
					Store store = supplier.getSuppStores().iterator().next();
					assertThat( store.getName() ).isEqualTo( fnac.getName() );
				}
		);
	}

	@Test
	public void testBasic(SessionFactoryScope scope) {
		Employer e = new Employer();
		Employee e1 = new Employee();
		scope.inTransaction(
				session -> {
					session.persist( e1 );
					Set<Employee> erColl = new HashSet<>();
					Collection<Employer> eeColl = new ArrayList<>();
					erColl.add( e1 );
					eeColl.add( e );
					e.setEmployees( erColl );
					e1.setEmployers( eeColl );
				}
		);

		scope.inTransaction(
				session -> {
					Employer er = session.getReference( Employer.class, e.getId() );
					assertThat( er ).isNotNull();
					assertThat( er.getEmployees() ).isNotNull();
					assertThat( er.getEmployees().size() ).isEqualTo( 1 );
					Employee eeFromDb = (Employee) er.getEmployees().iterator().next();
					assertThat( eeFromDb.getId() ).isEqualTo( e1.getId() );
				}
		);

		scope.inSession(
				session -> {
					try {
						session.getTransaction().begin();
						Employee ee = session.find( Employee.class, e1.getId() );
						assertThat( ee ).isNotNull();
						assertThat( Hibernate.isInitialized( ee.getEmployers() ) )
								.describedAs( "ManyToMany mappedBy lazyness" )
								.isFalse();
						session.getTransaction().commit();
						assertThat( Hibernate.isInitialized( ee.getEmployers() ) )
								.describedAs( "ManyToMany mappedBy lazyness" )
								.isFalse();
					}
					finally {
						if ( session.getTransaction().isActive() ) {
							session.getTransaction().rollback();
						}
					}
				}
		);

		scope.inTransaction(
				session -> {
					Employee ee = session.find( Employee.class, e1.getId() );
					assertThat( ee ).isNotNull();
					Employer er = ee.getEmployers().iterator().next();
					assertThat( Hibernate.isInitialized( er ) )
							.describedAs( "second join non lazy" )
							.isTrue();
					session.remove( er );
					session.remove( ee );
				}
		);
	}

	@Test
	public void testOrderByEmployee(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Employer employer = new Employer();
					Employee employee1 = new Employee();
					employee1.setName( "Emmanuel" );
					Employee employee2 = new Employee();
					employee2.setName( "Alice" );
					session.persist( employee1 );
					session.persist( employee2 );
					Set<Employee> erColl = new HashSet<>();
					Collection<Employer> eeColl = new ArrayList<>();
					Collection<Employer> eeColl2 = new ArrayList<>();
					erColl.add( employee1 );
					erColl.add( employee2 );
					eeColl.add( employer );
					eeColl2.add( employer );
					employer.setEmployees( erColl );
					employee1.setEmployers( eeColl );
					employee2.setEmployers( eeColl2 );

					session.flush();
					session.clear();

					employer = session.find( Employer.class, employer.getId() );
					assertThat( employer ).isNotNull();
					assertThat( employer.getEmployees() ).isNotNull();
					assertThat( employer.getEmployees().size() ).isEqualTo( 2 );
					Employee eeFromDb = (Employee) employer.getEmployees().iterator().next();
					assertThat( eeFromDb.getName() ).isEqualTo( employee2.getName() );
				}
		);
	}

	// HHH-4394
	@Test
	public void testOrderByContractor(SessionFactoryScope scope) {
		scope.inTransaction(
				sesssion -> {
					// tag::tagname[]
					// create some test entities
					Employer employer = new Employer();
					Contractor contractor1 = new Contractor();
					contractor1.setName( "Emmanuel" );
					contractor1.setHourlyRate( 100.0f );
					Contractor contractor2 = new Contractor();
					contractor2.setName( "Hardy" );
					contractor2.setHourlyRate( 99.99f );
					sesssion.persist( contractor1 );
					sesssion.persist( contractor2 );

					// add contractors to employer
					List<Contractor> setOfContractors = new ArrayList<>();
					setOfContractors.add( contractor1 );
					setOfContractors.add( contractor2 );
					employer.setContractors( setOfContractors );

					// add employer to contractors
					Collection<Employer> employerListContractor1 = new ArrayList<>();
					employerListContractor1.add( employer );
					contractor1.setEmployers( employerListContractor1 );

					Collection<Employer> employerListContractor2 = new ArrayList<>();
					employerListContractor2.add( employer );
					contractor2.setEmployers( employerListContractor2 );

					sesssion.flush();
					sesssion.clear();

					// assertions
					employer = sesssion.find( Employer.class, employer.getId() );
					assertThat( employer ).isNotNull();
					assertThat( employer.getContractors() ).isNotNull();
					assertThat( employer.getContractors().size() ).isEqualTo( 2 );
					Contractor firstContractorFromDb = (Contractor) employer.getContractors().iterator().next();
					assertThat( firstContractorFromDb.getName() ).isEqualTo( contractor2.getName() );
					// end::tagname[]
				}
		);
	}

	@Test
	public void testRemoveInBetween(SessionFactoryScope scope) {
		Employer e = new Employer();
		Employee ee1 = new Employee();
		Employee ee2 = new Employee();
		scope.inTransaction(
				session -> {
					session.persist( ee1 );
					session.persist( ee2 );
					Set<Employee> erColl = new HashSet<>();
					Collection<Employer> eeColl = new ArrayList<>();
					erColl.add( ee1 );
					erColl.add( ee2 );
					eeColl.add( e );
					e.setEmployees( erColl );
					ee1.setEmployers( eeColl );
					//s.persist(ee);
				}
		);

		scope.inTransaction(
				session -> {
					Employer er = session.getReference( Employer.class, e.getId() );
					assertThat( er ).isNotNull();
					assertThat( er.getEmployees() ).isNotNull();
					assertThat( er.getEmployees().size() ).isEqualTo( 2 );
					Iterator<Employee> iterator = er.getEmployees().iterator();
					Employee eeFromDb = iterator.next();
					if ( eeFromDb.getId().equals( ee1.getId() ) ) {
						eeFromDb = iterator.next();
					}
					assertThat( eeFromDb.getId() ).isEqualTo( ee2.getId() );
					er.getEmployees().remove( eeFromDb );
					eeFromDb.getEmployers().remove( er );
				}
		);

		scope.inSession(
				session -> {
					try {
						session.getTransaction().begin();
						Employee ee = session.find( Employee.class, ee1.getId() );
						assertThat( ee ).isNotNull();
						assertThat( Hibernate.isInitialized( ee.getEmployers() ) )
								.describedAs( "ManyToMany mappedBy lazyness" )
								.isFalse();
						session.getTransaction().commit();
						assertThat( Hibernate.isInitialized( ee.getEmployers() ) )
								.describedAs( "ManyToMany mappedBy lazyness" )
								.isFalse();
					}
					finally {
						if ( session.getTransaction().isActive() ) {
							session.getTransaction().rollback();
						}
					}
				}
		);

		scope.inTransaction(
				session -> {
					Employee ee = session.find( Employee.class, ee1.getId() );
					assertThat( ee ).isNotNull();
					Employer er = ee.getEmployers().iterator().next();
					assertThat( Hibernate.isInitialized( er ) )
							.describedAs( "second join non lazy" )
							.isTrue();
					assertThat( er.getEmployees().size() ).isEqualTo( 1 );
					session.remove( er );
					session.remove( ee );
				}
		);
	}

	@Test
	public void testSelf(SessionFactoryScope scope) {
		Friend friend = new Friend();
		Friend sndF = new Friend();
		scope.inTransaction(
				session -> {
					friend.setName( "Starsky" );
					sndF.setName( "Hutch" );
					Set<Friend> frnds = new HashSet<>();
					frnds.add( sndF );
					friend.setFriends( frnds );
					//Starsky is a friend of Hutch but hutch is not
					session.persist( friend );
				}
		);

		scope.inTransaction(
				session -> {
					Friend f = session.getReference( Friend.class, friend.getId() );
					assertThat( f ).isNotNull();
					assertThat( f.getFriends() ).isNotNull();
					assertThat( f.getFriends().size() ).isEqualTo( 1 );
					Friend fromDb2ndFrnd = f.getFriends().iterator().next();
					assertThat( fromDb2ndFrnd.getId() ).isEqualTo( sndF.getId() );
					assertThat( fromDb2ndFrnd.getFriends().size() ).isEqualTo( 0 );
				}
		);
	}

	@Test
	public void testCompositePk(SessionFactoryScope scope) {
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

		Set<Woman> womens = new HashSet<>();
		womens.add( w1 );
		m1.setWomens( womens );
		Set<Woman> womens2 = new HashSet<>();
		womens2.add( w1 );
		womens2.add( w2 );
		m2.setWomens( womens2 );

		Set<Man> mens = new HashSet<>();
		mens.add( m1 );
		mens.add( m2 );
		w1.setMens( mens );
		Set<Man> mens2 = new HashSet<>();
		mens2.add( m2 );
		w2.setMens( mens2 );

		scope.inTransaction(
				session -> {
					session.persist( m1 );
					session.persist( m2 );
				}
		);

		scope.inTransaction(
				session -> {
					Man m = session.getReference( Man.class, m1pk );
					assertThat( m.getWomens() ).isNotEmpty();
					assertThat( m.getWomens().size() ).isEqualTo( 1 );
					Woman w = session.getReference( Woman.class, w1pk );
					assertThat( w.getMens() ).isNotEmpty();
					assertThat( w.getMens().size() ).isEqualTo( 2 );
				}
		);
	}

	@Test
	public void testAssociationTableUniqueConstraints(SessionFactoryScope scope) {
		scope.inSession(
				session -> {
					Permission readAccess = new Permission();
					readAccess.setPermission( "read" );
					readAccess.setExpirationDate( new Date() );
					Collection<Permission> coll = new ArrayList<>( 2 );
					coll.add( readAccess );
					coll.add( readAccess );
					Group group = new Group();
					group.setId( 1 );
					group.setPermissions( coll );
					session.getTransaction().begin();
					try {
						session.persist( group );
						session.getTransaction().commit();
						fail( "Unique constraints not applied on association table" );
					}
					catch (Exception e) {
						//success
						session.getTransaction().rollback();
					}
				}
		);
	}

	@Test
	public void testAssociationTableAndOrderBy(SessionFactoryScope scope) {
		scope.inTransaction(
				s -> {
					s.enableFilter( "Groupfilter" );

					Permission readAccess = new Permission();
					readAccess.setPermission( "read" );
					readAccess.setExpirationDate( new Date() );

					Permission writeAccess = new Permission();
					writeAccess.setPermission( "write" );
					writeAccess.setExpirationDate( new Date( new Date().getTime() - 10 * 60 * 1000 ) );

					Collection<Permission> coll = new ArrayList<>( 2 );
					coll.add( readAccess );
					coll.add( writeAccess );

					Group group = new Group();
					group.setId( 1 );
					group.setPermissions( coll );

					s.persist( group );
					s.flush();
					s.clear();
					group = s.get( Group.class, group.getId() );
					s.createQuery( "select g from Group g join fetch g.permissions", Group.class ).list();
					assertThat( group.getPermissions().iterator().next().getPermission() ).isEqualTo( "write" );
				}
		);
	}

	@Test
	public void testAssociationTableAndOrderByWithSet(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.enableFilter( "Groupfilter" );

					Permission readAccess = new Permission();
					readAccess.setPermission( "read" );
					readAccess.setExpirationDate( new Date() );

					Permission writeAccess = new Permission();
					writeAccess.setPermission( "write" );
					writeAccess.setExpirationDate( new Date( new Date().getTime() - 10 * 60 * 1000 ) );

					Permission executeAccess = new Permission();
					executeAccess.setPermission( "execute" );
					executeAccess.setExpirationDate( new Date( new Date().getTime() - 5 * 60 * 1000 ) );

					Set<Permission> coll = new HashSet<>( 3 );
					coll.add( readAccess );
					coll.add( writeAccess );
					coll.add( executeAccess );

					GroupWithSet group = new GroupWithSet();
					group.setId( 1 );
					group.setPermissions( coll );

					session.persist( group );
					session.flush();
					session.clear();

					group = session.find( GroupWithSet.class, group.getId() );
					session.createQuery( "select g from Group g join fetch g.permissions", Group.class ).list();
					Iterator<Permission> permIter = group.getPermissions().iterator();
					assertThat( permIter.next().getPermission() ).isEqualTo( "write" );
					assertThat( permIter.next().getPermission() ).isEqualTo( "execute" );
					assertThat( permIter.next().getPermission() ).isEqualTo( "read" );
				}
		);
	}

	@Test
	public void testJoinedSubclassManyToMany(SessionFactoryScope scope) {
		Zone a = new Zone();
		InspectorPrefixes i = new InspectorPrefixes( "dgi" );
		scope.inTransaction(
				session -> {
					session.persist( a );
					session.persist( i );
					i.getAreas().add( a );
				}
		);

		scope.inTransaction(
				session -> {
					InspectorPrefixes ip = session.find( InspectorPrefixes.class, i.getId() );
					assertThat( ip ).isNotNull();
					assertThat( ip.getAreas().size() ).isEqualTo( 1 );
					assertThat( ip.getAreas().get( 0 ).getId() ).isEqualTo( a.getId() );
					session.remove( ip );
					session.remove( ip.getAreas().get( 0 ) );
				}
		);
	}

	@Test
	public void testJoinedSubclassManyToManyWithNonPkReference(SessionFactoryScope scope) {
		InspectorPrefixes i = new InspectorPrefixes( "dgi" );
		Zone a = new Zone();
		scope.inTransaction(
				session -> {
					i.setName( "Inspector" );
					session.persist( a );
					session.persist( i );
					i.getDesertedAreas().add( a );
				}
		);

		scope.inTransaction(
				session -> {
					InspectorPrefixes ip = session.find( InspectorPrefixes.class, i.getId() );
					assertThat( ip ).isNotNull();
					assertThat( ip.getDesertedAreas().size() ).isEqualTo( 1 );
					assertThat( ip.getDesertedAreas().get( 0 ).getId() ).isEqualTo( a.getId() );
					session.remove( ip );
					session.remove( ip.getDesertedAreas().get( 0 ) );
				}
		);
	}

	@Test
	public void testReferencedColumnNameToSuperclass(SessionFactoryScope scope) {
		scope.inTransaction(
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
					building = session.find( Building.class, building.getId() );
					assertThat( building.getCompany().getName() ).isEqualTo( comp.getName() );
				}
		);
	}

	@Test
	@JiraKey(value = "HHH-4685")
	public void testManyToManyEmbeddableBiDirectionalDotNotationInMappedBy(SessionFactoryScope scope) {
		// Section 11.1.25
		// The ManyToMany annotation may be used within an embeddable class contained within an entity class to specify a
		// relationship to a collection of entities[101]. If the relationship is bidirectional and the entity containing
		// the embeddable class is the owner of the relationship, the non-owning side must use the mappedBy element of the
		// ManyToMany annotation to specify the relationship field or property of the embeddable class. The dot (".")
		// notation syntax must be used in the mappedBy element to indicate the relationship attribute within the embedded
		// attribute. The value of each identifier used with the dot notation is the name of the respective embedded field
		// or property.
		Employee e = new Employee();
		scope.inTransaction(
				session -> {
					e.setName( "Sharon" );
					List<PhoneNumber> phoneNumbers = new ArrayList<>();
					Collection<Employee> employees = new ArrayList<>();
					employees.add( e );
					ContactInfo contactInfo = new ContactInfo();
					PhoneNumber number = new PhoneNumber();
					number.setEmployees( employees );
					phoneNumbers.add( number );
					contactInfo.setPhoneNumbers( phoneNumbers );
					e.setContactInfo( contactInfo );
					session.persist( e );
				}
		);

		scope.inTransaction(
				session -> {
					Employee employee = session.find( e.getClass(), e.getId() );
					// follow both directions of many to many association
					assertThat( employee.getName() )
							.isSameAs( employee.getContactInfo().getPhoneNumbers().get( 0 ).getEmployees().iterator()
									.next().getName() );
				}
		);
	}

	@Test
	@JiraKey(value = "HHH-4685")
	public void testOneToManyEmbeddableBiDirectionalDotNotationInMappedBy(SessionFactoryScope scope) {
		// Section 11.1.26
		// The ManyToOne annotation may be used within an embeddable class to specify a relationship from the embeddable
		// class to an entity class. If the relationship is bidirectional, the non-owning OneToMany entity side must use the
		// mappedBy element of the OneToMany annotation to specify the relationship field or property of the embeddable field
		// or property on the owning side of the relationship. The dot (".") notation syntax must be used in the mappedBy
		// element to indicate the relationship attribute within the embedded attribute. The value of each identifier used
		// with the dot notation is the name of the respective embedded field or property.
		Employee e = new Employee();
		scope.inTransaction(
				session -> {
					JobInfo job = new JobInfo();
					job.setJobDescription( "Sushi Chef" );
					ProgramManager pm = new ProgramManager();
					Collection<Employee> employees = new ArrayList<>();
					employees.add( e );
					pm.setManages( employees );
					job.setPm( pm );
					e.setJobInfo( job );
					session.persist( e );
				}
		);

		scope.inTransaction(
				session -> {
					Employee employee = session.find( e.getClass(), e.getId() );
					assertThat( employee.getJobInfo().getJobDescription() )
							.describedAs( "same job in both directions" )
							.isSameAs( employee.getJobInfo().getPm().getManages().iterator().next().getJobInfo()
									.getJobDescription() );
				}
		);
	}
}
