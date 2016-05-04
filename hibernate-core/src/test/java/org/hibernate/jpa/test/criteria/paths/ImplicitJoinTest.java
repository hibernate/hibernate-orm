/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.criteria.paths;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Root;

import org.hibernate.jpa.test.metamodel.AbstractMetamodelSpecificTest;
import org.hibernate.jpa.test.metamodel.LineItem;
import org.hibernate.jpa.test.metamodel.LineItem_;
import org.hibernate.jpa.test.metamodel.Order;
import org.hibernate.jpa.test.metamodel.Order_;

import org.junit.Test;

/**
 * @author Steve Ebersole
 */
public class ImplicitJoinTest extends AbstractMetamodelSpecificTest {
	@Test
	public void testImplicitJoinFromExplicitCollectionJoin() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();

		CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
		CriteriaQuery<Order> criteria = criteriaBuilder.createQuery( Order.class );
		Root<Order> orderRoot = criteria.from( Order.class );
		Join<Order, LineItem> lineItemsJoin = orderRoot.join( Order_.lineItems );
		criteria.where( criteriaBuilder.lt( lineItemsJoin.get( LineItem_.quantity ), 2 ) );
		criteria.select( orderRoot ).distinct( true );
		TypedQuery<Order> query = em.createQuery( criteria );
		query.getResultList();

		em.getTransaction().commit();
		em.close();
	}
}
