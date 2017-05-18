/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.internal;

import org.hibernate.sql.ast.produce.metamodel.spi.BasicValuedExpressableType;
import org.hibernate.sql.ast.produce.result.internal.QueryResultScalarImpl;
import org.hibernate.sql.ast.produce.result.spi.QueryResultGenerator;
import org.hibernate.sql.ast.tree.spi.expression.Expression;
import org.hibernate.sql.ast.tree.spi.select.SqlSelectable;

/**
 * A non-Navigable basic scalar selection (a function, a literal, etc)
 *
 * @author Steve Ebersole
 */
public class BasicValuedNonNavigableSelection extends NonNavigableSelectionSupport {
	private final QueryResultGenerator queryResultGenerator;

	public BasicValuedNonNavigableSelection(
			Expression selectedExpression,
			String resultVariable,
			SqlSelectable sqlSelectable) {
		this(
				selectedExpression,
				resultVariable,
				(sqlSelectionResolver, creationContext) -> new QueryResultScalarImpl(
						selectedExpression,
						sqlSelectionResolver.resolveSqlSelection( sqlSelectable ),
						resultVariable,
						(BasicValuedExpressableType) selectedExpression.getType()
				)
		);
	}

	public BasicValuedNonNavigableSelection(
			Expression selectedExpression,
			String resultVariable,
			QueryResultGenerator queryResultGenerator) {
		super( selectedExpression, resultVariable );
		this.queryResultGenerator = queryResultGenerator;
	}

	@Override
	protected QueryResultGenerator getQueryResultGenerator() {
		return queryResultGenerator;
	}
}
