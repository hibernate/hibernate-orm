/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.pipeline.internal;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.boot.jaxb.Origin;
import org.hibernate.boot.jaxb.SourceType;
import org.hibernate.boot.jaxb.internal.MappingBinder;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityMappingsImpl;
import org.hibernate.boot.jaxb.spi.Binding;
import org.hibernate.boot.mapping.internal.context.MappingResolutionContributions;
import org.hibernate.boot.model.TypeContributions;
import org.hibernate.boot.model.convert.internal.ConverterDescriptors;
import org.hibernate.boot.model.relational.AuxiliaryDatabaseObject;
import org.hibernate.boot.model.relational.Sequence;
import org.hibernate.boot.internal.BootstrapContextImpl;
import org.hibernate.boot.internal.InFlightMetadataCollectorImpl;
import org.hibernate.boot.mapping.internal.context.MetadataBuildingContextRootImpl;
import org.hibernate.boot.mapping.internal.context.MetadataBuildingContextRootInput;
import org.hibernate.boot.mapping.internal.context.RootMappingDefaults;
import org.hibernate.boot.registry.BootstrapServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.mapping.internal.context.BindingContextImpl;
import org.hibernate.boot.mapping.internal.context.BindingOptionsImpl;
import org.hibernate.boot.mapping.internal.context.BindingStateImpl;
import org.hibernate.boot.mapping.internal.context.InFlightMetadataCollectorAdapter;
import org.hibernate.boot.mapping.internal.binders.BindingCoordinator;
import org.hibernate.boot.mapping.internal.categorize.CategorizedDomainModel;
import org.hibernate.boot.mapping.internal.categorize.DomainModelCategorizer;
import org.hibernate.boot.pipeline.internal.source.PreparedMappingSources;
import org.hibernate.boot.pipeline.internal.source.MappingSourcePreparationContext;
import org.hibernate.boot.pipeline.internal.source.MappingSources;
import org.hibernate.boot.pipeline.internal.settings.ResolvedBootstrapSettings;
import org.hibernate.boot.pipeline.internal.settings.ResolvedMappingSettings;
import org.hibernate.boot.spi.AdditionalMappingContributions;
import org.hibernate.boot.spi.AdditionalMappingContributor;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.boot.spi.EffectiveMappingDefaults;
import org.hibernate.boot.spi.InFlightMetadataCollector;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.HibernateException;
import org.hibernate.mapping.Table;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.MutableClassDetailsRegistry;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.spi.TypeConfiguration;
import org.hibernate.usertype.CompositeUserType;

import jakarta.persistence.AttributeConverter;

import static org.hibernate.boot.BootLogging.BOOT_LOGGER;

/// Resolves ORM boot metadata from mapping sources and resolved settings.
///
/// This class owns the phase order for the metadata collector.  It turns resolved
/// bootstrap settings and mapping sources into ORM's boot-time
/// [MetadataImplementor] while preserving intermediate boot-model products in
/// [ResolvedMapping].
///
/// Higher-level entry points should resolve settings and mapping sources
/// before calling this resolver.
///
/// @since 9.0
/// @author Steve Ebersole
public class MappingResolutionPipeline {
	/// Resolve ORM boot metadata and retain intermediate boot-model products.
	///
	/// This is the preferred prototype entry point for later SessionFactory
	/// construction experiments because it exposes the categorized model,
	/// binding-state bridge, and resolved settings that were used to produce the
	/// mapping.
	///
	/// @param bootstrapSettings Resolved bootstrap settings
	/// @param mappingSettings Resolved mapping/model-build settings
	/// @param mappingSources Mapping sources supplied by the
	/// entry point
	/// @param serviceRegistry Service registry for the metadata build
	///
	/// @return The resolved mapping product
	public static ResolvedMapping resolve(
			ResolvedBootstrapSettings bootstrapSettings,
			ResolvedMappingSettings mappingSettings,
			MappingSources mappingSources,
			ServiceRegistry serviceRegistry) {
		return resolve(
				bootstrapSettings,
				mappingSettings,
				mappingSources,
				MappingCustomizations.NONE,
				serviceRegistry
		);
	}

