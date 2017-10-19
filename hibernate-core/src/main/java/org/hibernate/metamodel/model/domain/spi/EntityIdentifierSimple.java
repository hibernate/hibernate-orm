/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.spi;

import org.hibernate.query.sqm.tree.expression.domain.SqmEntityIdentifierReferenceSimple;
import org.hibernate.query.sqm.tree.expression.domain.SqmEntityTypedReference;
import org.hibernate.query.sqm.tree.expression.domain.SqmNavigableContainerReference;
import org.hibernate.query.sqm.tree.expression.domain.SqmNavigableReference;
import org.hibernate.query.sqm.tree.from.SqmFrom;

/**
 * @author Steve Ebersole
 */
public interface EntityIdentifierSimple<O,J>
		extends EntityIdentifier<O,J>, SingularPersistentAttribute<O,J>, BasicValuedNavigable<J> {
	@Override
	default void visitNavigable(NavigableVisitationStrategy visitor) {
		visitor.visitSimpleIdentifier( this );
	}

	@Override
	default PersistentAttributeType getPersistentAttributeType() {
		return PersistentAttributeType.BASIC;
	}

	@Override
	default PersistenceType getPersistenceType() {
		return PersistenceType.BASIC;
	}

	@Override
	default boolean matchesNavigableName(String navigableName) {
		return LEGACY_NAVIGABLE_ID.equals( navigableName )
				|| NAVIGABLE_ID.equals( navigableName )
				|| getAttributeName().equals( navigableName );
	}

	@Override
	default SqmNavigableReference createSqmExpression(
			SqmFrom sourceSqmFrom,
			SqmNavigableContainerReference containerReference,
			SqmReferenceCreationContext creationContext) {
		return new SqmEntityIdentifierReferenceSimple(
				(SqmEntityTypedReference) containerReference,
				this
		);
	}
}
