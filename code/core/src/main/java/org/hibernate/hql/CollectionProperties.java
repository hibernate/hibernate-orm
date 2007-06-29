// $Id: CollectionProperties.java 5699 2005-02-13 11:50:11Z oneovthafew $
package org.hibernate.hql;

import org.hibernate.persister.collection.CollectionPropertyNames;

import java.util.HashMap;
import java.util.Map;

/**
 * Provides a map of collection function names to the corresponding property names.
 *
 * @author josh Aug 16, 2004 7:51:45 PM
 */
public final class CollectionProperties {
	public static final Map HQL_COLLECTION_PROPERTIES;

	private static final String COLLECTION_INDEX_LOWER = CollectionPropertyNames.COLLECTION_INDEX.toLowerCase();

	static {
		HQL_COLLECTION_PROPERTIES = new HashMap();
		HQL_COLLECTION_PROPERTIES.put( CollectionPropertyNames.COLLECTION_ELEMENTS.toLowerCase(), CollectionPropertyNames.COLLECTION_ELEMENTS );
		HQL_COLLECTION_PROPERTIES.put( CollectionPropertyNames.COLLECTION_INDICES.toLowerCase(), CollectionPropertyNames.COLLECTION_INDICES );
		HQL_COLLECTION_PROPERTIES.put( CollectionPropertyNames.COLLECTION_SIZE.toLowerCase(), CollectionPropertyNames.COLLECTION_SIZE );
		HQL_COLLECTION_PROPERTIES.put( CollectionPropertyNames.COLLECTION_MAX_INDEX.toLowerCase(), CollectionPropertyNames.COLLECTION_MAX_INDEX );
		HQL_COLLECTION_PROPERTIES.put( CollectionPropertyNames.COLLECTION_MIN_INDEX.toLowerCase(), CollectionPropertyNames.COLLECTION_MIN_INDEX );
		HQL_COLLECTION_PROPERTIES.put( CollectionPropertyNames.COLLECTION_MAX_ELEMENT.toLowerCase(), CollectionPropertyNames.COLLECTION_MAX_ELEMENT );
		HQL_COLLECTION_PROPERTIES.put( CollectionPropertyNames.COLLECTION_MIN_ELEMENT.toLowerCase(), CollectionPropertyNames.COLLECTION_MIN_ELEMENT );
		HQL_COLLECTION_PROPERTIES.put( COLLECTION_INDEX_LOWER, CollectionPropertyNames.COLLECTION_INDEX );
	}

	private CollectionProperties() {
	}

	public static boolean isCollectionProperty(String name) {
		String key = name.toLowerCase();
		// CollectionPropertyMapping processes everything except 'index'.
		if ( COLLECTION_INDEX_LOWER.equals( key ) ) {
			return false;
		}
		else {
			return HQL_COLLECTION_PROPERTIES.containsKey( key );
		}
	}

	public static String getNormalizedPropertyName(String name) {
		return ( String ) HQL_COLLECTION_PROPERTIES.get( name );
	}

	public static boolean isAnyCollectionProperty(String name) {
		String key = name.toLowerCase();
		return HQL_COLLECTION_PROPERTIES.containsKey( key );
	}
}
