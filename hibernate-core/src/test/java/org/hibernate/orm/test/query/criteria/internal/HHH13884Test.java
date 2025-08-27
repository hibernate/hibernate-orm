/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.criteria.internal;

import jakarta.persistence.criteria.Order;

import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.tree.select.SqmSortSpecification;

import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * @author seregamorph
 */
@JiraKey(value = "HHH-13884")
public class HHH13884Test {

	@Test
	public void testDefaultReversedOrderImpl() {
		SqmExpression<?> expression = mock( SqmExpression.class );

		SqmSortSpecification order = new SqmSortSpecification( expression );

		assertEquals( expression, order.getExpression() );
		assertTrue( "Order should be ascending by default", order.isAscending() );

		Order reversed = order.reverse();

		assertEquals( expression, reversed.getExpression() );
		assertFalse( "Reversed Order should be descending", reversed.isAscending() );

		assertNotSame( "Order.reverse() should create new instance by the contract", order, reversed );
	}
}
