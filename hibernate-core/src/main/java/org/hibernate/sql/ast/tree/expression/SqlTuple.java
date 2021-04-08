/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.expression;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hibernate.metamodel.mapping.MappingModelExpressable;
import org.hibernate.sql.ast.SqlAstWalker;

/**
 * @author Steve Ebersole
 */
public class SqlTuple implements Expression, SqlTupleContainer {
	private final List<? extends Expression> expressions;
	private final MappingModelExpressable valueMapping;

	public SqlTuple(List<? extends Expression> expressions, MappingModelExpressable valueMapping) {
		this.expressions = expressions;
		this.valueMapping = valueMapping;
	}

	@Override
	public MappingModelExpressable getExpressionType() {
		return valueMapping;
	}

	public List<? extends Expression> getExpressions(){
		return expressions;
	}

	@Override
	public void accept(SqlAstWalker sqlTreeWalker) {
		sqlTreeWalker.visitTuple( this );
	}

	@Override
	public SqlTuple getSqlTuple() {
		return this;
	}

	public static class Builder {
		private final MappingModelExpressable valueMapping;

		private List<Expression> expressions;

		public Builder(MappingModelExpressable valueMapping) {
			this.valueMapping = valueMapping;
		}

		public Builder(MappingModelExpressable valueMapping, int jdbcTypeCount) {
			this( valueMapping );
			expressions = new ArrayList<>( jdbcTypeCount );
		}

		public void addSubExpression(Expression expression) {
			if ( expressions == null ) {
				expressions = new ArrayList<>();
			}

			expressions.add( expression );
		}

		public SqlTuple buildTuple() {
			return new SqlTuple( expressions == null ? Collections.emptyList() : expressions, valueMapping );
		}
	}
}