	/// Resolve ORM boot metadata and retain intermediate boot-model products.
	public static ResolvedMapping resolve(
			ResolvedBootstrapSettings bootstrapSettings,
			ResolvedMappingSettings mappingSettings,
			MappingSources mappingSources,
			MappingCustomizations mappingCustomizations,
			ServiceRegistry serviceRegistry) {
		final var typeConfiguration = new TypeConfiguration();
		final var standardServiceRegistry = getStandardServiceRegistry( serviceRegistry );
		final var buildingPlan = new MappingResolutionOptionsImpl( standardServiceRegistry );
		final var bootstrapContext = new BootstrapContextImpl( standardServiceRegistry, buildingPlan, typeConfiguration );
		buildingPlan.setBootstrapContext( bootstrapContext );
		if ( bootstrapSettings.jpaBootstrap() ) {
			bootstrapContext.markAsJpaBootstrap();
		}
			final var resolvedMapping = resolve(
					bootstrapSettings,
					mappingSettings,
					mappingSources,
					mappingCustomizations,
					bootstrapContext,
					buildingPlan
			);
		FunctionRegistryCoordinator.populate(
				resolvedMapping.metadata().getFunctionRegistry(),
				mappingCustomizations,
				standardServiceRegistry,
				typeConfiguration
		);
		return resolvedMapping;
	}

	public static ResolvedMapping resolve(
			ResolvedBootstrapSettings bootstrapSettings,
			ResolvedMappingSettings mappingSettings,
			MappingSources mappingSources,
			MappingCustomizations mappingCustomizations,
			BootstrapContext bootstrapContext,
			MappingResolutionOptions buildingPlan) {
		mappingCustomizations = mappingCustomizations == null ? MappingCustomizations.NONE : mappingCustomizations;
		final MetadataBuildingContext metadataBuildingContext = createMetadataBuildingContext(
				mappingSettings,
				mappingCustomizations,
				bootstrapContext,
				(MappingResolutionOptionsImpl) buildingPlan
		);
		metadataBuildingContext.getTypeConfiguration().scope( metadataBuildingContext );
		applyTypeContributions( metadataBuildingContext );
		final PreparedMappingSources resolvedMappingSources = buildPreparedMappingSources(
				mappingSources,
				mappingSettings,
				metadataBuildingContext
		);
		return resolve(
				metadataBuildingContext,
				mappingSettings,
				resolvedMappingSources,
				mappingCustomizations
		);
	}

	/// Resolve ORM boot metadata from resources already adapted by a legacy caller.
	public static ResolvedMapping resolve(
			ResolvedMappingSettings mappingSettings,
			PreparedMappingSources resolvedMappingSources,
			MappingCustomizations mappingCustomizations,
			BootstrapContext bootstrapContext,
			MappingResolutionOptions buildingPlan) {
		mappingCustomizations = mappingCustomizations == null ? MappingCustomizations.NONE : mappingCustomizations;
		final var functionRegistry = FunctionRegistryCoordinator.create();
		final var mappingContributions = mappingContributions( mappingSettings, mappingCustomizations )
				.functionRegistry( functionRegistry )
				.build();
		final var metadataCollector = new InFlightMetadataCollectorImpl( bootstrapContext, buildingPlan, mappingContributions );
		final var metadataBuildingContext = new MetadataBuildingContextRootImpl( MetadataBuildingContextRootInput.create(
				"orm",
				bootstrapContext,
				buildingPlan,
				metadataCollector,
				new RootMappingDefaults(
						buildingPlan.getMappingDefaults(),
						metadataCollector.getPersistenceUnitMetadata()
				)
		) );
		bootstrapContext.getTypeConfiguration().scope( metadataBuildingContext );
		applyTypeContributions( metadataBuildingContext );
		final var resolvedMapping = resolve(
				metadataBuildingContext,
				mappingSettings,
				resolvedMappingSources,
				mappingCustomizations
		);
		FunctionRegistryCoordinator.populate(
				functionRegistry,
				mappingCustomizations,
				bootstrapContext.getServiceRegistry(),
				bootstrapContext.getTypeConfiguration()
		);
		return resolvedMapping;
	}

