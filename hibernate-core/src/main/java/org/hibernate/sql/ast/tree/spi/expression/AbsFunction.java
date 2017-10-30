/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.spi.expression;

import org.hibernate.sql.ast.consume.spi.SqlAstWalker;
import org.hibernate.sql.ast.produce.metamodel.spi.BasicValuedExpressableType;
import org.hibernate.sql.results.internal.SqlSelectionImpl;
import org.hibernate.sql.results.spi.SqlSelection;

/**
 * @author Steve Ebersole
 */
public class AbsFunction extends AbstractStandardFunction {
	private final Expression argument;

	public AbsFunction(Expression argument) {
		this.argument = argument;
	}

	public Expression getArgument() {
		return argument;
	}

	@Override
	public void accept(SqlAstWalker sqlTreeWalker) {
		sqlTreeWalker.visitAbsFunction( this );
	}

	@Override
	public BasicValuedExpressableType getType() {
		return (BasicValuedExpressableType) argument.getType();
	}

	@Override
	public SqlSelection createSqlSelection(int jdbcPosition) {
		return new SqlSelectionImpl(
				jdbcPosition,
				this,
				getType().getBasicType().getSqlSelectionReader()
		);
	}
}
