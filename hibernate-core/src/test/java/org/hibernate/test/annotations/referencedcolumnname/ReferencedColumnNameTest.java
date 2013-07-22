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
package org.hibernate.test.annotations.referencedcolumnname;

import java.math.BigDecimal;
import java.util.Iterator;

import org.junit.Test;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Restrictions;
import org.hibernate.testing.FailureExpectedWithNewMetamodel;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Emmanuel Bernard
 */
@FailureExpectedWithNewMetamodel
public class ReferencedColumnNameTest extends BaseCoreFunctionalTestCase {
	@Test
	public void testManyToOne() throws Exception {
		Session s;
		Transaction tx;
		s = openSession();
		tx = s.beginTransaction();
		Postman pm = new Postman( "Bob", "A01" );
		House house = new House();
		house.setPostman( pm );
		house.setAddress( "Rue des pres" );
		s.persist( pm );
		s.persist( house );
		tx.commit();
		s.close();
		s = openSession();
		tx = s.beginTransaction();
		house = (House) s.get( House.class, house.getId() );
		assertNotNull( house.getPostman() );
		assertEquals( "Bob", house.getPostman().getName() );
		pm = house.getPostman();
		s.delete( house );
		s.delete( pm );
		tx.commit();
		s.close();
	}

	@Test
	public void testOneToMany() throws Exception {
		Session s;
		Transaction tx;
		s = openSession();
		tx = s.beginTransaction();

		Rambler rambler = new Rambler( "Emmanuel" );
		Bag bag = new Bag( "0001", rambler );
		rambler.getBags().add( bag );
		s.persist( rambler );

		tx.commit();
		s.close();

		s = openSession();
		tx = s.beginTransaction();

		bag = (Bag) s.createQuery( "select b from Bag b left join fetch b.owner" ).uniqueResult();
		assertNotNull( bag );
		assertNotNull( bag.getOwner() );

		rambler = (Rambler) s.createQuery( "select r from Rambler r left join fetch r.bags" ).uniqueResult();
		assertNotNull( rambler );
		assertNotNull( rambler.getBags() );
		assertEquals( 1, rambler.getBags().size() );
		s.delete( rambler.getBags().iterator().next() );
		s.delete( rambler );

		tx.commit();
		s.close();
	}

	@Test
	public void testUnidirectionalOneToMany() throws Exception {
		Session s;
		Transaction tx;
		s = openSession();
		tx = s.beginTransaction();

		Clothes clothes = new Clothes( "underwear", "interesting" );
		Luggage luggage = new Luggage( "Emmanuel", "Cabin Luggage" );
		luggage.getHasInside().add( clothes );
		s.persist( luggage );

		tx.commit();
		s.close();

		s = openSession();
		tx = s.beginTransaction();

		luggage = (Luggage) s.createQuery( "select l from Luggage l left join fetch l.hasInside" ).uniqueResult();
		assertNotNull( luggage );
		assertNotNull( luggage.getHasInside() );
		assertEquals( 1, luggage.getHasInside().size() );

		s.delete( luggage.getHasInside().iterator().next() );
		s.delete( luggage );

		tx.commit();
		s.close();
	}

	@Test
	public void testManyToMany() throws Exception {
		Session s;
		Transaction tx;
		s = openSession();
		tx = s.beginTransaction();

		House whiteHouse = new House();
		whiteHouse.setAddress( "1600 Pennsylvania Avenue, Washington" );
		Inhabitant bill = new Inhabitant();
		bill.setName( "Bill Clinton" );
		Inhabitant george = new Inhabitant();
		george.setName( "George W Bush" );
		s.persist( george );
		s.persist( bill );
		whiteHouse.getHasInhabitants().add( bill );
		whiteHouse.getHasInhabitants().add( george );
		//bill.getLivesIn().add( whiteHouse );
		//george.getLivesIn().add( whiteHouse );

		s.persist( whiteHouse );
		tx.commit();
		s = openSession();
		tx = s.beginTransaction();

		whiteHouse = (House) s.get( House.class, whiteHouse.getId() );
		assertNotNull( whiteHouse );
		assertEquals( 2, whiteHouse.getHasInhabitants().size() );

		tx.commit();
		s.clear();
		tx = s.beginTransaction();
		bill = (Inhabitant) s.get( Inhabitant.class, bill.getId() );
		assertNotNull( bill );
		assertEquals( 1, bill.getLivesIn().size() );
		assertEquals( whiteHouse.getAddress(), bill.getLivesIn().iterator().next().getAddress() );

		whiteHouse = bill.getLivesIn().iterator().next();
		s.delete( whiteHouse );
		Iterator it = whiteHouse.getHasInhabitants().iterator();
		while ( it.hasNext() ) {
			s.delete( it.next() );
		}
		tx.commit();
		s.close();
	}

