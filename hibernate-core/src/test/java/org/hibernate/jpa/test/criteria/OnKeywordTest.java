/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.criteria;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CollectionJoin;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.hibernate.jpa.test.metamodel.AbstractMetamodelSpecificTest;
import org.hibernate.jpa.test.metamodel.LineItem;
import org.hibernate.jpa.test.metamodel.LineItem_;
import org.hibernate.jpa.test.metamodel.Order;
import org.hibernate.jpa.test.metamodel.Order_;

import org.junit.Test;

/**
 * Similar to {@link org.hibernate.test.jpa.ql.OnKeywordTest}, but here testing from JPA criteria queries.
 *
 * @author Steve Ebersole
 */
public class OnKeywordTest extends AbstractMetamodelSpecificTest {
	@Test
	public void basicTest() {
		EntityManager em = getOrCreateEntityManager();
		CriteriaQuery<Order> criteria = em.getCriteriaBuilder().createQuery( Order.class );
		Root<Order> root = criteria.from( Order.class );
		criteria.select( root );
		CollectionJoin<Order,LineItem> lineItemsJoin = root.join( Order_.lineItems );
		lineItemsJoin.on(
				em.getCriteriaBuilder().gt(
						lineItemsJoin.get( LineItem_.quantity ),
						em.getCriteriaBuilder().literal( 20 )
				)
		);
		em.createQuery( criteria ).getResultList();
		em.close();
	}
}
