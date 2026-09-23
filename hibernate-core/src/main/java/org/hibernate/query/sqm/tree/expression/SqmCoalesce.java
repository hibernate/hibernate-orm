/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.expression;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import jakarta.annotation.Nonnull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.query.criteria.JpaCoalesce;
import org.hibernate.query.criteria.JpaExpression;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.SqmBindableType;
import org.hibernate.query.sqm.function.SqmFunctionDescriptor;
import org.hibernate.query.sqm.tree.SqmCacheable;
import org.hibernate.query.sqm.tree.SqmCopyContext;

import jakarta.persistence.criteria.Expression;
import org.hibernate.query.sqm.tree.SqmRenderContext;
import org.hibernate.type.descriptor.java.JavaType;

import static org.hibernate.internal.util.collections.CollectionHelper.isEmpty;
import static org.hibernate.query.internal.QueryHelper.highestPrecedenceType2;

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
		internalApplyInferableType(
				highestPrecedenceType2( getNodeType(), expression.getNodeType() ),
				highestPrecedenceType2( getJavaTypeDescriptor(), expression.getJavaTypeDescriptor() )
		);
	}

	@Override
	protected void internalApplyInferableType(@Nullable SqmBindableType<?> newType) {
		super.internalApplyInferableType( newType );
		for ( var argument : arguments ) {
			argument.applyInferableType( newType );
		}
	}

	@Override
	protected void internalApplyInferableType(@Nullable SqmBindableType<?> newType, @Nullable JavaType<?> newJavaType) {
		super.internalApplyInferableType( newType, newJavaType );
		for ( var argument : arguments ) {
			argument.applyInferableType( newType, newJavaType );
		}
	}

	@Nonnull
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

	@Override
	public SqmCoalesce<T> value(Expression<? extends T> value) {
		value( (SqmExpression<? extends T>) value );
		return this;
	}

	@Override
	public SqmCoalesce<T> value(JpaExpression<? extends T> value) {
		//noinspection unchecked
		value( (SqmExpression<T>) value );
		return this;
	}

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
