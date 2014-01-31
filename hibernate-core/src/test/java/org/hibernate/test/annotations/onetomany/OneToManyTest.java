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
package org.hibernate.test.annotations.onetomany;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.metamodel.spi.binding.EntityBinding;
import org.hibernate.metamodel.spi.relational.TableSpecification;
import org.hibernate.metamodel.spi.relational.Value;

import org.hibernate.testing.FailureExpectedWithNewMetamodel;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.hibernate.test.annotations.Customer;
import org.hibernate.test.annotations.Discount;
import org.hibernate.test.annotations.Passport;
import org.hibernate.test.annotations.Ticket;
import org.hibernate.test.annotations.TicketComparator;
import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Test various case of a one to many relationship.
 *
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 */
@SuppressWarnings("unchecked")
@FailureExpectedWithNewMetamodel
public class OneToManyTest extends BaseCoreFunctionalTestCase {
	@Test
	public void testColumnDefinitionPropagation() throws Exception {
		Session s;
		s = openSession();
		s.getTransaction().begin();
		Politician casimir = new Politician();
		casimir.setName( "Casimir" );
		PoliticalParty dream = new PoliticalParty();
		dream.setName( "Dream" );
		dream.addPolitician( casimir );
		s.persist( dream );
		s.getTransaction().commit();
		s.clear();

		Transaction tx = s.beginTransaction();
		s.delete( s.get( PoliticalParty.class, dream.getName() ) );
		tx.commit();
		s.close();
	}

	@Test
	public void testListWithBagSemanticAndOrderBy() throws Exception {
		Session s;
		Transaction tx;
		s = openSession();
		tx = s.beginTransaction();
		City paris = new City();
		paris.setName( "Paris" );
		s.persist( paris );
		Street rochechoir = new Street();
		rochechoir.setStreetName( "Rochechoir" );
		rochechoir.setCity( paris );
		Street chmpsElysees = new Street();
		chmpsElysees.setStreetName( "Champs Elysees" );
		chmpsElysees.setCity( paris );
		Street grandeArmee = new Street();
		grandeArmee.setStreetName( "Grande Armee" );
		grandeArmee.setCity( paris );
		s.persist( rochechoir );
		s.persist( chmpsElysees );
		s.persist( grandeArmee );
		paris.addMainStreet( chmpsElysees );
		paris.addMainStreet( grandeArmee );

		s.flush();
		s.clear();

		// Assert the primary key value relationship amongst the 3 streets...
		Assert.assertTrue( rochechoir.getId() < chmpsElysees.getId() );
		Assert.assertTrue( chmpsElysees.getId() < grandeArmee.getId() );

		paris = ( City ) s.get( City.class, paris.getId() );

		// City.streets is defined to be ordered by name primarily...
		assertEquals( 3, paris.getStreets().size() );
		assertEquals( chmpsElysees.getStreetName(), paris.getStreets().get( 0 ).getStreetName() );
		assertEquals( grandeArmee.getStreetName(), paris.getStreets().get( 1 ).getStreetName() );
		// City.mainStreets is defined to be ordered by street id
		List<Street> mainStreets = paris.getMainStreets();
		assertEquals( 2, mainStreets.size() );
		Integer previousId = -1;
		for ( Street street : mainStreets ) {
			assertTrue( previousId < street.getId() );
			previousId = street.getId();
		}
		tx.rollback();
		s.close();

	}

	@Test
	public void testUnidirectionalDefault() throws Exception {
		Session s;
		Transaction tx;
		Trainer trainer = new Trainer();
		trainer.setName( "First trainer" );
		Tiger regularTiger = new Tiger();
		regularTiger.setName( "Regular Tiger" );
		Tiger whiteTiger = new Tiger();
		whiteTiger.setName( "White Tiger" );
		trainer.setTrainedTigers( new HashSet<Tiger>() );
		s = openSession();
		tx = s.beginTransaction();
		s.persist( trainer );
		s.persist( regularTiger );
		s.persist( whiteTiger );
		trainer.getTrainedTigers().add( regularTiger );
		trainer.getTrainedTigers().add( whiteTiger );

		tx.commit();
		s.close();

		s = openSession();
		tx = s.beginTransaction();
		trainer = ( Trainer ) s.get( Trainer.class, trainer.getId() );
		assertNotNull( trainer );
		assertNotNull( trainer.getTrainedTigers() );
		assertEquals( 2, trainer.getTrainedTigers().size() );
		tx.rollback();
		s.close();

		s = openSession();
		tx = s.beginTransaction();
		trainer = new Trainer();
		trainer.setName( "new trainer" );
		trainer.setTrainedTigers( new HashSet<Tiger>() );
		trainer.getTrainedTigers().add( whiteTiger );
		try {
			s.persist( trainer );
			tx.commit();
			fail( "A one to many should not allow several trainer per Tiger" );
		}
		catch ( HibernateException ce ) {
			tx.rollback();
			//success
		}
		s.close();
	}

