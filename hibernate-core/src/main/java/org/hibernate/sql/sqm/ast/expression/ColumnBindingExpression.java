/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.sql.sqm.ast.expression;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.sql.sqm.ast.from.ColumnBinding;
import org.hibernate.sql.sqm.exec.results.spi.ReturnReader;
import org.hibernate.sql.sqm.convert.spi.SqlTreeWalker;
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
	public ReturnReader getReturnReader(int startPosition, boolean shallow, SessionFactoryImplementor sessionFactory) {
		throw new UnsupportedOperationException( "ColumnBindingExpression cannot be used in select-clause" );
	}

	@Override
	public void accept(SqlTreeWalker sqlTreeWalker) {
		sqlTreeWalker.visitColumnBindingExpression( this );
	}

	public ColumnBinding getColumnBinding() {
		return columnBinding;
	}
}
