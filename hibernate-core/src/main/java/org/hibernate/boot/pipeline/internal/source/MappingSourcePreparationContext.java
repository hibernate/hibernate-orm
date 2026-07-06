/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.pipeline.internal.source;

import java.util.function.Function;

import org.hibernate.boot.jaxb.internal.MappingBinder;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.models.spi.ModelsContext;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.spi.ServiceRegistryImplementor;

/// Context used while assembling [PreparedMappingSources].
///
/// This context keeps source collection tied to the early services it actually
/// needs: Hibernate Models access and resource/class-loading services.  It
/// intentionally does not depend on `MetadataBuildingContext`, which is created
/// later when the resolved mapping sources are categorized and bound.
///
/// @since 9.0
/// @author Steve Ebersole
public record MappingSourcePreparationContext(
		/// Model context used to resolve class and package details.
		ModelsContext modelsContext,

		/// Class-loading services used while resolving resource names and
		/// scanner implementations.
		ClassLoaderService classLoaderService,

		/// Access to XML binding settings.
		Function<String, Object> xmlBindingSettings) {

	public MappingSourcePreparationContext(ModelsContext modelsContext, ServiceRegistry serviceRegistry) {
		this(
				modelsContext,
				serviceRegistry.requireService( ClassLoaderService.class ),
				xmlBindingSettings( serviceRegistry )
		);
	}

	/// Access to class-loading services used while resolving resource names and
	/// scanner implementations.
	public ClassLoaderService getClassLoaderService() {
		return classLoaderService;
	}

	/// Create a mapping binder backed by this context's class-loading service
	/// and XML binding settings.
	public MappingBinder createMappingBinder() {
		return new MappingBinder( classLoaderService, xmlBindingSettings );
	}

	private static Function<String, Object> xmlBindingSettings(ServiceRegistry serviceRegistry) {
		return (settingName) -> {
			final var configurationService =
					serviceRegistry instanceof ServiceRegistryImplementor serviceRegistryImplementor
							? serviceRegistryImplementor.fromRegistryOrChildren( ConfigurationService.class )
							: serviceRegistry.getService( ConfigurationService.class );
			return configurationService == null ? null : configurationService.getSettings().get( settingName );
		};
	}
}
