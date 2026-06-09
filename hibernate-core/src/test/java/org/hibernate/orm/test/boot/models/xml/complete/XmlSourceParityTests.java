/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.models.xml.complete;

import org.hibernate.annotations.LazyGroup;
import org.hibernate.annotations.ListIndexBase;
import org.hibernate.annotations.Mutability;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLInsert;
import org.hibernate.annotations.SQLUpdate;
import org.hibernate.boot.model.process.spi.ManagedResources;
import org.hibernate.boot.model.source.internal.annotations.AdditionalManagedResourcesImpl;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.ClassDetailsRegistry;
import org.hibernate.models.spi.FieldDetails;
import org.hibernate.models.spi.ModelsContext;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.orm.test.boot.models.SourceModelTestHelper.createBuildingContext;

@SuppressWarnings("JUnitMalformedDeclaration")
public class XmlSourceParityTests {
	@Test
	@ServiceRegistry
	void testXmlSourceParity(ServiceRegistryScope registryScope) {
		final AdditionalManagedResourcesImpl.Builder managedResourcesBuilder = new AdditionalManagedResourcesImpl.Builder();
		managedResourcesBuilder.addXmlMappings( "mappings/models/complete/xml-source-parity.xml" );
		final ManagedResources managedResources = managedResourcesBuilder.build();

		final ModelsContext modelsContext = createBuildingContext( managedResources, registryScope.getRegistry() );
		final ClassDetailsRegistry classDetailsRegistry = modelsContext.getClassDetailsRegistry();
		final ClassDetails classDetails = classDetailsRegistry.getClassDetails( XmlSourceParityEntity.class.getName() );

		final FieldDetails nameField = classDetails.findFieldByName( "name" );
		final LazyGroup lazyGroup = nameField.getDirectAnnotationUsage( LazyGroup.class );
		assertThat( lazyGroup ).isNotNull();
		assertThat( lazyGroup.value() ).isEqualTo( "name-group" );

		final FieldDetails detailsField = classDetails.findFieldByName( "details" );
		final Mutability mutability = detailsField.getDirectAnnotationUsage( Mutability.class );
		assertThat( mutability ).isNotNull();
		assertThat( mutability.value() ).isEqualTo( XmlSourceParityMutabilityPlan.class );

		final FieldDetails tagsField = classDetails.findFieldByName( "tags" );
		final LazyGroup tagsLazyGroup = tagsField.getDirectAnnotationUsage( LazyGroup.class );
		assertThat( tagsLazyGroup ).isNotNull();
		assertThat( tagsLazyGroup.value() ).isEqualTo( "tag-group" );

		final ListIndexBase listIndexBase = tagsField.getDirectAnnotationUsage( ListIndexBase.class );
		assertThat( listIndexBase ).isNotNull();
		assertThat( listIndexBase.value() ).isEqualTo( 3 );

		final SQLInsert[] sqlInserts = classDetails.getRepeatedAnnotationUsages( SQLInsert.class, modelsContext );
		assertThat( sqlInserts ).singleElement().satisfies( (sqlInsert) -> {
			assertThat( sqlInsert.table() ).isEqualTo( "xml_source_details" );
			assertThat( sqlInsert.sql() ).isEqualTo( "insert into xml_source_details (details, id) values (?, ?)" );
		} );

		final SQLUpdate[] sqlUpdates = classDetails.getRepeatedAnnotationUsages( SQLUpdate.class, modelsContext );
		assertThat( sqlUpdates ).singleElement().satisfies( (sqlUpdate) -> {
			assertThat( sqlUpdate.table() ).isEqualTo( "xml_source_details" );
			assertThat( sqlUpdate.sql() ).isEqualTo( "update xml_source_details set details = ? where id = ?" );
		} );

		final SQLDelete[] sqlDeletes = classDetails.getRepeatedAnnotationUsages( SQLDelete.class, modelsContext );
		assertThat( sqlDeletes ).singleElement().satisfies( (sqlDelete) -> {
			assertThat( sqlDelete.table() ).isEqualTo( "xml_source_details" );
			assertThat( sqlDelete.sql() ).isEqualTo( "delete from xml_source_details where id = ?" );
		} );
	}
}
