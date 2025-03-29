/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
