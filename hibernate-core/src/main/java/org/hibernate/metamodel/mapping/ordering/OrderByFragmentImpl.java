/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.mapping.ordering;

import java.util.List;

import org.hibernate.metamodel.mapping.ordering.ast.OrderingSpecification;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.select.QuerySpec;

/**
 * @author Steve Ebersole
 */
public class OrderByFragmentImpl implements OrderByFragment {
	private final List<OrderingSpecification> fragmentSpecs;

	public OrderByFragmentImpl(List<OrderingSpecification> fragmentSpecs) {
		this.fragmentSpecs = fragmentSpecs;
	}

	public List<OrderingSpecification> getFragmentSpecs() {
		return fragmentSpecs;
	}

	@Override
	public void apply(QuerySpec ast, TableGroup tableGroup, SqlAstCreationState creationState) {
		for ( int i = 0; i < fragmentSpecs.size(); i++ ) {
			final var orderingSpec = fragmentSpecs.get( i );
			orderingSpec.getExpression().apply(
					ast,
					tableGroup,
					orderingSpec.getCollation(),
					orderingSpec.getOrderByValue(),
					orderingSpec.getSortOrder(),
					orderingSpec.getNullPrecedence(),
					creationState
			);
		}
	}
}
