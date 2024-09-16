/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel;

import java.util.SortedMap;
import java.util.SortedSet;

import org.hibernate.Incubating;
import org.hibernate.boot.BootLogging;

import jakarta.persistence.metamodel.PluralAttribute;

/**
 * Classifications of the plurality.
 *
 * @see org.hibernate.collection.spi.CollectionSemantics
 *
 * @since 6.0
 */
@Incubating
public enum CollectionClassification {
	/**
	 * An Object or primitive array.  Roughly follows the semantics
	 * of {@link #LIST}
	 */
	ARRAY( PluralAttribute.CollectionType.COLLECTION, true ),

	/**
	 * A non-unique, unordered collection.  Represented
	 * as {@link java.util.Collection} or {@link java.util.List}
	 */
	BAG( PluralAttribute.CollectionType.COLLECTION, false ),

	/**
	 * A {@link #BAG} with a generated id for each element
	 */
	ID_BAG( PluralAttribute.CollectionType.COLLECTION, false ),

	/**
	 * A non-unique, ordered collection following the requirements of {@link java.util.List}
	 *
	 * @see org.hibernate.cfg.AvailableSettings#DEFAULT_LIST_SEMANTICS
	 */
	LIST( PluralAttribute.CollectionType.LIST, true ),

	/**
	 * A unique, unordered collection following the requirements of {@link java.util.Set}
	 */
	SET( PluralAttribute.CollectionType.SET, false ),

	/**
	 * A sorted {@link #SET} using either natural sorting of the elements or a
	 * specified {@link java.util.Comparator}.  Represented
	 * as {@link java.util.SortedSet} or {@link java.util.Set}
	 *
	 * @see org.hibernate.annotations.SortNatural
	 * @see org.hibernate.annotations.SortComparator
	 */
	SORTED_SET( PluralAttribute.CollectionType.SET, false ),

	/**
	 * A {@link #SET} that is ordered using an order-by fragment
	 * as the collection is loaded.  Does not maintain ordering
	 * while in memory if the contents change.  Represented
	 * as {@link java.util.Set}.
	 *
	 * @see jakarta.persistence.OrderBy
	 * @see org.hibernate.annotations.OrderBy
	 */
	ORDERED_SET( PluralAttribute.CollectionType.SET, false ),

	/**
	 * A collection following the semantics of {@link java.util.Map}
	 */
	MAP( PluralAttribute.CollectionType.MAP, true ),

	/**
	 * A sorted {@link #MAP} using either natural sorting of the keys or a
	 * specified {@link java.util.Comparator}.  Represented
	 * as {@link java.util.SortedMap} or {@link java.util.Map}
	 *
	 * @see org.hibernate.annotations.SortNatural
	 * @see org.hibernate.annotations.SortComparator
	 */
	SORTED_MAP( PluralAttribute.CollectionType.MAP, true ),

	/**
	 * A {@link #MAP} that is ordered using an order-by fragment
	 * as the collection is loaded.  Does not maintain ordering
	 * while in memory if the contents change.  Represented
	 * as {@link java.util.Map}.
	 *
	 * @see jakarta.persistence.OrderBy
	 * @see org.hibernate.annotations.OrderBy
	 */
	ORDERED_MAP( PluralAttribute.CollectionType.MAP, true );

	private final PluralAttribute.CollectionType jpaClassification;
	private final boolean isIndexed;

	CollectionClassification(PluralAttribute.CollectionType jpaClassification, boolean isIndexed) {
		this.jpaClassification = jpaClassification;
		this.isIndexed = isIndexed;
	}

	public PluralAttribute.CollectionType toJpaClassification() {
		return jpaClassification;
	}

	public boolean isIndexed() {
		return isIndexed;
	}

	public boolean isRowUpdatePossible() {
		// anything other than BAG and SET
		return this != BAG && this != SET;
	}

	/**
	 * One of:<ul>
	 *     <li>{@link org.hibernate.metamodel.CollectionClassification} instance</li>
	 *     <li>{@link org.hibernate.metamodel.CollectionClassification} name (case insensitive)</li>
	 *     <li>{@link Class} reference for either {@link java.util.List} or {@link java.util.Collection}</li>
	 * </ul>
	 */
	public static CollectionClassification interpretSetting(Object value) {
		if ( value == null ) {
			return null;
		}
		else if ( value instanceof CollectionClassification ) {
			return (CollectionClassification) value;
		}
		else if ( value instanceof String ) {
			final String string = (String) value;
			for ( CollectionClassification collectionClassification : values() ) {
				if ( collectionClassification.name().equalsIgnoreCase( string ) ) {
					return collectionClassification;
				}
			}
			return null;
		}
		else if ( value instanceof Class ) {
			return interpretClass( (Class<?>) value );
		}
		else {
			return null;
		}
	}

	private static CollectionClassification interpretClass(Class<?> configuredClass) {
		if ( java.util.List.class.isAssignableFrom(configuredClass) ) {
			return LIST;
		}
		if ( SortedSet.class.isAssignableFrom(configuredClass) ) {
			return SORTED_SET;
		}
		if ( java.util.Set.class.isAssignableFrom(configuredClass) ) {
			return SET;
		}
		if ( SortedMap.class.isAssignableFrom(configuredClass) ) {
			return SORTED_MAP;
		}
		if ( java.util.Map.class.isAssignableFrom(configuredClass) ) {
			return MAP;
		}
		if ( java.util.Collection.class.isAssignableFrom(configuredClass) ) {
			return BAG;
		}

		BootLogging.BOOT_LOGGER.debugf(
				"Unexpected Class specified for CollectionClassification resolution (`%s`) - " +
						"should be one of `%s`, `%s`, `%s`, `%s`, `%s` or `%s`  (or subclass of)",
				configuredClass.getName(),
				java.util.List.class.getName(),
				SortedSet.class.getName(),
				java.util.Set.class.getName(),
				SortedMap.class.isAssignableFrom(configuredClass),
				java.util.Map.class.isAssignableFrom(configuredClass),
				java.util.Collection.class.getName()
		);

		return null;
	}
}
