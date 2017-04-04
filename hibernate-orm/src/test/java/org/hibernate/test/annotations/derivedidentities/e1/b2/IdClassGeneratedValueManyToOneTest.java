/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.derivedidentities.e1.b2;

import java.math.BigDecimal;
import java.util.List;

import org.junit.Test;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * A test.
 *
 * @author <a href="mailto:stale.pedersen@jboss.org">Stale W. Pedersen</a>
 */
public class IdClassGeneratedValueManyToOneTest extends BaseCoreFunctionalTestCase {
	@Test
	public void testComplexIdClass() {
		Session s = openSession();
		Transaction tx = s.beginTransaction();

		Customer c1 = new Customer(
				"foo", "bar", "contact1", "100", new BigDecimal( 1000 ), new BigDecimal( 1000 ), new BigDecimal( 1000 ));
		s.persist( c1 );
		s.flush();
        s.clear();
		
//      why does this cause a failure?        
//		Customer c2 = new Customer(
//              "foo1", "bar1", "contact2", "200", new BigDecimal( 2000 ), new BigDecimal( 2000 ), new BigDecimal( 2000 ));
//		s.persist( c2 );
//		s.flush();
//        s.clear();
		
		Item boat = new Item();
		boat.setId( "1" );
		boat.setName( "cruiser" );
		boat.setPrice( new BigDecimal( 500 ) );
		boat.setDescription( "a boat" );
		boat.setCategory( 42 );

		s.persist( boat );
		s.flush();
		s.clear();

		c1.addInventory( boat, 10, new BigDecimal( 5000 ) );
		s.merge( c1 );
		s.flush();
		s.clear();

		Customer c12 = ( Customer ) s.createQuery( "select c from Customer c" ).uniqueResult();

		List<CustomerInventory> inventory = c12.getInventories();

		assertEquals( 1, inventory.size() );
		assertEquals( 10, inventory.get( 0 ).getQuantity() );

		tx.rollback();
		s.close();

		assertTrue( true );
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
