/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.collection.basic;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import org.hibernate.Hibernate;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DomainModel(
		annotatedClasses = {
				CollectionOneToManySizeTest.Order.class, CollectionOneToManySizeTest.OrderItem.class
		}
)
@SessionFactory
public class CollectionOneToManySizeTest {

	@Test
	@JiraKey("HHH-3319")
	public void prepareData(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Order o = new Order();
					o.id = 1L;
					o.orderItems = new HashSet<>();
					OrderItem oi1 = new OrderItem();
					oi1.id = 1L;
					oi1.order = o;
					OrderItem oi2 = new OrderItem();
					oi2.id = 2L;
					oi2.order = o;
					oi2.status = 1;

					o.orderItems.add( oi1 );
					o.orderItems.add( oi2 );

					session.persist( o );
					session.persist( oi1 );
					session.persist( oi2 );
					session.flush();
				}
		);
		scope.inTransaction(
				session -> {
					Order o = session.find( Order.class, 1L );

					Set<OrderItem> orderItems = o.orderItems;
					assertEquals( 1, Hibernate.size( orderItems ) );
					assertTrue( Hibernate.contains( orderItems, session.getReference( OrderItem.class, 1L ) ) );
					assertFalse( Hibernate.contains( orderItems, session.getReference( OrderItem.class, 2L ) ) );
					assertFalse( Hibernate.isInitialized( orderItems ) );
				}
		);
	}

	@Entity
	public static class Order {
		@Id
		Long id;
		@OneToMany(mappedBy = "order")
		@SQLRestriction("status=0")
		Set<OrderItem> orderItems;
	}

	@Entity
	public static class OrderItem {
		@Id
		Long id;
		@ManyToOne(fetch = FetchType.LAZY)
		Order order;
		int status;
	}

}
