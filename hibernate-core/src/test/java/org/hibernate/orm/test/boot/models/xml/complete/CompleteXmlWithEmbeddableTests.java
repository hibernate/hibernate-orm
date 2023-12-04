/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.orm.test.boot.models.xml.complete;

import org.hibernate.boot.internal.BootstrapContextImpl;
import org.hibernate.boot.internal.MetadataBuilderImpl;
import org.hibernate.boot.model.process.spi.ManagedResources;
import org.hibernate.boot.models.categorize.spi.AttributeMetadata;
import org.hibernate.boot.models.categorize.spi.CategorizedDomainModel;
import org.hibernate.boot.models.categorize.spi.EntityHierarchy;
import org.hibernate.boot.models.categorize.spi.EntityTypeMetadata;
import org.hibernate.orm.test.boot.models.process.ManagedResourcesImpl;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;

import org.junit.jupiter.api.Test;

import jakarta.persistence.AccessType;
import jakarta.persistence.Basic;
import jakarta.persistence.Embedded;
import jakarta.persistence.Id;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.boot.models.categorize.spi.AttributeMetadata.AttributeNature.BASIC;
import static org.hibernate.boot.models.categorize.spi.AttributeMetadata.AttributeNature.EMBEDDED;
import static org.hibernate.boot.models.categorize.spi.ManagedResourcesProcessor.processManagedResources;

/**
 * @author Steve Ebersole
 */
public class CompleteXmlWithEmbeddableTests {
	@Test
	void testIt() {
		final ManagedResourcesImpl.Builder managedResourcesBuilder = new ManagedResourcesImpl.Builder();
		managedResourcesBuilder.addXmlMappings( "mappings/models/complete/simple-person.xml" );
		final ManagedResources managedResources = managedResourcesBuilder.build();

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

			final EntityHierarchy hierarchy = categorizedDomainModel.getEntityHierarchies().iterator().next();
			final EntityTypeMetadata personMetadata = hierarchy.getRoot();
			assertThat( personMetadata.getAccessType() ).isEqualTo( AccessType.FIELD );

			assertThat( personMetadata.getAttributes() ).hasSize( 2 );

			final AttributeMetadata idAttribute = personMetadata.findAttribute( "id" );
			assertThat( idAttribute.getNature() ).isEqualTo( BASIC );
			assertThat( idAttribute.getMember().getAnnotationUsage( Basic.class ) ).isNotNull();
			assertThat( idAttribute.getMember().getAnnotationUsage( Id.class ) ).isNotNull();

			final AttributeMetadata nameAttribute = personMetadata.findAttribute( "name" );
			assertThat( nameAttribute.getNature() ).isEqualTo( EMBEDDED );
			assertThat( nameAttribute.getMember().getAnnotationUsage( Embedded.class ) ).isNotNull();
		}
	}
}
