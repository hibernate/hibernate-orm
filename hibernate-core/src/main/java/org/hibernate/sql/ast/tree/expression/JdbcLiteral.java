/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast.tree.expression;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.hibernate.cache.MutableCacheKeyBuilder;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.IndexedConsumer;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.MappingModelExpressible;
import org.hibernate.query.sqm.sql.internal.DomainResultProducer;
import org.hibernate.sql.ast.SqlAstWalker;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.basic.BasicResult;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.JavaTypedExpressible;
import org.hibernate.type.descriptor.java.MutabilityPlan;

/**
 * Represents a literal in the SQL AST.  This form accepts a {@link JdbcMapping} and acts
 * as its own {@link MappingModelExpressible}.
 *
 * @see QueryLiteral
 *
 * @author Steve Ebersole
 */
public class JdbcLiteral<T> implements Literal, MappingModelExpressible<T>, DomainResultProducer<T>, JavaTypedExpressible<T> {
	private final T literalValue;
	private final JdbcMapping jdbcMapping;

	public JdbcLiteral(T literalValue, JdbcMapping jdbcMapping) {
		this.literalValue = literalValue;
		this.jdbcMapping = jdbcMapping;
	}

	@Override
	public Object getLiteralValue() {
		return literalValue;
	}

	@Override
	public JdbcMapping getJdbcMapping() {
		return jdbcMapping;
	}

	@Override
	public void accept(SqlAstWalker sqlTreeWalker) {
		sqlTreeWalker.visitJdbcLiteral( this );
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
				literalValue,
				startPosition,
				executionContext.getSession()
		);
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// MappingModelExpressible

	@Override
	public MappingModelExpressible<T> getExpressionType() {
		return this;
	}

	@Override
	public int getJdbcTypeCount() {
		return 1;
	}

	@Override
	public JdbcMapping getJdbcMapping(int index) {
		if ( index != 0 ) {
			throw new IndexOutOfBoundsException( index );
		}
		return jdbcMapping;
	}

	@Override
	public JdbcMapping getSingleJdbcMapping() {
		return jdbcMapping;
	}

	@Override
	public int forEachJdbcType(int offset, IndexedConsumer<JdbcMapping> action) {
		action.accept( offset, jdbcMapping );
		return getJdbcTypeCount();
	}

	@Override
	public Object disassemble(Object value, SharedSessionContractImplementor session) {
		return value;
	}

	@Override
	public void addToCacheKey(MutableCacheKeyBuilder cacheKey, Object value, SharedSessionContractImplementor session) {
		if ( value != null ) {
			cacheKey.addValue( disassemble( value, jdbcMapping.getJdbcJavaType().getMutabilityPlan(), session ) );
			cacheKey.addHashCode( hashCode( value, jdbcMapping.getJavaTypeDescriptor() ) );
		}
	}

	private static <T> int hashCode(Object value, JavaType<T> javaTypeDescriptor) {
		return javaTypeDescriptor.extractHashCode( (T) value );
	}

	private static <T> Serializable disassemble(
			Object value, MutabilityPlan<T> mutabilityPlan,
			SharedSessionContractImplementor session) {
		return mutabilityPlan.disassemble( (T) value, session );
	}

	@Override
	public <X, Y> int forEachDisassembledJdbcValue(
			Object value,
			int offset,
			X x,
			Y y,
			JdbcValuesBiConsumer<X, Y> valuesConsumer,
			SharedSessionContractImplementor session) {
		valuesConsumer.consume( offset, x, y, value, jdbcMapping );
		return getJdbcTypeCount();
	}

	@Override
	public <X, Y> int forEachJdbcValue(
			Object value,
			int offset,
			X x,
			Y y,
			JdbcValuesBiConsumer<X, Y> valuesConsumer,
			SharedSessionContractImplementor session) {
		valuesConsumer.consume( offset, x, y, value, jdbcMapping );
		return getJdbcTypeCount();
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// DomainResultProducer

	@Override
	public DomainResult<T> createDomainResult(String resultVariable, DomainResultCreationState creationState) {
		final SqlAstCreationState sqlAstCreationState = creationState.getSqlAstCreationState();
		final SqlSelection sqlSelection =
				sqlAstCreationState.getSqlExpressionResolver()
						.resolveSqlSelection(
								this,
								jdbcMapping.getJdbcJavaType(),
								null,
								sqlAstCreationState.getCreationContext().getTypeConfiguration()
						);
		return new BasicResult<>( sqlSelection.getValuesArrayPosition(), resultVariable, jdbcMapping );
	}

	@Override
	public void applySqlSelections(DomainResultCreationState creationState) {
		final SqlAstCreationState sqlAstCreationState = creationState.getSqlAstCreationState();
		sqlAstCreationState.getSqlExpressionResolver().resolveSqlSelection(
				this,
				jdbcMapping.getJdbcJavaType(),
				null,
				sqlAstCreationState.getCreationContext().getTypeConfiguration()
		);
	}

	@Override
	public JavaType<T> getExpressibleJavaType() {
		return (JavaType<T>)
				jdbcMapping.getJavaTypeDescriptor();
	}
}
