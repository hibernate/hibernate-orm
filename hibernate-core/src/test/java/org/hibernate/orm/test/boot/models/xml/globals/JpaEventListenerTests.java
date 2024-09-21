/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.models.xml.globals;

import java.util.List;

import org.hibernate.boot.internal.BootstrapContextImpl;
import org.hibernate.boot.internal.MetadataBuilderImpl;
import org.hibernate.boot.model.process.spi.ManagedResources;
import org.hibernate.boot.model.source.internal.annotations.AdditionalManagedResourcesImpl;
import org.hibernate.boot.models.categorize.spi.CategorizedDomainModel;
import org.hibernate.boot.models.spi.JpaEventListener;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.models.spi.MethodDetails;

import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.boot.models.categorize.spi.ManagedResourcesProcessor.processManagedResources;
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

		final StandardServiceRegistry serviceRegistry = registryScope.getRegistry();
		final BootstrapContextImpl bootstrapContext = new BootstrapContextImpl(
				serviceRegistry,
				new MetadataBuilderImpl.MetadataBuildingOptionsImpl( serviceRegistry )
		);
		final CategorizedDomainModel categorizedDomainModel = processManagedResources(
				managedResources,
				bootstrapContext
		);
		final List<JpaEventListener> registrations = categorizedDomainModel
				.getGlobalRegistrations()
				.getEntityListenerRegistrations();
		assertThat( registrations ).hasSize( 1 );
		final JpaEventListener registration = registrations.get( 0 );
		final MethodDetails postPersistMethod = registration.getPostPersistMethod();
		assertThat( postPersistMethod ).isNotNull();
		assertThat( postPersistMethod.getReturnType() ).isEqualTo( VOID_CLASS_DETAILS );
		assertThat( postPersistMethod.getArgumentTypes() ).hasSize( 1 );

	}

}
