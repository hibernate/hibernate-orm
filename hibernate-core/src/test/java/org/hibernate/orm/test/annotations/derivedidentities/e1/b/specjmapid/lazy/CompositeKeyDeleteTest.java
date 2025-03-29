/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.derivedidentities.e1.b.specjmapid.lazy;

import java.math.BigDecimal;
import java.util.List;

import junit.framework.Assert;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.orm.test.annotations.derivedidentities.e1.b.specjmapid.Item;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

public class CompositeKeyDeleteTest extends BaseCoreFunctionalTestCase {

@Override
protected String getBaseForMappings() {
	return "";
}

@Override
public String[] getMappings() {
	return new String[] { "org/hibernate/orm/test/annotations/derivedidentities/e1/b/specjmapid/lazy/order_orm.xml" };
}
/**
	* This test checks to make sure the non null column is not updated with a
	* null value when a CustomerInventory is removed.
	*/
@Test
public void testRemove() {
	Session s = openSession();
	Transaction tx = s.beginTransaction();

	CustomerTwo c1 = new CustomerTwo(
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
	Integer id = c1.getId();
	tx.commit();
	s.close();

	s = openSession();
	tx = s.beginTransaction();

	CustomerTwo c12 = ( CustomerTwo) s.createQuery( "select c from CustomerTwo c" ).uniqueResult();
	Assert.assertNotNull(c12);
	List<CustomerInventoryTwo> list = c12.getInventories();
	Assert.assertNotNull(list);
	Assert.assertEquals(2, list.size());
	CustomerInventoryTwo ci = list.get(1);
	list.remove(ci);
	s.remove(ci);
	s.flush();

	tx.commit();//fail
	s.close();

}

@Override
protected Class[] getAnnotatedClasses() {
	return new Class[] {
			CustomerTwo.class,
			CustomerInventoryTwo.class,
			CustomerInventoryTwoPK.class,
			Item.class

	};
}
}
