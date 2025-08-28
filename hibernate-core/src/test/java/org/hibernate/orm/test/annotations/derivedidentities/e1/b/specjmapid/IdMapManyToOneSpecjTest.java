/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.derivedidentities.e1.b.specjmapid;

import java.math.BigDecimal;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.Transaction;

import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
/**
 * A test.
 *
 * @author Stale W. Pedersen
 */
public class IdMapManyToOneSpecjTest extends BaseNonConfigCoreFunctionalTestCase {

	@Test
	public void testComplexIdClass() {
		Session s = openSession();
		Transaction tx = s.beginTransaction();

		Customer c1 = new Customer(
				"foo", "bar", "contact1", "100", new BigDecimal( 1000 ), new BigDecimal( 1000 ), new BigDecimal( 1000 )
		);

		s.persist( c1 );
		s.flush();
		s.clear();

		Item boat = new Item();
		boat.setId( "1" );
		boat.setName( "cruiser" );
		boat.setPrice( new BigDecimal( 500 ) );
		boat.setDescription( "a boat" );
		boat.setCategory( 42 );

		s.persist( boat );


		Item house = new Item();
		house.setId( "2" );
		house.setName( "blada" );
		house.setPrice( new BigDecimal( 5000 ) );
		house.setDescription( "a house" );
		house.setCategory( 74 );

		s.persist( house );
		s.flush();
		s.clear();

		c1.addInventory( boat, 10, new BigDecimal( 5000 ) );

		c1.addInventory( house, 100, new BigDecimal( 50000 ) );
		s.merge( c1 );
		tx.commit();


		tx = s.beginTransaction();
		Customer c12 = ( Customer ) s.createQuery( "select c from Customer c" ).uniqueResult();

		List<CustomerInventory> inventory = c12.getInventories();

		assertEquals( 2, inventory.size() );
		assertEquals( 10, inventory.get( 0 ).getQuantity() );
		assertEquals( "2", inventory.get(1).getVehicle().getId());


		Item house2 = new Item();
		house2.setId( "3" );
		house2.setName( "blada" );
		house2.setPrice( new BigDecimal( 5000 ) );
		house2.setDescription( "a house" );
		house2.setCategory( 74 );

		s.persist( house2 );
		s.flush();
		s.clear();

		c12.addInventory( house2, 200, new BigDecimal( 500000 ) );
		s.merge( c12 );

		s.flush();
		s.clear();

		Customer c13 = ( Customer ) s.createQuery( "select c from Customer c where c.id = " + c12.getId() )
				.uniqueResult();
		assertEquals( 3, c13.getInventories().size() );



		Customer customer2 = new Customer(
				"foo2", "bar2", "contact12", "1002", new BigDecimal( 10002 ), new BigDecimal( 10002 ), new BigDecimal( 1000 ));
		customer2.setId(2);
		s.persist(customer2);

		customer2.addInventory(boat, 10, new BigDecimal(400));
		customer2.addInventory(house2, 3, new BigDecimal(4000));
		s.merge(customer2);

		Customer c23 = ( Customer ) s.createQuery( "select c from Customer c where c.id = 2" ).uniqueResult();
		assertEquals( 2, c23.getInventories().size() );

		tx.rollback();
		s.close();
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				Customer.class,
				CustomerInventory.class,
				CustomerInventoryPK.class,
				Item.class

		};
	}
}
