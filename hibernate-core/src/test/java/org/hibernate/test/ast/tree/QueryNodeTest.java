/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id$

package org.hibernate.test.ast.tree;

import java.util.List;

import javax.persistence.EntityGraph;
import javax.persistence.EntityManager;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.hibernate.testing.TestForIssue;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

/**
 * @author zoller27osu
 */
public class QueryNodeTest extends BaseEntityManagerFunctionalTestCase {
	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class[] {
			Order.class,
			OrderItem.class,
			Shipment.class
		};
	}

    @Test
    @TestForIssue( jiraKey = "HHH-12960" )
    public void hhh12960Test() throws Exception {
        EntityManager entityManager = getOrCreateEntityManager();
        entityManager.getTransaction().begin();
        
        Shipment shipment = new Shipment( 4 );
        entityManager.persist( shipment );
        
        Order order1 = new Order( 1, shipment );
        Order order2 = new Order( 2, shipment );
        entityManager.persist( order1 );
        entityManager.persist( order2 );
        
        OrderItem orderItem1_5 = new OrderItem( 5, order1 );
        OrderItem orderItem1_3 = new OrderItem( 3, order1 );
        entityManager.persist( orderItem1_5 );
        entityManager.persist( orderItem1_3 );
        
        OrderItem orderItem2_7 = new OrderItem( 7, order2 );
        entityManager.persist( orderItem2_7 );

        EntityGraph<Order> graph = entityManager.createEntityGraph( Order.class );
        graph.addSubgraph( "orderItems", OrderItem.class );

        entityManager.getTransaction().commit();
        
        String q = "SELECT o FROM Order o GROUP BY o.shipment.id";
        entityManager.getTransaction().begin();
        List<Order> orders = entityManager.createQuery(q, Order.class).getResultList();
        entityManager.getTransaction().commit();
        assertEquals(1, orders.size());

        entityManager.getTransaction().commit();
        entityManager.close();
    }
}
