/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.persister.collection;

/**
 * The names of all the collection properties.
 *
 * @author josh
 */
public final class CollectionPropertyNames {
	private CollectionPropertyNames() {
	}

	public static final String COLLECTION_SIZE = "size";
	public static final String COLLECTION_ELEMENTS = "elements";
	public static final String COLLECTION_INDICES = "indices";
	public static final String COLLECTION_MAX_INDEX = "maxIndex";
	public static final String COLLECTION_MIN_INDEX = "minIndex";
	public static final String COLLECTION_MAX_ELEMENT = "maxElement";
	public static final String COLLECTION_MIN_ELEMENT = "minElement";
	public static final String COLLECTION_INDEX = "index";
}
