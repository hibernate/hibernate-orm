/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.pipeline.internal.source;

import org.hibernate.boot.model.process.internal.ManagedResourceValidation;
import org.hibernate.boot.pipeline.internal.settings.ResolvedBootstrapSettings;
import org.hibernate.boot.pipeline.internal.settings.ResolvedMappingSettings;
import org.hibernate.boot.pipeline.internal.settings.SettingsResolver;
import org.hibernate.jpa.boot.internal.PersistenceUnitInfoDescriptor;
import org.hibernate.jpa.boot.spi.PersistenceUnitDescriptor;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/// Adapts a persistence unit to the source-collection contract selected by its bootstrap entry point.
/// Standalone entry points require provider discovery, while container entry points supply
/// an authoritative inventory of classes, package descriptors, and module descriptors.
/// This choice is explicit; it is not inferred from the descriptor's runtime type.
///
/// [org.hibernate.jpa.HibernatePersistenceProvider] selects the adapter when creating a factory
/// or generating schema. Metadata-only callers make the same choice before invoking
/// [org.hibernate.boot.pipeline.internal.BootstrapPipeline]. Both contracts discover default XML mappings
/// in the persistence-unit archives when XML mapping is enabled.
///
/// An adapter retains its descriptor and collects sources on demand. It does not prepare the
/// domain model or bind XML; those are responsibilities of [PreparedMappingSources].
///
/// @author Steve Ebersole
public sealed interface PersistenceUnitSources {
	/// The descriptor retained by this adapter. Bootstrap uses it to resolve settings and establish
	/// the persistence unit's service registries before collecting sources.
	///
	/// @return the non-null descriptor supplied when this adapter was created, without discovery
	PersistenceUnitDescriptor descriptor();

	/// Collects the persistence unit's sources according to this adapter's discovery contract.
	/// The shared persistence-unit request construction in
	/// [org.hibernate.boot.pipeline.internal.BootstrapPipeline] calls this method for factory creation,
	/// schema generation, and metadata inspection. [PreparedMappingSources] also calls it when
	/// preparing sources directly from an adapter.
	///
	/// Callers must supply settings resolved for this persistence unit, including integration
	/// overrides. The discovery context is required for both contracts: container inventories
	/// bypass type scanning, but still need archive access for default XML discovery.
	/// Collection may perform archive I/O, but does not resolve model details or bind XML.
	/// It does not register a class transformer; full bootstrap performs that step separately
	/// before collection via [#registerClassTransformer(Map)].
	///
	/// @param settings non-null resolved bootstrap settings
	/// @param mappingSettings non-null resolved mapping settings controlling, among other things, XML discovery
	/// @param context non-null services used for discovery
	/// @return a fresh, mutable source accumulator for subsequent preparation; results are not cached
	MappingSources collect(
			ResolvedBootstrapSettings settings,
			ResolvedMappingSettings mappingSettings,
			ContributionDiscoveryContext context);

	/// Convenience form for callers which have not already resolved mapping settings.
	/// Derives them from the supplied bootstrap settings and the descriptor's default to-one
	/// fetch type, then delegates to [#collect(ResolvedBootstrapSettings, ResolvedMappingSettings, ContributionDiscoveryContext)].
	/// Bootstrap orchestration uses the three-argument form to preserve its already resolved settings.
	///
	/// @param settings non-null resolved bootstrap settings, including integration overrides
	/// @param context non-null services used for discovery
	/// @return a fresh source accumulator collected using the derived mapping settings
	default MappingSources collect(
			ResolvedBootstrapSettings settings,
			ContributionDiscoveryContext context) {
		return collect(
				settings,
				SettingsResolver.resolveMappingSettings( settings, descriptor().getDefaultToOneFetchType() ),
				context
		);
	}

