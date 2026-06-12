/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.pipeline.internal.settings;

import java.util.Map;

import org.hibernate.jpa.HibernatePersistenceConfiguration;
import org.hibernate.jpa.boot.spi.PersistenceUnitDescriptor;
import org.hibernate.service.ServiceRegistry;

import jakarta.persistence.FetchType;

/// Facade for resolving the phase-specific settings buckets used by the new
/// bootstrap pipeline.
///
/// @implNote The implementation intentionally delegates to the narrower phase resolvers
/// so bootstrap, mapping, and SessionFactory settings keep separate ownership.
///
/// @see BootstrapSettingsResolver
/// @see MappingSettingsResolver
/// @see SessionFactorySettingsResolver
///
/// @since 9.0
/// @author Steve Ebersole
public class SettingsResolver {
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// BootstrapSettingsResolver

	/// @see BootstrapSettingsResolver#resolve(Map)
	public static ResolvedBootstrapSettings resolveBootstrapSettings(Map<?, ?> configurationValues) {
		return BootstrapSettingsResolver.resolve( configurationValues );
	}

	/// @see BootstrapSettingsResolver#resolve(Map,boolean)
	public static ResolvedBootstrapSettings resolveBootstrapSettings(
			Map<?, ?> configurationValues,
			boolean jpaBootstrap) {
		return BootstrapSettingsResolver.resolve( configurationValues, jpaBootstrap );
	}

	/// @see BootstrapSettingsResolver#resolve(HibernatePersistenceConfiguration)
	public static ResolvedBootstrapSettings resolveBootstrapSettings(
			HibernatePersistenceConfiguration persistenceConfiguration) {
		return BootstrapSettingsResolver.resolve( persistenceConfiguration );
	}

	/// @see BootstrapSettingsResolver#resolve(HibernatePersistenceConfiguration, Map)
	public static ResolvedBootstrapSettings resolveBootstrapSettings(
			HibernatePersistenceConfiguration persistenceConfiguration,
			Map<?, ?> integrationSettings) {
		return BootstrapSettingsResolver.resolve( persistenceConfiguration, integrationSettings );
	}

	/// @see BootstrapSettingsResolver#resolve(PersistenceUnitDescriptor, Map)
	public static ResolvedBootstrapSettings resolveBootstrapSettings(
			PersistenceUnitDescriptor persistenceUnitDescriptor,
			Map<?, ?> integrationSettings) {
		return BootstrapSettingsResolver.resolve( persistenceUnitDescriptor, integrationSettings );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// MappingSettingsResolver

	/// @see MappingSettingsResolver#resolve(ResolvedBootstrapSettings, FetchType)
	public static ResolvedMappingSettings resolveMappingSettings(
			ResolvedBootstrapSettings bootstrapSettings,
			FetchType defaultToOneFetchType) {
		return MappingSettingsResolver.resolve( bootstrapSettings, defaultToOneFetchType );
	}

	/// @see MappingSettingsResolver#resolve(Map, FetchType)
	public static ResolvedMappingSettings resolveMappingSettings(
			Map<String, Object> configurationValues,
			FetchType defaultToOneFetchType) {
		return MappingSettingsResolver.resolve( configurationValues, defaultToOneFetchType );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// SessionFactorySettingsResolver

	/// @see SessionFactorySettingsResolver#resolve(ResolvedBootstrapSettings, ServiceRegistry)
	public static ResolvedSessionFactorySettings resolveSessionFactorySettings(
			ResolvedBootstrapSettings bootstrapSettings,
			ServiceRegistry serviceRegistry) {
		return SessionFactorySettingsResolver.resolve( bootstrapSettings, serviceRegistry );
	}
}
