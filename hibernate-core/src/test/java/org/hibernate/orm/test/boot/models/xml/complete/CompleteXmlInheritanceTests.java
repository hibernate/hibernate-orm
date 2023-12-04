/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.orm.test.boot.models.xml.complete;

import org.hibernate.boot.internal.MetadataBuilderImpl.MetadataBuildingOptionsImpl;
import org.hibernate.boot.model.process.spi.ManagedResources;
import org.hibernate.boot.models.categorize.spi.AttributeMetadata;
import org.hibernate.boot.models.categorize.spi.CategorizedDomainModel;
import org.hibernate.boot.models.categorize.spi.EntityHierarchy;
import org.hibernate.boot.models.categorize.spi.EntityTypeMetadata;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.orm.test.boot.models.BootstrapContextTesting;
import org.hibernate.orm.test.boot.models.SourceModelTestHelper;
import org.hibernate.orm.test.boot.models.process.ManagedResourcesImpl;

import org.junit.jupiter.api.Test;

import org.jboss.jandex.Index;

import jakarta.persistence.Id;

import static jakarta.persistence.InheritanceType.JOINED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.boot.models.categorize.spi.ManagedResourcesProcessor.processManagedResources;
import static org.hibernate.models.internal.SimpleClassLoading.SIMPLE_CLASS_LOADING;

/**
 * @author Steve Ebersole
 */
public class CompleteXmlInheritanceTests {
	@Test
	void testIt() {

		final ManagedResourcesImpl.Builder managedResourcesBuilder = new ManagedResourcesImpl.Builder();
		managedResourcesBuilder.addXmlMappings( "mappings/models/complete/simple-inherited.xml" );
		final ManagedResources managedResources = managedResourcesBuilder.build();

		final Index jandexIndex = SourceModelTestHelper.buildJandexIndex(
				SIMPLE_CLASS_LOADING,
				Root.class,
				Sub.class
		);

		try (StandardServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder().build()) {
			final BootstrapContextTesting bootstrapContext = new BootstrapContextTesting(
					jandexIndex,
					serviceRegistry,
					new MetadataBuildingOptionsImpl( serviceRegistry )
			);
			final CategorizedDomainModel categorizedDomainModel = processManagedResources( managedResources, bootstrapContext );

			assertThat( categorizedDomainModel.getEntityHierarchies() ).hasSize( 1 );
			final EntityHierarchy hierarchy = categorizedDomainModel.getEntityHierarchies().iterator().next();
			assertThat( hierarchy.getInheritanceType() ).isEqualTo( JOINED );

			final EntityTypeMetadata rootMetadata = hierarchy.getRoot();
			assertThat( rootMetadata.getClassDetails().getClassName() ).isEqualTo( Root.class.getName() );
			final AttributeMetadata idAttr = rootMetadata.findAttribute( "id" );
			assertThat( idAttr.getMember().getAnnotationUsage( Id.class ) ).isNotNull();
		}
	}
}
