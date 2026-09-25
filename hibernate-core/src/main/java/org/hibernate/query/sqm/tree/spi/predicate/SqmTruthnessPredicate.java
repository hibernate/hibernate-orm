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
public class SqmTruthnessPredicate extends AbstractNegatableSqmPredicate {
	private final SqmExpression<?> expression;
	private final boolean value;

	public SqmTruthnessPredicate(@Nonnull SqmExpression<?> expression, boolean value, boolean negated, @Nonnull NodeBuilder nodeBuilder) {
		super( negated, nodeBuilder );
		this.expression = expression;
		this.value = value;
	}

	public boolean getBooleanValue() {
		return value;
	}

	@Nonnull
	@Override
	public SqmTruthnessPredicate copy(@Nonnull SqmCopyContext context) {
		final SqmTruthnessPredicate existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final SqmTruthnessPredicate predicate = context.registerCopy(
				this,
				new SqmTruthnessPredicate(
						expression.copy( context ),
						getBooleanValue(),
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
		return walker.visitIsTruePredicate( this );
	}

	@Override
	public void appendHqlString(@Nonnull StringBuilder hql, @Nonnull SqmRenderContext context) {
		expression.appendHqlString( hql, context );
		hql.append(" is ");
		if ( isNegated() ) {
			hql.append( "not " );
		}
		hql.append( getBooleanValue() );
	}

	@Override
	public boolean equals(@Nullable Object object) {
		return object instanceof SqmTruthnessPredicate that
			&& this.isNegated() == that.isNegated()
			&& this.value == that.value
			&& this.expression.equals( that.expression );
	}

	@Override
	public int hashCode() {
		int result = Boolean.hashCode( isNegated() );
		result = 31 * result + expression.hashCode();
		result = 31 * result + Boolean.hashCode( value );
		return result;
	}

	@Override
	public boolean isCompatible(@Nullable Object object) {
		return object instanceof SqmTruthnessPredicate that
			&& this.isNegated() == that.isNegated()
			&& this.value == that.value
			&& this.expression.isCompatible( that.expression );
	}

	@Override
	public int cacheHashCode() {
		int result = Boolean.hashCode( isNegated() );
		result = 31 * result + expression.cacheHashCode();
		result = 31 * result + Boolean.hashCode( value );
		return result;
	}

	@Nonnull
	@Override
	protected SqmNegatablePredicate createNegatedNode() {
		return new SqmTruthnessPredicate( expression, getBooleanValue(), !isNegated(), nodeBuilder() );
	}
}
