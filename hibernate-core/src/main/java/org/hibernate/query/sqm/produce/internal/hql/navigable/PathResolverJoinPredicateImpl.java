/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.produce.internal.hql.navigable;

import org.hibernate.query.sqm.domain.SqmAttribute;
import org.hibernate.query.sqm.domain.SqmPluralAttribute;
import org.hibernate.query.sqm.domain.SqmPluralAttributeElement.ElementClassification;
import org.hibernate.query.sqm.domain.SqmSingularAttribute;
import org.hibernate.query.sqm.domain.SqmSingularAttribute.SingularAttributeClassification;
import org.hibernate.query.sqm.SemanticException;
import org.hibernate.query.sqm.produce.spi.ResolutionContext;
import org.hibernate.query.sqm.tree.expression.domain.SqmNavigableReference;
import org.hibernate.query.sqm.tree.expression.domain.SqmNavigableSourceReference;
import org.hibernate.query.sqm.tree.from.SqmFromExporter;
import org.hibernate.query.sqm.tree.from.SqmQualifiedJoin;

/**
 * PathResolver implementation for paths found in a join predicate.
 *
 * @author Steve Ebersole
 */
public class PathResolverJoinPredicateImpl extends PathResolverBasicImpl {
	private final SqmQualifiedJoin joinRhs;

	public PathResolverJoinPredicateImpl(
			ResolutionContext resolutionContext,
			SqmQualifiedJoin joinRhs) {
		super( resolutionContext );
		this.joinRhs = joinRhs;
	}

	@Override
	public boolean canReuseImplicitJoins() {
		return false;
	}

	@Override
	@SuppressWarnings("StatementWithEmptyBody")
	protected void validatePathRoot(SqmNavigableReference binding) {
		// make sure no incoming FromElement comes from a FromElementSpace other
		// than the FromElementSpace joinRhs comes from
		if ( joinRhs.getContainingSpace() != ( (SqmFromExporter) binding ).getExportedFromElement().getContainingSpace() ) {
			throw new SemanticException(
					"Qualified join predicate referred to FromElement [" +
							binding.asLoggableText() + "] outside the FromElementSpace containing the join"
			);
		}
	}

	@Override
	protected void validateIntermediateAttributeJoin(
			SqmNavigableSourceReference sourceBinding,
			SqmAttribute joinedAttribute) {
		super.validateIntermediateAttributeJoin( sourceBinding, joinedAttribute );

		if ( SqmSingularAttribute.class.isInstance( joinedAttribute ) ) {
			final SqmSingularAttribute attrRef = (SqmSingularAttribute) joinedAttribute;
			if ( attrRef.getAttributeTypeClassification() == SingularAttributeClassification.ANY
					|| attrRef.getAttributeTypeClassification() == SingularAttributeClassification.MANY_TO_ONE
					| attrRef.getAttributeTypeClassification() == SingularAttributeClassification.ONE_TO_ONE ) {
				throw new SemanticException(
						"On-clause predicate of a qualified join cannot contain implicit entity joins : " +
								joinedAttribute.getAttributeName()
				);
			}
		}
		else {
			final SqmPluralAttribute attrRef = (SqmPluralAttribute) joinedAttribute;
			if ( attrRef.getElementDescriptor().getClassification() == ElementClassification.ANY
					|| attrRef.getElementDescriptor().getClassification() == ElementClassification.ONE_TO_MANY
					|| attrRef.getElementDescriptor().getClassification() == ElementClassification.MANY_TO_MANY ) {
				throw new SemanticException(
						"On-clause predicate of a qualified join cannot contain implicit collection joins : " +
								joinedAttribute.getAttributeName()
				);
			}
		}
	}
}
