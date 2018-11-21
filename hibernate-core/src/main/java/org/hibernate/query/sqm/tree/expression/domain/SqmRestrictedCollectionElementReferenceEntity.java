/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression.domain;

import java.util.function.Supplier;

import org.hibernate.metamodel.model.domain.spi.CollectionElementEntity;

/**
 * Specialized SqmRestrictedCollectionElementReference for entity-valued
 * collection elements
 *
 * @author Steve Ebersole
 */
public interface SqmRestrictedCollectionElementReferenceEntity
		extends SqmRestrictedCollectionElementReference, SqmNavigableContainerReference, SqmEntityTypedReference {
	@Override
	CollectionElementEntity getReferencedNavigable();

	@Override
	CollectionElementEntity getExpressableType();

	@Override
	Supplier<? extends CollectionElementEntity> getInferableType();
}
