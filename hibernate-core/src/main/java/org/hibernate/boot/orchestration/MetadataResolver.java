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
	/// @param bootstrapSettings Resolved bootstrap and mapping settings
	/// @param sourceContributions Mapping-source contributions supplied by the
	/// entry point
	/// @param serviceRegistry Service registry for the metadata build
	///
	/// @return The resolved metadata product
	public static ResolvedMetadata resolve(
			ResolvedBootstrapSettings bootstrapSettings,
			BootstrapSourceContributions sourceContributions,
			ServiceRegistry serviceRegistry) {
		final MetadataBuildingContext metadataBuildingContext = createMetadataBuildingContext(
				serviceRegistry,
				bootstrapSettings
		);
		applyTypeContributions( metadataBuildingContext );
		final AvailableResources availableResources = buildAvailableResources(
				sourceContributions,
				bootstrapSettings,
				metadataBuildingContext
		);
		final CategorizedDomainModel categorizedDomainModel = categorize(
				availableResources,
				metadataBuildingContext
		);
		final BindingStateImpl bindingState = bind(
				categorizedDomainModel,
				bootstrapSettings,
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
			ResolvedBootstrapSettings bootstrapSettings,
			MetadataBuildingContext metadataBuildingContext) {
		return AvailableResources.from(
				sourceContributions,
				new AvailableResourcesContext(
						metadataBuildingContext.getBootstrapContext().getModelsContext(),
						metadataBuildingContext.getBootstrapContext().getServiceRegistry()
				),
				bootstrapSettings
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
			ResolvedBootstrapSettings bootstrapSettings,
			MetadataBuildingContext metadataBuildingContext) {
		final BindingStateImpl bindingState = new BindingStateImpl(
				metadataBuildingContext,
				new InFlightMetadataCollectorAdapter( metadataBuildingContext.getMetadataCollector() )
		);
		BindingCoordinator.coordinateBinding(
				categorizedDomainModel,
				bindingState,
				new BindingOptionsImpl( metadataBuildingContext, bootstrapSettings.mappingSettings() ),
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
			ResolvedBootstrapSettings bootstrapSettings) {
		final var standardServiceRegistry = MetadataBuilderImpl.getStandardServiceRegistry( serviceRegistry );
		final var metadataBuilder = new MetadataBuilderImpl( new MetadataSources( standardServiceRegistry ), standardServiceRegistry );
		metadataBuilder.applyDefaultToOneFetchType( bootstrapSettings.mappingSettings().defaultToOneFetchType() );
		bootstrapSettings.mappingSettings()
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
