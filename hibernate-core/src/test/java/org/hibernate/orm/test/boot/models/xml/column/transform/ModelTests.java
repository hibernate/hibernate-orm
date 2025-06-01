/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.models.xml.column.transform;

import org.hibernate.annotations.ColumnTransformer;
import org.hibernate.boot.model.process.spi.ManagedResources;
import org.hibernate.boot.model.source.internal.annotations.AdditionalManagedResourcesImpl;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Property;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.FieldDetails;
import org.hibernate.models.spi.ModelsContext;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
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
		final ModelsContext ModelsContext = createBuildingContext( managedResources, serviceRegistry );

		final ClassDetails classDetails = ModelsContext.getClassDetailsRegistry().getClassDetails( Item.class.getName() );
		final FieldDetails costField = classDetails.findFieldByName( "cost" );
		final ColumnTransformer transformerAnn = costField.getAnnotationUsage( ColumnTransformer.class, ModelsContext );
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
