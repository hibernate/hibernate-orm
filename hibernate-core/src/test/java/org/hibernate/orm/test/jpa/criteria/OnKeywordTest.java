/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.jpa.criteria;

import jakarta.persistence.criteria.CollectionJoin;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

import org.hibernate.jpa.test.metamodel.LineItem;
import org.hibernate.jpa.test.metamodel.LineItem_;
import org.hibernate.jpa.test.metamodel.Order;
import org.hibernate.jpa.test.metamodel.Order_;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.junit.jupiter.api.Test;

/**
 * Similar to {@link org.hibernate.orm.test.query.hql.OnKeywordTest}, but here testing from JPA criteria queries.
 *
 * @author Steve Ebersole
 */
public class OnKeywordTest extends AbstractCriteriaTest {
	@Test
	public void basicTest(EntityManagerFactoryScope scope) {

		scope.inEntityManager(
				entityManager -> {
					CriteriaQuery<Order> criteria = entityManager.getCriteriaBuilder().createQuery( Order.class );
					Root<Order> root = criteria.from( Order.class );
					criteria.select( root );
					CollectionJoin<Order, LineItem> lineItemsJoin = root.join( Order_.lineItems );
					lineItemsJoin.on(
							entityManager.getCriteriaBuilder().gt(
									lineItemsJoin.get( LineItem_.quantity ),
									entityManager.getCriteriaBuilder().literal( 20 )
							)
					);
					entityManager.createQuery( criteria ).getResultList();
				}
		);
	}
}
