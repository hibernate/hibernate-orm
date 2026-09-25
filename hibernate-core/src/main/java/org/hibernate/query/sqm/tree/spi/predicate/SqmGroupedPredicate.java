package org.hibernate.query.sqm.tree.spi.predicate;

import jakarta.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.query.sqm.spi.NodeBuilder;
import org.hibernate.query.sqm.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.spi.SqmCopyContext;

import jakarta.annotation.Nonnull;
import jakarta.persistence.criteria.Expression;
import org.hibernate.query.sqm.tree.spi.SqmRenderContext;

/**
 * @author Steve Ebersole
 */
public class SqmGroupedPredicate extends AbstractSqmPredicate {
	private final SqmPredicate subPredicate;

	public SqmGroupedPredicate(@Nonnull SqmPredicate subPredicate, @Nonnull NodeBuilder nodeBuilder) {
		super( subPredicate.getExpressible(), nodeBuilder );
		this.subPredicate = subPredicate;
	}

	@Nonnull
	@Override
	public SqmGroupedPredicate copy(@Nonnull SqmCopyContext context) {
		final SqmGroupedPredicate existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final SqmGroupedPredicate predicate = context.registerCopy(
				this,
				new SqmGroupedPredicate(
						subPredicate.copy( context ),
						nodeBuilder()
				)
		);
		copyTo( predicate, context );
		return predicate;
	}

	@Nonnull
	public SqmPredicate getSubPredicate() {
		return subPredicate;
	}

	@Nullable
	@Override
	public <T> T accept(@Nonnull SemanticQueryWalker<T> walker) {
		return walker.visitGroupedPredicate( this );
	}

	@Override
	public boolean isNegated() {
		return false;
	}

	@Nonnull
	@Override
	public List<Expression<Boolean>> getExpressions() {
		final List<Expression<Boolean>> expressions = new ArrayList<>( 1 );
		expressions.add( subPredicate );
		return expressions;
	}

	@Nonnull
	@Override
	public SqmPredicate not() {
		return new SqmNegatedPredicate( this, nodeBuilder() );
	}

	@Override
	public void appendHqlString(@Nonnull StringBuilder hql, @Nonnull SqmRenderContext context) {
		hql.append( '(' );
		subPredicate.appendHqlString( hql, context );
		hql.append( ')' );
	}

	@Override
	public boolean equals(@Nullable Object object) {
		return object instanceof SqmGroupedPredicate that
			&& subPredicate.equals( that.subPredicate );
	}

	@Override
	public int hashCode() {
		return subPredicate.hashCode();
	}

	@Override
	public boolean isCompatible(@Nullable Object object) {
		return object instanceof SqmGroupedPredicate that
			&& subPredicate.isCompatible( that.subPredicate );
	}

	@Override
	public int cacheHashCode() {
		return subPredicate.cacheHashCode();
	}
}
