/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.jpa.boot.discovery.spi;

import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.jpa.boot.discovery.archive.spi.ArchiveDescriptorFactory;
import org.hibernate.service.JavaServiceLoadable;

import java.util.Map;

/// Provider for [Discovery] instances.
///
/// @author Steve Ebersole
@JavaServiceLoadable
public interface DiscoveryProvider {
	/// Create a scanner.
	///
	/// @todo (jpa4) : integrate BootstrapContext here if possible.
	///
	/// @param archiveDescriptorFactory Environment factory for [ArchiveDescriptor] references.
	/// @param classLoaderService Access to class-loading features
	/// @param properties Configuration properties.  Raw to better fit with {@linkplain java.util.Properties}, etc.
	Discovery builderScanner(
			ArchiveDescriptorFactory archiveDescriptorFactory,
			ClassLoaderService classLoaderService,
			@SuppressWarnings("rawtypes") Map properties);
}
