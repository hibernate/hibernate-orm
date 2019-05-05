/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.expression;

import org.hibernate.sql.SqlExpressableType;
import org.hibernate.sql.ast.consume.spi.SqlAstWalker;
import org.hibernate.sql.results.internal.SqlSelectionImpl;
import org.hibernate.sql.results.spi.SqlSelection;
import org.hibernate.type.descriptor.java.spi.BasicJavaDescriptor;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * @author Gavin King
 */
public class CeilingFunction extends AbstractStandardFunction {
	private final Expression argument;
	private SqlExpressableType type;

	public CeilingFunction(Expression argument, SqlExpressableType type) {
		this.argument = argument;
		this.type = type;
	}

	@Override
	public SqlExpressableType getExpressableType() {
		return type;
	}

	public Expression getArgument() {
		return argument;
	}

	@Override
	public void accept(SqlAstWalker sqlTreeWalker) {
		sqlTreeWalker.visitCeilingFunction( this );
	}

	@Override
	public SqlExpressableType getType() {
		return type;
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
