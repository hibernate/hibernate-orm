/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.function;

import java.util.List;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.IndexedConsumer;
import org.hibernate.mapping.Selectable;
import org.hibernate.mapping.Table;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.JdbcMappingContainer;
import org.hibernate.metamodel.mapping.SqlExpressible;
import org.hibernate.type.descriptor.converter.spi.BasicValueConverter;
import org.hibernate.query.ReturnableType;
import org.hibernate.query.sqm.sql.internal.DomainResultProducer;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
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
public class SelfRenderingFunctionSqlAstExpression
		implements SelfRenderingExpression, Selectable, SqlExpressible, DomainResultProducer, FunctionExpression {
	private final String functionName;
	private final FunctionRenderer renderer;
	private final List<? extends SqlAstNode> sqlAstArguments;
	private final @Nullable ReturnableType<?> type;
	private final @Nullable JdbcMappingContainer expressible;

	/**
	 * @deprecated Use {@link #SelfRenderingFunctionSqlAstExpression(String, FunctionRenderer, List, ReturnableType, JdbcMappingContainer)} instead
	 */
	@Deprecated(forRemoval = true)
	public SelfRenderingFunctionSqlAstExpression(
			String functionName,
			FunctionRenderingSupport renderer,
			List<? extends SqlAstNode> sqlAstArguments,
			@Nullable ReturnableType<?> type,
			@Nullable JdbcMappingContainer expressible) {
		this.functionName = functionName;
		this.renderer = renderer::render;
		this.sqlAstArguments = sqlAstArguments;
		this.type = type;
		//might be null due to code in SelfRenderingFunctionSqlAstExpression
		this.expressible = expressible;
	}

	public SelfRenderingFunctionSqlAstExpression(
			String functionName,
			FunctionRenderer renderer,
			List<? extends SqlAstNode> sqlAstArguments,
			@Nullable ReturnableType<?> type,
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

	/**
	 * @deprecated Use {@link #getFunctionRenderer()} instead
	 */
	@Deprecated(forRemoval = true)
	protected FunctionRenderingSupport getRenderer() {
		return renderer;
	}

	protected FunctionRenderer getFunctionRenderer() {
		return renderer;
	}

	protected @Nullable ReturnableType<?> getType() {
		return type;
	}

	@Override
	public SqlSelection createSqlSelection(
			int jdbcPosition,
			int valuesArrayPosition,
			JavaType javaType,
			boolean virtual,
			TypeConfiguration typeConfiguration) {
		return new SqlSelectionImpl(
				jdbcPosition,
				valuesArrayPosition,
				this,
				virtual
		);
	}

	@Override
	public DomainResult<?> createDomainResult(
			String resultVariable,
			DomainResultCreationState creationState) {
		final JdbcMapping jdbcMapping = getJdbcMapping();
		final JavaType<?> jdbcJavaType;
		final BasicValueConverter<?, ?> converter;
		if ( jdbcMapping != null ) {
			jdbcJavaType = jdbcMapping.getJdbcJavaType();
			converter = jdbcMapping.getValueConverter();
		}
		else if ( type != null ) {
			jdbcJavaType = type.getExpressibleJavaType();
			converter = null;
		}
		else {
			jdbcJavaType = null;
			converter = null;
		}
		final SqlAstCreationState sqlAstCreationState = creationState.getSqlAstCreationState();
		return new BasicResult(
				sqlAstCreationState.getSqlExpressionResolver()
						.resolveSqlSelection(
								this,
								jdbcJavaType,
								null,
								sqlAstCreationState.getCreationContext()
										.getMappingMetamodel().getTypeConfiguration()
						)
						.getValuesArrayPosition(),
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
	public String getAlias(Dialect dialect) {
		return null;
	}

	@Override
	public String getAlias(Dialect dialect, Table table) {
		return null;
	}

	@Override
	public boolean isFormula() {
		return false;
	}

	@Override
	public String getTemplate(Dialect dialect, TypeConfiguration typeConfiguration, SqmFunctionRegistry registry) {
		return null;
	}

	@Override
	public String getText(Dialect dialect) {
		return null;
	}

	@Override
	public String getText() {
		return null;
	}

	@Override
	public String getCustomReadExpression() {
		return null;
	}

	@Override
	public String getCustomWriteExpression() {
		return null;
	}

	@Override
	public JdbcMapping getJdbcMapping() {
		if ( type instanceof SqlExpressible ) {
			return ( (SqlExpressible) type ).getJdbcMapping();
		}
		else if ( expressible instanceof SqlExpressible ) {
			return ( (SqlExpressible) expressible ).getJdbcMapping();
		}
		else {
			return null;
		}
	}

	@Override
	public void applySqlSelections(DomainResultCreationState creationState) {
		final SqlAstCreationState sqlAstCreationState = creationState.getSqlAstCreationState();
		final JdbcMapping jdbcMapping = getJdbcMapping();
		sqlAstCreationState.getSqlExpressionResolver()
				.resolveSqlSelection(
					this,
					jdbcMapping != null
							? jdbcMapping.getJdbcJavaType()
							: type.getExpressibleJavaType(),
					null,
					sqlAstCreationState.getCreationContext()
							.getMappingMetamodel().getTypeConfiguration()
			);
	}

	@Override
	public int forEachJdbcType(int offset, IndexedConsumer<JdbcMapping> action) {
		throw new UnsupportedOperationException();
	}
}
