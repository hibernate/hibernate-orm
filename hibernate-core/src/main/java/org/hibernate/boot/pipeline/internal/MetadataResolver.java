/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.pipeline.internal;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.jaxb.Origin;
import org.hibernate.boot.jaxb.SourceType;
import org.hibernate.boot.jaxb.internal.MappingBinder;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityMappingsImpl;
import org.hibernate.boot.jaxb.spi.Binding;
import org.hibernate.boot.model.TypeContributions;
import org.hibernate.boot.model.relational.AuxiliaryDatabaseObject;
import org.hibernate.boot.model.relational.Sequence;
import org.hibernate.boot.internal.InFlightMetadataCollectorImpl;
import org.hibernate.boot.internal.MetadataBuilderImpl;
import org.hibernate.boot.internal.MetadataBuildingContextRootImpl;
import org.hibernate.boot.internal.RootMappingDefaults;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.selector.spi.StrategySelector;
import org.hibernate.boot.mapping.internal.context.BindingContextImpl;
import org.hibernate.boot.mapping.internal.context.BindingOptionsImpl;
import org.hibernate.boot.mapping.internal.context.BindingStateImpl;
import org.hibernate.boot.mapping.internal.context.InFlightMetadataCollectorAdapter;
import org.hibernate.boot.mapping.internal.binders.BindingCoordinator;
import org.hibernate.boot.mapping.internal.categorize.CategorizedDomainModel;
import org.hibernate.boot.mapping.internal.categorize.DomainModelCategorizer;
import org.hibernate.boot.pipeline.internal.source.AvailableResources;
import org.hibernate.boot.pipeline.internal.source.AvailableResourcesContext;
import org.hibernate.boot.pipeline.internal.source.MappingSourceContributions;
import org.hibernate.boot.pipeline.internal.settings.ResolvedBootstrapSettings;
import org.hibernate.boot.pipeline.internal.settings.ResolvedMappingSettings;
import org.hibernate.boot.spi.AdditionalMappingContributions;
import org.hibernate.boot.spi.AdditionalMappingContributor;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.boot.spi.EffectiveMappingDefaults;
import org.hibernate.boot.spi.InFlightMetadataCollector;
import org.hibernate.boot.spi.MetadataBuilderContributor;
import org.hibernate.boot.spi.MetadataBuildingOptions;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.mapping.Table;
import org.hibernate.models.internal.MutableClassDetailsRegistry;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.usertype.CompositeUserType;
import org.hibernate.models.spi.ClassDetails;

import jakarta.persistence.AttributeConverter;

import static org.hibernate.jpa.boot.spi.JpaSettings.METADATA_BUILDER_CONTRIBUTOR;

/// Resolves ORM boot metadata from source contributions and resolved settings.
///
/// This class owns the phase order for the metadata collector.  It turns resolved
/// bootstrap settings and source contributions into ORM's boot-time
/// [MetadataImplementor] while preserving intermediate boot-model products in
/// [ResolvedMetadata].
///
/// Higher-level entry points should resolve settings and source contributions
/// before calling this resolver.
///
/// @since 9.0
/// @author Steve Ebersole
public class MetadataResolver {
	/// Resolve ORM boot metadata and retain intermediate boot-model products.
	///
	/// This is the preferred prototype entry point for later SessionFactory
	/// construction experiments because it exposes the categorized model,
	/// binding-state bridge, and resolved settings that were used to produce the
	/// metadata.
	///
	/// @param bootstrapSettings Resolved bootstrap settings
	/// @param mappingSettings Resolved mapping/model-build settings
	/// @param sourceContributions Mapping-source contributions supplied by the
	/// entry point
	/// @param serviceRegistry Service registry for the metadata build
	///
	/// @return The resolved metadata product
	public static ResolvedMetadata resolve(
			ResolvedBootstrapSettings bootstrapSettings,
			ResolvedMappingSettings mappingSettings,
			MappingSourceContributions sourceContributions,
			ServiceRegistry serviceRegistry) {
		return resolve(
				bootstrapSettings,
				mappingSettings,
				sourceContributions,
				MetadataCustomizations.NONE,
				serviceRegistry
		);
	}

	/// Resolve ORM boot metadata and retain intermediate boot-model products.
	public static ResolvedMetadata resolve(
			ResolvedBootstrapSettings bootstrapSettings,
			ResolvedMappingSettings mappingSettings,
			MappingSourceContributions sourceContributions,
			MetadataCustomizations metadataCustomizations,
			ServiceRegistry serviceRegistry) {
		metadataCustomizations = metadataCustomizations == null ? MetadataCustomizations.NONE : metadataCustomizations;
		final MetadataBuildingContext metadataBuildingContext = createMetadataBuildingContext(
				serviceRegistry,
				bootstrapSettings,
				mappingSettings,
				metadataCustomizations
		);
		metadataBuildingContext.getBootstrapContext().getTypeConfiguration().scope( metadataBuildingContext );
		applyTypeContributions( metadataBuildingContext );
		final AvailableResources availableResources = buildAvailableResources(
				sourceContributions,
				mappingSettings,
				metadataBuildingContext
		);
		return resolve(
				metadataBuildingContext,
				mappingSettings,
				availableResources,
				metadataCustomizations
		);
	}

