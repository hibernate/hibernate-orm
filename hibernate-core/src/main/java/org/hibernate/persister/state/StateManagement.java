/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.state;

import org.hibernate.Internal;
import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.collection.mutation.DeleteRowsCoordinator;
import org.hibernate.persister.collection.mutation.InsertRowsCoordinator;
import org.hibernate.persister.collection.mutation.RemoveCoordinator;
import org.hibernate.persister.collection.mutation.UpdateRowsCoordinator;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.mutation.DeleteCoordinator;
import org.hibernate.persister.entity.mutation.InsertCoordinator;
import org.hibernate.persister.entity.mutation.UpdateCoordinator;
import org.hibernate.persister.state.internal.AuditStateManagement;
import org.hibernate.persister.state.internal.HistoryStateManagement;
import org.hibernate.persister.state.internal.SoftDeleteStateManagement;
import org.hibernate.persister.state.internal.StandardStateManagement;
import org.hibernate.persister.state.internal.TemporalStateManagement;

/**
 * Aggregates the coordinators for a given state management strategy.
 *
 * @author Gavin King
 *
 * @see StandardStateManagement
 * @see SoftDeleteStateManagement
 * @see TemporalStateManagement
 * @see HistoryStateManagement
 * @see AuditStateManagement
 */
@Internal
public interface StateManagement {

	static StateManagement forEntity(PersistentClass persistentClass, SessionFactoryOptions options) {
		final var rootClass = persistentClass.getRootClass();
		if ( rootClass.isTemporalized() ) {
			return selectTemporalStateManagement( options );
		}
		else if ( rootClass.getSoftDeleteStrategy() != null ) {
			return SoftDeleteStateManagement.INSTANCE;
		}
		else if ( rootClass.isAudited() ) {
			return AuditStateManagement.INSTANCE;
		}
		else {
			return StandardStateManagement.INSTANCE;
		}
	}

	static StateManagement forCollection(Collection collectionBinding, SessionFactoryOptions options) {
		if ( collectionBinding.isTemporalized() ) {
			return selectTemporalStateManagement( options );
		}
		else if ( collectionBinding.getSoftDeleteStrategy() != null ) {
			return SoftDeleteStateManagement.INSTANCE;
		}
		else if ( collectionBinding.isAudited() ) {
			return AuditStateManagement.INSTANCE;
		}
		else {
			return StandardStateManagement.INSTANCE;
		}
	}

	private static StateManagement selectTemporalStateManagement(SessionFactoryOptions options) {
		return switch ( options.getTemporalTableStrategy() ) {
			case NATIVE -> StandardStateManagement.INSTANCE;
			case SINGLE_TABLE -> TemporalStateManagement.INSTANCE;
			case HISTORY_TABLE -> HistoryStateManagement.INSTANCE;
			case AUTO -> throw new IllegalArgumentException();
		};
	}

	InsertCoordinator createInsertCoordinator(EntityPersister persister);

	UpdateCoordinator createUpdateCoordinator(EntityPersister persister);

	UpdateCoordinator createMergeCoordinator(EntityPersister persister);

	DeleteCoordinator createDeleteCoordinator(EntityPersister persister);

	InsertRowsCoordinator createInsertRowsCoordinator(CollectionPersister persister);

	UpdateRowsCoordinator createUpdateRowsCoordinator(CollectionPersister persister);

	DeleteRowsCoordinator createDeleteRowsCoordinator(CollectionPersister persister);

	RemoveCoordinator createRemoveCoordinator(CollectionPersister persister);
}