	private static ResolvedMapping resolve(
			MetadataBuildingContext metadataBuildingContext,
			ResolvedMappingSettings mappingSettings,
			PreparedMappingSources resolvedMappingSources,
			MappingCustomizations mappingCustomizations) {
		final CategorizedDomainModel categorizedDomainModel = categorize(
				resolvedMappingSources,
				metadataBuildingContext
		);
		final BindingStateImpl bindingState = bind(
				categorizedDomainModel,
				mappingSettings,
				metadataBuildingContext
		);
		bindAdditionalMappingContributions(
				mappingSettings,
				metadataBuildingContext,
				bindingState
		);
		applyQueryImports( mappingCustomizations, metadataBuildingContext );
		final MetadataImplementor metadata = finalizeMetadata( metadataBuildingContext );
		final var resolutionDetailsCollector = bindingState.getMappingResolutionState().resolutionDetailsCollector();
		return new ResolvedMapping(
				metadata,
				resolutionDetailsCollector,
				org.hibernate.boot.serial.internal.RuntimeMappingHandoffSnapshot.from(
						bindingState.getBootBindingModel(),
						metadata
				)
		);
	}

	private static void applyQueryImports(
			MappingCustomizations mappingCustomizations,
			MetadataBuildingContext metadataBuildingContext) {
		mappingCustomizations.queryImports().forEach( (importName, target) ->
				metadataBuildingContext.getMetadataCollector().addImport( importName, target.getName() )
		);
	}

	private static PreparedMappingSources buildPreparedMappingSources(
			MappingSources mappingSources,
			ResolvedMappingSettings mappingSettings,
			MetadataBuildingContext metadataBuildingContext) {
		return PreparedMappingSources.from(
				mappingSources,
				new MappingSourcePreparationContext(
						metadataBuildingContext.getModelsContext(),
						metadataBuildingContext.getServiceRegistry()
				),
				mappingSettings
		);
	}

	private static void bindAdditionalMappingContributions(
			ResolvedMappingSettings mappingSettings,
			MetadataBuildingContext metadataBuildingContext,
			BindingStateImpl bindingState) {
		final PreparedMappingSources contributedResources = collectAdditionalMappingContributions(
				mappingSettings,
				metadataBuildingContext
		);
		if ( contributedResources == null ) {
			return;
		}

		final MetadataBuildingContext contributedMetadataBuildingContext = contributorContext(
				contributorName( contributedResources ),
				metadataBuildingContext
		);
		final CategorizedDomainModel contributedDomainModel = categorize(
				contributedResources,
				contributedMetadataBuildingContext
		);
		final MetadataBuildingContext previousMetadataBuildingContext =
				bindingState.useMetadataBuildingContext( contributedMetadataBuildingContext );
		try {
			coordinateBinding(
					contributedDomainModel,
					mappingSettings,
					contributedMetadataBuildingContext,
					bindingState
			);
		}
		finally {
			bindingState.useMetadataBuildingContext( previousMetadataBuildingContext );
		}
	}

	private static MetadataBuildingContext contributorContext(
			String contributor,
			MetadataBuildingContext metadataBuildingContext) {
		return new MetadataBuildingContextRootImpl(
				MetadataBuildingContextRootInput.contributor( contributor, metadataBuildingContext )
		);
	}

	private static String contributorName(PreparedMappingSources contributedResources) {
		String contributorName = null;
		for ( Binding<JaxbEntityMappingsImpl> xmlMapping : contributedResources.xmlMappings() ) {
			final String originName = xmlMapping.getOrigin().getName();
			if ( contributorName == null ) {
				contributorName = originName;
			}
			else if ( !contributorName.equals( originName ) ) {
				return "orm";
			}
		}
		return contributorName == null ? "orm" : contributorName;
	}

	private static PreparedMappingSources collectAdditionalMappingContributions(
			ResolvedMappingSettings mappingSettings,
			MetadataBuildingContext metadataBuildingContext) {
		final ClassLoaderService classLoaderService = metadataBuildingContext.getClassLoaderService();
		final AdditionalMappingContributionsImpl contributions = new AdditionalMappingContributionsImpl(
				mappingSettings,
				metadataBuildingContext,
				classLoaderService
		);
		for ( AdditionalMappingContributor contributor : classLoaderService.loadJavaServices( AdditionalMappingContributor.class ) ) {
			contributions.setCurrentContributor( contributor.getContributorName() );
			try {
				contributor.contribute(
						contributions,
						metadataBuildingContext.getMetadataCollector(),
						classLoaderService,
						metadataBuildingContext
				);
			}
			finally {
				contributions.setCurrentContributor( null );
			}
		}
		return contributions.complete();
	}

