/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.tooling.modelgen;

import java.math.BigDecimal;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

import org.hibernate.testing.orm.domain.userguide.tooling.Customer;
import org.hibernate.testing.orm.domain.userguide.tooling.Item;
import org.hibernate.testing.orm.domain.userguide.tooling.Order;
import org.hibernate.testing.orm.domain.userguide.tooling.Order_;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

/**
 * @author Steve Ebersole
 */
@DomainModel( annotatedClasses = { Order.class, Item.class, Customer.class } )
@SessionFactory
public class ModelGenTests {
	@Test
	public void testIt(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			//tag::tooling-modelgen-usage[]
			final CriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();
			final CriteriaQuery<Customer> criteria = criteriaBuilder.createQuery( Customer.class );

			final Root<Order> root = criteria.from( Order.class );

			criteria.select( root.get( Order_.customer ) );
			criteria.where( criteriaBuilder.greaterThan( root.get( Order_.totalCost ), new BigDecimal( 100 ) ) );
			//end::tooling-modelgen-usage[]
		} );
	}
}
