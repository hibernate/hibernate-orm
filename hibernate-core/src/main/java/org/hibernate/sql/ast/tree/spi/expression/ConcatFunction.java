/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.spi.expression;

import java.util.List;

import org.hibernate.sql.SqlExpressableType;
import org.hibernate.sql.ast.consume.spi.SqlAstWalker;
import org.hibernate.sql.ast.produce.spi.SqlExpressable;
import org.hibernate.sql.results.internal.SqlSelectionImpl;
import org.hibernate.sql.results.spi.SqlSelection;
import org.hibernate.type.descriptor.java.spi.BasicJavaDescriptor;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * @author Steve Ebersole
 */
public class ConcatFunction implements StandardFunction {
	private final List<Expression> expressions;
	private final SqlExpressableType type;

	public ConcatFunction(
			List<Expression> expressions,
			SqlExpressableType type) {
		this.expressions = expressions;
		this.type = type;
	}

	public List<Expression> getExpressions() {
		return expressions;
	}

	@Override
	public SqlExpressableType getExpressableType() {
		return type;
	}

	@Override
	public SqlExpressableType getType() {
		return getExpressableType();
	}

	@Override
	public SqlExpressable getExpressable() {
		return this;
	}

	@Override
	public void accept(SqlAstWalker  walker) {
		walker.visitConcatFunction( this );
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
				getType().getJdbcValueExtractor()
		);
	}
}
