/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.collection.spi;

import org.hibernate.Incubating;
import org.hibernate.mapping.Collection;

/**
 * Resolve the collection semantics for the given mapped collection.
 *
 * @apiNote Ideally, this would act as the contract that allows pluggable
 *          resolution of non-Java Collection types, perhaps as part of a
 *          generalized reflection on the attribute to determine its
 *          nature/classification
 *
 * @author Steve Ebersole
 */
@Incubating
public interface CollectionSemanticsResolver {
	// really need some form of access to the attribute site
	<CE,E> CollectionSemantics<CE,E> resolveRepresentation(Collection bootDescriptor);
}
