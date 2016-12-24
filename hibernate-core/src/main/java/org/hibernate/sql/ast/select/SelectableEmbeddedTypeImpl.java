/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.sql.ast.select;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.sql.ast.expression.Expression;
import org.hibernate.sql.ast.from.ColumnBinding;
import org.hibernate.sql.convert.results.internal.ReturnCompositeImpl;
import org.hibernate.sql.convert.results.spi.Return;
import org.hibernate.sql.convert.results.spi.ReturnResolutionContext;
import org.hibernate.type.CompositeType;

/**
 * @author Steve Ebersole
 */
public class SelectableEmbeddedTypeImpl implements Selectable {
	private final Expression selectedExpression;
	private final List<ColumnBinding> columnBindings;
	private final CompositeType embeddedType;

	public SelectableEmbeddedTypeImpl(
			Expression selectedExpression,
			List<ColumnBinding> columnBindings,
			CompositeType embeddedType) {
		this.selectedExpression = selectedExpression;
		this.columnBindings = columnBindings;
		this.embeddedType = embeddedType;
	}

	@Override
	public Expression getSelectedExpression() {
		return selectedExpression;
	}

	@Override
	public Return toQueryReturn(ReturnResolutionContext returnResolutionContext, String resultVariable) {
		// todo : not sure what we will need here yet...
		final List<SqlSelection> sqlSelections = new ArrayList<>();
		for ( ColumnBinding columnBinding : columnBindings ) {
			sqlSelections.add(
					returnResolutionContext.resolveSqlSelection( columnBinding )
			);
		}

		return new ReturnCompositeImpl(
				this,
				resultVariable,
				sqlSelections,
				embeddedType
		);
	}
}
