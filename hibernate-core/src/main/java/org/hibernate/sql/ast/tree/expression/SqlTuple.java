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

import org.hibernate.metamodel.mapping.MappingModelExpressible;
import org.hibernate.query.sqm.SqmExpressible;
import org.hibernate.query.sqm.sql.internal.DomainResultProducer;
import org.hibernate.sql.ast.SqlAstWalker;
import org.hibernate.sql.ast.SqlTreeCreationLogger;
import org.hibernate.sql.ast.tree.update.Assignable;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.tuple.TupleResult;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * @author Steve Ebersole
 */
public class SqlTuple implements Expression, SqlTupleContainer, DomainResultProducer, Assignable {
	private final List<? extends Expression> expressions;
	private final MappingModelExpressible valueMapping;

	public SqlTuple(List<? extends Expression> expressions, MappingModelExpressible valueMapping) {
		this.expressions = expressions;
		this.valueMapping = valueMapping;

		if ( SqlTreeCreationLogger.LOGGER.isDebugEnabled() ) {
			final int size = expressions.size();
			if ( size < 2 ) {
				SqlTreeCreationLogger.LOGGER.debugf(
						"SqlTuple created with `%s` expression(s)",
						size
				);
			}
		}
	}

	@Override
	public MappingModelExpressible getExpressionType() {
		return valueMapping;
	}

	public List<? extends Expression> getExpressions(){
		return expressions;
	}

	@Override
	public List<ColumnReference> getColumnReferences() {
		return (List<ColumnReference>) expressions;
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
		final JavaType javaType = ( (SqmExpressible<?>) valueMapping ).getExpressibleJavaType();
		final int[] valuesArrayPositions = new int[expressions.size()];
		for ( int i = 0; i < expressions.size(); i++ ) {
			final Expression expression = expressions.get( i );
			valuesArrayPositions[i] = creationState.getSqlAstCreationState().getSqlExpressionResolver().resolveSqlSelection(
					expression,
					expression.getExpressionType().getSingleJdbcMapping().getJdbcJavaType(),
					null,
					creationState.getSqlAstCreationState().getCreationContext().getMappingMetamodel().getTypeConfiguration()
			).getValuesArrayPosition();
		}

		return new TupleResult(
				valuesArrayPositions,
				resultVariable,
				javaType
		);
	}

	@Override
	public void applySqlSelections(DomainResultCreationState creationState) {
		throw new UnsupportedOperationException();
	}

	public static class Builder {
		private final MappingModelExpressible valueMapping;

		private List<Expression> expressions;

		public Builder(MappingModelExpressible valueMapping) {
			this.valueMapping = valueMapping;
		}

		public Builder(MappingModelExpressible valueMapping, int jdbcTypeCount) {
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
