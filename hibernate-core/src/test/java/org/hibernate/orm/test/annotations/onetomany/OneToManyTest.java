/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.onetomany;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import org.hibernate.AnnotationException;
import org.hibernate.Hibernate;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Table;
import org.hibernate.metamodel.CollectionClassification;
import org.hibernate.orm.test.annotations.Customer;
import org.hibernate.orm.test.annotations.Discount;
import org.hibernate.orm.test.annotations.Passport;
import org.hibernate.orm.test.annotations.Ticket;
import org.hibernate.orm.test.annotations.TicketComparator;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SettingProvider;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Fail.fail;
import static org.hibernate.cfg.AvailableSettings.DEFAULT_LIST_SEMANTICS;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Test various case of a one to many relationship.
 *
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 */
@DomainModel(
		annotatedClasses = {
				Troop.class,
				Soldier.class,
				Customer.class,
				Ticket.class,
				Discount.class,
				Passport.class,
				Parent.class,
				Child.class,
				Trainer.class,
				Tiger.class,
				Monkey.class,
				City.class,
				Street.class,
				PoliticalParty.class,
				Politician.class,
				Person.class,
				Organisation.class,
				OrganisationUser.class,
				Model.class,
				OneToManyTest.OnDeleteUnidirectionalOneToManyParent.class,
				OneToManyTest.OnDeleteUnidirectionalOneToManyChild.class
		},
		xmlMappings = "org/hibernate/orm/test/annotations/onetomany/orm.xml"
)
@SessionFactory
@ServiceRegistry(
		settingProviders = @SettingProvider(
				settingName = DEFAULT_LIST_SEMANTICS,
				provider = OneToManyTest.ListSemanticProvider.class
		)
)
public class OneToManyTest {

	public static class ListSemanticProvider implements SettingProvider.Provider<CollectionClassification> {

		@Override
		public CollectionClassification getSetting() {
			return CollectionClassification.BAG;
		}
	}

	@AfterEach
	public void afterEach(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncateMappedObjects();
	}

