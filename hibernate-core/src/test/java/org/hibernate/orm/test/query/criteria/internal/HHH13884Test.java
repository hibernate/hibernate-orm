/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.criteria.internal;

import jakarta.persistence.criteria.Order;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.tree.select.SqmSortSpecification;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * @author seregamorph
 */
@JiraKey(value = "HHH-13884")
@BaseUnitTest
public class HHH13884Test {

	@Test
	public void testDefaultReversedOrderImpl() {
		SqmExpression<?> expression = mock( SqmExpression.class );

		SqmSortSpecification order = new SqmSortSpecification( expression );

		assertThat( order.getExpression() ).isEqualTo( expression );
		assertThat( order.isAscending() )
				.describedAs( "Order should be ascending by default" )
				.isTrue();

		Order reversed = order.reverse();

		assertThat( reversed.getExpression() ).isEqualTo( expression );
		assertThat( reversed.isAscending() )
				.describedAs( "Reversed Order should be descending" )
				.isFalse();

		assertThat( reversed )
				.describedAs( "Order.reverse() should create new instance by the contract" )
				.isNotSameAs( order );
	}
}
