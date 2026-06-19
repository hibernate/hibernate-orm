/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.mapping.internal.categorize;

import java.util.List;

import org.hibernate.boot.model.convert.spi.ConverterRegistry;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.models.mapping.internal.xml.PersistenceUnitMetadata;
import org.hibernate.boot.spi.EffectiveMappingDefaults;
import org.hibernate.models.spi.ClassDetailsRegistry;

import jakarta.persistence.SharedCacheMode;

/// Categorization-time access to bootstrap services and shared state.
///
/// The context exposes the categorization inputs and working state needed by the
/// categorizer and the metadata objects it creates.  It deliberately names the
/// needed collaborators instead of exposing wider bootstrap contracts such as
/// `BootstrapContext` or `MetadataBuildingContext`.
///
/// Services exposed here are inputs to, or working state for, categorization.  They
/// are intentionally separate from {@link CategorizedDomainModel}, which represents
/// the categorized result consumed by later binding phases.
///
/// @since 9.0
/// @author Steve Ebersole
public interface CategorizationContext {
	default PersistentAttributeMemberResolver getPersistentAttributeMemberResolver() {
		return StandardPersistentAttributeMemberResolver.INSTANCE;
	}

	default PersistentAttributeMemberResolver getAttributeMemberResolver() {
		return getPersistentAttributeMemberResolver();
	}

	PersistenceUnitMetadata getPersistenceUnitMetadata();

	EffectiveMappingDefaults getEffectiveMappingDefaults();

	ClassDetailsRegistry getClassDetailsRegistry();

	SharedCacheMode getSharedCacheMode();

	GlobalRegistrations getGlobalRegistrations();

	ConverterRegistry getConverterRegistry();

	Database getDatabase();

	List<JpaEventListener> getDefaultEventListeners();
}
