/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.hql.internal;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.hibernate.persister.collection.CollectionPropertyNames;

/**
 * Provides a map of collection function names to the corresponding property names.
 *
 * @author josh
 */
public final class CollectionProperties {
	public static final Map HQL_COLLECTION_PROPERTIES;

	private static final String COLLECTION_INDEX_LOWER = CollectionPropertyNames.COLLECTION_INDEX.toLowerCase(Locale.ROOT);

	static {
		HQL_COLLECTION_PROPERTIES = new HashMap();
		HQL_COLLECTION_PROPERTIES.put( CollectionPropertyNames.COLLECTION_ELEMENTS.toLowerCase(Locale.ROOT), CollectionPropertyNames.COLLECTION_ELEMENTS );
		HQL_COLLECTION_PROPERTIES.put( CollectionPropertyNames.COLLECTION_INDICES.toLowerCase(Locale.ROOT), CollectionPropertyNames.COLLECTION_INDICES );
		HQL_COLLECTION_PROPERTIES.put( CollectionPropertyNames.COLLECTION_SIZE.toLowerCase(Locale.ROOT), CollectionPropertyNames.COLLECTION_SIZE );
		HQL_COLLECTION_PROPERTIES.put( CollectionPropertyNames.COLLECTION_MAX_INDEX.toLowerCase(Locale.ROOT), CollectionPropertyNames.COLLECTION_MAX_INDEX );
		HQL_COLLECTION_PROPERTIES.put( CollectionPropertyNames.COLLECTION_MIN_INDEX.toLowerCase(Locale.ROOT), CollectionPropertyNames.COLLECTION_MIN_INDEX );
		HQL_COLLECTION_PROPERTIES.put( CollectionPropertyNames.COLLECTION_MAX_ELEMENT.toLowerCase(Locale.ROOT), CollectionPropertyNames.COLLECTION_MAX_ELEMENT );
		HQL_COLLECTION_PROPERTIES.put( CollectionPropertyNames.COLLECTION_MIN_ELEMENT.toLowerCase(Locale.ROOT), CollectionPropertyNames.COLLECTION_MIN_ELEMENT );
		HQL_COLLECTION_PROPERTIES.put( COLLECTION_INDEX_LOWER, CollectionPropertyNames.COLLECTION_INDEX );
	}

	private CollectionProperties() {
	}

	@SuppressWarnings("SimplifiableIfStatement")
	public static boolean isCollectionProperty(String name) {
		final String key = name.toLowerCase(Locale.ROOT);
		// CollectionPropertyMapping processes everything except 'index'.
		if ( COLLECTION_INDEX_LOWER.equals( key ) ) {
			return false;
		}
		else {
			return HQL_COLLECTION_PROPERTIES.containsKey( key );
		}
	}

	public static String getNormalizedPropertyName(String name) {
		return (String) HQL_COLLECTION_PROPERTIES.get( name );
	}

	public static boolean isAnyCollectionProperty(String name) {
		final String key = name.toLowerCase(Locale.ROOT);
		return HQL_COLLECTION_PROPERTIES.containsKey( key );
	}
}
