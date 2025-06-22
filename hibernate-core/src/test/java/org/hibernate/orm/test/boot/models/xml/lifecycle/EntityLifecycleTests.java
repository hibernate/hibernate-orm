/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.models.xml.lifecycle;

import java.util.List;
import java.util.stream.Collectors;

import org.hibernate.boot.model.process.spi.ManagedResources;
import org.hibernate.boot.model.source.internal.annotations.AdditionalManagedResourcesImpl;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.ClassDetailsRegistry;
import org.hibernate.models.spi.MethodDetails;
import org.hibernate.models.spi.ModelsContext;
import org.hibernate.orm.test.boot.models.xml.SimpleEntity;

import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.EntityListeners;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostRemove;
import jakarta.persistence.PostUpdate;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreRemove;
import jakarta.persistence.PreUpdate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.orm.test.boot.models.SourceModelTestHelper.createBuildingContext;

/**
 * @author Marco Belladelli
 */
public class EntityLifecycleTests {
	@Test
	@ServiceRegistry
	void testEntityLifecycle(ServiceRegistryScope registryScope) {
		final ManagedResources managedResources = new AdditionalManagedResourcesImpl.Builder()
				.addXmlMappings( "mappings/models/lifecycle/entity-lifecycle.xml" )
				.build();
		final ModelsContext ModelsContext = createBuildingContext(
				managedResources,
				registryScope.getRegistry()
		);
		final ClassDetailsRegistry classDetailsRegistry = ModelsContext.getClassDetailsRegistry();
		final ClassDetails classDetails = classDetailsRegistry.getClassDetails( SimpleEntity.class.getName() );
		assertThat( classDetails.getName() ).isEqualTo( SimpleEntity.class.getName() );

		// lifecycle callback methods
		getMethodDetails( classDetails, "prePersist" ).forEach( method -> {
			final PrePersist prePersist = method.getDirectAnnotationUsage( PrePersist.class );
			if ( !method.getArgumentTypes().isEmpty() ) {
				assertThat( prePersist ).isNull();
			}
			else {
				assertThat( prePersist ).isNotNull();
			}
		} );
		assertThat( getMethodDetails( classDetails, "preRemove" ).get( 0 ).getDirectAnnotationUsage( PreRemove.class ) ).isNotNull();
		assertThat( getMethodDetails( classDetails, "preUpdate" ).get( 0 ).getDirectAnnotationUsage( PreUpdate.class ) ).isNotNull();

		// entity listeners
		final EntityListeners entityListenersAnn = classDetails.getDirectAnnotationUsage( EntityListeners.class );
		assertThat( entityListenersAnn ).isNotNull();
		final Class<?>[] entityListeners = entityListenersAnn.value();
		assertThat( entityListeners ).hasSize( 1 );
		final Class<?> listener = entityListeners[0];
		assertThat( listener.getName() ).isEqualTo( SimpleEntityListener.class.getName() );
		final ClassDetails listenerClassDetails = classDetailsRegistry.getClassDetails( listener.getName() );
		getMethodDetails( listenerClassDetails, "postPersist" ).forEach( method -> {
			final PostPersist prePersist = method.getDirectAnnotationUsage( PostPersist.class );
			if ( method.getArgumentTypes().size() != 1 ) {
				assertThat( prePersist ).isNull();
			}
			else {
				assertThat( prePersist ).isNotNull();
			}
		} );
		assertThat( getMethodDetails( listenerClassDetails, "postRemove" ).get( 0 ).getDirectAnnotationUsage( PostRemove.class ) ).isNotNull();
		assertThat( getMethodDetails( listenerClassDetails, "postUpdate" ).get( 0 ).getDirectAnnotationUsage( PostUpdate.class ) ).isNotNull();
		assertThat( getMethodDetails( listenerClassDetails, "postLoad" ).get( 0 ).getDirectAnnotationUsage( PostLoad.class ) ).isNotNull();

	}

	private List<MethodDetails> getMethodDetails(ClassDetails classDetails, String name) {
		return classDetails.getMethods()
				.stream()
				.filter( m -> m.getName().equals( name ) )
				.collect( Collectors.toList() );
	}
}
