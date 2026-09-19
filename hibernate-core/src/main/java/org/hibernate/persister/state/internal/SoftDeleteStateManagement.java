/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.state.internal;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.hibernate.mapping.Collection;
import org.hibernate.mapping.RootClass;
import org.hibernate.action.queue.spi.decompose.entity.EntityMutationPlanContributor;
import org.hibernate.action.queue.internal.decompose.entity.SoftDeleteEntityMutationPlanContributor;
import org.hibernate.metamodel.mapping.AuxiliaryMapping;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.mapping.internal.MappingModelCreationProcess;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.mutation.DeleteCoordinator;
import org.hibernate.persister.entity.mutation.DeleteCoordinatorSoft;
import org.hibernate.persister.state.spi.StateManagementGraphIntegration;

import static org.hibernate.boot.model.internal.SoftDeleteHelper.resolveSoftDeleteMapping;

/**
 * @author Gavin King
 *
 * @since 7.4
 */
public final class SoftDeleteStateManagement extends AbstractStateManagement {
	public static final SoftDeleteStateManagement INSTANCE = new SoftDeleteStateManagement();

	private final StateManagementGraphIntegration graphIntegration = new StateManagementGraphIntegration() {
		@Nonnull
		@Override
		public EntityMutationPlanContributor createEntityMutationPlanContributor(@Nonnull EntityPersister persister) {
			return new SoftDeleteEntityMutationPlanContributor( persister, persister.getFactory() );
		}
	};

	private SoftDeleteStateManagement() {
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Graph ActionQueue integration

	@Nonnull
	@Override
	public StateManagementGraphIntegration getGraphIntegration() {
		return graphIntegration;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Legacy ActionQueue integration

	@Nonnull
	@Override
	public DeleteCoordinator createDeleteCoordinator(@Nonnull EntityPersister persister) {
		return new DeleteCoordinatorSoft( persister, persister.getFactory() );
	}

	@Nullable
	@Override
	public AuxiliaryMapping createAuxiliaryMapping(
			@Nonnull EntityPersister persister,
			@Nonnull RootClass rootClass,
			@Nonnull MappingModelCreationProcess creationProcess) {
		return resolveSoftDeleteMapping( persister, rootClass, persister.getIdentifierTableName(), creationProcess );
	}

	@Nullable
	@Override
	public AuxiliaryMapping createAuxiliaryMapping(
			@Nonnull PluralAttributeMapping pluralAttributeMapping,
			@Nonnull Collection bootDescriptor,
			@Nonnull MappingModelCreationProcess creationProcess) {
		return resolveSoftDeleteMapping( pluralAttributeMapping, bootDescriptor,
				pluralAttributeMapping.getSeparateCollectionTable(), creationProcess );
	}
}