	private static CategorizedDomainModel categorize(
			PreparedMappingSources resolvedMappingSources,
			MetadataBuildingContext metadataBuildingContext) {
		return DomainModelCategorizer.categorize(
				resolvedMappingSources,
				metadataBuildingContext
		);
	}

	private static BindingStateImpl bind(
			CategorizedDomainModel categorizedDomainModel,
			ResolvedMappingSettings mappingSettings,
			MetadataBuildingContext metadataBuildingContext) {
		final BindingStateImpl bindingState = new BindingStateImpl(
				metadataBuildingContext,
				new InFlightMetadataCollectorAdapter( metadataBuildingContext.getMetadataCollector() )
		);
		coordinateBinding(
				categorizedDomainModel,
				mappingSettings,
				metadataBuildingContext,
				bindingState
		);
		return bindingState;
	}

	private static void coordinateBinding(
			CategorizedDomainModel categorizedDomainModel,
			ResolvedMappingSettings mappingSettings,
			MetadataBuildingContext metadataBuildingContext,
			BindingStateImpl bindingState) {
		BindingCoordinator.coordinateBinding(
				categorizedDomainModel,
				bindingState,
				new BindingOptionsImpl( metadataBuildingContext, mappingSettings ),
				new BindingContextImpl(
						categorizedDomainModel,
						metadataBuildingContext
				)
		);
	}

	private static MetadataImplementor finalizeMetadata(MetadataBuildingContext metadataBuildingContext) {
//		final MetadataImplementor metadata = metadataBuildingContext.getMetadataCollector();
//		metadata.orderColumns( false );
//		metadata.validate();
//		return metadata;
		return ( (InFlightMetadataCollectorImpl) metadataBuildingContext.getMetadataCollector() ).buildMetadataInstance( metadataBuildingContext );
	}

	private static MetadataBuildingContext createMetadataBuildingContext(
			ResolvedMappingSettings mappingSettings,
			MappingCustomizations mappingCustomizations,
			BootstrapContext bootstrapContext,
			MappingResolutionOptionsImpl buildingPlan) {
		final var standardServiceRegistry = bootstrapContext.getServiceRegistry();
		final var mappingContributions = mappingContributions( mappingSettings, mappingCustomizations )
				.functionRegistry( FunctionRegistryCoordinator.create() );

		buildingPlan.applyDefaultToOneFetchType( mappingSettings.defaultToOneFetchType() );
		if ( mappingCustomizations.implicitNamingStrategy() != null ) {
			buildingPlan.applyImplicitNamingStrategy( mappingCustomizations.implicitNamingStrategy() );
		}
		if ( mappingCustomizations.physicalNamingStrategy() != null ) {
			buildingPlan.applyPhysicalNamingStrategy( mappingCustomizations.physicalNamingStrategy() );
		}
		if ( mappingCustomizations.columnOrderingStrategy() != null ) {
			buildingPlan.applyColumnOrderingStrategy( mappingCustomizations.columnOrderingStrategy() );
		}
		if ( mappingCustomizations.sharedCacheMode() != null ) {
			buildingPlan.applySharedCacheMode( mappingCustomizations.sharedCacheMode() );
		}

		mappingCustomizations.typeContributors().forEach( typeContributor ->
			typeContributor.contribute(
					typeContributions( bootstrapContext, buildingPlan, mappingContributions ),
					standardServiceRegistry
			)
		);

		mappingCustomizations.basicTypeRegistrations().forEach( registration ->
			buildingPlan.applyBasicType(
					registration.getBasicType(),
					registration.getRegistrationKeys()
			)
		);

		mappingCustomizations.userTypeRegistrations().forEach( registration ->
			buildingPlan.applyBasicType(
					registration.type(),
					registration.keys()
			)
		);

		final var metadataCollector = new InFlightMetadataCollectorImpl(
				bootstrapContext,
				buildingPlan,
				mappingContributions.build()
		);
		return new MetadataBuildingContextRootImpl( MetadataBuildingContextRootInput.create(
				"orm",
				bootstrapContext,
				buildingPlan,
				metadataCollector,
				new RootMappingDefaults(
						buildingPlan.getMappingDefaults(),
						metadataCollector.getPersistenceUnitMetadata()
				)
		) );
	}

