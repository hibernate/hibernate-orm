/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.collection.internal;

import java.util.EnumMap;

import org.hibernate.HibernateException;
import org.hibernate.collection.UnknownClassificationException;
import org.hibernate.collection.spi.CollectionClassification;
import org.hibernate.collection.spi.PersistentCollectionRepresentation;
import org.hibernate.collection.spi.PersistentCollectionRepresentationResolver;
import org.hibernate.mapping.Array;
import org.hibernate.mapping.Bag;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.IdentifierBag;
import org.hibernate.mapping.List;
import org.hibernate.mapping.Set;

/**
 * Standard Hibernate PersistentCollectionRepresentationFactory implementation
 *
 * @author Steve Ebersole
 */
public class PersistentCollectionRepresentationResolverImpl implements PersistentCollectionRepresentationResolver {
	private final EnumMap<CollectionClassification,PersistentCollectionRepresentation> standardRepresentations = new EnumMap<>( CollectionClassification.class );

	public PersistentCollectionRepresentationResolverImpl() {
		standardRepresentations.put( CollectionClassification.ARRAY, PersistentArrayRepresentation.INSTANCE );
		standardRepresentations.put( CollectionClassification.BAG, PersistentBagRepresentation.INSTANCE );
		standardRepresentations.put( CollectionClassification.IDBAG, PersistentIdentifierBagRepresentation.INSTANCE );
		standardRepresentations.put( CollectionClassification.LIST, PersistentListRepresentation.INSTANCE );
		standardRepresentations.put( CollectionClassification.MAP, PersistentMapRepresentation.INSTANCE );
		standardRepresentations.put( CollectionClassification.SET, PersistentSetRepresentation.INSTANCE );

		standardRepresentations.put( CollectionClassification.SORTED_MAP, PersistentSortedMapRepresentation.INSTANCE );
		standardRepresentations.put( CollectionClassification.SORTED_SET, PersistentSortedSetRepresentation.INSTANCE );

		standardRepresentations.put( CollectionClassification.ORDERED_MAP, PersistentOrderedMapRepresentation.INSTANCE );
		standardRepresentations.put( CollectionClassification.ORDERED_SET, PersistentOrderedSetRepresentation.INSTANCE );
	}

	@Override
	public PersistentCollectionRepresentation resolveRepresentation(Collection bootDescriptor) {
		final CollectionClassification classification = determineClassification( bootDescriptor );
		final PersistentCollectionRepresentation representation = standardRepresentations.get( classification );
		if ( representation == null ) {
			throw new UnknownClassificationException( classification );
		}
		return representation;
	}

	@SuppressWarnings("WeakerAccess")
	protected CollectionClassification determineClassification(Collection bootDescriptor) {
		// todo (6.0) : allow to define a specific representation on the collection mapping
		//
		// for now use the default

		if ( List.class.isInstance( bootDescriptor ) ) {
			return CollectionClassification.LIST;
		}

		if ( Set.class.isInstance( bootDescriptor ) ) {
			if ( bootDescriptor.isSorted() ) {
				return CollectionClassification.SORTED_SET;
			}

			if ( bootDescriptor.hasOrder() ) {
				return CollectionClassification.ORDERED_SET;
			}

			return CollectionClassification.SET;
		}

		if ( Bag.class.isInstance( bootDescriptor ) ) {
			return CollectionClassification.BAG;
		}

		if ( IdentifierBag.class.isInstance( bootDescriptor ) ) {
			return CollectionClassification.IDBAG;
		}

		if ( org.hibernate.mapping.Map.class.isInstance( bootDescriptor ) ) {
			if ( bootDescriptor.isSorted() ) {
				return CollectionClassification.SORTED_MAP;
			}

			if ( bootDescriptor.hasOrder() ) {
				return CollectionClassification.ORDERED_MAP;
			}

			return CollectionClassification.MAP;
		}

		if ( Collection.class.isInstance( bootDescriptor ) ) {
			return CollectionClassification.BAG;
		}

		if ( Array.class.isInstance( bootDescriptor ) ) {
			return CollectionClassification.ARRAY;
		}

		throw new HibernateException( "Unhandled org.hibernate.mapping.Collection classification : " + bootDescriptor.getClass() );
	}
}
