/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.annotations.derivedidentities.e1.b2;

import java.math.BigDecimal;
import java.util.List;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A test.
 *
 * @author <a href="mailto:stale.pedersen@jboss.org">Stale W. Pedersen</a>
 */
@DomainModel(
		annotatedClasses = {
				Customer.class,
				CustomerInventory.class,
				CustomerInventoryPK.class,
				Item.class

		}
)
@SessionFactory
public class IdClassGeneratedValueManyToOneTest {

	@AfterEach
	public void tearDown(SessionFactoryScope scope){
		scope.inTransaction(
				session -> {
					session.createQuery( "delete from CustomerInventory" ).executeUpdate();
					session.createQuery( "delete from Item" ).executeUpdate();
					session.createQuery( "delete from Customer" ).executeUpdate();
				}
		);
	}

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
					session.flush();
					session.clear();

					c1.addInventory( boat, 10, new BigDecimal( 5000 ) );
					session.merge( c1 );
					session.flush();
					session.clear();

					Customer c12 = (Customer) session.createQuery( "select c from Customer c" ).uniqueResult();

					List<CustomerInventory> inventory = c12.getInventories();

					assertEquals( 1, inventory.size() );
					assertEquals( 10, inventory.get( 0 ).getQuantity() );
				}
		);
	}

	@Test
	public void testNullEmbedded(SessionFactoryScope scope) {
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

					final Customer customer = session.find( Customer.class, c1.getId() );

					assertTrue( customer.getInventories().isEmpty() );
				}
		);
	}
}
