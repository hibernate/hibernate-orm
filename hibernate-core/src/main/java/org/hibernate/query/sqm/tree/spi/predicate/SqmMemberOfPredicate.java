package org.hibernate.query.sqm.tree.spi.predicate;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.hibernate.metamodel.model.domain.SimpleDomainType;
import org.hibernate.query.SemanticException;
import org.hibernate.query.sqm.spi.NodeBuilder;
import org.hibernate.query.sqm.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.spi.SqmBindableType;
import org.hibernate.query.sqm.tree.spi.SqmCopyContext;
import org.hibernate.query.sqm.tree.spi.SqmRenderContext;
import org.hibernate.query.sqm.tree.spi.domain.SqmPluralValuedSimplePath;
import org.hibernate.query.sqm.tree.spi.expression.SqmExpression;


import static org.hibernate.internal.util.NullnessUtil.castNonNull;
import static org.hibernate.query.sqm.internal.TypecheckUtil.areTypesComparable;

/**
 * @author Steve Ebersole
 */
public class SqmMemberOfPredicate extends AbstractNegatableSqmPredicate {
	private final SqmExpression<?> leftHandExpression;
	private final SqmPluralValuedSimplePath<?> pluralPath;

	public SqmMemberOfPredicate(@Nonnull SqmExpression<?> leftHandExpression, @Nonnull SqmPluralValuedSimplePath<?> pluralPath, @Nonnull NodeBuilder nodeBuilder) {
		this( leftHandExpression, pluralPath, false, nodeBuilder );
	}

	public SqmMemberOfPredicate(
			@Nonnull SqmExpression<?> leftHandExpression,
			@Nonnull SqmPluralValuedSimplePath<?> pluralPath,
			boolean negated,
			@Nonnull NodeBuilder nodeBuilder) {
		super( negated, nodeBuilder );

		this.pluralPath = pluralPath;
		this.leftHandExpression = leftHandExpression;

		final SimpleDomainType<?> elementType = pluralPath.getPluralAttribute().getElementType();
		final SqmBindableType<?> simpleDomainType = nodeBuilder.resolveExpressible( elementType );

		if ( !areTypesComparable( leftHandExpression.getNodeType(), simpleDomainType, nodeBuilder ) ) {
			throw new SemanticException(
					String.format(
							"Cannot compare left expression of type '%s' with right expression of type '%s'",
							castNonNull( leftHandExpression.getNodeType() ).getTypeName(),
							pluralPath.getNodeType().getTypeName()
					)
			);
		}

		leftHandExpression.applyInferableType( simpleDomainType );
	}

	@Nonnull
	@Override
	public SqmMemberOfPredicate copy(@Nonnull SqmCopyContext context) {
		final SqmMemberOfPredicate existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final SqmMemberOfPredicate predicate = context.registerCopy(
				this,
				new SqmMemberOfPredicate(
						leftHandExpression.copy( context ),
						pluralPath.copy( context ),
						isNegated(),
						nodeBuilder()
				)
		);
		copyTo( predicate, context );
		return predicate;
	}

	@Nonnull
	public SqmExpression<?> getLeftHandExpression() {
		return leftHandExpression;
	}

	@Nonnull
	public SqmPluralValuedSimplePath<?> getPluralPath() {
		return pluralPath;
	}

	@Nullable
	@Override
	public <T> T accept(@Nonnull SemanticQueryWalker<T> walker) {
		return walker.visitMemberOfPredicate( this );
	}

	@Override
	public void appendHqlString(@Nonnull StringBuilder hql, @Nonnull SqmRenderContext context) {
		leftHandExpression.appendHqlString( hql, context );
		if ( isNegated() ) {
			hql.append( " not" );
		}
		hql.append( " member of " );
		pluralPath.appendHqlString( hql, context );
	}

	@Override
	public boolean equals(@Nullable Object object) {
		return object instanceof SqmMemberOfPredicate that
			&& this.isNegated() == that.isNegated()
			&& leftHandExpression.equals( that.leftHandExpression )
			&& pluralPath.equals( that.pluralPath );
	}

	@Override
	public int hashCode() {
		int result = Boolean.hashCode( isNegated() );
		result = 31 * result + leftHandExpression.hashCode();
		result = 31 * result + pluralPath.hashCode();
		return result;
	}

	@Override
	public boolean isCompatible(@Nullable Object object) {
		return object instanceof SqmMemberOfPredicate that
			&& this.isNegated() == that.isNegated()
			&& leftHandExpression.isCompatible( that.leftHandExpression )
			&& pluralPath.isCompatible( that.pluralPath );
	}

	@Override
	public int cacheHashCode() {
		int result = Boolean.hashCode( isNegated() );
		result = 31 * result + leftHandExpression.cacheHashCode();
		result = 31 * result + pluralPath.cacheHashCode();
		return result;
	}

	@Nonnull
	@Override
	protected SqmNegatablePredicate createNegatedNode() {
		return new SqmMemberOfPredicate( leftHandExpression, pluralPath, !isNegated(), nodeBuilder() );
	}
}
