/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.referencedcolumnname;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.boot.model.naming.ImplicitNamingStrategyLegacyJpaImpl;
import org.hibernate.cfg.Configuration;
import org.hibernate.dialect.TeradataDialect;
import org.hibernate.testing.SkipForDialect;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.Iterator;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Root;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Emmanuel Bernard
 */
public class ReferencedColumnNameTest extends BaseCoreFunctionalTestCase {
	@Test
	public void testManyToOne() {
		Postman postman = new Postman( "Bob", "A01" );
		House house = new House();
		house.setPostman( postman );
		house.setAddress( "Rue des pres" );
		inTransaction(
				s -> {
					s.persist( postman );
					s.persist( house );
				}
		);

		inTransaction(
				s -> {
					House h = s.get( House.class, house.getId() );
					assertNotNull( h.getPostman() );
					assertEquals( "Bob", h.getPostman().getName() );
					Postman pm = h.getPostman();
					s.delete( h );
					s.delete( pm );
				}
		);
	}

	@Test
	public void testOneToMany() {
		inTransaction(
				s -> {
					Rambler rambler = new Rambler( "Emmanuel" );
					Bag bag = new Bag( "0001", rambler );
					rambler.getBags().add( bag );
					s.persist( rambler );
				}
		);

		inTransaction(
				s -> {
					Bag bag = (Bag) s.createQuery( "select b from Bag b left join fetch b.owner" ).uniqueResult();
					assertNotNull( bag );
					assertNotNull( bag.getOwner() );

					Rambler rambler = (Rambler) s.createQuery( "select r from Rambler r left join fetch r.bags" ).uniqueResult();
					assertNotNull( rambler );
					assertNotNull( rambler.getBags() );
					assertEquals( 1, rambler.getBags().size() );
					s.delete( rambler.getBags().iterator().next() );
					s.delete( rambler );
				}
		);
	}

	@Test
		@SkipForDialect(
						value = TeradataDialect.class,
						jiraKey = "HHH-8190",
						comment = "uses Teradata reserved word - type"
				)
	public void testUnidirectionalOneToMany() {
		inTransaction(
				s -> {
					Clothes clothes = new Clothes( "underwear", "interesting" );
					Luggage luggage = new Luggage( "Emmanuel", "Cabin Luggage" );
					luggage.getHasInside().add( clothes );
					s.persist( luggage );
				}
		);

		inTransaction(
				s -> {
					Luggage luggage = (Luggage) s.createQuery( "select l from Luggage l left join fetch l.hasInside" )
							.uniqueResult();
					assertNotNull( luggage );
					assertNotNull( luggage.getHasInside() );
					assertEquals( 1, luggage.getHasInside().size() );

					s.delete( luggage.getHasInside().iterator().next() );
					s.delete( luggage );

				}
		);
	}

	@Test
	public void testManyToMany(){
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

		whiteHouse = s.get( House.class, whiteHouse.getId() );
		assertNotNull( whiteHouse );
		assertEquals( 2, whiteHouse.getHasInhabitants().size() );

		tx.commit();
		s.clear();
		tx = s.beginTransaction();
		bill = s.get( Inhabitant.class, bill.getId() );
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
	public void testManyToOneReferenceManyToOne() {
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
		inTransaction(
				s -> {
					s.persist( item );
					s.persist( vendor );
					s.persist( cost );
					s.persist( wItem );
					s.flush();
					s.clear();
					WarehouseItem warehouseItem = s.get(WarehouseItem.class, wItem.getId() );
					assertNotNull( warehouseItem.getDefaultCost().getItem() );
				}
		);
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

		inTransaction(
				s -> {
					s.save( house );
					s.flush();

					HousePlaces get = s.get( HousePlaces.class, house.id );
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

					CriteriaBuilder criteriaBuilder = s.getCriteriaBuilder();
					CriteriaQuery<HousePlaces> criteria = criteriaBuilder.createQuery( HousePlaces.class );
					Root<HousePlaces> root = criteria.from( HousePlaces.class );
					Join<Object, Object> join = root.join( "places" ).join( "livingRoom" );
					criteria.where( criteriaBuilder.equal( join.get( "owner" ), "mine" ) );

					assertNotNull(s.createQuery( criteria ).uniqueResult());

//					assertNotNull( s.createCriteria( HousePlaces.class ).add( Restrictions.eq( "places.livingRoom.owner", "mine" ) )
//										   .uniqueResult() );

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

					criteria = criteriaBuilder.createQuery( HousePlaces.class );
					root = criteria.from( HousePlaces.class );
					join = root.join( "neighbourPlaces" ).join( "livingRoom" );
					criteria.where( criteriaBuilder.equal( join.get( "owner" ), "his" ) );

					assertNotNull(s.createQuery( criteria ).uniqueResult());

//					assertNotNull( s.createCriteria( HousePlaces.class )
//										   .add( Restrictions.eq( "neighbourPlaces.livingRoom.owner", "his" ) ).uniqueResult() );
					s.delete( house );

				}
		);
	}

	@Override
	protected void configure(Configuration configuration) {
		super.configure( configuration );
		configuration.setImplicitNamingStrategy( ImplicitNamingStrategyLegacyJpaImpl.INSTANCE );
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
				HousePlaces.class
		};
	}
}