	/// Resolve ORM boot metadata from Hibernate's native source accumulator.
	///
	/// This overload is used by native [org.hibernate.boot.MetadataSources]
	/// entry points because native sources can already contain bound XML mapping
	/// documents, not just source names.
	public static ResolvedMetadata resolve(
			ResolvedBootstrapSettings bootstrapSettings,
			ResolvedMappingSettings mappingSettings,
			MetadataSources metadataSources,
			ServiceRegistry serviceRegistry) {
		final var metadataCustomizations = new MetadataCustomizations(
				metadataSources.getExtraQueryImports(),
				null,
				null,
				null
		);
		final MetadataBuildingContext metadataBuildingContext = createMetadataBuildingContext(
				serviceRegistry,
				bootstrapSettings,
				mappingSettings,
				metadataCustomizations
		);
		metadataBuildingContext.getBootstrapContext().getTypeConfiguration().scope( metadataBuildingContext );
		applyTypeContributions( metadataBuildingContext );
		final AvailableResources availableResources = AvailableResources.from(
				metadataSources,
				new AvailableResourcesContext(
						metadataBuildingContext.getBootstrapContext().getModelsContext(),
						metadataBuildingContext.getBootstrapContext().getServiceRegistry()
				)
		);
		return resolve(
				metadataBuildingContext,
				mappingSettings,
				availableResources,
				metadataCustomizations
		);
	}

	/// Resolve ORM boot metadata from Hibernate's native source accumulator using
	/// an already-customized metadata building context.
	public static ResolvedMetadata resolve(
			ResolvedMappingSettings mappingSettings,
			MetadataSources metadataSources,
			BootstrapContext bootstrapContext,
			MetadataBuildingOptions buildingOptions) {
		final var metadataCustomizations = new MetadataCustomizations(
				metadataSources.getExtraQueryImports(),
				null,
				null,
				null
		);
		final var metadataCollector = new InFlightMetadataCollectorImpl( bootstrapContext, buildingOptions );
		final var metadataBuildingContext = new MetadataBuildingContextRootImpl(
				"orm",
				bootstrapContext,
				buildingOptions,
				metadataCollector,
				new RootMappingDefaults(
						buildingOptions.getMappingDefaults(),
						metadataCollector.getPersistenceUnitMetadata()
				)
		);
		bootstrapContext.getTypeConfiguration().scope( metadataBuildingContext );
		applyTypeContributions( metadataBuildingContext );
		final AvailableResources availableResources = AvailableResources.from(
				metadataSources,
				new AvailableResourcesContext(
						bootstrapContext.getModelsContext(),
						bootstrapContext.getServiceRegistry()
				)
		);
		return resolve(
				metadataBuildingContext,
				mappingSettings,
				availableResources,
				metadataCustomizations
		);
	}

