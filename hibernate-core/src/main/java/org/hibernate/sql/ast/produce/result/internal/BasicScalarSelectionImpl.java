/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.produce.result.internal;

import org.hibernate.persister.queryable.spi.BasicValuedExpressableType;
import org.hibernate.sql.ast.produce.result.spi.AbstractSelection;
import org.hibernate.sql.ast.produce.result.spi.QueryResultGenerator;
import org.hibernate.sql.ast.tree.spi.expression.Expression;
import org.hibernate.sql.ast.tree.spi.select.SqlSelectable;

/**
 * @author Steve Ebersole
 */
public class BasicScalarSelectionImpl extends AbstractSelection {
	public BasicScalarSelectionImpl(
			Expression selectedExpression,
			String resultVariable,
			QueryResultGenerator queryResultGenerator) {
		super( selectedExpression, resultVariable, queryResultGenerator );
	}

	public BasicScalarSelectionImpl(
			Expression selectedExpression,
			String resultVariable,
			SqlSelectable sqlSelectable) {
		super(
				selectedExpression,
				resultVariable,
				sqlSelectionResolver -> new QueryResultScalarImpl(
						selectedExpression,
						sqlSelectionResolver.resolveSqlSelection( sqlSelectable ),
						resultVariable,
						(BasicValuedExpressableType) selectedExpression.getType()
				)
		);
	}
}
