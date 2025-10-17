/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.idclass;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.hibernate.cfg.MappingSettings.JPA_METAMODEL_POPULATION;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Gavin King
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@DomainModel(xmlMappings = "/org/hibernate/orm/test/idclass/Customer.hbm.xml")
@ServiceRegistry(settings = @Setting(name=JPA_METAMODEL_POPULATION, value = "disabled"))
@SessionFactory
public class IdClassHbmTest {
	@AfterEach
	void dropTestData(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	@Test
	public void testIdClass(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (s) -> {
			var customer = new FavoriteCustomer("JBoss", "RouteOne", "Detroit");
			s.persist(customer);
		} );

		var customerId = new CustomerId("JBoss", "RouteOne");

		factoryScope.inTransaction( (s) -> {
			var customer = s.find( Customer.class, customerId );
			assertEquals( "Detroit", customer.getAddress() );
			assertEquals( customerId.getCustomerName(), customer.getCustomerName() );
			assertEquals( customerId.getOrgName(), customer.getOrgName() );
		} );

		factoryScope.inTransaction( (s) -> {
			var customer = s.createQuery("from Customer where id.customerName = 'RouteOne'", Customer.class).uniqueResult();
			assertEquals( "Detroit", customer.getAddress() );
			assertEquals( customerId.getCustomerName(), customer.getCustomerName() );
			assertEquals( customerId.getOrgName(), customer.getOrgName() );
		} );

		factoryScope.inTransaction( (s) -> {
			var customer = s.createQuery("from Customer where customerName = 'RouteOne'", Customer.class).uniqueResult();
			assertEquals( "Detroit", customer.getAddress() );
			assertEquals( customerId.getCustomerName(), customer.getCustomerName() );
			assertEquals( customerId.getOrgName(), customer.getOrgName() );
		} );
	}

}
