/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.derivedidentities.e1.b.specjmapid.lazy;

import org.hibernate.orm.test.annotations.derivedidentities.e1.b.specjmapid.Item;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SessionFactory
@DomainModel(
		xmlMappings = "org/hibernate/orm/test/annotations/derivedidentities/e1/b/specjmapid/lazy/order_orm.xml",
		annotatedClasses = {
				CustomerTwo.class,
				CustomerInventoryTwo.class,
				CustomerInventoryTwoPK.class,
				Item.class
		}
)
public class CompositeKeyDeleteTest {

	/**
	 * This test checks to make sure the non null column is not updated with a
	 * null value when a CustomerInventory is removed.
	 */
	@Test
	public void testRemove(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					CustomerTwo c1 = new CustomerTwo(
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
				}
		);

		scope.inTransaction(
				session -> {
					CustomerTwo c12 = session.createQuery( "select c from CustomerTwo c", CustomerTwo.class )
							.uniqueResult();
					assertThat( c12 ).isNotNull();
					List<CustomerInventoryTwo> list = c12.getInventories();
					assertThat( list ).isNotNull();
					assertThat( list.size() ).isEqualTo( 2 );
					CustomerInventoryTwo ci = list.get( 1 );
					list.remove( ci );
					session.remove( ci );
					session.flush();
				}
		);
	}
}
