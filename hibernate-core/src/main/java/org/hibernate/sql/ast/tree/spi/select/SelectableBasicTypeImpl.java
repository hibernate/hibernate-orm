/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.sql.ast.tree.spi.select;

import org.hibernate.persister.queryable.spi.BasicValuedExpressableType;
import org.hibernate.sql.ast.produce.result.spi.ColumnReferenceResolver;
import org.hibernate.sql.ast.tree.spi.expression.Expression;
import org.hibernate.sql.ast.produce.result.internal.QueryResultScalarImpl;
import org.hibernate.sql.ast.produce.result.spi.QueryResult;
import org.hibernate.sql.ast.produce.result.spi.QueryResultCreationContext;
import org.hibernate.type.spi.BasicType;

/**
 * @author Steve Ebersole
 */
public class SelectableBasicTypeImpl implements Selectable {
	private final Expression expression;
	private final SqlSelectable sqlSelectable;
	private final BasicValuedExpressableType type;

	public SelectableBasicTypeImpl(
			Expression expression,
			SqlSelectable sqlSelectable,
			BasicValuedExpressableType type) {
		this.expression = expression;
		this.sqlSelectable = sqlSelectable;
		this.type = type;
	}

	@Override
	public Selection createSelection(
			Expression selectedExpression, String resultVariable, ColumnReferenceResolver columnReferenceResolver) {
		return null;
	}
}
