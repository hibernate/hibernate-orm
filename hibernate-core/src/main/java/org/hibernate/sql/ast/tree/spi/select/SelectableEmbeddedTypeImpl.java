/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.sql.ast.tree.spi.select;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.sql.ast.produce.result.internal.QueryResultCompositeImpl;
import org.hibernate.sql.ast.produce.result.spi.QueryResult;
import org.hibernate.sql.ast.produce.result.spi.QueryResultCreationContext;
import org.hibernate.sql.ast.tree.spi.expression.ColumnReference;
import org.hibernate.sql.ast.tree.spi.expression.Expression;
import org.hibernate.type.spi.EmbeddedType;

/**
 * @author Steve Ebersole
 */
public class SelectableEmbeddedTypeImpl implements Selectable {
	private final Expression selectedExpression;
	private final List<ColumnReference> columnBindings;
	private final EmbeddedType embeddedType;

	public SelectableEmbeddedTypeImpl(
			Expression selectedExpression,
			List<ColumnReference> columnBindings,
			EmbeddedType embeddedType) {
		this.selectedExpression = selectedExpression;
		this.columnBindings = columnBindings;
		this.embeddedType = embeddedType;
	}

	@Override
	public QueryResult toQueryReturn(QueryResultCreationContext returnResolutionContext, String resultVariable) {
		// todo : not sure what we will need here yet...
		final List<SqlSelection> sqlSelections = new ArrayList<>();
		for ( ColumnReference columnBinding : columnBindings ) {
			sqlSelections.add(
					returnResolutionContext.resolveSqlSelection( columnBinding )
			);
		}

		return new QueryResultCompositeImpl(
				this,
				resultVariable,
				sqlSelections,
				embeddedType
		);
	}
}
