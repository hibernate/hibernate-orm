/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.pipeline.internal.source;

import java.util.Objects;

import jakarta.persistence.PersistenceConfiguration;

import org.hibernate.boot.pipeline.internal.settings.ResolvedBootstrapSettings;
import org.hibernate.boot.pipeline.internal.settings.ResolvedMappingSettings;
import org.hibernate.jpa.HibernatePersistenceConfiguration;

/// Processes mapping sources defined by [PersistenceConfiguration] and [HibernatePersistenceConfiguration].
///
/// `BootstrapPipeline` uses this processor for configuration-based bootstrap: standard JPA
/// configuration supplies explicit declarations,  while Hibernate's configuration extension
/// additionally supplies archive boundaries for discovery.  Persistence-unit descriptors
/// follow the separate [PersistenceUnitSources] path.
///
/// The caller chooses the operation. [#declared(PersistenceConfiguration)] collects only
/// supplied declarations, even when passed a Hibernate configuration. [#discover] starts
/// with those same declarations and supplements them through archive discovery.
/// Bootstrap selects discovery for a Hibernate configuration and declaration collection otherwise.
/// [PreparedMappingSources] also uses these operations for direct source preparation.
///
/// This processor is stateless. Each call returns a fresh [MappingSources] accumulator,
/// keeping class, package-descriptor, module-descriptor, and XML inputs separate.
/// Model-detail resolution and XML binding happen later in [PreparedMappingSources].
///
/// @see org.hibernate.boot.pipeline.internal.BootstrapPipeline
///
/// @author Steve Ebersole
public final class ConfigurationMappingProcessor {
	/// Collects the mapping declarations explicitly supplied by the configuration.
	/// Includes managed classes, package descriptors, module descriptors, and named XML resources.
	/// For a [HibernatePersistenceConfiguration], also includes its class-name and package-name
	/// registrations and XML mapping URIs and URLs.
	///
	/// Used directly by standard JPA configuration bootstrap and the corresponding
	/// [PreparedMappingSources] preparation overload, and as the starting point for [#discover].
	/// Passing Hibernate's configuration extension to this method does not enable discovery:
	/// no archives are scanned and no default XML mappings are sought.
	/// Explicit XML declarations are retained here; preparation decides whether to bind them
	/// according to the resolved mapping settings.
	///
	/// @param configuration non-null configuration supplying the declarations
	/// @return a fresh, mutable source accumulator containing the explicit declarations
	public static MappingSources declared(PersistenceConfiguration configuration) {
		final var sources = new MappingSources( configuration.managedClasses(), configuration.mappingFiles() );
		if ( configuration instanceof HibernatePersistenceConfiguration hibernateConfiguration ) {
			sources.addManagedClassNames( hibernateConfiguration.managedClassNames() )
					.addPackages( hibernateConfiguration.packageNames() )
					.addMappingUris( hibernateConfiguration.mappingFileUris() )
					.addMappingUrls( hibernateConfiguration.mappingFileUrls() );
		}
		return sources.addPackages( configuration.managedPackageDescriptors() )
				.addModules( configuration.managedModuleDescriptors() );
	}

	/// Collects explicit declarations and supplements them using Hibernate's archive discovery.
	/// Used by bootstrap for [HibernatePersistenceConfiguration], and by the
	/// [PreparedMappingSources] overload accepting that configuration type.
	///
	/// Starts with [#declared(PersistenceConfiguration)], then adds classes, package descriptors,
	/// module descriptors, and mapping files discovered by scanning the configured root and
	/// referenced archives. [DefaultXmlMappingDiscovery] additionally locates META-INF/orm.xml
	/// within those archives when XML mapping is enabled. Discovery is bounded by the configured
	/// archives; an absent root and empty archive list leave only the explicit declarations.
	///
	/// Callers supply settings resolved for this configuration and services for archive access.
	/// Discovery may perform archive I/O, but does not resolve model details or bind XML.
	/// Results are not cached; each invocation performs collection anew.
	///
	/// @param configuration non-null Hibernate configuration supplying declarations and archive boundaries
	/// @param settings non-null resolved bootstrap settings used to configure scanning
	/// @param mappingSettings non-null resolved mapping settings, including whether XML mapping is enabled
	/// @param context non-null services used for discovery
	/// @return a fresh, mutable source accumulator containing explicit and discovered sources
	public static MappingSources discover(
			HibernatePersistenceConfiguration configuration,
			ResolvedBootstrapSettings settings,
			ResolvedMappingSettings mappingSettings,
			ContributionDiscoveryContext context) {
		Objects.requireNonNull( settings );
		Objects.requireNonNull( mappingSettings );
		Objects.requireNonNull( context );
		final var sources = declared( configuration );
		final var scanned = HibernatePersistenceConfigurationScanner.performScanning(
				configuration, settings, mappingSettings, context.classLoaderService() );
		sources.addManagedClassNames( scanned.discoveredClasses() )
				.addPackages( scanned.discoveredPackages() )
				.addModules( scanned.discoveredModules() );
		scanned.mappingFiles().forEach( uri -> sources.addXmlMappingSource( XmlMappingSource.discoveredUri( uri ) ) );
		if ( mappingSettings.xmlMappingEnabled() ) {
			DefaultXmlMappingDiscovery.collect( sources, configuration.rootUrl(), configuration.jarFileUrls(), settings, context );
		}
		return sources;
	}

	private ConfigurationMappingProcessor() {
	}
}
