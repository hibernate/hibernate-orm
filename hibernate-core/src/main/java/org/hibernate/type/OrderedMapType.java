/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type;

import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.metamodel.CollectionClassification;

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
		return anticipatedSize > 0
				? CollectionHelper.linkedMap()
				: CollectionHelper.linkedMapOfSize( anticipatedSize );
	}

}
