/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.spi.predicate;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.hibernate.query.sqm.spi.NodeBuilder;
import org.hibernate.query.sqm.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.spi.SqmCopyContext;
import org.hibernate.query.sqm.tree.spi.SqmRenderContext;
import org.hibernate.query.sqm.tree.spi.expression.SqmExpression;


/**
 * @author Gavin King
 */
public class SqmExistsPredicate extends AbstractNegatableSqmPredicate {
	private final SqmExpression<?> expression;

	public SqmExistsPredicate(
			@Nonnull SqmExpression<?> expression,
			@Nonnull NodeBuilder nodeBuilder) {
		this( expression, false, nodeBuilder );
	}

	public SqmExistsPredicate(
			@Nonnull SqmExpression<?> expression,
			boolean negated,
			@Nonnull NodeBuilder nodeBuilder) {
		super( negated, nodeBuilder );
		this.expression = expression;

		expression.applyInferableType( expression.getNodeType(), expression.getJavaTypeDescriptor() );
	}

	@Nonnull
	@Override
	public SqmExistsPredicate copy(@Nonnull SqmCopyContext context) {
		final SqmExistsPredicate existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final SqmExistsPredicate predicate = context.registerCopy(
				this,
				new SqmExistsPredicate(
						expression.copy( context ),
						isNegated(),
						nodeBuilder()
				)
		);
		copyTo( predicate, context );
		return predicate;
	}

	@Nonnull
	public SqmExpression<?> getExpression() {
		return expression;
	}

	@Nullable
	@Override
	public <T> T accept(@Nonnull SemanticQueryWalker<T> walker) {
		return walker.visitExistsPredicate( this );
	}

	@Override
	public void appendHqlString(@Nonnull StringBuilder hql, @Nonnull SqmRenderContext context) {
		if ( isNegated() ) {
			hql.append( "not exists " );
		}
		else {
			hql.append( "exists " );
		}
		expression.appendHqlString( hql, context );
	}

	@Override
	public boolean equals(@Nullable Object object) {
		return object instanceof SqmExistsPredicate that
			&& this.isNegated() == that.isNegated()
			&& this.expression.equals( that.expression );
	}

	@Override
	public int hashCode() {
		int result = Boolean.hashCode( isNegated() );
		result = 31 * result + expression.hashCode();
		return result;
	}

	@Override
	public boolean isCompatible(@Nullable Object object) {
		return object instanceof SqmExistsPredicate that
			&& this.isNegated() == that.isNegated()
			&& this.expression.isCompatible( that.expression );
	}

	@Override
	public int cacheHashCode() {
		int result = Boolean.hashCode( isNegated() );
		result = 31 * result + expression.cacheHashCode();
		return result;
	}

	@Nonnull
	@Override
	protected SqmNegatablePredicate createNegatedNode() {
		return new SqmExistsPredicate( expression, !isNegated(), nodeBuilder() );
	}
}
