/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.produce.internal.hql.navigable;

import org.hibernate.metamodel.model.domain.spi.PersistentAttribute;
import org.hibernate.metamodel.model.domain.spi.PluralPersistentAttribute;
import org.hibernate.metamodel.model.domain.spi.SingularPersistentAttribute;
import org.hibernate.metamodel.model.domain.spi.SingularPersistentAttribute.SingularAttributeClassification;
import org.hibernate.query.sqm.SemanticException;
import org.hibernate.query.sqm.produce.spi.ResolutionContext;
import org.hibernate.query.sqm.tree.expression.domain.SqmNavigableReference;
import org.hibernate.query.sqm.tree.expression.domain.SqmNavigableContainerReference;
import org.hibernate.query.sqm.tree.from.SqmFromExporter;
import org.hibernate.query.sqm.tree.from.SqmQualifiedJoin;

import static org.hibernate.metamodel.model.domain.spi.CollectionElement.*;

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

	protected void validateIntermediateAttributeJoin(
			SqmNavigableContainerReference sourceBinding,
			PersistentAttribute joinedAttribute) {
		super.validateIntermediateAttributeJoin( sourceBinding, joinedAttribute );

		if ( SingularPersistentAttribute.class.isInstance( joinedAttribute ) ) {
			final SingularPersistentAttribute attrRef = (SingularPersistentAttribute) joinedAttribute;
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
			final PluralPersistentAttribute attrRef = (PluralPersistentAttribute) joinedAttribute;
			if ( attrRef.getPersistentCollectionDescriptor().getElementDescriptor().getClassification() == ElementClassification.ANY
					|| attrRef.getPersistentCollectionDescriptor().getElementDescriptor().getClassification() == ElementClassification.ONE_TO_MANY
					|| attrRef.getPersistentCollectionDescriptor().getElementDescriptor().getClassification() == ElementClassification.MANY_TO_MANY ) {
				throw new SemanticException(
						"On-clause predicate of a qualified join cannot contain implicit collection joins : " +
								joinedAttribute.getAttributeName()
				);
			}
		}
	}
}
