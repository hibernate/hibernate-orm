/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.models.xml.globals;

import java.util.List;

import org.hibernate.boot.mapping.internal.categorize.DomainModelCategorizer;
import org.hibernate.boot.mapping.internal.categorize.JpaEventListener;
import org.hibernate.boot.pipeline.internal.source.PreparedMappingSources;
import org.hibernate.orm.test.boot.models.XmlHelper;
import org.hibernate.testing.boot.MetadataBuildingContextTestingImpl;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.orm.test.boot.models.SourceModelTestHelper.SIMPLE_CLASS_LOADING;
import static org.hibernate.models.spi.ClassDetails.VOID_CLASS_DETAILS;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
public class JpaEventListenerTests {
	@Test
	@ServiceRegistry
	void testGlobalRegistration(ServiceRegistryScope registryScope) {
		final var metadataBuildingContext = new MetadataBuildingContextTestingImpl( registryScope.getRegistry() );
		final var categorizedDomainModel = DomainModelCategorizer.categorize(
				new PreparedMappingSources(
						List.of(),
						List.of(),
						List.of( XmlHelper.bindMapping( "mappings/models/globals.xml", SIMPLE_CLASS_LOADING ) )
				),
				metadataBuildingContext
		);
		final List<JpaEventListener> registrations =
				categorizedDomainModel.getGlobalRegistrations().getEntityListenerRegistrations();
		assertThat( registrations ).hasSize( 1 );
		final JpaEventListener registration = registrations.get( 0 );
		final var postPersistMethod = registration.getPostPersistMethod();
		assertThat( postPersistMethod ).isNotNull();
		assertThat( postPersistMethod.getReturnType() ).isEqualTo( VOID_CLASS_DETAILS );
		assertThat( postPersistMethod.getArgumentTypes() ).hasSize( 1 );
	}
}