	@Test
	public void testUnidirectionalExplicit() throws Exception {
		Session s;
		Transaction tx;
		Trainer trainer = new Trainer();
		trainer.setName( "First trainer" );
		Monkey regularMonkey = new Monkey();
		regularMonkey.setName( "Regular Monkey" );
		Monkey miniMonkey = new Monkey();
		miniMonkey.setName( "Mini Monkey" );
		trainer.setTrainedMonkeys( new HashSet<Monkey>() );
		s = openSession();
		tx = s.beginTransaction();
		s.persist( trainer );
		s.persist( regularMonkey );
		s.persist( miniMonkey );
		trainer.getTrainedMonkeys().add( regularMonkey );
		trainer.getTrainedMonkeys().add( miniMonkey );
		tx.commit();
		s.close();

		s = openSession();
		tx = s.beginTransaction();
		trainer = ( Trainer ) s.get( Trainer.class, trainer.getId() );
		assertNotNull( trainer );
		assertNotNull( trainer.getTrainedMonkeys() );
		assertEquals( 2, trainer.getTrainedMonkeys().size() );

		//test suppression of trainer wo monkey
		final Set<Monkey> monkeySet = new HashSet( trainer.getTrainedMonkeys() );
		s.delete( trainer );
		s.flush();
		tx.commit();

		s.clear();

		tx = s.beginTransaction();
		for ( Monkey m : monkeySet ) {
			final Object managedMonkey = s.get( Monkey.class, m.getId() );
			assertNotNull( "No trainers but monkeys should still be here", managedMonkey );
		}

		//clean up
		for ( Monkey m : monkeySet ) {
			final Object managedMonkey = s.get( Monkey.class, m.getId() );
			s.delete(managedMonkey);
		}
		s.flush();
		tx.commit();
		s.close();
	}

	@Test
	public void testFetching() throws Exception {
		Session s;
		Transaction tx;
		s = openSession();
		tx = s.beginTransaction();
		Troop t = new Troop();
		t.setName( "Final cut" );
		Soldier vandamme = new Soldier();
		vandamme.setName( "JC Vandamme" );
		t.addSoldier( vandamme );
		Soldier rambo = new Soldier();
		rambo.setName( "Rambo" );
		t.addSoldier( rambo );
		s.persist( t );
		tx.commit();
		s.close();

		s = openSession();
		tx = s.beginTransaction();
		t = ( Troop ) s.get( Troop.class, t.getId() );
		assertNotNull( t.getSoldiers() );
		assertFalse( Hibernate.isInitialized( t.getSoldiers() ) );
		assertEquals( 2, t.getSoldiers().size() );
		assertEquals( rambo.getName(), t.getSoldiers().iterator().next().getName() );
		tx.commit();
		s.close();

		s = openSession();
		tx = s.beginTransaction();
		t = ( Troop ) s.createQuery( "from " + Troop.class.getName() + " as t where t.id = :id" )
				.setParameter( "id", t.getId() ).uniqueResult();
		assertFalse( Hibernate.isInitialized( t.getSoldiers() ) );
		tx.commit();
		s.close();

		s = openSession();
		tx = s.beginTransaction();
		rambo = ( Soldier ) s.get( Soldier.class, rambo.getId() );
		assertTrue( Hibernate.isInitialized( rambo.getTroop() ) );
		tx.commit();
		s.close();

		s = openSession();
		tx = s.beginTransaction();
		rambo = ( Soldier ) s.createQuery( "from " + Soldier.class.getName() + " as s where s.id = :rid" )
				.setParameter( "rid", rambo.getId() ).uniqueResult();
		assertTrue( "fetching strategy used when we do query", Hibernate.isInitialized( rambo.getTroop() ) );
		tx.commit();
		s.close();
	}

	@Test
	public void testCascadeDeleteOrphan() throws Exception {
		Session s;
		Transaction tx;
		s = openSession();
		tx = s.beginTransaction();
		Troop disney = new Troop();
		disney.setName( "Disney" );
		Soldier mickey = new Soldier();
		mickey.setName( "Mickey" );
		disney.addSoldier( mickey );
		s.persist( disney );
		tx.commit();
		s.close();
		s = openSession();
		tx = s.beginTransaction();
		Troop troop = ( Troop ) s.get( Troop.class, disney.getId() );
		Soldier soldier = troop.getSoldiers().iterator().next();
		tx.commit();
		s.close();
		troop.getSoldiers().clear();
		s = openSession();
		tx = s.beginTransaction();
		s.merge( troop );
		tx.commit();
		s.close();
		s = openSession();
		tx = s.beginTransaction();
		soldier = ( Soldier ) s.get( Soldier.class, mickey.getId() );
		assertNull( "delete-orphan should work", soldier );
		troop = ( Troop ) s.get( Troop.class, disney.getId() );
		s.delete( troop );
		tx.commit();
		s.close();
	}