	/// Gives the entry-point contract an opportunity to register the container class transformer.
	/// [org.hibernate.boot.pipeline.internal.BootstrapPipeline] invokes this hook before collecting sources
	/// for factory creation, schema generation, or metadata inspection. Direct source collection
	/// and preparation do not invoke it.
	///
	/// The default implementation does nothing, as standalone bootstrap has no container
	/// registration contract. The container adapter delegates to its descriptor, which decides
	/// whether registration is needed according to the enhancement configuration and container capabilities.
	///
	/// @param integrationSettings integration overrides supplied to the bootstrap entry point
	default void registerClassTransformer(Map<?, ?> integrationSettings) {
	}

	/// Selects provider discovery for a persistence unit whose descriptor supplies declarations.
	/// Used by the provider's factory and schema entry points taking a persistence-unit name,
	/// and by metadata tooling working from a parsed persistence unit.
	///
	/// Collection supplements the declared inventory by scanning eligible persistence-unit
	/// archives. Callers adapting a container-supplied inventory must instead select
	/// [#container(PersistenceUnitInfoDescriptor)]. Creating this adapter performs no discovery.
	///
	/// @param descriptor non-null persistence-unit declarations, retained without taking a snapshot
	/// @return an adapter which performs provider discovery when sources are collected
	static PersistenceUnitSources standalone(PersistenceUnitDescriptor descriptor) {
		return new Standalone( descriptor );
	}

	/// Selects the authoritative-inventory contract for a container persistence unit.
	/// Used by the provider's factory and schema entry points taking a
	/// [jakarta.persistence.spi.PersistenceUnitInfo].
	///
	/// The container is responsible for supplying the complete class, package-descriptor,
	/// and module-descriptor inventory. Collection does not invoke the provider type scanner,
	/// even if one is configured. Default XML discovery still applies to the persistence-unit
	/// root and referenced archives. Creating this adapter neither scans nor registers a transformer.
	///
	/// @param descriptor non-null container descriptor, retained without taking a snapshot
	/// @return an adapter which trusts the supplied inventory and supports container transformer registration
	static PersistenceUnitSources container(PersistenceUnitInfoDescriptor descriptor) {
		return new Container( descriptor );
	}

	/// Used when Hibernate is responsible for discovering persistence-unit contents from declarations.
	/// The descriptor supplies a starting inventory and archive boundaries; its lists need not
	/// enumerate every class, package descriptor, or module descriptor belonging to the unit.
	///
	/// Selected by [org.hibernate.jpa.HibernatePersistenceProvider] for
	/// `createEntityManagerFactory(String, Map)` and `generateSchema(String, Map)`, after locating
	/// the named persistence unit in persistence.xml. Metadata tooling likewise uses this subtype
	/// when inspecting a parsed persistence unit without creating a factory.
	///
	/// Collection supplements the supplied inventory by scanning the persistence-unit root
	/// (unless unlisted classes are excluded) and referenced archives. It also discovers default
	/// XML mappings when enabled. This subtype has no container class-transformer registration hook.
	/// Choose it based on the entry point's discovery responsibility, not merely because the
	/// descriptor happens to come from XML; container bootstrap uses [Container].
	///
	/// @param descriptor non-null declarations to supplement through provider discovery
	record Standalone(PersistenceUnitDescriptor descriptor) implements PersistenceUnitSources {
		public Standalone {
			Objects.requireNonNull( descriptor );
		}

		/// Supplements the descriptor's inventory with provider scanning results and default XML mappings.
		/// Excluding unlisted classes suppresses type scanning of the persistence-unit root,
		/// but does not suppress scanning referenced archives or discovering the root's default XML mapping.
		@Override
		public MappingSources collect(
				ResolvedBootstrapSettings settings,
				ResolvedMappingSettings mappingSettings,
				ContributionDiscoveryContext context) {
			Objects.requireNonNull( settings );
			Objects.requireNonNull( mappingSettings );
			Objects.requireNonNull( context );
			final var sources = declarations( descriptor );
			final var scanned = HibernatePersistenceConfigurationScanner.performScanning(
					descriptor, settings, mappingSettings, context.classLoaderService() );
			sources.addManagedClassNames( scanned.discoveredClasses() )
					.addPackages( scanned.discoveredPackages() ).addModules( scanned.discoveredModules() );
			scanned.mappingFiles().forEach( uri -> sources.addXmlMappingSource( XmlMappingSource.discoveredUri( uri ) ) );
			collectDefaultMappings( sources, descriptor, settings, mappingSettings, context );
			return sources;
		}
	}

