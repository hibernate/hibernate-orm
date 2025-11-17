/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.collection.internal;

import org.hibernate.collection.spi.CollectionSemantics;
import org.hibernate.collection.spi.CollectionSemanticsResolver;
import org.hibernate.mapping.Collection;

/**
 * Standard implementation of CollectionSemanticsResolver
 *
 * @author Steve Ebersole
 */
public class StandardCollectionSemanticsResolver implements CollectionSemanticsResolver {
	/**
	 * Singleton access
	 */
	public static final StandardCollectionSemanticsResolver INSTANCE = new StandardCollectionSemanticsResolver();

	@Override
	public CollectionSemantics resolveRepresentation(Collection bootDescriptor) {
		return bootDescriptor.getCollectionSemantics();
	}
}
