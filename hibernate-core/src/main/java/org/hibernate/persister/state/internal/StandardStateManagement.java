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

/**
 * @author Gavin King
 */
@Internal
public final class StandardStateManagement extends AbstractStateManagement {
	public static final StandardStateManagement INSTANCE = new StandardStateManagement();

	private StandardStateManagement() {
	}

	@Override
	public AuxiliaryMapping createAuxiliaryMapping(
			EntityPersister persister,
			RootClass rootClass,
			MappingModelCreationProcess creationProcess) {
		// for NATIVE temporal tables
		return TemporalStateManagement.INSTANCE.createAuxiliaryMapping( persister, rootClass, creationProcess);
	}

	@Override
	public AuxiliaryMapping createAuxiliaryMapping(
			PluralAttributeMapping pluralAttributeMapping,
			Collection bootDescriptor,
			MappingModelCreationProcess creationProcess) {
		// for NATIVE temporal tables
		return TemporalStateManagement.INSTANCE.createAuxiliaryMapping( pluralAttributeMapping, bootDescriptor, creationProcess);
	}
}
