/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.collection.mutation;

import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.persister.collection.CollectionPersister;

/**
 * Coordinates the logical insertion of collection entries which are not yet persistent.
 * <p>
 * Insertions are determined by {@linkplain EntryFilter filtering} the entries obtained
 * from {@link PersistentCollection#entries(CollectionPersister)}.
 * <p>
 * A "logical" insertion because the actual SQL used may be an UPDATE in the case of
 * one-to-many mappings to set the foreign-key
 *
 * @see CollectionPersister#recreate
 * @see CollectionPersister#insertRows
 * @see RowMutationOperations#getInsertRowOperation()
 * @see RowMutationOperations#getInsertRowValues()
 *
 * @author Steve Ebersole
 */
public interface InsertRowsCoordinator extends CollectionOperationCoordinator {
	/**
	 * Perform the creation.
	 *
	 * @apiNote `entryChecker` allows simultaneously handling for both "insert" and
	 * "recreate" operations based on the checker's inclusion/exclusion of each entry
	 */
	void insertRows(
			PersistentCollection<?> collection,
			Object id,
			EntryFilter entryChecker,
			SharedSessionContractImplementor session);

	/**
	 * A tri-predicate for including / excluding collection entries
	 * from iterative processing inside {@link #insertRows}.
	 */
	@FunctionalInterface
	interface EntryFilter {
		/**
		 * Whether the entry should be included
		 *
		 * @return {@code true} indicates the entry should be included and {@code false}
		 * indicates it should be excluded
		 */
		boolean include(Object entry, int position, PersistentCollection<?> collection, PluralAttributeMapping attributeDescriptor);

		/**
		 * The inverse of {@link #include}.  Here, {@code true} indicates exclusion and
		 * {@code false} indicates inclusion
		 */
		default boolean exclude(Object entry, int i, PersistentCollection<?> collection, PluralAttributeMapping attributeDescriptor) {
			return !include( entry, i, collection, attributeDescriptor );
		}
	}
}
