/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.orm.test.boot.models.xml.column.transform;

import org.hibernate.annotations.ColumnTransformer;
import org.hibernate.boot.internal.BootstrapContextImpl;
import org.hibernate.boot.internal.MetadataBuilderImpl;
import org.hibernate.boot.model.process.spi.ManagedResources;
import org.hibernate.boot.model.source.internal.annotations.AdditionalManagedResourcesImpl;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.cfg.MappingSettings;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Property;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.FieldDetails;
import org.hibernate.models.spi.SourceModelBuildingContext;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.orm.test.boot.models.SourceModelTestHelper.createBuildingContext;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
public class ModelTests {
	@ServiceRegistry
	@Test
	void testMappingXml(ServiceRegistryScope scope) {
		final ManagedResources managedResources = new AdditionalManagedResourcesImpl.Builder()
				.addXmlMappings( "mappings/models/column/transform/mapping.xml" )
				.build();
		final StandardServiceRegistry serviceRegistry = scope.getRegistry();
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

		final ClassDetails classDetails = sourceModelBuildingContext.getClassDetailsRegistry().getClassDetails( Item.class.getName() );
		final FieldDetails costField = classDetails.findFieldByName( "cost" );
		final ColumnTransformer transformerAnn = costField.getAnnotationUsage( ColumnTransformer.class, sourceModelBuildingContext );
		assertThat( transformerAnn ).isNotNull();
		assertThat( transformerAnn.read() ).isEqualTo( "cost / 100.00" );
		assertThat( transformerAnn.write() ).isEqualTo( "? * 100.00" );
	}

	@ServiceRegistry(settings = @Setting(name = MappingSettings.TRANSFORM_HBM_XML, value = "true"))
	@Test
	void testHbmXml(ServiceRegistryScope scope) {
		final ManagedResources managedResources = new AdditionalManagedResourcesImpl.Builder( true, true )
				.addXmlMappings( "mappings/models/column/transform/hbm.xml" )
				.build();
		final StandardServiceRegistry serviceRegistry = scope.getRegistry();
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

		final ClassDetails classDetails = sourceModelBuildingContext.getClassDetailsRegistry().getClassDetails( Item.class.getName() );
		final FieldDetails costField = classDetails.findFieldByName( "cost" );
		final ColumnTransformer transformerAnn = costField.getAnnotationUsage( ColumnTransformer.class, sourceModelBuildingContext );
		assertThat( transformerAnn ).isNotNull();
		assertThat( transformerAnn.read() ).isEqualTo( "cost / 100.00" );
		assertThat( transformerAnn.write() ).isEqualTo( "? * 100.00" );
	}

	@ServiceRegistry
	@DomainModel(xmlMappings = "mappings/models/column/transform/mapping.xml")
	@Test
	void testMappingModel(DomainModelScope domainModelScope) {
		domainModelScope.withHierarchy( Item.class, (rootClass) -> {
			final Property costProperty = rootClass.getProperty( "cost" );
			assertThat( costProperty.getColumns() ).hasSize( 1 );
			final Column column = costProperty.getColumns().get( 0 );
			assertThat( column.getCustomRead() ).isEqualTo( "cost / 100.00" );
			assertThat( column.getCustomWrite() ).isEqualTo( "? * 100.00" );
		} );
	}
}
