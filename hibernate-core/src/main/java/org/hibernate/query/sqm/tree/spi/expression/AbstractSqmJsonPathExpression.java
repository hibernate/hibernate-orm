/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.spi.expression;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.hibernate.Incubating;
import org.hibernate.internal.util.QuotingHelper;
import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.query.sqm.spi.NodeBuilder;
import org.hibernate.query.sqm.function.FunctionRenderer;
import org.hibernate.query.sqm.function.SelfRenderingSqmFunction;
import org.hibernate.query.sqm.function.SqmFunctionDescriptor;
import org.hibernate.query.sqm.produce.function.ArgumentsValidator;
import org.hibernate.query.sqm.produce.function.FunctionReturnTypeResolver;
import org.hibernate.query.sqm.sql.spi.SqmToSqlAstConverter;
import org.hibernate.query.sqm.tree.spi.SqmCacheable;
import org.hibernate.query.sqm.tree.spi.SqmCopyContext;
import org.hibernate.query.sqm.tree.spi.SqmRenderContext;
import org.hibernate.query.sqm.tree.spi.SqmTypedNode;
import org.hibernate.sql.ast.spi.query.expression.Expression;
import org.hibernate.sql.ast.spi.query.expression.JsonPathPassingClause;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Base class for expressions that contain a json path. Maintains a map of expressions for identifiers.
 *
 * @since 7.0
 */
@Incubating(since = "6.2")
public abstract class AbstractSqmJsonPathExpression<T> extends SelfRenderingSqmFunction<T> {

	private @Nullable Map<String, SqmExpression<?>> passingExpressions;

	public AbstractSqmJsonPathExpression(
			@Nonnull SqmFunctionDescriptor descriptor,
			@Nonnull FunctionRenderer renderer,
			@Nonnull List<? extends SqmTypedNode<?>> arguments,
			@Nullable ReturnableType<T> impliedResultType,
			@Nullable ArgumentsValidator argumentsValidator,
			@Nonnull FunctionReturnTypeResolver returnTypeResolver,
			@Nonnull NodeBuilder nodeBuilder,
			@Nonnull String name) {
		super(
				descriptor,
				renderer,
				arguments,
				impliedResultType,
				argumentsValidator,
				returnTypeResolver,
				nodeBuilder,
				name
		);
	}

	protected AbstractSqmJsonPathExpression(
			@Nonnull SqmFunctionDescriptor descriptor,
			@Nonnull FunctionRenderer renderer,
			@Nonnull List<? extends SqmTypedNode<?>> arguments,
			@Nullable ReturnableType<T> impliedResultType,
			@Nullable ArgumentsValidator argumentsValidator,
			@Nonnull FunctionReturnTypeResolver returnTypeResolver,
			@Nonnull NodeBuilder nodeBuilder,
			@Nonnull String name,
			@Nullable Map<String, SqmExpression<?>> passingExpressions) {
		super(
				descriptor,
				renderer,
				arguments,
				impliedResultType,
				argumentsValidator,
				returnTypeResolver,
				nodeBuilder,
				name
		);
		this.passingExpressions = passingExpressions;
	}

	@Nonnull
	public Map<String, SqmExpression<?>> getPassingExpressions() {
		return passingExpressions == null ? Collections.emptyMap() : Collections.unmodifiableMap( passingExpressions );
	}

	protected void addPassingExpression(@Nonnull String identifier, @Nonnull SqmExpression<?> expression) {
		if ( passingExpressions == null ) {
			passingExpressions = new HashMap<>();
		}
		passingExpressions.put( identifier, expression );
	}

	protected @Nullable Map<String, SqmExpression<?>> copyPassingExpressions(@Nonnull SqmCopyContext context) {
		if ( passingExpressions == null ) {
			return null;
		}
		final HashMap<String, SqmExpression<?>> copy = new HashMap<>( passingExpressions.size() );
		for ( Map.Entry<String, SqmExpression<?>> entry : passingExpressions.entrySet() ) {
			copy.put( entry.getKey(), entry.getValue().copy( context ) );
		}
		return copy;
	}

	protected @Nullable JsonPathPassingClause createJsonPathPassingClause(@Nonnull SqmToSqlAstConverter walker) {
		if ( passingExpressions == null || passingExpressions.isEmpty() ) {
			return null;
		}
		final HashMap<String, Expression> converted = new HashMap<>( passingExpressions.size() );
		for ( Map.Entry<String, SqmExpression<?>> entry : passingExpressions.entrySet() ) {
			converted.put( entry.getKey(), (Expression) entry.getValue().accept( walker ) );
		}
		return new JsonPathPassingClause( converted );
	}

	protected void appendPassingExpressionHqlString(@Nonnull StringBuilder sb, @Nonnull SqmRenderContext context) {
		final var passingExpressions = this.passingExpressions;
		if ( passingExpressions != null && !passingExpressions.isEmpty() ) {
			sb.append( " passing " );
			for ( Map.Entry<String, SqmExpression<?>> entry : passingExpressions.entrySet() ) {
				entry.getValue().appendHqlString( sb, context );
				sb.append( " as " );
				QuotingHelper.appendDoubleQuoteEscapedString( sb, entry.getKey() );
			}
		}
	}

	@Override
	public boolean equals(@Nullable Object other) {
		return super.equals( other )
			&& other instanceof AbstractSqmJsonPathExpression<?> that
			&& Objects.equals( passingExpressions, that.passingExpressions );
	}

	@Override
	public int hashCode() {
		int result = super.hashCode();
		result = 31 * result + Objects.hashCode( passingExpressions );
		return result;
	}

	@Override
	public boolean isCompatible(@Nullable Object other) {
		return super.isCompatible( other )
			&& other instanceof AbstractSqmJsonPathExpression<?> that
			&& SqmCacheable.areCompatible( passingExpressions, that.passingExpressions );
	}

	@Override
	public int cacheHashCode() {
		int result = super.cacheHashCode();
		result = 31 * result + SqmCacheable.cacheHashCode( passingExpressions );
		return result;
	}
}