	private static ResolvedMetadata resolve(
			MetadataBuildingContext metadataBuildingContext,
			ResolvedMappingSettings mappingSettings,
			AvailableResources availableResources,
			MetadataCustomizations metadataCustomizations) {
		applyBootstrapContextConverters( metadataBuildingContext );
		final CategorizedDomainModel categorizedDomainModel = categorize(
				availableResources,
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
		applyQueryImports( metadataCustomizations, metadataBuildingContext );
		final MetadataImplementor metadata = finalizeMetadata( metadataBuildingContext );
		return new ResolvedMetadata(
				metadata,
				categorizedDomainModel,
				bindingState
		);
	}

	private static void applyQueryImports(
			MetadataCustomizations metadataCustomizations,
			MetadataBuildingContext metadataBuildingContext) {
		metadataCustomizations.queryImports().forEach( (importName, target) ->
				metadataBuildingContext.getMetadataCollector().addImport( importName, target.getName() )
		);
	}

	private static AvailableResources buildAvailableResources(
			MappingSourceContributions sourceContributions,
			ResolvedMappingSettings mappingSettings,
			MetadataBuildingContext metadataBuildingContext) {
		return AvailableResources.from(
				sourceContributions,
				new AvailableResourcesContext(
						metadataBuildingContext.getBootstrapContext().getModelsContext(),
						metadataBuildingContext.getBootstrapContext().getServiceRegistry()
				),
				mappingSettings
		);
	}

	private static void bindAdditionalMappingContributions(
			ResolvedMappingSettings mappingSettings,
			MetadataBuildingContext metadataBuildingContext,
			BindingStateImpl bindingState) {
		final AvailableResources contributedResources = collectAdditionalMappingContributions(
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
				contributor,
				metadataBuildingContext.getBootstrapContext(),
				metadataBuildingContext.getBuildingOptions(),
				metadataBuildingContext.getMetadataCollector(),
				new RootMappingDefaults(
						metadataBuildingContext.getBuildingOptions().getMappingDefaults(),
						metadataBuildingContext.getMetadataCollector().getPersistenceUnitMetadata()
				)
		);
	}

	private static String contributorName(AvailableResources contributedResources) {
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

	private static AvailableResources collectAdditionalMappingContributions(
			ResolvedMappingSettings mappingSettings,
			MetadataBuildingContext metadataBuildingContext) {
		final ClassLoaderService classLoaderService = metadataBuildingContext.getBootstrapContext()
				.getServiceRegistry()
				.requireService( ClassLoaderService.class );
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
			AvailableResources availableResources,
			MetadataBuildingContext metadataBuildingContext) {
		return DomainModelCategorizer.categorize(
				availableResources,
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
						metadataBuildingContext.getBootstrapContext()
				)
		);
	}

	private static void applyBootstrapContextConverters(MetadataBuildingContext metadataBuildingContext) {
		metadataBuildingContext.getBootstrapContext()
				.getAttributeConverters()
				.forEach( metadataBuildingContext.getMetadataCollector()::addAttributeConverter );
	}

	private static MetadataImplementor finalizeMetadata(MetadataBuildingContext metadataBuildingContext) {
//		final MetadataImplementor metadata = metadataBuildingContext.getMetadataCollector();
//		metadata.orderColumns( false );
//		metadata.validate();
//		return metadata;
		return ( (InFlightMetadataCollectorImpl) metadataBuildingContext.getMetadataCollector() ).buildMetadataInstance( metadataBuildingContext );
	}

	private static MetadataBuildingContext createMetadataBuildingContext(
			ServiceRegistry serviceRegistry,
			ResolvedBootstrapSettings bootstrapSettings,
			ResolvedMappingSettings mappingSettings,
			MetadataCustomizations metadataCustomizations) {
		final var standardServiceRegistry = MetadataBuilderImpl.getStandardServiceRegistry( serviceRegistry );
		final var metadataBuilder = new MetadataBuilderImpl( new MetadataSources( standardServiceRegistry ), standardServiceRegistry );
		metadataBuilder.applyDefaultToOneFetchType( mappingSettings.defaultToOneFetchType() );
		mappingSettings
				.cacheRegionDefinitions()
				.forEach( metadataBuilder::applyCacheRegionDefinition );
		metadataCustomizations
				.cacheRegionDefinitions()
				.forEach( metadataBuilder::applyCacheRegionDefinition );
		metadataCustomizations
				.typeContributors()
				.forEach( metadataBuilder::applyTypes );
		metadataCustomizations
				.functionContributors()
				.forEach( metadataBuilder::applyFunctions );
		applyMetadataBuilderContributor( bootstrapSettings, serviceRegistry, metadataBuilder );

		final var bootstrapContext = metadataBuilder.getBootstrapContext();
		if ( bootstrapSettings.jpaBootstrap() ) {
			bootstrapContext.markAsJpaBootstrap();
		}

		final var buildingOptions = metadataBuilder.getMetadataBuildingOptions();
		final var metadataCollector = new InFlightMetadataCollectorImpl( bootstrapContext, buildingOptions );
		return new MetadataBuildingContextRootImpl(
				"orm",
				bootstrapContext,
				buildingOptions,
				metadataCollector,
				new RootMappingDefaults(
						buildingOptions.getMappingDefaults(),
						metadataCollector.getPersistenceUnitMetadata()
				)
		);
	}

	private static void applyMetadataBuilderContributor(
			ResolvedBootstrapSettings bootstrapSettings,
			ServiceRegistry serviceRegistry,
			MetadataBuilderImpl metadataBuilder) {
		final var setting = bootstrapSettings.configurationValues().get( METADATA_BUILDER_CONTRIBUTOR );
		if ( setting == null ) {
			return;
		}
		if ( !( setting instanceof MetadataBuilderContributor )
				&& !( setting instanceof Class<?> )
				&& !( setting instanceof String ) ) {
			throw new IllegalArgumentException(
					"The provided hibernate.metadata_builder_contributor setting value [%s] is not supported"
							.formatted( setting )
			);
		}
		serviceRegistry.requireService( StrategySelector.class )
				.resolveStrategy( MetadataBuilderContributor.class, setting )
				.contribute( metadataBuilder );
	}

	private static void applyTypeContributions(MetadataBuildingContext metadataBuildingContext) {
		final var bootstrapContext = metadataBuildingContext.getBootstrapContext();
		final var metadataCollector = metadataBuildingContext.getMetadataCollector();
		final var typeConfiguration = bootstrapContext.getTypeConfiguration();
		final var serviceRegistry = bootstrapContext.getServiceRegistry();
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
				metadataBuildingContext.getBuildingOptions().getCompositeUserTypes().add( type );
			}
		};

		serviceRegistry.requireService( JdbcServices.class )
				.getDialect()
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
					metadataBuildingContext.getBootstrapContext()
							.getModelsContext()
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
			metadataBuildingContext.getBootstrapContext()
					.getModelsContext()
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

		private AvailableResources complete() {
			if ( additionalManagedClassDetails == null && additionalXmlMappings == null ) {
				return null;
			}
			return new AvailableResources(
					additionalManagedClassDetails == null ? List.of() : additionalManagedClassDetails,
					List.of(),
					additionalXmlMappings == null ? List.of() : additionalXmlMappings,
					true
			);
		}
	}
}
