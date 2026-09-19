/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.state.internal;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.hibernate.mapping.Collection;
import org.hibernate.mapping.RootClass;
import org.hibernate.metamodel.mapping.AuxiliaryMapping;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.mapping.internal.MappingModelCreationProcess;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.temporal.TemporalTableStrategy;

/**
 * State management for temporal entities and collections with
 * {@linkplain TemporalTableStrategy#NATIVE dialect-native temporary tables}.
 *
 * @author Gavin King
 *
 * @since 7.4
 */
public final class NativeTemporalStateManagement extends AbstractStateManagement {
	public static final NativeTemporalStateManagement INSTANCE = new NativeTemporalStateManagement();

	private NativeTemporalStateManagement() {
	}

	@Nullable
	@Override
	public AuxiliaryMapping createAuxiliaryMapping(
			@Nonnull EntityPersister persister,
			@Nonnull RootClass rootClass,
			@Nonnull MappingModelCreationProcess creationProcess) {
		return TemporalStateManagement.INSTANCE.createAuxiliaryMapping( persister, rootClass, creationProcess);
	}

	@Nullable
	@Override
	public AuxiliaryMapping createAuxiliaryMapping(
			@Nonnull PluralAttributeMapping pluralAttributeMapping,
			@Nonnull Collection bootDescriptor,
			@Nonnull MappingModelCreationProcess creationProcess) {
		return TemporalStateManagement.INSTANCE.createAuxiliaryMapping( pluralAttributeMapping, bootDescriptor, creationProcess);
	}
}
