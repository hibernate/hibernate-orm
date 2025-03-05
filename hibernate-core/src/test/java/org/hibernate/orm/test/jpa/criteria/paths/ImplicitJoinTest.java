/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.criteria.paths;

import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Root;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;

import org.junit.jupiter.api.Test;

/**
 * @author Steve Ebersole
 */
@Jpa(annotatedClasses = {
		Order.class,
		LineItem.class
})
public class ImplicitJoinTest {

	@Test
	public void testImplicitJoinFromExplicitCollectionJoin(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
					CriteriaQuery<Order> criteria = criteriaBuilder.createQuery( Order.class );
					Root<Order> orderRoot = criteria.from( Order.class );
					Join<Order, LineItem> lineItemsJoin = orderRoot.join( Order_.lineItems );
					criteria.where( criteriaBuilder.lt( lineItemsJoin.get( LineItem_.quantity ), 2 ) );
					criteria.select( orderRoot ).distinct( true );
					TypedQuery<Order> query = entityManager.createQuery( criteria );
					query.getResultList();
				}
		);
	}
}
