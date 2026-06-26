/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.categorize;

import java.util.List;

import org.hibernate.boot.model.convert.spi.ConverterRegistry;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.mapping.internal.xml.PersistenceUnitMetadata;
import org.hibernate.boot.spi.EffectiveMappingDefaults;
import org.hibernate.models.spi.ClassDetailsRegistry;

import jakarta.persistence.SharedCacheMode;

/// Standard CategorizationContext impl
///
/// @since 9.0
/// @author Steve Ebersole
public record CategorizationContextImpl(
		PersistenceUnitMetadata persistenceUnitMetadata,
		EffectiveMappingDefaults effectiveMappingDefaults,
		ClassDetailsRegistry classDetailsRegistry,
		SharedCacheMode sharedCacheMode,
		GlobalRegistrations globalRegistrations,
		ConverterRegistry converterRegistry,
		Database database) implements CategorizationContext {
	@Override
	public GlobalRegistrations getGlobalRegistrations() {
		return globalRegistrations;
	}

	@Override
	public PersistenceUnitMetadata getPersistenceUnitMetadata() {
		return persistenceUnitMetadata;
	}

	@Override
	public EffectiveMappingDefaults getEffectiveMappingDefaults() {
		return effectiveMappingDefaults;
	}

	@Override
	public ClassDetailsRegistry getClassDetailsRegistry() {
		return classDetailsRegistry;
	}

	@Override
	public SharedCacheMode getSharedCacheMode() {
		return sharedCacheMode;
	}

	@Override
	public ConverterRegistry getConverterRegistry() {
		return converterRegistry;
	}

	@Override
	public Database getDatabase() {
		return database;
	}

	@Override
	public List<JpaEventListener> getDefaultEventListeners() {
		return getGlobalRegistrations().getEntityListenerRegistrations();
	}
}
