/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.function;

import java.util.List;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.IndexedConsumer;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.JdbcMappingContainer;
import org.hibernate.metamodel.mapping.SqlExpressible;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.type.descriptor.converter.spi.BasicValueConverter;
import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.query.sqm.sql.internal.DomainResultProducer;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.FunctionExpression;
import org.hibernate.sql.ast.tree.expression.SelfRenderingExpression;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.basic.BasicResult;
import org.hibernate.sql.results.internal.SqlSelectionImpl;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.spi.TypeConfiguration;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Representation of a function call in the SQL AST for impls that know how to
 * render themselves.
 *
 * @author Steve Ebersole
 */
public class SelfRenderingFunctionSqlAstExpression<T>
		implements SelfRenderingExpression, SqlExpressible, DomainResultProducer<T>, FunctionExpression {
	private final String functionName;
	private final FunctionRenderer renderer;
	private final List<? extends SqlAstNode> sqlAstArguments;
	private final @Nullable ReturnableType<T> type;
	private final @Nullable JdbcMappingContainer expressible;

	public SelfRenderingFunctionSqlAstExpression(
			String functionName,
			FunctionRenderer renderer,
			List<? extends SqlAstNode> sqlAstArguments,
			@Nullable ReturnableType<T> type,
			@Nullable JdbcMappingContainer expressible) {
		this.functionName = functionName;
		this.renderer = renderer;
		this.sqlAstArguments = sqlAstArguments;
		this.type = type;
		//might be null due to code in SelfRenderingFunctionSqlAstExpression
		this.expressible = expressible;
	}

	@Override
	public String getFunctionName() {
		return functionName;
	}

	@Override
	public List<? extends SqlAstNode> getArguments() {
		return sqlAstArguments;
	}

	@Override
	public JdbcMappingContainer getExpressionType() {
		return type instanceof SqlExpressible
				? (JdbcMappingContainer) type
				: expressible;
	}

	FunctionRenderer getFunctionRenderer() {
		return renderer;
	}

	@Nullable ReturnableType<?> getType() {
		return type;
	}

	@Override
	public SqlSelection createSqlSelection(
			int jdbcPosition,
			int valuesArrayPosition,
			JavaType javaType,
			boolean virtual,
			TypeConfiguration typeConfiguration) {
		return new SqlSelectionImpl( jdbcPosition, valuesArrayPosition, this, virtual );
	}

	@Override
	public DomainResult<T> createDomainResult(
			String resultVariable,
			DomainResultCreationState creationState) {
		final JdbcMapping jdbcMapping = getJdbcMapping();
		final JavaType<T> jdbcJavaType;
		final BasicValueConverter<T, ?> converter;
		if ( jdbcMapping != null ) {
			jdbcJavaType = (JavaType<T>) jdbcMapping.getJdbcJavaType();
			converter = (BasicValueConverter<T, ?>) jdbcMapping.getValueConverter();
		}
		else if ( type != null ) {
			jdbcJavaType = type.getExpressibleJavaType();
			converter = null;
		}
		else {
			jdbcJavaType = null;
			converter = null;
		}
		return new BasicResult<>(
				resolveSqlSelection( creationState, jdbcJavaType ).getValuesArrayPosition(),
				resultVariable,
				type == null ? null : type.getExpressibleJavaType(),
				converter,
				null,
				false,
				false
		);
	}

	@Override
	public void renderToSql(
			SqlAppender sqlAppender,
			SqlAstTranslator<?> walker,
			SessionFactoryImplementor sessionFactory) {
		renderer.render( sqlAppender, sqlAstArguments, type, walker );
	}

	@Override
	public JdbcMapping getJdbcMapping() {
		if ( type instanceof SqlExpressible sqlExpressible ) {
			return sqlExpressible.getJdbcMapping();
		}
		else if ( expressible instanceof SqlExpressible sqlExpressible ) {
			return sqlExpressible.getJdbcMapping();
		}
		else {
			return null;
		}
	}

	@Override
	public void applySqlSelections(DomainResultCreationState creationState) {
		final JdbcMapping jdbcMapping = getJdbcMapping();
		resolveSqlSelection( creationState,
				jdbcMapping == null ? type.getExpressibleJavaType() : jdbcMapping.getJdbcJavaType() );
	}

	private SqlSelection resolveSqlSelection(DomainResultCreationState creationState, JavaType<?> javaType) {
		final SqlAstCreationState sqlAstCreationState = creationState.getSqlAstCreationState();
		final TypeConfiguration typeConfiguration =
				sqlAstCreationState.getCreationContext().getMappingMetamodel().getTypeConfiguration();
		return sqlAstCreationState.getSqlExpressionResolver()
				.resolveSqlSelection( this, javaType, null, typeConfiguration );
	}

	@Override
	public int forEachJdbcType(int offset, IndexedConsumer<JdbcMapping> action) {
		throw new UnsupportedOperationException();
	}
}
