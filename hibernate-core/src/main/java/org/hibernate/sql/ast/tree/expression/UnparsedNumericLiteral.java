/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast.tree.expression;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.JdbcMappingContainer;
import org.hibernate.query.sqm.sql.internal.DomainResultProducer;
import org.hibernate.query.sqm.tree.expression.NumericTypeCategory;
import org.hibernate.sql.ast.SqlAstWalker;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.basic.BasicResult;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * A numeric literal coming from an HQL query, which needs special handling
 *
 * @see org.hibernate.query.sqm.tree.expression.SqmHqlNumericLiteral
 *
 * @author Steve Ebersole
 */
public class UnparsedNumericLiteral<N extends Number> implements Literal, DomainResultProducer<N> {
	private final String literalValue;
	private final NumericTypeCategory typeCategory;
	private final JdbcMapping jdbcMapping;

	public UnparsedNumericLiteral(String literalValue, NumericTypeCategory typeCategory, JdbcMapping jdbcMapping) {
		this.literalValue = literalValue;
		this.typeCategory = typeCategory;
		this.jdbcMapping = jdbcMapping;
	}

	@Override
	public N getLiteralValue() {
		return typeCategory.parseLiteralValue( literalValue );
	}

	@Override
	public void bindParameterValue(
			PreparedStatement statement,
			int startPosition,
			JdbcParameterBindings jdbcParameterBindings,
			ExecutionContext executionContext) throws SQLException {
		//noinspection unchecked
		jdbcMapping.getJdbcValueBinder().bind(
				statement,
				getLiteralValue(),
				startPosition,
				executionContext.getSession()
		);
	}

	public String getUnparsedLiteralValue() {
		return literalValue;
	}

	public JdbcMapping getJdbcMapping() {
		return jdbcMapping;
	}

	@Override
	public JdbcMappingContainer getExpressionType() {
		return getJdbcMapping();
	}

	@Override
	public DomainResult<N> createDomainResult(String resultVariable, DomainResultCreationState creationState) {
		final SqlExpressionResolver sqlExpressionResolver =
				creationState.getSqlAstCreationState().getSqlExpressionResolver();
		final TypeConfiguration typeConfiguration =
				creationState.getSqlAstCreationState().getCreationContext().getTypeConfiguration();

		final SqlSelection sqlSelection = sqlExpressionResolver.resolveSqlSelection(
				this,
				getJdbcMapping().getJdbcJavaType(),
				null,
				typeConfiguration
		);

		return new BasicResult<>(
				sqlSelection.getValuesArrayPosition(),
				resultVariable,
				jdbcMapping
		);
	}

	@Override
	public void applySqlSelections(DomainResultCreationState creationState) {
		creationState.getSqlAstCreationState().getSqlExpressionResolver().resolveSqlSelection(
				this,
				jdbcMapping.getJdbcJavaType(),
				null,
				creationState.getSqlAstCreationState().getCreationContext().getMappingMetamodel().getTypeConfiguration()
		);
	}

	@Override
	public void accept(SqlAstWalker sqlTreeWalker) {
		sqlTreeWalker.visitUnparsedNumericLiteral( this );
	}
}
