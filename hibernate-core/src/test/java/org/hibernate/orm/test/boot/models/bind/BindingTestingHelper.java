/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.models.bind;

import java.util.Set;

import org.hibernate.boot.internal.BootstrapContextImpl;
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
import org.hibernate.boot.models.categorize.spi.EntityHierarchy;
import org.hibernate.boot.models.source.AvailableResources;
import org.hibernate.boot.models.source.AvailableResourcesContext;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.jpa.HibernatePersistenceConfiguration;
import org.hibernate.testing.boot.MetadataBuildingContextTestingImpl;

/**
 * @author Steve Ebersole
 */
public class BindingTestingHelper {
	public static void checkDomainModel(
			DomainModelCheck check,
			StandardServiceRegistry serviceRegistry,
			Class<?>... domainClasses) {
		final BootstrapContextImpl bootstrapContext = buildBootstrapContext( serviceRegistry );

		final InFlightMetadataCollectorImpl metadataCollector = new InFlightMetadataCollectorImpl(
				bootstrapContext,
				bootstrapContext.getMetadataBuildingOptions()
		);

		final MetadataBuildingContextRootImpl metadataBuildingContext = new MetadataBuildingContextRootImpl(
				"models",
				bootstrapContext,
				bootstrapContext.getMetadataBuildingOptions(),
				metadataCollector,
				new RootMappingDefaults(
						bootstrapContext.getMetadataBuildingOptions().getMappingDefaults(),
						metadataCollector.getPersistenceUnitMetadata()
				)
		);
		final AvailableResources availableResources = buildAvailableResources( metadataBuildingContext, domainClasses );
		final CategorizedDomainModel categorizedDomainModel = DomainModelCategorizer.categorize(
				availableResources,
				metadataBuildingContext
		);
		final BindingStateImpl bindingState = new BindingStateImpl(
				metadataBuildingContext,
				new InFlightMetadataCollectorAdapter( metadataCollector )
		);
		final BindingOptionsImpl bindingOptions = new BindingOptionsImpl( metadataBuildingContext );
		final BindingContextImpl bindingContext = new BindingContextImpl(
				categorizedDomainModel,
				bootstrapContext
		);

		BindingCoordinator.coordinateBinding(
				categorizedDomainModel,
				bindingState,
				bindingOptions,
				bindingContext
		);
		final MetadataImplementor metadata = metadataCollector.buildMetadataInstance( metadataBuildingContext );
		metadata.orderColumns( false );
		metadata.validate();

		check.checkDomainModel( new DomainModelCheckContext() {
			@Override
			public InFlightMetadataCollectorImpl getMetadataCollector() {
				return metadataCollector;
			}

			@Override
			public MetadataImplementor getMetadata() {
				return metadata;
			}

			@Override
			public BindingStateImpl getBindingState() {
				return bindingState;
			}
		} );
	}

	public static Set<EntityHierarchy> buildHierarchyMetadata(Class<?>... classes) {
		try (StandardServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder().build()) {
			final MetadataBuildingContext metadataBuildingContext = new MetadataBuildingContextTestingImpl( serviceRegistry );
			final AvailableResources availableResources = buildAvailableResources( metadataBuildingContext, classes );

			final CategorizedDomainModel categorizedDomainModel = DomainModelCategorizer.categorize(
					availableResources,
					metadataBuildingContext
			);

			return categorizedDomainModel.getEntityHierarchies();
		}
	}

	public static CategorizedDomainModel buildCategorizedDomainModel(Class<?>... classes) {
		try (StandardServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder().build()) {
			final MetadataBuildingContext metadataBuildingContext = new MetadataBuildingContextTestingImpl( serviceRegistry );
			final AvailableResources availableResources = buildAvailableResources( metadataBuildingContext, classes );

			return DomainModelCategorizer.categorize( availableResources, metadataBuildingContext );
		}
	}

	private static BootstrapContextImpl buildBootstrapContext(StandardServiceRegistry serviceRegistry) {
		final MetadataBuilderImpl.MetadataBuildingOptionsImpl metadataBuildingOptions =
				new MetadataBuilderImpl.MetadataBuildingOptionsImpl( serviceRegistry );
		final BootstrapContextImpl bootstrapContext = new BootstrapContextImpl( serviceRegistry, metadataBuildingOptions );
		metadataBuildingOptions.setBootstrapContext( bootstrapContext );
		return bootstrapContext;
	}

	private static AvailableResources buildAvailableResources(MetadataBuildingContext metadataBuildingContext, Class<?>... classes) {
		final HibernatePersistenceConfiguration persistenceConfiguration = new HibernatePersistenceConfiguration( "test" );
		for ( Class<?> clazz : classes ) {
			persistenceConfiguration.managedClass( clazz );
		}
		return AvailableResources.from(
				persistenceConfiguration,
				new AvailableResourcesContext(
						metadataBuildingContext.getBootstrapContext().getModelsContext(),
						metadataBuildingContext.getBootstrapContext().getServiceRegistry()
				)
		);
	}

	public interface DomainModelCheckContext {
		InFlightMetadataCollectorImpl getMetadataCollector();

		MetadataImplementor getMetadata();

		BindingStateImpl getBindingState();
	}

	@FunctionalInterface
	public interface DomainModelCheck {
		void checkDomainModel(DomainModelCheckContext context);
	}
}
