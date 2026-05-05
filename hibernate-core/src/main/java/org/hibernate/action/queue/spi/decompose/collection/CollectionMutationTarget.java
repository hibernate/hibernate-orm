/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.spi.decompose.collection;

import org.hibernate.Incubating;
import org.hibernate.action.queue.spi.meta.CollectionTableDescriptor;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.persister.collection.mutation.CollectionTableMapping;
import org.hibernate.sql.model.MutationTarget;


/// @author Steve Ebersole
/// @since 8.0
@Incubating
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

	/// Whether the collection has at least one physical index column
	boolean hasPhysicalIndexColumn();

	@Override
	String getIdentifierTableName();
}
