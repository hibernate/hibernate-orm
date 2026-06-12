/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.pipeline.internal.source;

import org.hibernate.boot.jaxb.internal.MappingBinder;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.models.spi.ModelsContext;
import org.hibernate.service.ServiceRegistry;

/// Context used while assembling [AvailableResources].
///
/// This context keeps source collection tied to the early services it actually
/// needs: Hibernate Models access and resource/class-loading services.  It
/// intentionally does not depend on `MetadataBuildingContext`, which is created
/// later when the available resources are categorized and bound.
///
/// @since 9.0
/// @author Steve Ebersole
public record AvailableResourcesContext(
		/// Model context used to resolve class and package details.
		ModelsContext modelsContext,

		/// Service registry used for class loading and XML binding support.
		ServiceRegistry serviceRegistry) {

	/// Access to class-loading services used while resolving resource names and
	/// scanner implementations.
	public ClassLoaderService getClassLoaderService() {
		return serviceRegistry.requireService( ClassLoaderService.class );
	}

	/// Create a mapping binder backed by this context's service registry.
	public MappingBinder createMappingBinder() {
		return new MappingBinder( serviceRegistry );
	}
}
