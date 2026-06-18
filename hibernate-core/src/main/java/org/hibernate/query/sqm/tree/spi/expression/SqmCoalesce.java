/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.spi.expression;

import jakarta.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.hibernate.query.criteria.JpaCoalesce;
import org.hibernate.query.criteria.JpaExpression;
import org.hibernate.query.sqm.spi.NodeBuilder;
import org.hibernate.query.sqm.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.spi.SqmBindableType;
import org.hibernate.query.sqm.function.SqmFunctionDescriptor;
import org.hibernate.query.sqm.tree.spi.SqmCacheable;
import org.hibernate.query.sqm.tree.spi.SqmCopyContext;

import jakarta.annotation.Nonnull;
import jakarta.persistence.criteria.Expression;
import org.hibernate.query.sqm.tree.spi.SqmRenderContext;

import static org.hibernate.internal.util.collections.CollectionHelper.isEmpty;

/**
 * @author Steve Ebersole
 * @author Gavin King
 */
public class SqmCoalesce<T> extends AbstractSqmExpression<T> implements JpaCoalesce<T> {
	private final SqmFunctionDescriptor functionDescriptor;
	private final List<SqmExpression<? extends T>> arguments;

	public SqmCoalesce(NodeBuilder nodeBuilder) {
		this( null, nodeBuilder );
	}

	public SqmCoalesce(@Nullable SqmBindableType<T> type, NodeBuilder nodeBuilder) {
		super( type, nodeBuilder );
		functionDescriptor = nodeBuilder.getQueryEngine().getSqmFunctionRegistry().getFunctionDescriptor( "coalesce" );
		this.arguments = new ArrayList<>();
	}

	public SqmCoalesce(@Nullable SqmBindableType<T> type, int numberOfArguments, NodeBuilder nodeBuilder) {
		super( type, nodeBuilder );
		functionDescriptor = nodeBuilder.getQueryEngine().getSqmFunctionRegistry().getFunctionDescriptor( "coalesce" );
		this.arguments = new ArrayList<>( numberOfArguments );
	}

	@Override
	public SqmCoalesce<T> copy(SqmCopyContext context) {
		final var existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final SqmCoalesce<T> coalesce = context.registerCopy(
				this,
				new SqmCoalesce<>(
						getNodeType(),
						arguments.size(),
						nodeBuilder()
				)
		);
		for ( SqmExpression<? extends T> argument : arguments ) {
			coalesce.arguments.add( argument.copy( context ) );
		}
		copyTo( coalesce, context );
		return coalesce;
	}

	public SqmFunctionDescriptor getFunctionDescriptor() {
		return functionDescriptor;
	}

	public void value(SqmExpression<? extends T> expression) {
		arguments.add( expression );
	}

	public List<SqmExpression<? extends T>> getArguments() {
		return arguments;
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitCoalesce( this );
	}

	@Override
	public String asLoggableText() {
		return "coalesce(...)";
	}

	@Override
	public void appendHqlString(StringBuilder hql, SqmRenderContext context) {
		hql.append( "coalesce(" );
		arguments.get( 0 ).appendHqlString( hql, context );
		for ( int i = 1; i < arguments.size(); i++ ) {
			hql.append(", ");
			arguments.get( i ).appendHqlString( hql, context );
		}
		hql.append( ')' );
	}

	@Override
	public boolean equals(@Nullable Object object) {
		return object instanceof SqmCoalesce<?> that
			&& Objects.equals( this.arguments, that.arguments );
	}

	@Override
	public int hashCode() {
		return Objects.hashCode( arguments );
	}

	@Override
	public boolean isCompatible(Object object) {
		return object instanceof SqmCoalesce<?> that
			&& SqmCacheable.areCompatible( this.arguments, that.arguments );
	}

	@Override
	public int cacheHashCode() {
		return SqmCacheable.cacheHashCode( arguments );
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// JPA

	@Nonnull
	@Override
	public SqmCoalesce<T> value(@Nullable T value) {
		value( nodeBuilder().value( value, firstOrNull() ) );
		return this;
	}

	private @Nullable SqmExpression<T> firstOrNull() {
		if ( isEmpty( arguments ) ) {
			return null;
		}
		else {
			//noinspection unchecked
			return (SqmExpression<T>) arguments.get( 0 );
		}
	}

	@Nonnull
	@Override
	public SqmCoalesce<T> value(@Nonnull Expression<? extends T> value) {
		value( (SqmExpression<? extends T>) value );
		return this;
	}

	@Nonnull
	@Override
	public SqmCoalesce<T> value(@Nonnull JpaExpression<? extends T> value) {
		//noinspection unchecked
		value( (SqmExpression<T>) value );
		return this;
	}

	@Nonnull
	@Override
	@SuppressWarnings("unchecked")
	public SqmCoalesce<T> values(T... values) {
		final SqmExpression<T> firstOrNull = firstOrNull();
		for ( T value : values ) {
			value( nodeBuilder().value( value, firstOrNull ) );
		}
		return this;
	}
}
