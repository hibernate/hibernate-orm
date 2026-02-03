/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.state;

import org.hibernate.Internal;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.RootClass;
import org.hibernate.metamodel.mapping.AuxiliaryMapping;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.mapping.internal.MappingModelCreationProcess;
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
 * <p>
 * Every concrete implementation of this interface should declare a
 * field {@code public static final StateManagement INSTANCE}.
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

	InsertCoordinator createInsertCoordinator(EntityPersister persister);

	UpdateCoordinator createUpdateCoordinator(EntityPersister persister);

	UpdateCoordinator createMergeCoordinator(EntityPersister persister);

	DeleteCoordinator createDeleteCoordinator(EntityPersister persister);

	InsertRowsCoordinator createInsertRowsCoordinator(CollectionPersister persister);

	UpdateRowsCoordinator createUpdateRowsCoordinator(CollectionPersister persister);

	DeleteRowsCoordinator createDeleteRowsCoordinator(CollectionPersister persister);

	RemoveCoordinator createRemoveCoordinator(CollectionPersister persister);

	AuxiliaryMapping createAuxiliaryMapping(
			EntityPersister persister,
			RootClass rootClass,
			MappingModelCreationProcess creationProcess);

	AuxiliaryMapping createAuxiliaryMapping(
			PluralAttributeMapping pluralAttributeMapping,
			Collection bootDescriptor,
			MappingModelCreationProcess creationProcess);
}
