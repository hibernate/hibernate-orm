/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type;

import org.hibernate.metamodel.CollectionClassification;

import static org.hibernate.internal.util.collections.CollectionHelper.linkedMap;
import static org.hibernate.internal.util.collections.CollectionHelper.linkedMapOfSize;

/**
 * A specialization of the map type, with (resultset-based) ordering.
 */
public class OrderedMapType extends MapType {

	public OrderedMapType(String role, String propertyRef) {
		super( role, propertyRef );
	}

	@Override
	public CollectionClassification getCollectionClassification() {
		return CollectionClassification.ORDERED_MAP;
	}

	@Override
	public Object instantiate(int anticipatedSize) {
		return anticipatedSize > 0 ? linkedMap() : linkedMapOfSize( anticipatedSize );
	}

}
