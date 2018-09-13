/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id$

package org.hibernate.test.ast.tree;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;

import java.util.List;

import javax.persistence.EntityGraph;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.hibernate.testing.TestForIssue;
import org.junit.Test;

/**
 * @author zoller27osu
 */
public class QueryNodeTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class[]{
				ShipmentOrder.class,
				ShipmentOrderItem.class,
				Shipment.class
		};
	}

	@Test
	@TestForIssue(jiraKey = "HHH-12960")
	public void hhh12960Test() throws Exception {
		doInJPA( this::entityManagerFactory, em -> {
			Shipment shipment = new Shipment( 4 );
			em.persist( shipment );

			ShipmentOrder order1 = new ShipmentOrder( 1, shipment );
			ShipmentOrder order2 = new ShipmentOrder( 2, shipment );
			em.persist( order1 );
			em.persist( order2 );

			ShipmentOrderItem orderItem1_5 = new ShipmentOrderItem( 5, order1 );
			ShipmentOrderItem orderItem1_3 = new ShipmentOrderItem( 3, order1 );
			em.persist( orderItem1_5 );
			em.persist( orderItem1_3 );

			ShipmentOrderItem orderItem2_7 = new ShipmentOrderItem( 7, order2 );
			em.persist( orderItem2_7 );
		});

		doInJPA( this::entityManagerFactory, em -> {
			EntityGraph<ShipmentOrder> graph = em.createEntityGraph( ShipmentOrder.class );
			graph.addSubgraph( "orderItems", ShipmentOrderItem.class );

			String q = "SELECT o FROM ShipmentOrder o GROUP BY o.shipment.id";
			List<ShipmentOrder> orders = em.createQuery( q, ShipmentOrder.class ).getResultList();

			assertEquals( 1, orders.size() );
		});
	}
}
