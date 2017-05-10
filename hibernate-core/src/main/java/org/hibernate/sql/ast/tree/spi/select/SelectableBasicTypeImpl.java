/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.sql.ast.tree.spi.select;

import org.hibernate.sql.ast.tree.spi.expression.Expression;
import org.hibernate.sql.ast.produce.result.internal.ReturnScalarImpl;
import org.hibernate.sql.ast.produce.result.spi.Return;
import org.hibernate.sql.ast.produce.result.spi.QueryResultCreationContext;
import org.hibernate.type.spi.BasicType;

/**
 * @author Steve Ebersole
 */
public class SelectableBasicTypeImpl implements Selectable {
	private final Expression expression;
	private final SqlSelectable sqlSelectable;
	private final BasicType type;

	public SelectableBasicTypeImpl(
			Expression expression,
			SqlSelectable sqlSelectable,
			BasicType type) {
		this.expression = expression;
		this.sqlSelectable = sqlSelectable;
		this.type = type;
	}

	@Override
	public Expression getSelectedExpression() {
		return expression;
	}

	@Override
	public Return toQueryReturn(QueryResultCreationContext returnResolutionContext, String resultVariable) {
		return new ReturnScalarImpl(
				expression,
				returnResolutionContext.resolveSqlSelection( sqlSelectable ),
				resultVariable,
				type
		);
	}
}
