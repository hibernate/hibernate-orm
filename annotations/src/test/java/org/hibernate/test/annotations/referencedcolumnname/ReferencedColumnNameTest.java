//$Id$
package org.hibernate.test.annotations.referencedcolumnname;

import java.util.Iterator;
import java.math.BigDecimal;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.test.annotations.TestCase;

/**
 * @author Emmanuel Bernard
 */
public class ReferencedColumnNameTest extends TestCase {
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

	public ReferencedColumnNameTest(String x) {
		super( x );
	}

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
				WarehouseItem.class
		};
	}
}
