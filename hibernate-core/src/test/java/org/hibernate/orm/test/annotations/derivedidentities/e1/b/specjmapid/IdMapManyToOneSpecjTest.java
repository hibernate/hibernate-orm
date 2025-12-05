/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.derivedidentities.e1.b.specjmapid;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

/**
 * A test.
 *
 * @author Stale W. Pedersen
 */
@SessionFactory
@DomainModel(
		annotatedClasses = {
				Customer.class,
				CustomerInventory.class,
				CustomerInventoryPK.class,
				Item.class
		}
)
public class IdMapManyToOneSpecjTest {

	@Test
	public void testComplexIdClass(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Customer c1 = new Customer(
							"foo",
							"bar",
							"contact1",
							"100",
							new BigDecimal( 1000 ),
							new BigDecimal( 1000 ),
							new BigDecimal( 1000 )
					);

					session.persist( c1 );
					session.flush();
					session.clear();

					Item boat = new Item();
					boat.setId( "1" );
					boat.setName( "cruiser" );
					boat.setPrice( new BigDecimal( 500 ) );
					boat.setDescription( "a boat" );
					boat.setCategory( 42 );

					session.persist( boat );

					Item house = new Item();
					house.setId( "2" );
					house.setName( "blada" );
					house.setPrice( new BigDecimal( 5000 ) );
					house.setDescription( "a house" );
					house.setCategory( 74 );

					session.persist( house );
					session.flush();
					session.clear();

					c1.addInventory( boat, 10, new BigDecimal( 5000 ) );

					c1.addInventory( house, 100, new BigDecimal( 50000 ) );
					session.merge( c1 );
					session.getTransaction().commit();

					session.beginTransaction();
					Customer c12 = session.createSelectionQuery( "select c from Customer c", Customer.class )
							.uniqueResult();

					List<CustomerInventory> inventory = c12.getInventories();

					assertThat( inventory.size() ).isEqualTo( 2 );
					assertThat( inventory.get( 0 ).getQuantity() ).isEqualTo( 10 );
					assertThat( inventory.get( 1 ).getVehicle().getId() ).isEqualTo( "2" );

					Item house2 = new Item();
					house2.setId( "3" );
					house2.setName( "blada" );
					house2.setPrice( new BigDecimal( 5000 ) );
					house2.setDescription( "a house" );
					house2.setCategory( 74 );

					session.persist( house2 );
					session.flush();
					session.clear();

					c12.addInventory( house2, 200, new BigDecimal( 500000 ) );
					session.merge( c12 );

					session.flush();
					session.clear();

					Customer c13 = session
							.createSelectionQuery( "select c from Customer c where c.id = " + c12.getId(), Customer.class )
							.uniqueResult();
					assertThat( c13.getInventories().size() ).isEqualTo( 3 );

					Customer customer2 = new Customer(
							"foo2",
							"bar2",
							"contact12",
							"1002",
							new BigDecimal( 10002 ),
							new BigDecimal( 10002 ),
							new BigDecimal( 1000 )
					);
					customer2.setId( 2 );
					session.persist( customer2 );

					customer2.addInventory( boat, 10, new BigDecimal( 400 ) );
					customer2.addInventory( house2, 3, new BigDecimal( 4000 ) );
					session.merge( customer2 );

					Customer c23 = session.createQuery( "select c from Customer c where c.id = 2", Customer.class )
							.uniqueResult();
					assertThat( c23.getInventories().size() ).isEqualTo( 2 );
				}
		);
	}
}
