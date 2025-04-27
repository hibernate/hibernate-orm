/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.expression;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.hibernate.query.criteria.JpaCoalesce;
import org.hibernate.query.criteria.JpaExpression;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.SqmExpressible;
import org.hibernate.query.sqm.function.SqmFunctionDescriptor;
import org.hibernate.query.sqm.tree.SqmCopyContext;

import jakarta.persistence.criteria.Expression;
import org.hibernate.query.sqm.tree.SqmRenderContext;

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

	public SqmCoalesce(SqmExpressible<T> type, NodeBuilder nodeBuilder) {
		super( type, nodeBuilder );
		functionDescriptor = nodeBuilder.getQueryEngine().getSqmFunctionRegistry().findFunctionDescriptor( "coalesce" );
		this.arguments = new ArrayList<>();
	}

	public SqmCoalesce(SqmExpressible<T> type, int numberOfArguments, NodeBuilder nodeBuilder) {
		super( type, nodeBuilder );
		functionDescriptor = nodeBuilder.getQueryEngine().getSqmFunctionRegistry().findFunctionDescriptor( "coalesce" );
		this.arguments = new ArrayList<>( numberOfArguments );
	}

	@Override
	public SqmCoalesce<T> copy(SqmCopyContext context) {
		final SqmCoalesce<T> existing = context.getCopy( this );
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
	public boolean equals(Object object) {
		return object instanceof SqmCoalesce<?> that
			&& Objects.equals( this.arguments, that.arguments );
	}

	@Override
	public int hashCode() {
		return arguments.hashCode();
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// JPA

	@Override
	public SqmCoalesce<T> value(T value) {
		value( nodeBuilder().value( value, firstOrNull() ) );
		return this;
	}

	private SqmExpression<T> firstOrNull() {
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
