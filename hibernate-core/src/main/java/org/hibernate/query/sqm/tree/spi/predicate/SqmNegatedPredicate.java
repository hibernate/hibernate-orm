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
public class SqmNegatedPredicate extends AbstractSqmPredicate {
	private final SqmPredicate wrappedPredicate;

	public SqmNegatedPredicate(@Nonnull SqmPredicate wrappedPredicate, @Nonnull NodeBuilder nodeBuilder) {
		super( nodeBuilder.getBooleanType(), nodeBuilder );
		this.wrappedPredicate = wrappedPredicate;
	}

	@Nonnull
	@Override
	public SqmNegatedPredicate copy(@Nonnull SqmCopyContext context) {
		final SqmNegatedPredicate existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final SqmNegatedPredicate predicate = context.registerCopy(
				this,
				new SqmNegatedPredicate(
						wrappedPredicate.copy( context ),
						nodeBuilder()
				)
		);
		copyTo( predicate, context );
		return predicate;
	}

	@Nonnull
	public SqmPredicate getWrappedPredicate() {
		return wrappedPredicate;
	}

	@Nonnull
	@Override
	public List<Expression<Boolean>> getExpressions() {
		final List<Expression<Boolean>> expressions = new ArrayList<>( 1 );
		expressions.add( wrappedPredicate );
		return expressions;
	}

	@Nullable
	@Override
	public <T> T accept(@Nonnull SemanticQueryWalker<T> walker) {
		return walker.visitNegatedPredicate( this );
	}

	@Override
	public void appendHqlString(@Nonnull StringBuilder hql, @Nonnull SqmRenderContext context) {
		hql.append( "not (" );
		wrappedPredicate.appendHqlString( hql, context );
		hql.append( ')' );
	}

	@Override
	public boolean equals(@Nullable Object object) {
		return object instanceof SqmNegatedPredicate that
			&& wrappedPredicate.equals( that.wrappedPredicate );
	}

	@Override
	public int hashCode() {
		return wrappedPredicate.hashCode();
	}

	@Override
	public boolean isCompatible(@Nullable Object object) {
		return object instanceof SqmNegatedPredicate that
			&& wrappedPredicate.isCompatible( that.wrappedPredicate );
	}

	@Override
	public int cacheHashCode() {
		return wrappedPredicate.cacheHashCode();
	}

	@Override
	public boolean isNegated() {
		return true;
	}

	@Nonnull
	@Override
	public SqmPredicate not() {
		return new SqmNegatedPredicate( this, nodeBuilder() );
	}
}
