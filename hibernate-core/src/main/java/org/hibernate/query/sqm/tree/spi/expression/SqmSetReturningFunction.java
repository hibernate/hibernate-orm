/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.spi.expression;

import jakarta.annotation.Nullable;
import jakarta.annotation.Nonnull;
import java.util.List;
import java.util.Objects;

import org.hibernate.Incubating;
import org.hibernate.query.criteria.JpaSetReturningFunction;
import org.hibernate.query.sqm.tree.spi.SqmCacheable;
import org.hibernate.query.sqm.tree.spi.SqmRenderContext;
import org.hibernate.query.sqm.tuple.internal.AnonymousTupleType;
import org.hibernate.query.sqm.spi.NodeBuilder;
import org.hibernate.query.sqm.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.function.SqmSetReturningFunctionDescriptor;
import org.hibernate.query.sqm.sql.spi.SqmToSqlAstConverter;
import org.hibernate.query.sqm.tree.spi.AbstractSqmNode;
import org.hibernate.query.sqm.tree.spi.SqmCopyContext;
import org.hibernate.query.sqm.tree.spi.SqmTypedNode;
import org.hibernate.query.sqm.tree.spi.SqmVisitableNode;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.spi.query.from.TableGroup;

/**
 * A SQM set-returning function
 *
 * @since 7.0
 */
@Incubating(since = "6.2")
public abstract class SqmSetReturningFunction<T> extends AbstractSqmNode
		implements SqmVisitableNode, JpaSetReturningFunction<T> {
	// this function-name is the one used to resolve the descriptor from
	// the function registry (which may or may not be a db function name)
	private final String functionName;
	private final SqmSetReturningFunctionDescriptor functionDescriptor;

	private final List<? extends SqmTypedNode<?>> arguments;

	public SqmSetReturningFunction(
			@Nonnull String functionName,
			@Nonnull SqmSetReturningFunctionDescriptor functionDescriptor,
			@Nonnull List<? extends SqmTypedNode<?>> arguments,
			@Nonnull NodeBuilder criteriaBuilder) {
		super( criteriaBuilder );
		this.functionName = functionName;
		this.functionDescriptor = functionDescriptor;
		this.arguments = arguments;
	}

	@Nonnull
	@Override
	public abstract SqmSetReturningFunction<T> copy(@Nonnull SqmCopyContext context);

	@Nonnull
	public SqmSetReturningFunctionDescriptor getFunctionDescriptor() {
		return functionDescriptor;
	}

	@Nonnull
	@Override
	public String getFunctionName() {
		return functionName;
	}

	@Nonnull
	public abstract AnonymousTupleType<T> getType();

	@Nonnull
	public List<? extends SqmTypedNode<?>> getArguments() {
		return arguments;
	}

	@Nonnull
	public abstract TableGroup convertToSqlAst(
			@Nonnull NavigablePath navigablePath,
			@Nonnull String identifierVariable,
			boolean lateral,
			boolean canUseInnerJoins,
			boolean withOrdinality,
			@Nonnull SqmToSqlAstConverter walker);

	@Nullable
	@Override
	public <X> X accept(@Nonnull SemanticQueryWalker<X> walker) {
		return walker.visitSetReturningFunction( this );
	}

	@Override
	public void appendHqlString(@Nonnull StringBuilder hql, @Nonnull SqmRenderContext context) {
		hql.append( functionName );
		if ( arguments.isEmpty() ) {
			hql.append( "()" );
		}
		else {
			hql.append( '(' );
			arguments.get( 0 ).appendHqlString( hql, context );
			for ( int i = 1; i < arguments.size(); i++ ) {
				hql.append( ", " );
				arguments.get( i ).appendHqlString( hql, context );
			}
			hql.append( ')' );
		}
	}

	@Override
	public boolean equals(@Nullable Object object) {
		return object instanceof SqmSetReturningFunction<?> that
			&& this.getClass() == that.getClass()
			&& this.functionName.equals( that.functionName )
			&& Objects.equals( this.arguments, that.arguments );
	}

	@Override
	public int hashCode() {
		int result = functionName.hashCode();
		result = 31 * result + Objects.hashCode( arguments );
		return result;
	}

	@Override
	public boolean isCompatible(@Nullable Object object) {
		return object instanceof SqmSetReturningFunction<?> that
			&& this.getClass() == that.getClass()
			&& this.functionName.equals( that.functionName )
			&& SqmCacheable.areCompatible( this.arguments, that.arguments );
	}

	@Override
	public int cacheHashCode() {
		int result = functionName.hashCode();
		result = 31 * result + SqmCacheable.cacheHashCode( arguments );
		return result;
	}
}
