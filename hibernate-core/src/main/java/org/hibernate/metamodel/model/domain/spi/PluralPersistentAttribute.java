/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.spi;

import javax.persistence.metamodel.Type;

import org.hibernate.query.sqm.produce.spi.SqmCreationContext;
import org.hibernate.query.sqm.tree.expression.domain.SqmNavigableContainerReference;
import org.hibernate.query.sqm.tree.expression.domain.SqmPluralAttributeReference;
import org.hibernate.query.sqm.tree.from.SqmFrom;
import org.hibernate.sql.ast.produce.metamodel.spi.Fetchable;

/**
 * @author Steve Ebersole
 */
public interface PluralPersistentAttribute<O,C,E>
		extends NonIdPersistentAttribute<O,C>, NavigableContainer<C>,
		javax.persistence.metamodel.PluralAttribute<O,C,E>, Fetchable<C> {

	PersistentCollectionDescriptor<O,C,E> getPersistentCollectionDescriptor();

	@Override
	Class<C> getJavaType();

	@Override
	default void visitNavigable(NavigableVisitationStrategy visitor) {
		visitor.visitPluralAttribute( this );
	}

	@Override
	default PersistenceType getPersistenceType() {
		return getPersistentCollectionDescriptor().getPersistenceType();
	}

	@Override
	default CollectionType getCollectionType() {
		return getPersistentCollectionDescriptor().getCollectionClassification().toJpaClassification();
	}

	@Override
	default Type<E> getElementType() {
		return getPersistentCollectionDescriptor().getElementDescriptor();
	}

	@Override
	SqmPluralAttributeReference createSqmExpression(
			SqmFrom sourceSqmFrom,
			SqmNavigableContainerReference containerReference,
			SqmCreationContext creationContext);
}
