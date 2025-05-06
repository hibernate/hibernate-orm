/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.models.xml.globals;

import org.hibernate.boot.internal.BootstrapContextImpl;
import org.hibernate.boot.internal.InFlightMetadataCollectorImpl;
import org.hibernate.boot.internal.MetadataBuilderImpl;
import org.hibernate.boot.internal.MetadataBuildingContextRootImpl;
import org.hibernate.boot.model.process.spi.ManagedResources;
import org.hibernate.boot.model.process.spi.MetadataBuildingProcess;
import org.hibernate.boot.model.source.internal.annotations.AdditionalManagedResourcesImpl;
import org.hibernate.boot.model.source.internal.annotations.DomainModelSource;
import org.hibernate.boot.models.spi.JpaEventListener;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.spi.InFlightMetadataCollector;
import org.hibernate.models.spi.MethodDetails;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.boot.model.process.spi.MetadataBuildingProcess.coordinateProcessors;
import static org.hibernate.models.spi.ClassDetails.VOID_CLASS_DETAILS;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
public class JpaEventListenerTests {
	@Test
	@ServiceRegistry
	void testGlobalRegistration(ServiceRegistryScope registryScope) {
		final ManagedResources managedResources = new AdditionalManagedResourcesImpl.Builder()
				.addXmlMappings( "mappings/models/globals.xml" )
				.build();

		final InFlightMetadataCollector metadataCollector = buildMetadataCollector( managedResources, registryScope );
		final List<JpaEventListener> registrations = metadataCollector.getGlobalRegistrations().getEntityListenerRegistrations();
		assertThat( registrations ).hasSize( 1 );
		final JpaEventListener registration = registrations.get( 0 );
		final MethodDetails postPersistMethod = registration.getPostPersistMethod();
		assertThat( postPersistMethod ).isNotNull();
		assertThat( postPersistMethod.getReturnType() ).isEqualTo( VOID_CLASS_DETAILS );
		assertThat( postPersistMethod.getArgumentTypes() ).hasSize( 1 );
	}

	private InFlightMetadataCollector buildMetadataCollector(ManagedResources managedResources, ServiceRegistryScope registryScope) {
		final StandardServiceRegistry serviceRegistry = registryScope.getRegistry();

		final ClassLoaderService classLoaderService = serviceRegistry.requireService( ClassLoaderService.class );
		assert classLoaderService != null;

		final MetadataBuilderImpl.MetadataBuildingOptionsImpl options =
				new MetadataBuilderImpl.MetadataBuildingOptionsImpl( serviceRegistry );
		final BootstrapContextImpl bootstrapContext = new BootstrapContextImpl( serviceRegistry, options );
		options.setBootstrapContext( bootstrapContext );
		final InFlightMetadataCollectorImpl metadataCollector = new InFlightMetadataCollectorImpl( bootstrapContext, options );

		final DomainModelSource domainModelSource = MetadataBuildingProcess.processManagedResources(
				managedResources,
				metadataCollector,
				bootstrapContext,
				options.getMappingDefaults()
		);


		final MetadataBuildingContextRootImpl rootMetadataBuildingContext = new MetadataBuildingContextRootImpl(
				"orm",
				bootstrapContext,
				options,
				metadataCollector,
				domainModelSource.getEffectiveMappingDefaults()
		);

		managedResources.getAttributeConverterDescriptors().forEach( metadataCollector::addAttributeConverter );

		bootstrapContext.getTypeConfiguration().scope( rootMetadataBuildingContext );

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// Set up the processors and start binding
		//		NOTE : this becomes even more simplified after we move purely
		// 		to unified model
//		final IndexView jandexView = domainModelSource.getJandexIndex();

		coordinateProcessors(
				managedResources,
				options,
				rootMetadataBuildingContext,
				domainModelSource,
				metadataCollector
		);

		return metadataCollector;
	}
}
