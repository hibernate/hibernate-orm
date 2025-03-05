/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type;

import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.metamodel.CollectionClassification;

/**
 * A specialization of the set type, with (resultset-based) ordering.
 */
public class OrderedSetType extends SetType {

	public OrderedSetType(String role, String propertyRef) {
		super( role, propertyRef );
	}

	@Override
	public CollectionClassification getCollectionClassification() {
		return CollectionClassification.ORDERED_SET;
	}

	@Override
	public Object instantiate(int anticipatedSize) {
		return anticipatedSize > 0
				? CollectionHelper.linkedSetOfSize( anticipatedSize )
				: CollectionHelper.linkedSet();
	}

}
