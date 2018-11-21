/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression.domain;

import java.util.function.Supplier;

import org.hibernate.metamodel.model.domain.spi.CollectionElementEmbedded;

/**
 * Specialized SqmRestrictedCollectionElementReference for embeddable-valued
 * collection elements
 *
 * @author Steve Ebersole
 */
public interface SqmRestrictedCollectionElementReferenceEmbedded
		extends SqmRestrictedCollectionElementReference, SqmEmbeddableTypedReference {
	@Override
	CollectionElementEmbedded getExpressableType();

	@Override
	Supplier<? extends CollectionElementEmbedded> getInferableType();

	@Override
	CollectionElementEmbedded getReferencedNavigable();
}
