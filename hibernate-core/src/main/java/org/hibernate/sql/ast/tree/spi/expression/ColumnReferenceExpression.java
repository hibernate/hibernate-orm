/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.sql.ast.tree.spi.expression;

import org.hibernate.sql.ast.tree.spi.from.ColumnReference;
import org.hibernate.sql.ast.tree.spi.select.Selectable;
import org.hibernate.sql.ast.produce.ConversionException;
import org.hibernate.sql.ast.consume.spi.SqlSelectAstToJdbcSelectConverter;
import org.hibernate.type.spi.Type;

/**
 * @author Steve Ebersole
 */
public class ColumnReferenceExpression implements Expression {
	private final ColumnReference columnReference;

	public ColumnReferenceExpression(ColumnReference columnReference) {
		this.columnReference = columnReference;
	}

	@Override
	public Type getType() {
		return null;
	}

	@Override
	public Selectable getSelectable() {
		throw new ConversionException( "ColumnReferenceExpression is not Selectable" );
	}

	@Override
	public void accept(SqlSelectAstToJdbcSelectConverter walker) {
		walker.visitColumnBindingExpression( this );
	}

	public ColumnReference getColumnReference() {
		return columnReference;
	}
}
