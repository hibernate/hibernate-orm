/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.orm.test.boot.models.bind;

import java.util.Set;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.internal.BootstrapContextImpl;
import org.hibernate.boot.internal.InFlightMetadataCollectorImpl;
import org.hibernate.boot.internal.MetadataBuilderImpl;
import org.hibernate.boot.internal.MetadataBuildingContextRootImpl;
import org.hibernate.boot.model.process.spi.ManagedResources;
import org.hibernate.boot.model.process.spi.MetadataBuildingProcess;
import org.hibernate.boot.models.bind.internal.BindingContextImpl;
import org.hibernate.boot.models.bind.internal.BindingOptionsImpl;
import org.hibernate.boot.models.bind.internal.BindingStateImpl;
import org.hibernate.boot.models.bind.spi.BindingCoordinator;
import org.hibernate.boot.models.categorize.spi.CategorizedDomainModel;
import org.hibernate.boot.models.categorize.spi.EntityHierarchy;
import org.hibernate.boot.models.categorize.spi.ManagedResourcesProcessor;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.orm.test.boot.models.process.ManagedResourcesImpl;

/**
 * @author Steve Ebersole
 */
public class BindingTestingHelper {
	public static void checkDomainModel(
			DomainModelCheck check,
			StandardServiceRegistry serviceRegistry,
			Class<?>... domainClasses) {
		final BootstrapContextImpl bootstrapContext = buildBootstrapContext(
				serviceRegistry );
		final ManagedResources managedResources = buildManagedResources(
				domainClasses,
				bootstrapContext
		);

		final InFlightMetadataCollectorImpl metadataCollector = new InFlightMetadataCollectorImpl(
				bootstrapContext,
				bootstrapContext.getMetadataBuildingOptions()
		);

		final CategorizedDomainModel categorizedDomainModel = ManagedResourcesProcessor.processManagedResources(
				managedResources,
				bootstrapContext
		);

		final MetadataBuildingContextRootImpl metadataBuildingContext = new MetadataBuildingContextRootImpl(
				"models",
				bootstrapContext,
				bootstrapContext.getMetadataBuildingOptions(),
				metadataCollector
		);
		final BindingStateImpl bindingState = new BindingStateImpl( metadataBuildingContext );
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

		check.checkDomainModel( new DomainModelCheckContext() {
			@Override
			public InFlightMetadataCollectorImpl getMetadataCollector() {
				return metadataCollector;
			}

			@Override
			public BindingStateImpl getBindingState() {
				return bindingState;
			}
		} );
	}

	public interface DomainModelCheckContext {
		InFlightMetadataCollectorImpl getMetadataCollector();
		BindingStateImpl getBindingState();
	}

	@FunctionalInterface
	public interface DomainModelCheck {
		void checkDomainModel(DomainModelCheckContext context);
	}

	private static BootstrapContextImpl buildBootstrapContext(StandardServiceRegistry serviceRegistry) {
		final MetadataBuilderImpl.MetadataBuildingOptionsImpl metadataBuildingOptions = new MetadataBuilderImpl.MetadataBuildingOptionsImpl( serviceRegistry );
		final BootstrapContextImpl bootstrapContext = new BootstrapContextImpl( serviceRegistry, metadataBuildingOptions );
		metadataBuildingOptions.setBootstrapContext( bootstrapContext );
		return bootstrapContext;
	}

	private static ManagedResources buildManagedResources(
			Class<?>[] domainClasses,
			BootstrapContextImpl bootstrapContext) {
		final MetadataSources metadataSources = new MetadataSources( bootstrapContext.getServiceRegistry() );
		for ( int i = 0; i < domainClasses.length; i++ ) {
			metadataSources.addAnnotatedClass( domainClasses[i] );
		}
		return MetadataBuildingProcess.prepare( metadataSources, bootstrapContext );
	}

	public static Set<EntityHierarchy> buildHierarchyMetadata(Class<?>... classes) {
		final ManagedResources managedResources = new ManagedResourcesImpl.Builder()
				.addLoadedClasses(classes)
				.build();

		try (StandardServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder().build()) {
			final MetadataBuilderImpl.MetadataBuildingOptionsImpl metadataBuildingOptions = new MetadataBuilderImpl.MetadataBuildingOptionsImpl( serviceRegistry );
			final BootstrapContextImpl bootstrapContext = new BootstrapContextImpl( serviceRegistry, metadataBuildingOptions );

			final CategorizedDomainModel categorizedDomainModel = ManagedResourcesProcessor.processManagedResources(
					managedResources,
					bootstrapContext
			);

			return categorizedDomainModel.getEntityHierarchies();
		}
	}

	public static CategorizedDomainModel buildCategorizedDomainModel(Class<?>... classes) {
		final ManagedResources managedResources = new ManagedResourcesImpl.Builder()
				.addLoadedClasses(classes)
				.build();

		try (StandardServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder().build()) {
			final MetadataBuilderImpl.MetadataBuildingOptionsImpl metadataBuildingOptions = new MetadataBuilderImpl.MetadataBuildingOptionsImpl( serviceRegistry );
			final BootstrapContextImpl bootstrapContext = new BootstrapContextImpl( serviceRegistry, metadataBuildingOptions );

			return ManagedResourcesProcessor.processManagedResources( managedResources, bootstrapContext );
		}
	}
}