	/// Used when a container supplies the authoritative persistence-unit inventory through
	/// [jakarta.persistence.spi.PersistenceUnitInfo]. The container is responsible for enumerating
	/// the unit's classes, package descriptors, and module descriptors before invoking Hibernate.
	///
	/// Selected by [org.hibernate.jpa.HibernatePersistenceProvider] for
	/// `createContainerEntityManagerFactory(PersistenceUnitInfo, Map)` and
	/// `generateSchema(PersistenceUnitInfo, Map)`, wrapping the supplied information in a
	/// [PersistenceUnitInfoDescriptor]. The same contract applies to metadata inspection if its
	/// caller supplies a container inventory.
	///
	/// Collection accepts that inventory without invoking the provider type scanner. Default
	/// XML mappings are still discovered in the persistence-unit root and referenced archives
	/// when XML mapping is enabled: an authoritative type inventory does not suppress XML discovery.
	/// This subtype also delegates the bootstrap class-transformer registration hook to the
	/// container descriptor. Use [Standalone] when Hibernate must discover the type inventory itself.
	///
	/// @param descriptor non-null authoritative container inventory and container integration contract
	record Container(PersistenceUnitInfoDescriptor descriptor) implements PersistenceUnitSources {
		public Container {
			Objects.requireNonNull( descriptor );
		}

		/// Delegates the bootstrap registration hook to the container descriptor.
		@Override
		public void registerClassTransformer(Map<?, ?> integrationSettings) {
			descriptor.registerClassTransformer( integrationSettings );
		}

		/// Uses the descriptor's complete inventory without invoking provider type scanning,
		/// then discovers default XML mappings when enabled. The discovery context remains required
		/// for that archive lookup, even though classes, packages, and modules are already enumerated.
		@Override
		public MappingSources collect(
				ResolvedBootstrapSettings settings,
				ResolvedMappingSettings mappingSettings,
				ContributionDiscoveryContext context) {
			Objects.requireNonNull( settings );
			Objects.requireNonNull( mappingSettings );
			Objects.requireNonNull( context );
			final var sources = declarations( descriptor );
			collectDefaultMappings( sources, descriptor, settings, mappingSettings, context );
			return sources;
		}
	}

	/// Seeds either contract's accumulator from the descriptor's full inventory and named XML resources.
	/// Validates explicit managed-class names before discovery so that package-info and module-info
	/// cannot be presented as class declarations. Descriptor entries retain their separate categories.
	private static MappingSources declarations(PersistenceUnitDescriptor descriptor) {
		descriptor.getManagedClassNames().forEach( ManagedResourceValidation::validateClassName );
		return new MappingSources( List.of(), descriptor.getAllClassNames(), descriptor.getAllPackageDescriptors(),
				descriptor.getMappingFileNames(), List.of(), List.of() )
				.addModules( descriptor.getAllModuleDescriptors() )
				.includeUnlistedStructuralTypes( !descriptor.isExcludeUnlistedClasses() );
	}

	/// Shared by both contracts to discover META-INF/orm.xml in the persistence-unit root and
	/// referenced archives. This lookup is independent of the exclusion of unlisted classes;
	/// it does not search unrelated classloader-visible archives. Disabling XML mapping skips the lookup.
	private static void collectDefaultMappings(
			MappingSources sources,
			PersistenceUnitDescriptor descriptor,
			ResolvedBootstrapSettings settings,
			ResolvedMappingSettings mappingSettings,
			ContributionDiscoveryContext context) {
		if ( mappingSettings.xmlMappingEnabled() ) {
			DefaultXmlMappingDiscovery.collect(
					sources,
					descriptor.getPersistenceUnitRootUrl(),
					descriptor.getJarFileUrls(),
					settings,
					context
			);
		}
	}
}
