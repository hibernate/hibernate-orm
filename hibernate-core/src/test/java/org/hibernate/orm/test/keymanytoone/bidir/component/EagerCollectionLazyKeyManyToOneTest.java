/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.keymanytoone.bidir.component;

import java.util.List;

import org.hibernate.stat.spi.StatisticsImplementor;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Steve Ebersole
 */
@DomainModel(xmlMappings = {
		"org/hibernate/orm/test/keymanytoone/bidir/component/EagerCollectionLazyKeyManyToOneMapping.hbm.xml"
})
@SessionFactory(generateStatistics = true)
public class EagerCollectionLazyKeyManyToOneTest {

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testQueryingOnMany2One(SessionFactoryScope scope) {
		Customer cust = new Customer( "Acme, Inc." );
		Order order = new Order( new Order.Id( cust, 1 ) );
		cust.getOrders().add( order );

		scope.inTransaction(
				session -> {
					session.persist( cust );
				}
		);

		scope.inTransaction(
				session -> {
					List results = session.createQuery( "from Order o where o.id.customer.name = :name" )
							.setParameter( "name", cust.getName() )
							.list();
					assertEquals( 1, results.size() );
				}
		);

	}

	@Test
	public void testSaveCascadedToKeyManyToOne(SessionFactoryScope scope) {
		// test cascading a save to an association with a key-many-to-one which refers to a
		// just saved entity
		StatisticsImplementor statistics = scope.getSessionFactory().getStatistics();
		scope.inTransaction(
				session -> {
					Customer cust = new Customer( "Acme, Inc." );
					Order order = new Order( new Order.Id( cust, 1 ) );
					cust.getOrders().add( order );
					statistics.clear();
					session.persist( cust );
					session.flush();

					assertEquals( 2, statistics.getEntityInsertCount() );
				}
		);
	}

	@Test
	public void testLoadingStrategies(SessionFactoryScope scope) {

		Customer customer = new Customer( "Acme, Inc." );
		Order order = new Order( new Order.Id( customer, 1 ) );
		customer.getOrders().add( order );

		scope.inTransaction(
				session -> session.persist( customer )
		);

		scope.inTransaction(
				session -> {
					Customer cust = session.get( Customer.class, customer.getId() );
					assertEquals( 1, cust.getOrders().size() );
					session.clear();

					cust = (Customer) session.createQuery( "from Customer" ).uniqueResult();
					assertEquals( 1, cust.getOrders().size() );
					session.clear();

					cust = (Customer) session.createQuery( "from Customer c join fetch c.orders" ).uniqueResult();
					assertEquals( 1, cust.getOrders().size() );
					session.clear();
				}
		);
	}
}