	private static StandardServiceRegistry getStandardServiceRegistry(ServiceRegistry serviceRegistry) {
		if ( serviceRegistry == null ) {
			throw new HibernateException( "ServiceRegistry passed to MappingResolutionPipeline cannot be null" );
		}
		else if ( serviceRegistry instanceof StandardServiceRegistry standardServiceRegistry ) {
			return standardServiceRegistry;
		}
		else if ( serviceRegistry instanceof BootstrapServiceRegistry bootstrapServiceRegistry ) {
			BOOT_LOGGER.badServiceRegistry();
			return new StandardServiceRegistryBuilder( bootstrapServiceRegistry ).build();
		}
		else {
			throw new HibernateException(
					String.format(
							"Unexpected type of ServiceRegistry [%s] encountered in attempt to resolve metadata",
							serviceRegistry.getClass().getName()
					)
			);
		}
	}

	private static TypeContributions typeContributions(
			BootstrapContext bootstrapContext,
			MappingResolutionOptionsImpl buildingPlan,
			MappingResolutionContributions.Builder mappingContributions) {
		return new TypeContributions() {
			@Override
			public org.hibernate.type.spi.TypeConfiguration getTypeConfiguration() {
				return bootstrapContext.getTypeConfiguration();
			}

			@Override
			public void contributeType(org.hibernate.type.BasicType<?> type) {
				buildingPlan.applyBasicType( type );
			}

			@Override
			public void contributeType(org.hibernate.type.BasicType<?> type, String... keys) {
				buildingPlan.applyBasicType( type, keys );
			}

			@Override
			public void contributeType(org.hibernate.usertype.UserType<?> type, String... keys) {
				buildingPlan.applyBasicType( type, keys );
			}

			@Override
			public void contributeType(CompositeUserType<?> type) {
				buildingPlan.contributeCompositeUserType( type );
			}

			@Override
			public void contributeAttributeConverter(Class<? extends AttributeConverter<?, ?>> converterClass) {
				mappingContributions.addAttributeConverter( ConverterDescriptors.of( converterClass ) );
			}
		};
	}

	private static MappingResolutionContributions.Builder mappingContributions(
			ResolvedMappingSettings mappingSettings,
			MappingCustomizations mappingCustomizations) {
		return MappingResolutionContributions.builder()
				.addCacheRegionDefinitions( mappingSettings.cacheRegionDefinitions() )
				.addCacheRegionDefinitions( mappingCustomizations.cacheRegionDefinitions() )
				.addSqlFunctions( mappingCustomizations.sqlFunctions() )
				.addAuxiliaryDatabaseObjects( mappingCustomizations.auxiliaryDatabaseObjects() )
				.addAttributeConverters( mappingCustomizations.attributeConverters() );
	}

	private static void applyTypeContributions(MetadataBuildingContext metadataBuildingContext) {
		final var metadataCollector = metadataBuildingContext.getMetadataCollector();
		final var typeConfiguration = metadataBuildingContext.getTypeConfiguration();
		final var serviceRegistry = metadataBuildingContext.getServiceRegistry();
		final var typeContributions = new TypeContributions() {
			@Override
			public org.hibernate.type.spi.TypeConfiguration getTypeConfiguration() {
				return typeConfiguration;
			}

			@Override
			public void contributeAttributeConverter(Class<? extends AttributeConverter<?, ?>> converterClass) {
				metadataCollector.getConverterRegistry().addAttributeConverter( converterClass );
			}

			@Override
			public void contributeType(CompositeUserType<?> type) {
				metadataBuildingContext.getBuildingPlan().getCompositeUserTypes().add( type );
			}
		};

		metadataBuildingContext.getJdbcServices().getDialect()
				.contribute( typeContributions, serviceRegistry );
	}

