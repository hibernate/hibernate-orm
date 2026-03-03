/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.collection.mutation;

import org.hibernate.action.internal.CollectionUpdateAction;
import org.hibernate.action.queue.graph.MutationDecomposer;
import org.hibernate.action.queue.plan.PlannedOperationGroup;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.collection.BasicCollectionPersister;
import org.hibernate.persister.collection.OneToManyPersister;

import java.util.ArrayList;
import java.util.List;

/**
 * Decomposes a collection update action into planned operations by delegating to
 * the {@link DeleteRowsCoordinator}, {@link UpdateRowsCoordinator}, and {@link InsertRowsCoordinator}.
 * <p>
 * Collection update involves deleting removed entries, updating modified entries, and inserting new entries.
 *
 * @author Steve Ebersole
 */
public class CollectionUpdateDecomposer implements MutationDecomposer<CollectionUpdateAction> {
	@Override
	public List<PlannedOperationGroup> decompose(
			CollectionUpdateAction action,
			int ordinalBase,
			SharedSessionContractImplementor session) {
		final var persister = action.getPersister();
		final var collection = action.getCollection();
		final var key = action.getKey();

		final List<PlannedOperationGroup> groups = new ArrayList<>();

		// 1. DELETE operations for removed entries
		final DeleteRowsCoordinator deleteCoordinator = getDeleteRowsCoordinator( persister );
		if ( deleteCoordinator != null ) {
			groups.addAll( deleteCoordinator.decomposeDeleteRows( collection, key, ordinalBase, session ) );
		}

		// 2. UPDATE operations for modified entries
		final UpdateRowsCoordinator updateCoordinator = getUpdateRowsCoordinator( persister );
		if ( updateCoordinator != null ) {
			groups.addAll( updateCoordinator.decomposeUpdateRows( collection, key, ordinalBase + 1, session ) );
		}

		// 3. INSERT operations for new entries
		final InsertRowsCoordinator insertCoordinator = getInsertRowsCoordinator( persister );
		if ( insertCoordinator != null ) {
			groups.addAll( insertCoordinator.decomposeInsertRows(
					collection, key, collection::includeInInsert, ordinalBase + 2, session ) );
		}

		return groups;
	}

	private static DeleteRowsCoordinator getDeleteRowsCoordinator(
			org.hibernate.persister.collection.CollectionPersister persister) {
		if ( persister instanceof OneToManyPersister oneToMany ) {
			return oneToMany.getDeleteRowsCoordinator();
		}
		else if ( persister instanceof BasicCollectionPersister basic ) {
			return basic.getDeleteRowsCoordinator();
		}
		return null;
	}

	private static UpdateRowsCoordinator getUpdateRowsCoordinator(
			org.hibernate.persister.collection.CollectionPersister persister) {
		if ( persister instanceof OneToManyPersister oneToMany ) {
			return oneToMany.getUpdateRowsCoordinator();
		}
		else if ( persister instanceof BasicCollectionPersister basic ) {
			return basic.getUpdateRowsCoordinator();
		}
		return null;
	}

	private static InsertRowsCoordinator getInsertRowsCoordinator(
			org.hibernate.persister.collection.CollectionPersister persister) {
		if ( persister instanceof OneToManyPersister oneToMany ) {
			return oneToMany.getInsertRowsCoordinator();
		}
		else if ( persister instanceof BasicCollectionPersister basic ) {
			return basic.getInsertRowsCoordinator();
		}
		return null;
	}
}
