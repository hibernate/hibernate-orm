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
import org.hibernate.query.sqm.tree.spi.domain.SqmPluralValuedSimplePath;


/**
 * @author Steve Ebersole
 */
public class SqmEmptinessPredicate extends AbstractNegatableSqmPredicate {
	private final SqmPluralValuedSimplePath<?> pluralPath;

	public SqmEmptinessPredicate(
			@Nonnull SqmPluralValuedSimplePath<?> pluralPath,
			boolean negated,
			@Nonnull NodeBuilder nodeBuilder) {
		super( negated, nodeBuilder );
		this.pluralPath = pluralPath;
	}

	@Nonnull
	@Override
	public SqmEmptinessPredicate copy(@Nonnull SqmCopyContext context) {
		final SqmEmptinessPredicate existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final SqmEmptinessPredicate predicate = context.registerCopy(
				this,
				new SqmEmptinessPredicate(
						pluralPath.copy( context ),
						isNegated(),
						nodeBuilder()
				)
		);
		copyTo( predicate, context );
		return predicate;
	}

	@Nonnull
	public SqmPluralValuedSimplePath<?> getPluralPath() {
		return pluralPath;
	}

	@Nullable
	@Override
	public <T> T accept(@Nonnull SemanticQueryWalker<T> walker) {
		return walker.visitIsEmptyPredicate( this );
	}

	@Override
	public void appendHqlString(@Nonnull StringBuilder hql, @Nonnull SqmRenderContext context) {
		pluralPath.appendHqlString( hql, context );
		if ( isNegated() ) {
			hql.append( " is not empty" );
		}
		else {
			hql.append( " is empty" );
		}
	}

	@Override
	public boolean equals(@Nullable Object object) {
		return object instanceof SqmEmptinessPredicate that
			&& this.isNegated() == that.isNegated()
			&& pluralPath.equals( that.pluralPath );
	}

	@Override
	public int hashCode() {
		int result = Boolean.hashCode( isNegated() );
		result = 31 * result + pluralPath.hashCode();
		return result;
	}

	@Override
	public boolean isCompatible(@Nullable Object object) {
		return object instanceof SqmEmptinessPredicate that
			&& this.isNegated() == that.isNegated()
			&& pluralPath.isCompatible( that.pluralPath );
	}

	@Override
	public int cacheHashCode() {
		int result = Boolean.hashCode( isNegated() );
		result = 31 * result + pluralPath.cacheHashCode();
		return result;
	}

	@Nonnull
	@Override
	protected SqmNegatablePredicate createNegatedNode() {
		return new SqmEmptinessPredicate( pluralPath, !isNegated(), nodeBuilder() );
	}
}