	@Test
	public void testColumnDefinitionPropagation(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Politician casimir = new Politician();
					casimir.setName( "Casimir" );
					PoliticalParty dream = new PoliticalParty();
					dream.setName( "Dream" );
					dream.addPolitician( casimir );
					session.persist( dream );
					session.getTransaction().commit();
					session.clear();

					session.beginTransaction();
					session.remove( session.find( PoliticalParty.class, dream.getName() ) );
				}
		);
	}

	@Test
	public void testListWithBagSemanticAndOrderBy(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					City paris = new City();
					paris.setName( "Paris" );
					session.persist( paris );
					Street rochechoir = new Street();
					rochechoir.setStreetName( "Rochechoir" );
					rochechoir.setCity( paris );
					Street chmpsElysees = new Street();
					chmpsElysees.setStreetName( "Champs Elysees" );
					chmpsElysees.setCity( paris );
					Street grandeArmee = new Street();
					grandeArmee.setStreetName( "Grande Armee" );
					grandeArmee.setCity( paris );
					session.persist( rochechoir );
					session.persist( chmpsElysees );
					session.persist( grandeArmee );
					paris.addMainStreet( chmpsElysees );
					paris.addMainStreet( grandeArmee );

					session.flush();
					session.clear();

					// Assert the primary key value relationship amongst the 3 streets...
					assertThat( rochechoir.getId() ).isLessThan( chmpsElysees.getId() );
					assertThat( chmpsElysees.getId() ).isLessThan( grandeArmee.getId() );

					paris = session.find( City.class, paris.getId() );

					// City.streets is defined to be ordered by name primarily...
					assertThat( paris.getStreets().size() ).isEqualTo( 3 );
					assertThat( paris.getStreets().get( 0 ).getStreetName() ).isEqualTo( chmpsElysees.getStreetName() );
					assertThat( paris.getStreets().get( 1 ).getStreetName() ).isEqualTo( grandeArmee.getStreetName() );
					// City.mainStreets is defined to be ordered by street id
					List<Street> mainStreets = paris.getMainStreets();
					assertThat( mainStreets.size() ).isEqualTo( 2 );
					Integer previousId = -1;
					for ( Street street : mainStreets ) {
						assertThat( previousId ).isLessThan( street.getId() );
						previousId = street.getId();
					}
				}
		);
	}

	@Test
	public void testUnidirectionalDefault(SessionFactoryScope scope) {
		Trainer t = new Trainer();
		t.setName( "First trainer" );
		Tiger regularTiger = new Tiger();
		regularTiger.setName( "Regular Tiger" );
		Tiger whiteTiger = new Tiger();
		whiteTiger.setName( "White Tiger" );
		t.setTrainedTigers( new HashSet<>() );
		scope.inTransaction(
				session -> {
					session.persist( t );
					session.persist( regularTiger );
					session.persist( whiteTiger );
					t.getTrainedTigers().add( regularTiger );
					t.getTrainedTigers().add( whiteTiger );
				}
		);

		scope.inTransaction(
				session -> {
					Trainer trainer = session.find( Trainer.class, t.getId() );
					assertThat( trainer ).isNotNull();
					assertThat( trainer.getTrainedTigers() ).isNotNull();
					assertThat( trainer.getTrainedTigers().size() ).isEqualTo( 2 );
				}
		);

		assertThrows( ConstraintViolationException.class, () -> scope.inSession(
				session -> {
					Trainer trainer = new Trainer();
					trainer.setName( "new trainer" );
					trainer.setTrainedTigers( new HashSet<>() );
					trainer.getTrainedTigers().add( whiteTiger );
					try {
						session.getTransaction().begin();
						session.persist( trainer );
						session.getTransaction().commit();
						fail( "A one to many should not allow several trainer per Tiger" );
					}
					finally {
						if ( session.getTransaction().isActive() ) {
							session.getTransaction().rollback();
						}
					}
				}
		) );
	}

	@Test
	public void testUnidirectionalExplicit(SessionFactoryScope scope) {
		Trainer t = new Trainer();
		t.setName( "First trainer" );
		Monkey regularMonkey = new Monkey();
		regularMonkey.setName( "Regular Monkey" );
		Monkey miniMonkey = new Monkey();
		miniMonkey.setName( "Mini Monkey" );
		t.setTrainedMonkeys( new HashSet<>() );
		scope.inTransaction(
				session -> {
					session.persist( t );
					session.persist( regularMonkey );
					session.persist( miniMonkey );
					t.getTrainedMonkeys().add( regularMonkey );
					t.getTrainedMonkeys().add( miniMonkey );
				}
		);

		scope.inTransaction(
				session -> {
					Trainer trainer = session.find( Trainer.class, t.getId() );
					assertThat( trainer ).isNotNull();
					assertThat( trainer.getTrainedMonkeys() ).isNotNull();
					assertThat( trainer.getTrainedMonkeys().size() ).isEqualTo( 2 );

					//test suppression of trainer wo monkey
					final Set<Monkey> monkeySet = new HashSet<>( trainer.getTrainedMonkeys() );
					session.remove( trainer );
					session.flush();
					session.getTransaction().commit();

					session.clear();

					session.beginTransaction();
					for ( Monkey m : monkeySet ) {
						final Object managedMonkey = session.find( Monkey.class, m.getId() );
						assertThat( managedMonkey )
								.describedAs( "No trainers but monkeys should still be here" )
								.isNotNull();
					}

					//clean up
					for ( Monkey m : monkeySet ) {
						final Object managedMonkey = session.find( Monkey.class, m.getId() );
						session.remove( managedMonkey );
					}
					session.flush();
				}
		);
	}

	@Test
	public void testFetching(SessionFactoryScope scope) {
		Troop troop = new Troop();
		Soldier rambo = new Soldier();
		scope.inTransaction(
				session -> {
					troop.setName( "Final cut" );
					Soldier vandamme = new Soldier();
					vandamme.setName( "JC Vandamme" );
					troop.addSoldier( vandamme );
					rambo.setName( "Rambo" );
					troop.addSoldier( rambo );
					session.persist( troop );
				}
		);

		scope.inTransaction(
				session -> {
					Troop t = session.find( Troop.class, troop.getId() );
					assertThat( t.getSoldiers() ).isNotNull();
					assertThat( Hibernate.isInitialized( t.getSoldiers() ) ).isFalse();
					assertThat( t.getSoldiers().size() ).isEqualTo( 2 );
					assertThat( t.getSoldiers().iterator().next().getName() ).isEqualTo( rambo.getName() );
				}
		);

		scope.inTransaction(
				session -> {
					Troop t = session.createQuery( "from " + Troop.class.getName() + " as t where t.id = :id",
									Troop.class )
							.setParameter( "id", troop.getId() ).uniqueResult();
					assertThat( Hibernate.isInitialized( t.getSoldiers() ) ).isFalse();
				}
		);

		scope.inTransaction(
				session -> {
					Soldier r = session.find( Soldier.class, rambo.getId() );
					assertThat( Hibernate.isInitialized( r.getTroop() ) ).isTrue();
				}
		);

		scope.inTransaction(
				session -> {
					Soldier r = session.createQuery( "from " + Soldier.class.getName() + " as s where s.id = :rid",
									Soldier.class )
							.setParameter( "rid", rambo.getId() )
							.uniqueResult();
					assertThat( Hibernate.isInitialized( r.getTroop() ) )
							.describedAs( "fetching strategy used when we do query" )
							.isTrue();
				}
		);
	}

	@Test
	public void testCascadeDeleteOrphan(SessionFactoryScope scope) {
		Troop disney = new Troop();
		Soldier mickey = new Soldier();
		scope.inTransaction(
				session -> {
					disney.setName( "Disney" );
					mickey.setName( "Mickey" );
					disney.addSoldier( mickey );
					session.persist( disney );
				}
		);

		Troop troop = scope.fromTransaction(
				session -> {
					Troop t = session.find( Troop.class, disney.getId() );
					t.getSoldiers().iterator().next();
					return t;
				}
		);

		troop.getSoldiers().clear();

		scope.inTransaction(
				session ->
						session.merge( troop )
		);

		scope.inTransaction(
				session -> {
					Soldier soldier = session.find( Soldier.class, mickey.getId() );
					assertThat( soldier )
							.describedAs( "delete-orphan should work" )
							.isNull();
					session.remove( session.find( Troop.class, disney.getId() ) );
				}
		);
	}

	@Test
	public void testCascadeDelete(SessionFactoryScope scope) {
		Troop disney = new Troop();
		Soldier mickey = new Soldier();
		scope.inTransaction(
				session -> {
					disney.setName( "Disney" );
					mickey.setName( "Mickey" );
					disney.addSoldier( mickey );
					session.persist( disney );
				}
		);
		scope.inTransaction(
				session -> {
					Troop troop = session.find( Troop.class, disney.getId() );
					session.remove( troop );
				}
		);

		scope.inTransaction(
				session -> {
					Soldier soldier = session.find( Soldier.class, mickey.getId() );
					assertThat( soldier )
							.describedAs( "delete-orphan should work" )
							.isNull();
				}
		);
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsCascadeDeleteCheck.class)
	public void testCascadeDeleteWithUnidirectionalAssociation(SessionFactoryScope scope) {
		OnDeleteUnidirectionalOneToManyChild child = new OnDeleteUnidirectionalOneToManyChild();

		scope.inTransaction( session -> {
			OnDeleteUnidirectionalOneToManyParent parent = new OnDeleteUnidirectionalOneToManyParent();
			parent.children = Collections.singletonList( child );
			session.persist( parent );
		} );

		scope.inTransaction( session ->
				session.createMutationQuery( "delete from OnDeleteUnidirectionalOneToManyParent" ).executeUpdate()
		);

		scope.inTransaction( session -> {
			OnDeleteUnidirectionalOneToManyChild e1 = session.find(
					OnDeleteUnidirectionalOneToManyChild.class,
					child.id );
			assertThat( e1 ).describedAs( "delete cascade should work" ).isNull();
		} );
	}

	@Test
	public void testOnDeleteWithoutJoinColumn() {
		StandardServiceRegistry serviceRegistry = ServiceRegistryUtil.serviceRegistry();

		try {
			AnnotationException e = assertThrows( AnnotationException.class,
					() -> new MetadataSources( serviceRegistry )
							.addAnnotatedClass( OnDeleteUnidirectionalOneToMany.class )
							.addAnnotatedClass( ParentUnawareChild.class )
							.getMetadataBuilder()
							.build()
			);
			assertThat( e.getMessage() )
					.contains( "is annotated '@OnDelete' and must explicitly specify a '@JoinColumn'" );

		}
		finally {
			StandardServiceRegistryBuilder.destroy( serviceRegistry );
		}
	}

	@Test
	public void testSimpleOneToManySet(SessionFactoryScope scope) {
		Customer customer = new Customer();
		Ticket t2 = new Ticket();
		scope.inTransaction(
				session -> {
					Ticket t = new Ticket();
					t.setNumber( "33A" );
					t2.setNumber( "234ER" );
					session.persist( customer );
					//s.persist(t);
					SortedSet<Ticket> tickets = new TreeSet<>( new TicketComparator() );
					tickets.add( t );
					tickets.add( t2 );
					customer.setTickets( tickets );
				}
		);

		scope.inTransaction(
				session -> {
					Customer c = session.getReference( Customer.class, customer.getId() );
					assertThat( c ).isNotNull();
					assertThat( Hibernate.isInitialized( c.getTickets() ) ).isTrue();
					assertThat( c.getTickets() ).isNotNull();
					SortedSet<Ticket> tickets = c.getTickets();
					assertThat( tickets.size() ).isGreaterThan( 0 );
					assertThat( c.getTickets().first().getNumber() ).isEqualTo( t2.getNumber() );
				}
		);
	}

	@Test
	public void testSimpleOneToManyCollection(SessionFactoryScope scope) {
		Customer c = new Customer();
		scope.inTransaction(
				session -> {
					Discount d = new Discount();
					d.setDiscount( 10 );
					List<Discount> discounts = new ArrayList<>();
					discounts.add( d );
					d.setOwner( c );
					c.setDiscountTickets( discounts );
					session.persist( c );
				}
		);

		scope.inTransaction(
				session -> {
					Customer customer = session.getReference( Customer.class, c.getId() );
					assertThat( customer ).isNotNull();
					assertThat( Hibernate.isInitialized( customer.getDiscountTickets() ) )
							.isFalse();
					assertThat( customer.getDiscountTickets() ).isNotNull();
					Collection<Discount> collecDiscount = customer.getDiscountTickets();
					assertThat( collecDiscount.size() ).isGreaterThan( 0 );
				}
		);
	}

	@Test
	public void testJoinColumns(SessionFactoryScope scope) {
		Parent p = new Parent();
		ParentPk pk = new ParentPk();
		pk.firstName = "Bruce";
		pk.lastName = "Willis";
		pk.isMale = true;
		p.id = pk;
		p.age = 40;
		Child child = new Child();
		Child child2 = new Child();
		p.addChild( child );
		p.addChild( child2 );
		scope.inTransaction(
				session ->
						session.persist( p )
		);

		assertThat( child.id ).isNotNull();
		assertThat( child2.id ).isNotNull();
		assertThat( child.id ).isNotSameAs( child2.id );

		scope.inTransaction(
				session -> {
					Parent parent = session.find( Parent.class, pk );
					assertThat( parent.children ).isNotNull();
					Hibernate.initialize( parent.children );
					assertThat( parent.children.size() ).isEqualTo( 2 );
				}
		);
	}

	@Test
	@JiraKey(value = "HHH-4394")
	public void testOrderByOnSuperclassProperty(SessionFactoryScope scope) {
		OrganisationUser user = new OrganisationUser();
		user.setFirstName( "Emmanuel" );
		user.setLastName( "Bernard" );
		user.setIdPerson( 1L );
		user.setSomeText( "SomeText" );
		Organisation org = new Organisation();
		org.setIdOrganisation( 1L );
		org.setName( "S Diego Zoo" );
		user.setOrganisation( org );
		scope.inTransaction(
				session -> {
					session.persist( user );
					session.persist( org );
					session.flush();
					session.clear();
					session.createQuery( "select org from Organisation org left join fetch org.organisationUsers",
									Organisation.class )
							.list();
				}
		);
	}

	@Test
	@JiraKey(value = "HHH-4605")
	public void testJoinColumnConfiguredInXml(SessionFactoryScope scope) {
		PersistentClass pc = scope.getMetadataImplementor().getEntityBinding( Model.class.getName() );
		Table table = pc.getRootTable();
		boolean joinColumnFound = false;
		for ( Column column : table.getColumns() ) {
			if ( column.getName().equals( "model_manufacturer_join" ) ) {
				joinColumnFound = true;
				break;
			}
		}
		assertThat( joinColumnFound )
				.describedAs( "The mapping defines a joing column which could not be found in the metadata." )
				.isTrue();
	}

	@Entity(name = "OnDeleteUnidirectionalOneToManyParent")
	@jakarta.persistence.Table(name = "OneToManyParent")
	public static class OnDeleteUnidirectionalOneToManyParent {

		@Id
		@GeneratedValue
		Long id;

		@OneToMany(cascade = CascadeType.ALL)
		@JoinColumn(name = "a_id")
		@OnDelete(action = OnDeleteAction.CASCADE)
		List<OnDeleteUnidirectionalOneToManyChild> children;
	}

	@Entity(name = "OnDeleteUnidirectionalOneToManyChild")
	@jakarta.persistence.Table(name = "OneToManyChild")
	public static class OnDeleteUnidirectionalOneToManyChild {

		@Id
		@GeneratedValue
		Long id;
	}

	@Entity(name = "OnDeleteUnidirectionalOneToMany")
	@jakarta.persistence.Table(name = "OneToMany")
	public static class OnDeleteUnidirectionalOneToMany {

		@Id
		Long id;

		@OneToMany(cascade = CascadeType.ALL)
		@OnDelete(action = OnDeleteAction.CASCADE)
		List<ParentUnawareChild> children;
	}

	@Entity(name = "ParentUnawareChild")
	@jakarta.persistence.Table(name = "Child")
	public static class ParentUnawareChild {

		@Id
		Long id;
	}
}
