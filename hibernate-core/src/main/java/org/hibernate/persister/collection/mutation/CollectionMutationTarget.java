/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.collection.mutation;

import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.sql.model.MutationTarget;

/**
 * @author Steve Ebersole
 */
public interface CollectionMutationTarget extends MutationTarget<CollectionTableMapping> {
	@Override
	PluralAttributeMapping getTargetPart();

	CollectionTableMapping getCollectionTableMapping();

	@Override
	default CollectionTableMapping getIdentifierTableMapping() {
		return getCollectionTableMapping();
	}

	/**
	 * Whether the collection has at least one physical index column
	 */
	boolean hasPhysicalIndexColumn();
}
