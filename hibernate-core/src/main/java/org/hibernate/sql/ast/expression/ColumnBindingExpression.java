/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.sql.ast.expression;

import org.hibernate.sql.ast.from.ColumnBinding;
import org.hibernate.sql.ast.select.Selectable;
import org.hibernate.sql.convert.ConversionException;
import org.hibernate.sql.exec.spi.SqlAstSelectInterpreter;
import org.hibernate.type.spi.Type;

/**
 * @author Steve Ebersole
 */
public class ColumnBindingExpression implements Expression {
	private final ColumnBinding columnBinding;

	public ColumnBindingExpression(ColumnBinding columnBinding) {
		this.columnBinding = columnBinding;
	}

	@Override
	public Type getType() {
		return null;
	}

	@Override
	public Selectable getSelectable() {
		throw new ConversionException( "ColumnBindingExpression is not Selectable" );
	}

	@Override
	public void accept(SqlAstSelectInterpreter walker) {
		walker.visitColumnBindingExpression( this );
	}

	public ColumnBinding getColumnBinding() {
		return columnBinding;
	}
}
