/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.orm.test.boot.models.xml.complete;

import org.hibernate.boot.internal.MetadataBuilderImpl;
import org.hibernate.boot.internal.MetadataBuilderImpl.MetadataBuildingOptionsImpl;
import org.hibernate.boot.model.process.spi.ManagedResources;
import org.hibernate.boot.model.source.internal.annotations.AdditionalManagedResourcesImpl;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.ClassDetailsRegistry;
import org.hibernate.models.spi.FieldDetails;
import org.hibernate.models.spi.SourceModelBuildingContext;
import org.hibernate.orm.test.boot.models.BootstrapContextTesting;
import org.hibernate.orm.test.boot.models.SourceModelTestHelper;

import org.junit.jupiter.api.Test;

import org.jboss.jandex.Index;

import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;

import static jakarta.persistence.InheritanceType.JOINED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.models.internal.SimpleClassLoading.SIMPLE_CLASS_LOADING;
import static org.hibernate.orm.test.boot.models.SourceModelTestHelper.createBuildingContext;

/**
 * @author Steve Ebersole
 */
public class CompleteXmlInheritanceTests {
	@Test
	void testIt() {

		final AdditionalManagedResourcesImpl.Builder managedResourcesBuilder = new AdditionalManagedResourcesImpl.Builder();
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

			final SourceModelBuildingContext sourceModelBuildingContext = createBuildingContext(
					managedResources,
					false,
					new MetadataBuilderImpl.MetadataBuildingOptionsImpl( bootstrapContext.getServiceRegistry() ),
					bootstrapContext
			);

			final ClassDetailsRegistry classDetailsRegistry = sourceModelBuildingContext.getClassDetailsRegistry();
			final ClassDetails root = classDetailsRegistry.getClassDetails( Root.class.getName() );

			final Inheritance inheritance = root.getDirectAnnotationUsage( Inheritance.class );
			assertThat( inheritance.strategy() ).isEqualTo( JOINED );

			assertThat( root.getClassName() ).isEqualTo( Root.class.getName() );
			final FieldDetails idAttr = root.findFieldByName( "id" );
			assertThat( idAttr.getDirectAnnotationUsage( Id.class ) ).isNotNull();
		}
	}
}
