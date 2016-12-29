/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.sql.ast.select;

import org.hibernate.sql.ast.expression.Expression;
import org.hibernate.sql.convert.results.internal.ReturnScalarImpl;
import org.hibernate.sql.convert.results.spi.Return;
import org.hibernate.sql.convert.results.spi.ReturnResolutionContext;
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
	public Return toQueryReturn(ReturnResolutionContext returnResolutionContext, String resultVariable) {
		return new ReturnScalarImpl(
				expression,
				returnResolutionContext.resolveSqlSelection( sqlSelectable ),
				resultVariable,
				type
		);
	}
}