	private static class AdditionalMappingContributionsImpl implements AdditionalMappingContributions {
		private final ResolvedMappingSettings mappingSettings;
		private final MetadataBuildingContext metadataBuildingContext;
		private final MappingBinder mappingBinder;

		private List<ClassDetails> additionalManagedClassDetails;
		private List<Binding<JaxbEntityMappingsImpl>> additionalXmlMappings;
		private String currentContributor;

		private AdditionalMappingContributionsImpl(
				ResolvedMappingSettings mappingSettings,
				MetadataBuildingContext metadataBuildingContext,
				ClassLoaderService classLoaderService) {
			this.mappingSettings = mappingSettings;
			this.metadataBuildingContext = metadataBuildingContext;
			this.mappingBinder = mappingSettings.xmlMappingEnabled() ? new MappingBinder( classLoaderService, () -> false ) : null;
		}

		private void setCurrentContributor(String contributor) {
			currentContributor = contributor == null ? "orm" : contributor;
		}

		@Override
		public void contributeEntity(Class<?> entityType) {
			contributeManagedClass(
					metadataBuildingContext.getModelsContext()
							.getClassDetailsRegistry()
							.resolveClassDetails( entityType.getName() )
			);
		}

		@Override
		public void contributeManagedClass(ClassDetails classDetails) {
			if ( additionalManagedClassDetails == null ) {
				additionalManagedClassDetails = new ArrayList<>();
			}
			additionalManagedClassDetails.add( classDetails );
			metadataBuildingContext.getModelsContext()
					.getClassDetailsRegistry()
					.as( MutableClassDetailsRegistry.class )
					.addClassDetails( classDetails.getName(), classDetails );
		}

		@Override
		public void contributeBinding(InputStream xmlStream) {
			if ( mappingBinder != null ) {
				contributeBinding( mappingBinder.bind( xmlStream, new Origin( SourceType.INPUT_STREAM, currentContributor ) ) );
			}
		}

		@Override
		public void contributeBinding(JaxbEntityMappingsImpl mappingJaxbBinding) {
			if ( mappingSettings.xmlMappingEnabled() ) {
				contributeBinding( new Binding<>( mappingJaxbBinding, new Origin( SourceType.OTHER, currentContributor ) ) );
			}
		}

		private void contributeBinding(Binding<JaxbEntityMappingsImpl> binding) {
			if ( additionalXmlMappings == null ) {
				additionalXmlMappings = new ArrayList<>();
			}
			additionalXmlMappings.add( binding );
		}

		@Override
		public void contributeTable(Table table) {
			final InFlightMetadataCollector metadataCollector = metadataBuildingContext.getMetadataCollector();
			metadataCollector.getDatabase()
					.locateNamespace( table.getCatalogIdentifier(), table.getSchemaIdentifier() )
					.registerTable( table.getNameIdentifier(), table );
			metadataCollector.addTableNameBinding( table.getNameIdentifier(), table );
		}

		@Override
		public void contributeSequence(Sequence sequence) {
			final var sequenceName = sequence.getName();
			metadataBuildingContext.getMetadataCollector()
					.getDatabase()
					.locateNamespace( sequenceName.getCatalogName(), sequenceName.getSchemaName() )
					.registerSequence( sequenceName.getSequenceName(), sequence );
		}

		@Override
		public void contributeAuxiliaryDatabaseObject(AuxiliaryDatabaseObject auxiliaryDatabaseObject) {
			metadataBuildingContext.getMetadataCollector().addAuxiliaryDatabaseObject( auxiliaryDatabaseObject );
		}

		@Override
		public EffectiveMappingDefaults getEffectiveMappingDefaults() {
			return metadataBuildingContext.getEffectiveDefaults();
		}

		private PreparedMappingSources complete() {
			if ( additionalManagedClassDetails == null && additionalXmlMappings == null ) {
				return null;
			}
			return new PreparedMappingSources(
					additionalManagedClassDetails == null ? List.of() : additionalManagedClassDetails,
					List.of(),
					additionalXmlMappings == null ? List.of() : additionalXmlMappings,
					true
			);
		}
	}
}
