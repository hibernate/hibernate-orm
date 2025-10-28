/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhancement.association;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Luis Barreiro
 */
@BytecodeEnhanced
public class OneToManyAssociationTest {

	@Test
	public void test() {
		Customer customer = new Customer();
		assertTrue( customer.getInventories().isEmpty() );

		CustomerInventory customerInventory = new CustomerInventory();
		customerInventory.setCustomer( customer );

		assertEquals( 1, customer.getInventories().size() );
		assertTrue( customer.getInventories().contains( customerInventory ) );

		Customer anotherCustomer = new Customer();
		assertTrue( anotherCustomer.getInventories().isEmpty() );
		customerInventory.setCustomer( anotherCustomer );

		assertTrue( customer.getInventories().isEmpty() );
		assertEquals( 1, anotherCustomer.getInventories().size() );
		assertSame( customerInventory, anotherCustomer.getInventories().get( 0 ) );

		customer.addInventory( customerInventory );

		assertSame( customer, customerInventory.getCustomer() );
		assertTrue( anotherCustomer.getInventories().isEmpty() );
		assertEquals( 1, customer.getInventories().size() );

		customer.addInventory( new CustomerInventory() );
		assertEquals( 2, customer.getInventories().size() );

		// Test remove
		customer.removeInventory( customerInventory );
		assertEquals( 1, customer.getInventories().size() );

		// This happens (and is expected) because there was no snapshot taken before remove
		assertNotNull( customerInventory.getCustomer() );
	}

	// --- //

	@Entity
	private static class Customer {

		@Id
		Long id;

		String name;

		// HHH-13446 - Type not validated in bidirectional association mapping
		@OneToMany(cascade = CascadeType.ALL, mappedBy = "custId", fetch = FetchType.EAGER)
		List<CustomerInventory> inventoryIdList = new ArrayList<>();

		@OneToMany( mappedBy = "customer", cascade = CascadeType.ALL, fetch = FetchType.EAGER )
		List<CustomerInventory> customerInventories = new ArrayList<>();

		void addInventory(CustomerInventory inventory) {
			List<CustomerInventory> list = customerInventories;
			list.add( inventory );
			customerInventories = list;
		}

		List<CustomerInventory> getInventories() {
			return Collections.unmodifiableList( customerInventories );
		}

		void removeInventory(CustomerInventory inventory) {
			customerInventories.remove( inventory );
		}
	}

	@Entity
	private static class CustomerInventory {

		@Id
		Long id;

		@Id
		Long custId;

		@ManyToOne( cascade = CascadeType.MERGE )
		Customer customer;

		@ManyToOne( cascade = CascadeType.MERGE )
		String vehicle;

		Customer getCustomer() {
			return customer;
		}

		void setCustomer(Customer customer) {
			this.customer = customer;
		}

		void setVehicle(String vehicle) {
			this.vehicle = vehicle;
		}
	}
}
