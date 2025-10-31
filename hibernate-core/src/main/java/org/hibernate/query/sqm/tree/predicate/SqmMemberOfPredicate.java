/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.predicate;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.metamodel.model.domain.SimpleDomainType;
import org.hibernate.query.SemanticException;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.SqmBindableType;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.SqmRenderContext;
import org.hibernate.query.sqm.tree.domain.SqmPluralValuedSimplePath;
import org.hibernate.query.sqm.tree.expression.SqmExpression;


import static org.hibernate.internal.util.NullnessUtil.castNonNull;
import static org.hibernate.query.sqm.internal.TypecheckUtil.areTypesComparable;

/**
 * @author Steve Ebersole
 */
public class SqmMemberOfPredicate extends AbstractNegatableSqmPredicate {
	private final SqmExpression<?> leftHandExpression;
	private final SqmPluralValuedSimplePath<?> pluralPath;

	public SqmMemberOfPredicate(SqmExpression<?> leftHandExpression, SqmPluralValuedSimplePath<?> pluralPath, NodeBuilder nodeBuilder) {
		this( leftHandExpression, pluralPath, false, nodeBuilder );
	}

	public SqmMemberOfPredicate(
			SqmExpression<?> leftHandExpression,
			SqmPluralValuedSimplePath<?> pluralPath,
			boolean negated,
			NodeBuilder nodeBuilder) {
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

	@Override
	public SqmMemberOfPredicate copy(SqmCopyContext context) {
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

	public SqmExpression<?> getLeftHandExpression() {
		return leftHandExpression;
	}

	public SqmPluralValuedSimplePath<?> getPluralPath() {
		return pluralPath;
	}

	@Override
	public <T> T accept(SemanticQueryWalker<T> walker) {
		return walker.visitMemberOfPredicate( this );
	}

	@Override
	public void appendHqlString(StringBuilder hql, SqmRenderContext context) {
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
	public boolean isCompatible(Object object) {
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

	@Override
	protected SqmNegatablePredicate createNegatedNode() {
		return new SqmMemberOfPredicate( leftHandExpression, pluralPath, !isNegated(), nodeBuilder() );
	}
}
