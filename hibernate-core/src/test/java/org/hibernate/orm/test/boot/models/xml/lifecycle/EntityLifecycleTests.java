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
import org.hibernate.boot.models.categorize.spi.CategorizedDomainModel;
import org.hibernate.boot.models.categorize.spi.EntityTypeMetadata;
import org.hibernate.orm.test.boot.models.ManagedResourcesImpl;
import org.hibernate.orm.test.boot.models.xml.SimpleEntity;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.models.spi.AnnotationUsage;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.MethodDetails;

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
import static org.hibernate.boot.models.categorize.spi.ManagedResourcesProcessor.processManagedResources;

/**
 * @author Marco Belladelli
 */
public class EntityLifecycleTests {
	@Test
	void testEntityLifecycle() {
		final ManagedResources managedResources = new ManagedResourcesImpl.Builder()
				.addXmlMappings( "mappings/models/lifecycle/entity-lifecycle.xml" )
				.build();
		try (StandardServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder().build()) {
			final BootstrapContextImpl bootstrapContext = new BootstrapContextImpl(
					serviceRegistry,
					new MetadataBuilderImpl.MetadataBuildingOptionsImpl( serviceRegistry )
			);
			final CategorizedDomainModel categorizedDomainModel = processManagedResources(
					managedResources,
					bootstrapContext
			);

			assertThat( categorizedDomainModel.getEntityHierarchies() ).hasSize( 1 );
			final EntityTypeMetadata rootEntity = categorizedDomainModel.getEntityHierarchies()
					.iterator()
					.next()
					.getRoot();
			final ClassDetails classDetails = rootEntity.getClassDetails();
			assertThat( classDetails.getName() ).isEqualTo( SimpleEntity.class.getName() );

			// lifecycle callback methods
			getMethodDetails( classDetails, "prePersist" ).forEach( method -> {
				final AnnotationUsage<PrePersist> prePersist = method.getAnnotationUsage( PrePersist.class );
				if ( !method.getArgumentTypes().isEmpty() ) {
					assertThat( prePersist ).isNull();
				}
				else {
					assertThat( prePersist ).isNotNull();
				}
			} );
			assertThat( getMethodDetails( classDetails, "preRemove" ).get( 0 ).getAnnotationUsage( PreRemove.class ) ).isNotNull();
			assertThat( getMethodDetails( classDetails, "preUpdate" ).get( 0 ).getAnnotationUsage( PreUpdate.class ) ).isNotNull();

			// entity listeners
			final AnnotationUsage<EntityListeners> entityListenersAnn = classDetails.getAnnotationUsage( EntityListeners.class );
			assertThat( entityListenersAnn ).isNotNull();
			final List<ClassDetails> entityListeners = entityListenersAnn.getAttributeValue( "value" );
			assertThat( entityListeners ).hasSize( 1 );
			final ClassDetails listener = entityListeners.get( 0 );
			assertThat( listener.getName() ).isEqualTo( SimpleEntityListener.class.getName() );
			getMethodDetails( classDetails, "postPersist" ).forEach( method -> {
				final AnnotationUsage<PostPersist> prePersist = method.getAnnotationUsage( PostPersist.class );
				if ( method.getArgumentTypes().size() != 1 ) {
					assertThat( prePersist ).isNull();
				}
				else {
					assertThat( prePersist ).isNotNull();
				}
			} );
			assertThat( getMethodDetails( listener, "postRemove" ).get( 0 ).getAnnotationUsage( PostRemove.class ) ).isNotNull();
			assertThat( getMethodDetails( listener, "postUpdate" ).get( 0 ).getAnnotationUsage( PostUpdate.class ) ).isNotNull();
			assertThat( getMethodDetails( listener, "postLoad" ).get( 0 ).getAnnotationUsage( PostLoad.class ) ).isNotNull();
		}
	}

	private List<MethodDetails> getMethodDetails(ClassDetails classDetails, String name) {
		return classDetails.getMethods()
				.stream()
				.filter( m -> m.getName().equals( name ) )
				.collect( Collectors.toList() );
	}
}
