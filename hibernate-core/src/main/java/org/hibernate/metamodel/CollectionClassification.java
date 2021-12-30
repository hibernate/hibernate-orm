/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
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
 * @since 6.0
 */
@Incubating
public enum CollectionClassification {
	SET( PluralAttribute.CollectionType.SET ),
	LIST( PluralAttribute.CollectionType.LIST ),
	MAP( PluralAttribute.CollectionType.MAP ),
	BAG( PluralAttribute.CollectionType.COLLECTION ),
	SORTED_SET( PluralAttribute.CollectionType.SET ),
	ORDERED_SET( PluralAttribute.CollectionType.SET ),
	SORTED_MAP( PluralAttribute.CollectionType.MAP ),
	ORDERED_MAP( PluralAttribute.CollectionType.MAP ),
	ID_BAG( PluralAttribute.CollectionType.COLLECTION ),
	ARRAY( PluralAttribute.CollectionType.COLLECTION );

	private final PluralAttribute.CollectionType jpaClassification;

	CollectionClassification(PluralAttribute.CollectionType jpaClassification) {
		this.jpaClassification = jpaClassification;
	}

	public PluralAttribute.CollectionType toJpaClassification() {
		return jpaClassification;
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

		if ( value instanceof CollectionClassification ) {
			return ( (CollectionClassification) value );
		}

		if ( value instanceof String ) {
			final String string = (String) value;
			for ( CollectionClassification collectionClassification : values() ) {
				if ( collectionClassification.name().equalsIgnoreCase( string ) ) {
					return collectionClassification;
				}
			}
		}

		if ( value instanceof Class ) {
			final Class<?> configuredClass = (Class<?>) value;
			if ( java.util.List.class.isAssignableFrom( configuredClass ) ) {
				return LIST;
			}
			if ( SortedSet.class.isAssignableFrom( configuredClass ) ) {
				return SORTED_SET;
			}
			if ( java.util.Set.class.isAssignableFrom( configuredClass ) ) {
				return SET;
			}
			if ( SortedMap.class.isAssignableFrom( configuredClass ) ) {
				return SORTED_MAP;
			}
			if ( java.util.Map.class.isAssignableFrom( configuredClass ) ) {
				return MAP;
			}
			if ( java.util.Collection.class.isAssignableFrom( configuredClass ) ) {
				return BAG;
			}

			BootLogging.LOGGER.debugf(
					"Unexpected Class specified for CollectionClassification resolution (`%s`) - " +
							"should be one of `%s`, `%s`, `%s`, `%s`, `%s` or `%s`  (or subclass of)",
					configuredClass.getName(),
					java.util.List.class.getName(),
					SortedSet.class.getName(),
					java.util.Set.class.getName(),
					SortedMap.class.isAssignableFrom( configuredClass ),
					java.util.Map.class.isAssignableFrom( configuredClass ),
					java.util.Collection.class.getName()
			);
		}

		return null;
	}
}
