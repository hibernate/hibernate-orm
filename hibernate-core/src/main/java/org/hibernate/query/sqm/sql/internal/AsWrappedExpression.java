/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.sql.internal;

import org.hibernate.metamodel.mapping.JdbcMappingContainer;
import org.hibernate.sql.ast.SqlAstWalker;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.basic.BasicResult;
import org.hibernate.type.BasicType;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.spi.TypeConfiguration;

public class AsWrappedExpression<B> implements Expression, DomainResultProducer<B> {
	private final Expression wrappedExpression;
	private final BasicType<B> expressionType;

	public AsWrappedExpression(Expression wrappedExpression, BasicType<B> expressionType) {
		assert wrappedExpression instanceof DomainResultProducer : "AsWrappedExpression expected to be an instance of DomainResultProducer";
		this.wrappedExpression = wrappedExpression;
		this.expressionType = expressionType;
	}

	@Override
	public JdbcMappingContainer getExpressionType() {
		return expressionType;
	}

	@Override
	public ColumnReference getColumnReference() {
		return wrappedExpression.getColumnReference();
	}

	@Override
	public SqlSelection createSqlSelection(
			int jdbcPosition,
			int valuesArrayPosition,
			JavaType javaType,
			boolean virtual,
			TypeConfiguration typeConfiguration) {
		return wrappedExpression.createSqlSelection(
				jdbcPosition,
				valuesArrayPosition,
				javaType,
				virtual,
				typeConfiguration
		);
	}

	@Override
	public SqlSelection createDomainResultSqlSelection(
			int jdbcPosition,
			int valuesArrayPosition,
			JavaType javaType,
			boolean virtual,
			TypeConfiguration typeConfiguration) {
		return wrappedExpression.createDomainResultSqlSelection(
				jdbcPosition,
				valuesArrayPosition,
				javaType,
				virtual,
				typeConfiguration
		);
	}

	@Override
	public void accept(SqlAstWalker sqlTreeWalker) {
		wrappedExpression.accept( sqlTreeWalker );
	}

	@Override
	public DomainResult<B> createDomainResult(String resultVariable, DomainResultCreationState creationState) {
		final SqlAstCreationState sqlAstCreationState = creationState.getSqlAstCreationState();
		final SqlSelection sqlSelection = sqlAstCreationState.getSqlExpressionResolver()
				.resolveSqlSelection(
						wrappedExpression,
						wrappedExpression.getExpressionType().getSingleJdbcMapping().getJdbcJavaType(),
						null,
						sqlAstCreationState.getCreationContext()
								.getMappingMetamodel().getTypeConfiguration()
				);
		return new BasicResult<>(
				sqlSelection.getValuesArrayPosition(),
				resultVariable,
				expressionType.getExpressibleJavaType()
		);
	}

	@Override
	public void applySqlSelections(DomainResultCreationState creationState) {
		//noinspection unchecked
		( (DomainResultProducer<B>) wrappedExpression ).applySqlSelections( creationState );
	}
}
