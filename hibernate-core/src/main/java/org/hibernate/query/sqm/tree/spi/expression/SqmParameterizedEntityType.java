/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.spi.expression;

import jakarta.annotation.Nullable;
import org.hibernate.query.sqm.spi.NodeBuilder;
import org.hibernate.query.sqm.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.spi.SqmBindableType;
import org.hibernate.query.sqm.tree.spi.SqmCopyContext;
import org.hibernate.query.sqm.tree.spi.SqmRenderContext;
import org.hibernate.query.sqm.tree.spi.select.SqmSelectableNode;


/**
 * Entity type expression based on a parameter - `TYPE( :someParam )`
 *
 * @author Steve Ebersole
 */
public class SqmParameterizedEntityType<T> extends AbstractSqmExpression<T> implements SqmSelectableNode<T> {
	private final SqmParameter<T> discriminatorSource;

	public SqmExpression<T> getDiscriminatorSource() {
		return discriminatorSource;
	}

	public SqmParameterizedEntityType(SqmParameter<T> parameterExpression, NodeBuilder nodeBuilder) {
		super( SqmExpressionHelper.toSqmType( parameterExpression.getAnticipatedType(), nodeBuilder ), nodeBuilder );
		this.discriminatorSource = parameterExpression;
	}

	@Override
	public SqmParameterizedEntityType<T> copy(SqmCopyContext context) {
		final var existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final SqmParameterizedEntityType<T> expression = context.registerCopy(
				this,
				new SqmParameterizedEntityType<>(
						discriminatorSource.copy( context ),
						nodeBuilder()
				)
		);
		copyTo( expression, context );
		return expression;
	}

	@Override
	public void internalApplyInferableType(@Nullable SqmBindableType<?> type) {
		setExpressibleType( type );
		discriminatorSource.applyInferableType( type );
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitParameterizedEntityTypeExpression( this );
	}

	@Override
	public void appendHqlString(StringBuilder hql, SqmRenderContext context) {
		hql.append( "type(" );
		discriminatorSource.appendHqlString( hql, context );
		hql.append( ')' );
	}

	@Override
	public boolean equals(@Nullable Object object) {
		return object instanceof SqmParameterizedEntityType<?> that
			&& discriminatorSource.equals( that.discriminatorSource );
	}

	@Override
	public int hashCode() {
		return discriminatorSource.hashCode();
	}

	@Override
	public boolean isCompatible(Object object) {
		return object instanceof SqmParameterizedEntityType<?> that
			&& discriminatorSource.isCompatible( that.discriminatorSource );
	}

	@Override
	public int cacheHashCode() {
		return discriminatorSource.cacheHashCode();
	}
}
