/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.bytecode.enhancement.association;

import java.util.List;

import org.hibernate.test.bytecode.enhancement.AbstractEnhancerTestTask;
import org.junit.Assert;

/**
 * @author Luis Barreiro
 */
public class OneToManyAssociationTestTask extends AbstractEnhancerTestTask {

	public Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {Customer.class, CustomerInventory.class, Group.class, User.class};
	}

	public void prepare() {
	}

	public void execute() {
		Customer customer = new Customer();
		Assert.assertTrue( customer.getInventories().isEmpty() );

		CustomerInventory customerInventory = new CustomerInventory();
		customerInventory.setCustomer( customer );

		Assert.assertTrue( customer.getInventories().size() == 1 );
		Assert.assertTrue( customer.getInventories().contains( customerInventory ) );

		Customer anotherCustomer = new Customer();
		Assert.assertTrue( anotherCustomer.getInventories().isEmpty() );
		customerInventory.setCustomer( anotherCustomer );

		Assert.assertTrue( customer.getInventories().isEmpty() );
		Assert.assertTrue( anotherCustomer.getInventories().size() == 1 );
		Assert.assertTrue( anotherCustomer.getInventories().get( 0 ) == customerInventory );

		customer.addInventory( customerInventory );

		Assert.assertTrue( customerInventory.getCustomer() == customer );
		Assert.assertTrue( anotherCustomer.getInventories().isEmpty() );
		Assert.assertTrue( customer.getInventories().size() == 1 );

		customer.addInventory( new CustomerInventory() );
		Assert.assertTrue( customer.getInventories().size() == 2 );

		// Test remove

		List<CustomerInventory> inventories = customer.getInventories();
		inventories.remove( customerInventory );
		customer.setInventories( inventories );

		// This happens (and is expected) because there was no snapshot taken before remove
		Assert.assertNotNull( customerInventory.getCustomer() );
	}

	protected void cleanup() {
	}
}
