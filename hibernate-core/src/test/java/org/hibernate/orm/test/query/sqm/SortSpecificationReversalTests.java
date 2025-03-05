/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.sqm;

import org.hibernate.query.criteria.JpaOrder;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.tree.select.SqmSortSpecification;

import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.Test;

import static jakarta.persistence.criteria.Nulls.FIRST;
import static org.hibernate.query.SortDirection.ASCENDING;
import static org.hibernate.query.SortDirection.DESCENDING;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.mockito.Mockito.mock;

/**
 * @author seregamorph
 */
@JiraKey(value = "HHH-13884")
public class SortSpecificationReversalTests {

	@Test
	public void testDefaultSqmSortSpecificationReverse() {
		SqmExpression sortExpression = mock( SqmExpression.class );

		SqmSortSpecification order = new SqmSortSpecification( sortExpression, ASCENDING, FIRST );

		assertEquals( sortExpression, order.getSortExpression() );
		assertEquals( ASCENDING, order.getSortDirection() );
		assertEquals( FIRST, order.getNullPrecedence() );

		JpaOrder reversed = order.reverse();

		assertEquals( DESCENDING, reversed.getSortDirection() );
		assertEquals( FIRST, reversed.getNullPrecedence() );

		assertNotSame( "Order.reverse() should create new instance", order, reversed );
	}
}
