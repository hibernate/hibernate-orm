/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.mapping.ordering.ast;

import jakarta.persistence.criteria.Nulls;
import org.hibernate.query.SortDirection;
import org.hibernate.sql.ast.spi.creation.SqlAstCreationState;
import org.hibernate.sql.ast.spi.query.expression.Expression;
import org.hibernate.sql.ast.spi.query.expression.SelfRenderingSqlFragmentExpression;
import org.hibernate.sql.ast.spi.query.from.TableGroup;
import org.hibernate.sql.ast.spi.query.select.QuerySpec;
import org.hibernate.sql.ast.spi.query.select.SortSpecification;

/**
 * Represents a self rendering expression i.e. usually a literal used in an order-by fragment
 *
 * @apiNote This is Hibernate-specific feature.  For {@link jakarta.persistence.OrderBy} (JPA)
 * all path references are expected to be domain paths (attributes).
 *
 * @author Christian Beikov
 */
public class SelfRenderingOrderingExpression extends SelfRenderingSqlFragmentExpression implements OrderingExpression {

	public SelfRenderingOrderingExpression(String expression) {
		super( expression );
	}

	@Override
	public Expression resolve(
			QuerySpec ast,
			TableGroup tableGroup,
			String modelPartName,
			SqlAstCreationState creationState) {
		return this;
	}

	@Override
	public void apply(
			QuerySpec ast,
			TableGroup tableGroup,
			String collation,
			String modelPartName,
			SortDirection sortOrder,
			Nulls nullPrecedence,
			SqlAstCreationState creationState) {
		final Expression expression = resolve( ast, tableGroup, modelPartName, creationState );
		// It makes no sense to order by an expression multiple times
		// SQL Server even reports a query error in this case
		if ( ast.hasSortSpecifications() ) {
			for ( SortSpecification sortSpecification : ast.getSortSpecifications() ) {
				if ( sortSpecification.getSortExpression() == expression ) {
					return;
				}
			}
		}

		final Expression sortExpression =
				OrderingExpression.applyCollation( expression, collation, creationState );
		ast.addSortSpecification( new SortSpecification( sortExpression, sortOrder, nullPrecedence ) );
	}

	@Override
	public String toDescriptiveText() {
		return "unknown";
	}
}
