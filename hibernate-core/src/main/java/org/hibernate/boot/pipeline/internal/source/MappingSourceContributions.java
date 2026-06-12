/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.pipeline.internal.source;

import java.net.URI;
import java.net.URL;
import java.util.Collection;
import java.util.List;

import org.hibernate.Internal;
import org.hibernate.boot.internal.MappingSources;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.scan.spi.ScanningResult;
import org.hibernate.boot.pipeline.internal.settings.ResolvedBootstrapSettings;
import org.hibernate.boot.pipeline.internal.settings.ResolvedMappingSettings;
import org.hibernate.boot.pipeline.internal.settings.SettingsResolver;
import org.hibernate.jpa.HibernatePersistenceConfiguration;
import org.hibernate.jpa.boot.spi.PersistenceUnitDescriptor;

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
@Internal
public record MappingSourceContributions(
		/// Managed Java classes explicitly contributed by the entry point.
		List<Class<?>> managedClasses,

		/// Managed Java class names discovered or explicitly contributed without
		/// loading the class.
		List<String> managedClassNames,

		/// Package names discovered or explicitly contributed for package-level
		/// metadata.
		List<String> packageNames,

		/// XML mapping resources explicitly contributed by the entry point.
		List<String> mappingResources,

	/// XML mapping files discovered during archive scanning.
	List<URI> mappingFileUris,

	/// XML mapping files explicitly contributed by URL.
	List<URL> mappingFileUrls) {

	public MappingSourceContributions {
		managedClasses = managedClasses == null ? List.of() : List.copyOf( managedClasses );
		managedClassNames = managedClassNames == null ? List.of() : List.copyOf( managedClassNames );
		packageNames = packageNames == null ? List.of() : List.copyOf( packageNames );
		mappingResources = mappingResources == null ? List.of() : List.copyOf( mappingResources );
		mappingFileUris = mappingFileUris == null ? List.of() : List.copyOf( mappingFileUris );
		mappingFileUrls = mappingFileUrls == null ? List.of() : List.copyOf( mappingFileUrls );
	}

	public MappingSourceContributions(
			Collection<Class<?>> managedClasses,
			Collection<String> mappingResources) {
		this( managedClasses, List.of(), List.of(), mappingResources, List.of(), List.of() );
	}

	public MappingSourceContributions(
			Collection<Class<?>> managedClasses,
			Collection<String> managedClassNames,
			Collection<String> packageNames,
			Collection<String> mappingResources,
			Collection<URI> mappingFileUris) {
		this( managedClasses, managedClassNames, packageNames, mappingResources, mappingFileUris, List.of() );
	}

	public MappingSourceContributions(
			Collection<Class<?>> managedClasses,
			Collection<String> managedClassNames,
			Collection<String> packageNames,
			Collection<String> mappingResources,
			Collection<URI> mappingFileUris,
			Collection<URL> mappingFileUrls) {
		this(
				managedClasses == null ? List.of() : List.copyOf( managedClasses ),
				managedClassNames == null ? List.of() : List.copyOf( managedClassNames ),
				packageNames == null ? List.of() : List.copyOf( packageNames ),
				mappingResources == null ? List.of() : List.copyOf( mappingResources ),
				mappingFileUris == null ? List.of() : List.copyOf( mappingFileUris ),
				mappingFileUrls == null ? List.of() : List.copyOf( mappingFileUrls )
		);
	}

	/// Adapts native mapping-source declarations to internal source contributions.
	public static MappingSourceContributions from(MappingSources mappingSources) {
		return new MappingSourceContributions(
				mappingSources.getManagedClasses(),
				mappingSources.getManagedClassNames(),
				mappingSources.getPackageNames(),
				mappingSources.getMappingResources(),
				mappingSources.getMappingFileUris(),
				mappingSources.getMappingFileUrls()
		);
	}

	/// Adapts Jakarta Persistence's programmatic bootstrap configuration to
	/// neutral source contributions.
	public static MappingSourceContributions from(PersistenceConfiguration persistenceConfiguration) {
		if ( persistenceConfiguration instanceof HibernatePersistenceConfiguration hibernatePersistenceConfiguration ) {
			final var bootstrapSettings = SettingsResolver.resolveBootstrapSettings( hibernatePersistenceConfiguration );
			return from(
					hibernatePersistenceConfiguration,
					bootstrapSettings,
					SettingsResolver.resolveMappingSettings(
							bootstrapSettings,
							hibernatePersistenceConfiguration.defaultToOneFetchType()
					),
					null
			);
		}
		return new MappingSourceContributions(
				persistenceConfiguration.managedClasses(),
				persistenceConfiguration.mappingFiles()
		);
	}

	/// Adapts Hibernate's persistence-unit descriptor abstraction to neutral
	/// source contributions.
	public static MappingSourceContributions from(PersistenceUnitDescriptor persistenceUnitDescriptor) {
		return new MappingSourceContributions(
				List.of(),
				persistenceUnitDescriptor.getAllClassNames(),
				List.of(),
				persistenceUnitDescriptor.getMappingFileNames(),
				List.of(),
				List.of()
		);
	}

	/// Adapts Hibernate's programmatic JPA bootstrap configuration to neutral
	/// source contributions, including archive scanning.
	public static MappingSourceContributions from(
			HibernatePersistenceConfiguration persistenceConfiguration,
			ResolvedBootstrapSettings bootstrapSettings,
			ResolvedMappingSettings mappingSettings,
			ClassLoaderService classLoaderService) {
		if ( classLoaderService == null ) {
			return new MappingSourceContributions(
					persistenceConfiguration.managedClasses(),
					persistenceConfiguration.mappingFiles()
			);
		}
		final ScanningResult scanningResult = HibernatePersistenceConfigurationScanner.performScanning(
				persistenceConfiguration,
				bootstrapSettings,
				mappingSettings,
				classLoaderService
		);
		return new MappingSourceContributions(
				persistenceConfiguration.managedClasses(),
				scanningResult.discoveredClasses(),
				scanningResult.discoveredPackages(),
				persistenceConfiguration.mappingFiles(),
				scanningResult.mappingFiles(),
				List.of()
		);
	}
}
