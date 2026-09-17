/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.mapping.ordering;

import org.hibernate.metamodel.mapping.ordering.spi.OrderByFragment;

import jakarta.persistence.criteria.Nulls;
import org.hibernate.query.SortDirection;
import org.hibernate.sql.ast.spi.creation.SqlAstCreationState;
import org.hibernate.sql.ast.spi.query.expression.SelfRenderingSqlFragmentExpression;
import org.hibernate.sql.ast.spi.query.from.TableGroup;
import org.hibernate.sql.ast.spi.query.select.QuerySpec;
import org.hibernate.sql.ast.spi.query.select.SortSpecification;

/**
 * Applies a native SQL order-by fragment supplied by {@link org.hibernate.annotations.SQLOrder}.
 */
public class SqlOrderByFragment implements OrderByFragment {
	private final String fragment;

	public SqlOrderByFragment(String fragment) {
		this.fragment = fragment;
	}

	@Override
	public void apply(QuerySpec ast, TableGroup tableGroup, SqlAstCreationState creationState) {
		ast.addSortSpecification(
				new SortSpecification(
						new SelfRenderingSqlFragmentExpression( fragment ),
						SortDirection.ASCENDING,
						Nulls.NONE
				)
		);
	}
}
