/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.spi.expression;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.hibernate.sql.SqlExpressableType;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.consume.spi.SqlAstWalker;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.exec.spi.JdbcParameterBinder;
import org.hibernate.sql.exec.spi.JdbcParameterBinding;
import org.hibernate.sql.results.internal.SqlSelectionImpl;
import org.hibernate.sql.results.spi.SqlSelection;
import org.hibernate.type.descriptor.java.spi.BasicJavaDescriptor;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * @author Steve Ebersole
 */
public class LiteralParameter implements GenericParameter, JdbcParameterBinding, JdbcParameterBinder {
	private final Object value;
	private final SqlExpressableType type;

	public LiteralParameter(
			Object value,
			SqlExpressableType type,
			Clause clause,
			TypeConfiguration typeConfiguration) {
		this.value = value;
		this.type = type;
	}

	@Override
	public SqlExpressableType getExpressableType() {
		return type;
	}

	@Override
	public SqlSelection createSqlSelection(
			int jdbcPosition,
			int valuesArrayPosition,
			BasicJavaDescriptor javaTypeDescriptor,
			TypeConfiguration typeConfiguration) {
		return new SqlSelectionImpl(
				jdbcPosition,
				valuesArrayPosition,
				this,
				getExpressableType()
		);
	}

	@Override
	public JdbcParameterBinder getParameterBinder() {
		return this;
	}

	@Override
	public void accept(SqlAstWalker sqlTreeWalker) {
		sqlTreeWalker.visitGenericParameter( this );
	}

	@Override
	public SqlExpressableType getBindType() {
		return getExpressableType();
	}

	@Override
	public Object getBindValue() {
		return value;
	}

	@Override
	@SuppressWarnings("unchecked")
	public int bindParameterValue(
			PreparedStatement statement,
			int startPosition,
			ExecutionContext executionContext) throws SQLException {
		getExpressableType().getJdbcValueBinder().bind( statement, startPosition, value, executionContext );
		return 1;
	}
}
