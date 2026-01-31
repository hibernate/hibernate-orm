/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.state.internal;

import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.collection.mutation.DeleteRowsCoordinator;
import org.hibernate.persister.collection.mutation.InsertRowsCoordinator;
import org.hibernate.persister.collection.mutation.RemoveCoordinator;
import org.hibernate.persister.collection.mutation.UpdateRowsCoordinator;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.mutation.DeleteCoordinator;
import org.hibernate.persister.entity.mutation.InsertCoordinator;
import org.hibernate.persister.entity.mutation.UpdateCoordinator;
import org.hibernate.persister.state.StateManagement;

/**
 * State management for {@linkplain org.hibernate.annotations.Audited audited}
 * entities and collections.
 *
 * @author Gavin King
 */
public class AuditStateManagement implements StateManagement {
	/**
	 * The modification type stored in the
	 * {@linkplain org.hibernate.annotations.Audited#modificationType
	 * modification type column}.
	 */
	public enum ModificationType {
		/** Creation, encoded as 0 */
		ADD,
		/** Modification, encoded as 1 */
		MOD,
		/** Deletion, encoded as 2 */
		DEL
	}

	@Override
	public InsertCoordinator createInsertCoordinator(EntityPersister persister) {
		return null;
	}

	@Override
	public UpdateCoordinator createUpdateCoordinator(EntityPersister persister) {
		return null;
	}

	@Override
	public UpdateCoordinator createMergeCoordinator(EntityPersister persister) {
		return null;
	}

	@Override
	public DeleteCoordinator createDeleteCoordinator(EntityPersister persister) {
		return null;
	}

	@Override
	public InsertRowsCoordinator createInsertRowsCoordinator(CollectionPersister persister) {
		return null;
	}

	@Override
	public UpdateRowsCoordinator createUpdateRowsCoordinator(CollectionPersister persister) {
		return null;
	}

	@Override
	public DeleteRowsCoordinator createDeleteRowsCoordinator(CollectionPersister persister) {
		return null;
	}

	@Override
	public RemoveCoordinator createRemoveCoordinator(CollectionPersister persister) {
		return null;
	}
}
