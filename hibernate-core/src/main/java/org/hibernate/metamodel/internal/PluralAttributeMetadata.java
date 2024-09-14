/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.metamodel.internal;

import org.hibernate.metamodel.CollectionClassification;

/**
 * Attribute metadata contract for a plural attribute.
 *
 * @param <O> The owner type
 * @param <C> The attribute type (the collection type)
 * @param <E> The collection element type
 */
@SuppressWarnings("unused")
public interface PluralAttributeMetadata<O, C, E> extends AttributeMetadata<O, C> {
	/**
	 * The classification of the collection, indicating the collection semantics
	 * to be used.
	 */
	CollectionClassification getCollectionClassification();

	/**
	 * Retrieve the value context for the collection's elements.
	 *
	 * @return The value context for the collection's elements.
	 */
	ValueContext getElementValueContext();

	/**
	 * Retrieve the value context for the collection's keys (if a map, null otherwise).
	 *
	 * @return The value context for the collection's keys (if a map, null otherwise).
	 */
	ValueContext getMapKeyValueContext();
}
