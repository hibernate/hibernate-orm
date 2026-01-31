/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.state.internal;

import org.hibernate.Internal;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.collection.mutation.UpdateRowsCoordinator;
import org.hibernate.persister.collection.mutation.UpdateRowsCoordinatorNoOp;
import org.hibernate.persister.collection.mutation.UpdateRowsCoordinatorTemporal;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.mutation.DeleteCoordinator;
import org.hibernate.persister.entity.mutation.DeleteCoordinatorTemporal;
import org.hibernate.persister.entity.mutation.MergeCoordinatorTemporal;
import org.hibernate.persister.entity.mutation.UpdateCoordinator;
import org.hibernate.persister.entity.mutation.UpdateCoordinatorTemporal;

/**
 * State management for temporal entities and collections in the
 * {@linkplain org.hibernate.cfg.TemporalTableStrategy#SINGLE_TABLE
 * single table strategy}.
 *
 * @author Gavin King
 */
@Internal
public final class TemporalStateManagement extends AbstractStateManagement {
	public static final TemporalStateManagement INSTANCE = new TemporalStateManagement();

	private TemporalStateManagement() {
	}

	@Override
	public UpdateCoordinator createUpdateCoordinator(EntityPersister persister) {
		return new UpdateCoordinatorTemporal( persister, persister.getFactory() );
	}

	@Override
	public UpdateCoordinator createMergeCoordinator(EntityPersister persister) {
		return new MergeCoordinatorTemporal( persister, persister.getFactory() );
	}

	@Override
	public DeleteCoordinator createDeleteCoordinator(EntityPersister persister) {
		return new DeleteCoordinatorTemporal( persister, persister.getFactory() );
	}

	@Override
	public UpdateRowsCoordinator createUpdateRowsCoordinator(CollectionPersister persister) {
		if ( !isUpdatePossible( persister ) ) {
			return new UpdateRowsCoordinatorNoOp( resolveMutationTarget( persister ) );
		}
		else if ( persister.isOneToMany() ) {
			throw new UnsupportedOperationException();
		}
		else {
			return new UpdateRowsCoordinatorTemporal(
					resolveMutationTarget( persister ),
					persister.getRowMutationOperations(),
					persister.getFactory()
			);
		}
	}
}
