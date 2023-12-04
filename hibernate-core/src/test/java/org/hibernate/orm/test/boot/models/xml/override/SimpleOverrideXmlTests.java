/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.orm.test.boot.models.xml.override;

import org.hibernate.boot.internal.BootstrapContextImpl;
import org.hibernate.boot.internal.MetadataBuilderImpl;
import org.hibernate.boot.model.process.spi.ManagedResources;
import org.hibernate.boot.models.categorize.spi.AttributeMetadata;
import org.hibernate.boot.models.categorize.spi.CategorizedDomainModel;
import org.hibernate.boot.models.categorize.spi.EntityHierarchy;
import org.hibernate.boot.models.categorize.spi.EntityTypeMetadata;
import org.hibernate.orm.test.boot.models.process.ManagedResourcesImpl;
import org.hibernate.orm.test.boot.models.xml.SimpleEntity;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.models.spi.AnnotationUsage;

import org.junit.jupiter.api.Test;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Id;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.boot.models.categorize.spi.ManagedResourcesProcessor.processManagedResources;

/**
 * @author Steve Ebersole
 */
public class SimpleOverrideXmlTests {
	@Test
	void testSimpleCompleteEntity() {

		final ManagedResourcesImpl.Builder managedResourcesBuilder = new ManagedResourcesImpl.Builder();
		managedResourcesBuilder.addXmlMappings( "mappings/models/override/simple-override.xml" );
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
			final EntityTypeMetadata root = hierarchy.getRoot();
			assertThat( root.getClassDetails().getClassName() ).isEqualTo( SimpleEntity.class.getName() );
			assertThat( root.getNumberOfAttributes() ).isEqualTo( 2 );

			final AttributeMetadata idAttribute = root.findAttribute( "id" );
			assertThat( idAttribute.getNature() ).isEqualTo( AttributeMetadata.AttributeNature.BASIC );
			assertThat( idAttribute.getMember().getAnnotationUsage( Id.class ) ).isNotNull();

			final AttributeMetadata nameAttribute = root.findAttribute( "name" );
			assertThat( nameAttribute.getNature() ).isEqualTo( AttributeMetadata.AttributeNature.BASIC );
			assertThat( nameAttribute.getMember().getAnnotationUsage( Basic.class ) ).isNotNull();
			final AnnotationUsage<Column> nameColumnAnn = nameAttribute.getMember().getAnnotationUsage( Column.class );
			assertThat( nameColumnAnn ).isNotNull();
			assertThat( nameColumnAnn.<String>getAttributeValue( "name" ) ).isEqualTo( "description" );
			assertThat( nameColumnAnn.<String>getAttributeValue( "columnDefinition" ) ).isEqualTo( "nvarchar(512)" );
		}
	}
}
