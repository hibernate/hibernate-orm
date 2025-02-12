/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.predicate;

import org.hibernate.metamodel.model.domain.SimpleDomainType;
import org.hibernate.query.SemanticException;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.domain.SqmPluralValuedSimplePath;
import org.hibernate.query.sqm.tree.expression.SqmExpression;

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

		final SimpleDomainType<?> simpleDomainType = pluralPath.getPluralAttribute().getElementType();

		if ( !areTypesComparable(leftHandExpression.getNodeType(), simpleDomainType, nodeBuilder.getSessionFactory()) ) {
			throw new SemanticException(
					String.format(
							"Cannot compare left expression of type '%s' with right expression of type '%s'",
							leftHandExpression.getNodeType().getTypeName(),
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
	public void appendHqlString(StringBuilder sb) {
		leftHandExpression.appendHqlString( sb );
		if ( isNegated() ) {
			sb.append( " not" );
		}
		sb.append( " member of " );
		pluralPath.appendHqlString( sb );
	}

	@Override
	protected SqmNegatablePredicate createNegatedNode() {
		return new SqmMemberOfPredicate( leftHandExpression, pluralPath, !isNegated(), nodeBuilder() );
	}
}