	@Test
	public void testManyToOneReferenceManyToOne() throws Exception {
		Item item = new Item();
		item.setId( 1 );
		Vendor vendor = new Vendor();
		vendor.setId( 1 );
		ItemCost cost = new ItemCost();
		cost.setCost( new BigDecimal(1) );
		cost.setId( 1 );
		cost.setItem( item );
		cost.setVendor( vendor );
		WarehouseItem wItem = new WarehouseItem();
		wItem.setDefaultCost( cost );
		wItem.setId( 1 );
		wItem.setItem( item );
		wItem.setQtyInStock( new BigDecimal(1) );
		wItem.setVendor( vendor );
		Session s = openSession( );
		s.getTransaction().begin();
		s.persist( item );
		s.persist( vendor );
		s.persist( cost );
		s.persist( wItem );
		s.flush();
		s.clear();
		wItem = (WarehouseItem) s.get(WarehouseItem.class, wItem.getId() );
		assertNotNull( wItem.getDefaultCost().getItem() );
		s.getTransaction().rollback();
		s.close();
	}

	@Test
	public void testManyToOneInsideComponentReferencedColumn() {
		HousePlaces house = new HousePlaces();
		house.places = new Places();

		house.places.livingRoom = new Place();
		house.places.livingRoom.name = "First";
		house.places.livingRoom.owner = "mine";

		house.places.kitchen = new Place();
		house.places.kitchen.name = "Kitchen 1";

		house.neighbourPlaces = new Places();
		house.neighbourPlaces.livingRoom = new Place();
		house.neighbourPlaces.livingRoom.name = "Neighbour";
		house.neighbourPlaces.livingRoom.owner = "his";

		house.neighbourPlaces.kitchen = new Place();
		house.neighbourPlaces.kitchen.name = "His Kitchen";

		Session s = openSession();
		Transaction tx = s.beginTransaction();
		s.save( house );
		s.flush();

		HousePlaces get = (HousePlaces) s.get( HousePlaces.class, house.id );
		assertEquals( house.id, get.id );

		HousePlaces uniqueResult = (HousePlaces) s.createQuery( "from HousePlaces h where h.places.livingRoom.name='First'" )
				.uniqueResult();
		assertNotNull( uniqueResult );
		assertEquals( uniqueResult.places.livingRoom.name, "First" );
		assertEquals( uniqueResult.places.livingRoom.owner, "mine" );

		uniqueResult = (HousePlaces) s.createQuery( "from HousePlaces h where h.places.livingRoom.owner=:owner" )
				.setParameter( "owner", "mine" ).uniqueResult();
		assertNotNull( uniqueResult );
		assertEquals( uniqueResult.places.livingRoom.name, "First" );
		assertEquals( uniqueResult.places.livingRoom.owner, "mine" );

		assertNotNull( s.createCriteria( HousePlaces.class ).add( Restrictions.eq( "places.livingRoom.owner", "mine" ) )
				.uniqueResult() );

		// override
		uniqueResult = (HousePlaces) s.createQuery( "from HousePlaces h where h.neighbourPlaces.livingRoom.owner='his'" )
				.uniqueResult();
		assertNotNull( uniqueResult );
		assertEquals( uniqueResult.neighbourPlaces.livingRoom.name, "Neighbour" );
		assertEquals( uniqueResult.neighbourPlaces.livingRoom.owner, "his" );

		uniqueResult = (HousePlaces) s.createQuery( "from HousePlaces h where h.neighbourPlaces.livingRoom.name=:name" )
				.setParameter( "name", "Neighbour" ).uniqueResult();
		assertNotNull( uniqueResult );
		assertEquals( uniqueResult.neighbourPlaces.livingRoom.name, "Neighbour" );
		assertEquals( uniqueResult.neighbourPlaces.livingRoom.owner, "his" );

		assertNotNull( s.createCriteria( HousePlaces.class )
				.add( Restrictions.eq( "neighbourPlaces.livingRoom.owner", "his" ) ).uniqueResult() );

		tx.rollback();
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[]{
				House.class,
				Postman.class,
				Bag.class,
				Rambler.class,
				Luggage.class,
				Clothes.class,
				Inhabitant.class,
				Item.class,
				ItemCost.class,
				Vendor.class,
				WarehouseItem.class,
				Place.class,
				HousePlaces.class,
				Places.class
		};
	}
}
