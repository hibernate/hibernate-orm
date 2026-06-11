/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.orchestration;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.model.TypeContributions;
import org.hibernate.boot.internal.InFlightMetadataCollectorImpl;
import org.hibernate.boot.internal.MetadataBuilderImpl;
import org.hibernate.boot.internal.MetadataBuildingContextRootImpl;
import org.hibernate.boot.internal.RootMappingDefaults;
import org.hibernate.boot.models.bind.internal.BindingContextImpl;
import org.hibernate.boot.models.bind.internal.BindingOptionsImpl;
import org.hibernate.boot.models.bind.internal.BindingStateImpl;
import org.hibernate.boot.models.bind.internal.InFlightMetadataCollectorAdapter;
import org.hibernate.boot.models.bind.spi.BindingCoordinator;
import org.hibernate.boot.models.categorize.spi.CategorizedDomainModel;
import org.hibernate.boot.models.categorize.spi.DomainModelCategorizer;
import org.hibernate.boot.models.source.AvailableResources;
import org.hibernate.boot.models.source.AvailableResourcesContext;
import org.hibernate.boot.models.source.BootstrapSourceContributions;
import org.hibernate.boot.settings.ResolvedBootstrapSettings;
import org.hibernate.boot.settings.ResolvedMappingSettings;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.boot.spi.MetadataBuildingOptions;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.usertype.CompositeUserType;

import jakarta.persistence.AttributeConverter;

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
			BootstrapSourceContributions sourceContributions,
			ServiceRegistry serviceRegistry) {
		final MetadataBuildingContext metadataBuildingContext = createMetadataBuildingContext(
				serviceRegistry,
				bootstrapSettings,
				mappingSettings
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
				availableResources
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
		final MetadataBuildingContext metadataBuildingContext = createMetadataBuildingContext(
				serviceRegistry,
				bootstrapSettings,
				mappingSettings
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
				availableResources
		);
	}

	/// Resolve ORM boot metadata from Hibernate's native source accumulator using
	/// an already-customized metadata building context.
	public static ResolvedMetadata resolve(
			ResolvedMappingSettings mappingSettings,
			MetadataSources metadataSources,
			BootstrapContext bootstrapContext,
			MetadataBuildingOptions buildingOptions) {
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
				availableResources
		);
	}

	private static ResolvedMetadata resolve(
			MetadataBuildingContext metadataBuildingContext,
			ResolvedMappingSettings mappingSettings,
			AvailableResources availableResources) {
		final CategorizedDomainModel categorizedDomainModel = categorize(
				availableResources,
				metadataBuildingContext
		);
		final BindingStateImpl bindingState = bind(
				categorizedDomainModel,
				mappingSettings,
				metadataBuildingContext
		);
		final MetadataImplementor metadata = finalizeMetadata( metadataBuildingContext );
		return new ResolvedMetadata(
				metadata,
				categorizedDomainModel,
				bindingState
		);
	}

	private static AvailableResources buildAvailableResources(
			BootstrapSourceContributions sourceContributions,
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
		BindingCoordinator.coordinateBinding(
				categorizedDomainModel,
				bindingState,
				new BindingOptionsImpl( metadataBuildingContext, mappingSettings ),
				new BindingContextImpl(
						categorizedDomainModel,
						metadataBuildingContext.getBootstrapContext()
				)
		);
		return bindingState;
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
			ResolvedMappingSettings mappingSettings) {
		final var standardServiceRegistry = MetadataBuilderImpl.getStandardServiceRegistry( serviceRegistry );
		final var metadataBuilder = new MetadataBuilderImpl( new MetadataSources( standardServiceRegistry ), standardServiceRegistry );
		metadataBuilder.applyDefaultToOneFetchType( mappingSettings.defaultToOneFetchType() );
		mappingSettings
				.cacheRegionDefinitions()
				.forEach( metadataBuilder::applyCacheRegionDefinition );

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
}
