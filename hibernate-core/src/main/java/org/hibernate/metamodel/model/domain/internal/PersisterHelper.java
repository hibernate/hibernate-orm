/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.collection.spi.CollectionClassification;
import org.hibernate.mapping.Bag;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.IdentifierBag;

/**
 * For now mainly a helper for reflection into stuff not exposed on the entity/collection persister
 * contracts
 *
 * @author Steve Ebersole
 */
public class PersisterHelper {

	public static CollectionClassification interpretCollectionClassification(Collection collectionBinding) {
		if ( collectionBinding instanceof Bag || collectionBinding instanceof IdentifierBag ) {
			return CollectionClassification.BAG;
		}

		if ( collectionBinding instanceof org.hibernate.mapping.List ) {
			return CollectionClassification.LIST;
		}

		if ( collectionBinding instanceof org.hibernate.mapping.Set ) {
			return CollectionClassification.SET;
		}

		if ( collectionBinding instanceof org.hibernate.mapping.Map ) {
			return CollectionClassification.MAP;
		}

		final Class javaType = collectionBinding.getJavaTypeDescriptor().getJavaType();
		if ( Set.class.isAssignableFrom( javaType ) ) {
			return CollectionClassification.SET;
		}

		if ( Map.class.isAssignableFrom( javaType ) ) {
			return CollectionClassification.MAP;
		}

		if ( List.class.isAssignableFrom( javaType ) ) {
			return CollectionClassification.LIST;
		}

		return CollectionClassification.BAG;
	}
}
