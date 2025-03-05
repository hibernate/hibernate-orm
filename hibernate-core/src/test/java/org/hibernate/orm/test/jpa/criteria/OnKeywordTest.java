/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.criteria;

import jakarta.persistence.criteria.CollectionJoin;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

import org.hibernate.orm.test.jpa.metamodel.LineItem;
import org.hibernate.orm.test.jpa.metamodel.LineItem_;
import org.hibernate.orm.test.jpa.metamodel.Order;
import org.hibernate.orm.test.jpa.metamodel.Order_;

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
