/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.decompose.collection;

import org.hibernate.action.queue.meta.CollectionTableDescriptor;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.persister.collection.mutation.CollectionTableMapping;
import org.hibernate.sql.model.MutationTarget;


/**
 * @author Steve Ebersole
 */
public interface CollectionMutationTarget extends MutationTarget<CollectionTableMapping, CollectionTableDescriptor> {
	@Override
	PluralAttributeMapping getTargetPart();

	CollectionTableMapping getCollectionTableMapping();

	CollectionTableDescriptor getCollectionTableDescriptor();

	@Override
	default CollectionTableMapping getIdentifierTableMapping() {
		return getCollectionTableMapping();
	}

	@Override
	default CollectionTableDescriptor getIdentifierTableDescriptor() {
		return getCollectionTableDescriptor();
	}

	/**
	 * Whether the collection has at least one physical index column
	 */
	boolean hasPhysicalIndexColumn();

	@Override
	String getIdentifierTableName();
}
