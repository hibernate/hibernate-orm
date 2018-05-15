/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.spi.expression;

import org.hibernate.sql.SqlExpressableType;
import org.hibernate.sql.ast.consume.spi.SqlAstWalker;
import org.hibernate.sql.results.internal.SqlSelectionImpl;
import org.hibernate.sql.results.spi.SqlSelection;
import org.hibernate.type.descriptor.java.spi.BasicJavaDescriptor;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * @author Steve Ebersole
 */
public class AbsFunction extends AbstractStandardFunction {
	private final Expression argument;

	public AbsFunction(Expression argument) {
		this.argument = argument;
	}

	@Override
	public SqlExpressableType getExpressableType() {
		return argument.getType();
	}

	public Expression getArgument() {
		return argument;
	}

	@Override
	public void accept(SqlAstWalker sqlTreeWalker) {
		sqlTreeWalker.visitAbsFunction( this );
	}

	@Override
	public SqlExpressableType getType() {
		return getExpressableType();
	}

	@Override
	@SuppressWarnings("unchecked")
	public SqlSelection createSqlSelection(
			int jdbcPosition,
			int valuesArrayPosition,
			BasicJavaDescriptor javaTypeDescriptor,
			TypeConfiguration typeConfiguration) {
		return new SqlSelectionImpl(
				jdbcPosition,
				valuesArrayPosition,
				this,
				getType().getSqlTypeDescriptor().getSqlExpressableType(
						getType().getJavaTypeDescriptor(),
						typeConfiguration
				)
		);
	}
}
