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
import org.hibernate.query.sqm.SqmExpressable;
import org.hibernate.query.sqm.sql.internal.DomainResultProducer;
import org.hibernate.sql.ast.SqlAstWalker;
import org.hibernate.sql.ast.SqlTreeCreationLogger;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.tuple.TupleResult;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public class SqlTuple implements Expression, SqlTupleContainer, DomainResultProducer {
	private final List<? extends Expression> expressions;
	private final MappingModelExpressable valueMapping;

	public SqlTuple(List<? extends Expression> expressions, MappingModelExpressable valueMapping) {
		this.expressions = expressions;
		this.valueMapping = valueMapping;

		if ( expressions.size() < 2 ) {
			SqlTreeCreationLogger.LOGGER.debugf(
					"SqlTuple created with `%s` expression(s)",
					expressions.size()
			);
		}
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

	@Override
	public DomainResult createDomainResult(
			String resultVariable,
			DomainResultCreationState creationState) {
		final JavaTypeDescriptor javaTypeDescriptor = ( (SqmExpressable<?>) valueMapping ).getExpressableJavaTypeDescriptor();
		final int[] valuesArrayPositions = new int[expressions.size()];
		for ( int i = 0; i < expressions.size(); i++ ) {
			valuesArrayPositions[i] = creationState.getSqlAstCreationState().getSqlExpressionResolver().resolveSqlSelection(
					expressions.get( i ),
					javaTypeDescriptor,
					creationState.getSqlAstCreationState().getCreationContext().getDomainModel().getTypeConfiguration()
			).getValuesArrayPosition();
		}

		return new TupleResult(
				valuesArrayPositions,
				resultVariable,
				javaTypeDescriptor
		);
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
