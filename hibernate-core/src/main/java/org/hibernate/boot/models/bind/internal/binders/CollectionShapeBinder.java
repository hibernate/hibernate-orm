/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.bind.internal.binders;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.FetchMode;
import org.hibernate.annotations.Parameter;
import org.hibernate.boot.models.bind.internal.sources.CollectionSource;
import org.hibernate.boot.models.bind.spi.BindingState;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.List;

import jakarta.persistence.FetchType;

/// Applies collection-shape metadata after the concrete collection mapping exists.
///
/// `CollectionSource` decides the semantic classification of a plural member.
/// This helper applies the metadata that is orthogonal to table/element/key
/// binding, such as ordered and sorted collection settings.
///
/// @since 9.0
/// @author Steve Ebersole
class CollectionShapeBinder {
	static void apply(CollectionSource source, Collection collection, BindingState bindingState) {
		applyFetching( source, collection );
		applyCollectionType( source, collection );
		switch ( source.classification() ) {
			case ORDERED_SET, ORDERED_MAP -> applyOrdering( source, collection, bindingState );
			case SORTED_SET, SORTED_MAP -> applySorting( source, collection );
			default -> {
			}
		}
		if ( collection instanceof List list ) {
			applyListIndexBase( source, list );
		}
	}

	private static void applyFetching(CollectionSource source, Collection collection) {
		final FetchType fetchType = source.fetchType();
		collection.setLazy( fetchType == FetchType.LAZY );
		collection.setExtraLazy( false );
		collection.setFetchMode( fetchType == FetchType.EAGER ? FetchMode.JOIN : FetchMode.SELECT );
		if ( source.batchSize() >= 0 ) {
			collection.setBatchSize( source.batchSize() );
		}
	}

	private static void applyOrdering(CollectionSource source, Collection collection, BindingState bindingState) {
		final var sqlOrder = source.sqlOrder();
		if ( sqlOrder != null ) {
			collection.setOrderBy( sqlOrder.value() );
			return;
		}

		final var orderBy = source.orderBy();
		if ( orderBy != null ) {
			collection.setOrderBy( orderBy.value() );
		}
	}

	private static void applySorting(CollectionSource source, Collection collection) {
		collection.setSorted( true );

		final var sortComparator = source.sortComparator();
		if ( sortComparator != null ) {
			collection.setComparatorClassName( sortComparator.value().getName() );
		}
	}

	private static void applyListIndexBase(CollectionSource source, List collection) {
		final var listIndexBase = source.listIndexBase();
		if ( listIndexBase != null ) {
			collection.setBaseIndex( listIndexBase.value() );
		}
	}

	private static void applyCollectionType(CollectionSource source, Collection collection) {
		final var collectionType = source.collectionType();
		if ( collectionType == null ) {
			return;
		}

		collection.setTypeName( collectionType.type().getName() );
		collection.setTypeParameters( extractParameterMap( collectionType.parameters() ) );
	}

	private static Map<String, String> extractParameterMap(Parameter[] parameters) {
		final Map<String, String> result = new HashMap<>();
		for ( Parameter parameter : parameters ) {
			result.put( parameter.name(), parameter.value() );
		}
		return result;
	}
}
