/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.collection.mutation;

import org.hibernate.action.queue.meta.CollectionTableDescriptor;
import org.hibernate.action.queue.mutation.GraphMutationTarget;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;

/**
 * @author Steve Ebersole
 */
public interface CollectionGraphMutationTarget
		extends GraphMutationTarget<CollectionTableDescriptor> {
	@Override
	PluralAttributeMapping getTargetPart();

	CollectionTableDescriptor getCollectionTableDescriptor();
}
