/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.state.internal;

import org.hibernate.Internal;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.RootClass;
import org.hibernate.metamodel.mapping.AuxiliaryMapping;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.mapping.TemporalMapping;
import org.hibernate.metamodel.mapping.internal.MappingModelCreationProcess;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.collection.mutation.UpdateRowsCoordinator;
import org.hibernate.persister.collection.mutation.UpdateRowsCoordinatorNoOp;
import org.hibernate.persister.collection.mutation.UpdateRowsCoordinatorTemporal;
import org.hibernate.persister.entity.AbstractEntityPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.mutation.DeleteCoordinator;
import org.hibernate.persister.entity.mutation.DeleteCoordinatorTemporal;
import org.hibernate.persister.entity.mutation.InsertCoordinator;
import org.hibernate.persister.entity.mutation.InsertCoordinatorTemporal;
import org.hibernate.persister.entity.mutation.MergeCoordinatorTemporal;
import org.hibernate.persister.entity.mutation.UpdateCoordinator;
import org.hibernate.persister.entity.mutation.UpdateCoordinatorTemporal;

import static org.hibernate.boot.model.internal.TemporalHelper.resolveTemporalMapping;
import static org.hibernate.metamodel.mapping.internal.MappingModelCreationHelper.getTableIdentifierExpression;

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
	public InsertCoordinator createInsertCoordinator(EntityPersister persister) {
		return new InsertCoordinatorTemporal( persister, persister.getFactory() );
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

	@Override
	public AuxiliaryMapping createAuxiliaryMapping(
			EntityPersister persister,
			RootClass rootClass,
			MappingModelCreationProcess creationProcess) {
		final var temporalTable = rootClass.getTemporalTable();
		return resolveTemporalMapping(
				rootClass,
				temporalTable == null
						? persister.getIdentifierTableName()
						: ( (AbstractEntityPersister) persister )
								.determineTableName( temporalTable ),
				creationProcess
		);
	}

	@Override
	public TemporalMapping createAuxiliaryMapping(
			PluralAttributeMapping pluralAttributeMapping,
			Collection bootDescriptor,
			MappingModelCreationProcess creationProcess) {
		final var temporalTable = bootDescriptor.getTemporalTable();
		return resolveTemporalMapping(
				bootDescriptor,
				temporalTable == null
						? pluralAttributeMapping.getSeparateCollectionTable()
						: getTableIdentifierExpression( temporalTable, creationProcess ),
				creationProcess
		);
	}
}