	@Test
	public void testCascadeDelete() throws Exception {
		Session s;
		Transaction tx;
		s = openSession();
		tx = s.beginTransaction();
		Troop disney = new Troop();
		disney.setName( "Disney" );
		Soldier mickey = new Soldier();
		mickey.setName( "Mickey" );
		disney.addSoldier( mickey );
		s.persist( disney );
		tx.commit();
		s.close();
		s = openSession();
		tx = s.beginTransaction();
		Troop troop = ( Troop ) s.get( Troop.class, disney.getId() );
		s.delete( troop );
		tx.commit();
		s.close();
		s = openSession();
		tx = s.beginTransaction();
		Soldier soldier = ( Soldier ) s.get( Soldier.class, mickey.getId() );
		assertNull( "delete-orphan should work", soldier );
		tx.commit();
		s.close();
	}

	@Test
	public void testSimpleOneToManySet() throws Exception {
		Session s;
		Transaction tx;
		s = openSession();
		tx = s.beginTransaction();
		Ticket t = new Ticket();
		t.setNumber( "33A" );
		Ticket t2 = new Ticket();
		t2.setNumber( "234ER" );
		Customer c = new Customer();
		s.persist( c );
		//s.persist(t);
		SortedSet<Ticket> tickets = new TreeSet<Ticket>( new TicketComparator() );
		tickets.add( t );
		tickets.add( t2 );
		c.setTickets( tickets );

		tx.commit();
		s.close();

		s = openSession();
		tx = s.beginTransaction();
		c = ( Customer ) s.load( Customer.class, c.getId() );
		assertNotNull( c );
		assertTrue( Hibernate.isInitialized( c.getTickets() ) );
		assertNotNull( c.getTickets() );
		tickets = c.getTickets();
		assertTrue( tickets.size() > 0 );
		assertEquals( t2.getNumber(), c.getTickets().first().getNumber() );
		tx.commit();
		s.close();
	}

	@Test
	public void testSimpleOneToManyCollection() throws Exception {
		Session s;
		Transaction tx;
		s = openSession();
		tx = s.beginTransaction();
		Discount d = new Discount();
		d.setDiscount( 10 );
		Customer c = new Customer();
		List discounts = new ArrayList();
		discounts.add( d );
		d.setOwner( c );
		c.setDiscountTickets( discounts );
		s.persist( c );
		tx.commit();
		s.close();

		s = openSession();
		tx = s.beginTransaction();
		c = ( Customer ) s.load( Customer.class, c.getId() );
		assertNotNull( c );
		assertFalse( Hibernate.isInitialized( c.getDiscountTickets() ) );
		assertNotNull( c.getDiscountTickets() );
		Collection collecDiscount = c.getDiscountTickets();
		assertTrue( collecDiscount.size() > 0 );
		tx.commit();
		s.close();
	}

	@Test
	public void testJoinColumns() throws Exception {
		Parent parent = new Parent();
		ParentPk pk = new ParentPk();
		pk.firstName = "Bruce";
		pk.lastName = "Willis";
		pk.isMale = true;
		parent.id = pk;
		parent.age = 40;
		Child child = new Child();
		Child child2 = new Child();
		parent.addChild( child );
		parent.addChild( child2 );
		Session s;
		Transaction tx;
		s = openSession();
		tx = s.beginTransaction();
		s.persist( parent );
		tx.commit();
		s.close();

		assertNotNull( child.id );
		assertNotNull( child2.id );
		assertNotSame( child.id, child2.id );

		s = openSession();
		tx = s.beginTransaction();
		parent = ( Parent ) s.get( Parent.class, pk );
		assertNotNull( parent.children );
		Hibernate.initialize( parent.children );
		assertEquals( 2, parent.children.size() );
		tx.commit();
		s.close();
	}

	@Test
	@TestForIssue( jiraKey = "HHH-4394" )
	public void testOrderByOnSuperclassProperty() {
		OrganisationUser user = new OrganisationUser();
		user.setFirstName( "Emmanuel" );
		user.setLastName( "Bernard" );
		user.setIdPerson( 1l );
		user.setSomeText( "SomeText" );
		Organisation org = new Organisation();
		org.setIdOrganisation( 1l );
		org.setName( "S Diego Zoo" );
		user.setOrganisation( org );
		Session s = openSession();
		s.getTransaction().begin();
		s.persist( user );
		s.persist( org );
		s.flush();
		s.clear();
		s.createQuery( "select org from Organisation org left join fetch org.organisationUsers" ).list();
		s.getTransaction().rollback();
		s.close();
	}

	@Test
	@TestForIssue( jiraKey = "HHH-4605" )
	public void testJoinColumnConfiguredInXml() {
		boolean joinColumnFound = false;
		EntityBinding entityBinding = metadata().getEntityBinding( Model.class.getName() );
		TableSpecification table = entityBinding.getPrimaryTable();
		for ( Value value : table.values() ) {
			org.hibernate.metamodel.spi.relational.Column column =
					(org.hibernate.metamodel.spi.relational.Column) value;
			if(column.getColumnName().getText().equals( "model_manufacturer_join" )) {
				joinColumnFound = true;
			}
		}
		assertTrue( "The mapping defines a joing column which could not be found in the metadata.", joinColumnFound );
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
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
				Model.class
		};
	}

	@Override
	protected String[] getXmlFiles() {
		return new String[] { "org/hibernate/test/annotations/onetomany/orm.xml" };
	}
}
