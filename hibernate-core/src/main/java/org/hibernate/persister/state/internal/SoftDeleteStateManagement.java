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
import org.hibernate.metamodel.mapping.internal.MappingModelCreationProcess;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.mutation.DeleteCoordinator;
import org.hibernate.persister.entity.mutation.DeleteCoordinatorSoft;

import static org.hibernate.boot.model.internal.SoftDeleteHelper.resolveSoftDeleteMapping;

/**
 * @author Gavin King
 */
@Internal
public final class SoftDeleteStateManagement extends AbstractStateManagement {
	public static final SoftDeleteStateManagement INSTANCE = new SoftDeleteStateManagement();

	private SoftDeleteStateManagement() {
	}

	@Override
	public DeleteCoordinator createDeleteCoordinator(EntityPersister persister) {
		return new DeleteCoordinatorSoft( persister, persister.getFactory() );
	}

	@Override
	public AuxiliaryMapping createAuxiliaryMapping(
			EntityPersister persister,
			RootClass rootClass,
			MappingModelCreationProcess creationProcess) {
		return resolveSoftDeleteMapping( persister, rootClass, persister.getIdentifierTableName(), creationProcess );
	}

	@Override
	public AuxiliaryMapping createAuxiliaryMapping(
			PluralAttributeMapping pluralAttributeMapping,
			Collection bootDescriptor,
			MappingModelCreationProcess creationProcess) {
		return resolveSoftDeleteMapping( pluralAttributeMapping, bootDescriptor,
				pluralAttributeMapping.getSeparateCollectionTable(), creationProcess );
	}
}
