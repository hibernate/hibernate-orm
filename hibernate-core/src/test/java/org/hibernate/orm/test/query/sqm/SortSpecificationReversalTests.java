/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.sqm;

import org.hibernate.query.criteria.JpaOrder;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.tree.select.SqmSortSpecification;
import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.jupiter.api.Test;

import static jakarta.persistence.criteria.Nulls.FIRST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.query.SortDirection.ASCENDING;
import static org.hibernate.query.SortDirection.DESCENDING;
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

		assertThat( order.getSortExpression() ).isEqualTo( sortExpression );
		assertThat( order.getSortDirection() ).isEqualTo( ASCENDING );
		assertThat( order.getNullPrecedence() ).isEqualTo( FIRST );

		JpaOrder reversed = order.reverse();

		assertThat( reversed.getSortDirection() ).isEqualTo( DESCENDING );
		assertThat( reversed.getNullPrecedence() ).isEqualTo( FIRST );

		assertThat( reversed )
				.describedAs( "Order.reverse() should create new instance" )
				.isNotSameAs( order );
	}
}
