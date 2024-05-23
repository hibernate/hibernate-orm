/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.orm.test.boot.models.xml.lifecycle;

import java.util.List;
import java.util.stream.Collectors;

import org.hibernate.boot.internal.BootstrapContextImpl;
import org.hibernate.boot.internal.MetadataBuilderImpl;
import org.hibernate.boot.model.process.spi.ManagedResources;
import org.hibernate.boot.model.source.internal.annotations.AdditionalManagedResourcesImpl;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.ClassDetailsRegistry;
import org.hibernate.models.spi.MethodDetails;
import org.hibernate.models.spi.SourceModelBuildingContext;
import org.hibernate.orm.test.boot.models.xml.SimpleEntity;

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
	void testEntityLifecycle() {
		final ManagedResources managedResources = new AdditionalManagedResourcesImpl.Builder()
				.addXmlMappings( "mappings/models/lifecycle/entity-lifecycle.xml" )
				.build();
		try (StandardServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder().build()) {
			final BootstrapContextImpl bootstrapContext = new BootstrapContextImpl(
					serviceRegistry,
					new MetadataBuilderImpl.MetadataBuildingOptionsImpl( serviceRegistry )
			);
			final SourceModelBuildingContext sourceModelBuildingContext = createBuildingContext(
					managedResources,
					false,
					new MetadataBuilderImpl.MetadataBuildingOptionsImpl( bootstrapContext.getServiceRegistry() ),
					bootstrapContext
			);
			final ClassDetailsRegistry classDetailsRegistry = sourceModelBuildingContext.getClassDetailsRegistry();
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
	}

	private List<MethodDetails> getMethodDetails(ClassDetails classDetails, String name) {
		return classDetails.getMethods()
				.stream()
				.filter( m -> m.getName().equals( name ) )
				.collect( Collectors.toList() );
	}
}
