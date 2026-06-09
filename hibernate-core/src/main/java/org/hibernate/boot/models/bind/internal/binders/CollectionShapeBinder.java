/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.bind.internal.binders;

import org.hibernate.boot.models.bind.internal.sources.CollectionSource;
import org.hibernate.boot.models.bind.spi.BindingState;
import org.hibernate.mapping.Collection;

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
		switch ( source.classification() ) {
			case ORDERED_SET, ORDERED_MAP -> applyOrdering( source, collection, bindingState );
			case SORTED_SET, SORTED_MAP -> applySorting( source, collection );
			default -> {
			}
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
}
