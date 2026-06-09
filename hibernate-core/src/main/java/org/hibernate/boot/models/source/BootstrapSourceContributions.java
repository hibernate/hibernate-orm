/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.source;

import java.net.URI;
import java.util.Collection;
import java.util.List;

import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.scan.spi.ScanningResult;
import org.hibernate.boot.settings.ResolvedBootstrapSettings;
import org.hibernate.jpa.HibernatePersistenceConfiguration;

import jakarta.persistence.PersistenceConfiguration;

/// Mapping-source contributions collected from a bootstrap entry point.
///
/// This type intentionally carries source declarations only: managed classes,
/// discovered class and package names, and XML mappings.  Settings resolution
/// remains a separate concern handled by the orchestration layer, and scanning is
/// performed by entry-point adapters before this descriptor is consumed.
///
/// @since 9.0
/// @author Steve Ebersole
public record BootstrapSourceContributions(
		/// Managed Java classes explicitly contributed by the entry point.
		List<Class<?>> managedClasses,

		/// Managed Java class names discovered or explicitly contributed without
		/// loading the class.
		List<String> managedClassNames,

		/// Package names discovered or explicitly contributed for package-level
		/// metadata.
		List<String> packageNames,

		/// XML mapping resources explicitly contributed by the entry point.
		List<String> mappingFiles,

		/// XML mapping files discovered during archive scanning.
		List<URI> mappingFileUris) {

	public BootstrapSourceContributions {
		managedClasses = managedClasses == null ? List.of() : List.copyOf( managedClasses );
		managedClassNames = managedClassNames == null ? List.of() : List.copyOf( managedClassNames );
		packageNames = packageNames == null ? List.of() : List.copyOf( packageNames );
		mappingFiles = mappingFiles == null ? List.of() : List.copyOf( mappingFiles );
		mappingFileUris = mappingFileUris == null ? List.of() : List.copyOf( mappingFileUris );
	}

	public BootstrapSourceContributions(
			Collection<Class<?>> managedClasses,
			Collection<String> mappingFiles) {
		this( managedClasses, List.of(), List.of(), mappingFiles, List.of() );
	}

	public BootstrapSourceContributions(
			Collection<Class<?>> managedClasses,
			Collection<String> managedClassNames,
			Collection<String> packageNames,
			Collection<String> mappingFiles,
			Collection<URI> mappingFileUris) {
		this(
				managedClasses == null ? List.of() : List.copyOf( managedClasses ),
				managedClassNames == null ? List.of() : List.copyOf( managedClassNames ),
				packageNames == null ? List.of() : List.copyOf( packageNames ),
				mappingFiles == null ? List.of() : List.copyOf( mappingFiles ),
				mappingFileUris == null ? List.of() : List.copyOf( mappingFileUris )
		);
	}

	/// Adapts Jakarta Persistence's programmatic bootstrap configuration to
	/// neutral source contributions.
	public static BootstrapSourceContributions from(PersistenceConfiguration persistenceConfiguration) {
		if ( persistenceConfiguration instanceof HibernatePersistenceConfiguration hibernatePersistenceConfiguration ) {
			return from(
					hibernatePersistenceConfiguration,
					new org.hibernate.boot.settings.BootstrapSettingsResolver()
							.resolve( hibernatePersistenceConfiguration ),
					null
			);
		}
		return new BootstrapSourceContributions(
				persistenceConfiguration.managedClasses(),
				persistenceConfiguration.mappingFiles()
		);
	}

	/// Adapts Hibernate's programmatic JPA bootstrap configuration to neutral
	/// source contributions, including archive scanning.
	public static BootstrapSourceContributions from(
			HibernatePersistenceConfiguration persistenceConfiguration,
			ResolvedBootstrapSettings bootstrapSettings,
			ClassLoaderService classLoaderService) {
		if ( classLoaderService == null ) {
			return new BootstrapSourceContributions(
					persistenceConfiguration.managedClasses(),
					persistenceConfiguration.mappingFiles()
			);
		}
		final ScanningResult scanningResult = HibernatePersistenceConfigurationScanner.performScanning(
				persistenceConfiguration,
				bootstrapSettings,
				classLoaderService
		);
		return new BootstrapSourceContributions(
				persistenceConfiguration.managedClasses(),
				scanningResult.discoveredClasses(),
				scanningResult.discoveredPackages(),
				persistenceConfiguration.mappingFiles(),
				scanningResult.mappingFiles()
		);
	}
}
