/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.typedmanytoone;

import java.util.List;

import org.hibernate.Hibernate;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Gavin King
 */
@DomainModel( xmlMappings = "/org/hibernate/orm/test/mapping/typedmanytoone/Customer.hbm.xml")
@SessionFactory
public class TypedManyToOneTest {
	@Test
	public void testCreateQuery(SessionFactoryScope scope) {
		final Customer cust = new Customer();
		cust.setCustomerId("abc123");
		cust.setName("Matt");

		Address ship = new Address();
		ship.setStreet("peachtree rd");
		ship.setState("GA");
		ship.setCity("ATL");
		ship.setZip("30326");
		ship.setAddressId( new AddressId("SHIPPING", "xyz123") );
		ship.setCustomer(cust);

		Address bill = new Address();
		bill.setStreet("peachtree rd");
		bill.setState("GA");
		bill.setCity("ATL");
		bill.setZip("30326");
		bill.setAddressId( new AddressId("BILLING", "xyz123") );
		bill.setCustomer(cust);

		cust.setBillingAddress(bill);
		cust.setShippingAddress(ship);

		scope.inTransaction( (s) -> {
			s.persist(cust);
		} );

		scope.inTransaction( (s) -> {
			List<Customer> results = s.createQuery("from Customer cust left join fetch cust.billingAddress where cust.customerId='abc123'", Customer.class).list();
			Customer loaded = results.get(0);
			assertFalse( Hibernate.isInitialized( loaded.getShippingAddress() ) );
			assertTrue( Hibernate.isInitialized( loaded.getBillingAddress() ) );
			assertEquals( "30326", loaded.getBillingAddress().getZip() );
			assertEquals( "30326", loaded.getShippingAddress().getZip() );
			assertEquals( "BILLING", loaded.getBillingAddress().getAddressId().getType() );
			assertEquals( "SHIPPING", loaded.getShippingAddress().getAddressId().getType() );
		} );

		scope.inTransaction( (s) -> {
			final Customer merged = s.merge( cust );
			Address shipAddress = merged.getShippingAddress();
			merged.setShippingAddress(null);
			s.remove( shipAddress );
			s.flush();
			assertNull( s.find( "ShippingAddress", shipAddress.getAddressId() ) );
			s.remove( merged );
		} );
	}

	@Test
	public void testCreateQueryNull(SessionFactoryScope scope) {
		final Customer saved = scope.fromTransaction( (s) -> {
			Customer cust = new Customer();
			cust.setCustomerId("xyz123");
			cust.setName("Matt");
			s.persist(cust);

			return cust;
		} );

		scope.inTransaction( (s) -> {
			List<Customer> results = s.createQuery("from Customer cust left join fetch cust.billingAddress where cust.customerId='xyz123'", Customer.class ).list();
			//List results = s.createQuery("from Customer cust left join fetch cust.billingAddress left join fetch cust.shippingAddress").list();
			Customer loaded = results.get(0);
			assertNull( loaded.getShippingAddress() );
			assertNull( loaded.getBillingAddress() );
			s.remove( loaded );
		} );
	}

}
