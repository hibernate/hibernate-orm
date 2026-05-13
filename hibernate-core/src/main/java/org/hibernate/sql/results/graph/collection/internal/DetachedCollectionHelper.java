/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.results.graph.collection.internal;

import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.CollectionPart;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;

/**
 * Helper for producing plain Java collection values for collection-valued query results.
 *
 * @author Gavin King
 *
 * @since 8.0
 */
public final class DetachedCollectionHelper {
	public static Object loadAndCopy(
			PluralAttributeMapping attributeMapping,
			Object collectionKey,
			SharedSessionContractImplementor session,
			CollectionPart.Nature selectedPartNature) {
		if ( collectionKey == null ) {
			return null;
		}
		else {
			final var collectionDescriptor = attributeMapping.getCollectionDescriptor();
			final Object collection =
					collectionDescriptor.getCollectionType()
							.getCollection( collectionKey, session, null, true );
			if ( collection instanceof PersistentCollection<?> persistentCollection ) {
				session.initializeCollection( persistentCollection, false );
			}
			if ( collection == null ) {
				return null;
			}
			else if ( selectedPartNature != null ) {
				return collectionDescriptor.getCollectionSemantics()
						.copyPart( collection, collectionDescriptor, selectedPartNature );
			}
			else {
				return collectionDescriptor.getCollectionSemantics()
						.copy( collection, collectionDescriptor );
			}
		}
	}
}
