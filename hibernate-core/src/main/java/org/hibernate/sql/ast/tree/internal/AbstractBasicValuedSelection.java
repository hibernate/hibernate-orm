/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.internal;

import org.hibernate.persister.common.BasicValuedNavigable;
import org.hibernate.persister.queryable.spi.BasicValuedExpressableType;
import org.hibernate.sql.ast.produce.result.internal.QueryResultScalarImpl;
import org.hibernate.sql.ast.produce.result.spi.QueryResultGenerator;
import org.hibernate.sql.ast.tree.spi.expression.Expression;
import org.hibernate.sql.ast.tree.spi.expression.domain.NavigableReferenceExpression;
import org.hibernate.sql.ast.tree.spi.select.SqlSelectable;

/**
 * @author Steve Ebersole
 */
public class AbstractBasicValuedSelection extends AbstractSelection {
	private final QueryResultGenerator queryResultGenerator;

	public AbstractBasicValuedSelection(
			Expression selectedExpression,
			String resultVariable) {
		this(
				selectedExpression,
				resultVariable,
				(columnReferenceResolver, sqlSelectionResolver, creationContext) -> {
					final SqlSelectable sqlSelectable;
					if ( selectedExpression instanceof NavigableReferenceExpression ) {
						final NavigableReferenceExpression navigableReference = (NavigableReferenceExpression) selectedExpression;
						final BasicValuedNavigable navigable = (BasicValuedNavigable) navigableReference.getNavigable();
						sqlSelectable = columnReferenceResolver.resolveColumnReference( navigable.getBoundColumn() );
					}
					else {
						assert selectedExpression instanceof SqlSelectable;
						sqlSelectable = (SqlSelectable) selectedExpression;
					}

					return new QueryResultScalarImpl(
							selectedExpression,
							sqlSelectionResolver.resolveSqlSelection( sqlSelectable ),
							resultVariable,
							(BasicValuedExpressableType) selectedExpression.getType()
					);
				}
		);
	}

	public AbstractBasicValuedSelection(
			Expression selectedExpression,
			String resultVariable,
			QueryResultGenerator queryResultGenerator) {
		super( selectedExpression, resultVariable );
		this.queryResultGenerator = queryResultGenerator;;
	}

	@Override
	protected QueryResultGenerator getQueryResultGenerator() {
		return queryResultGenerator;
	}

}
