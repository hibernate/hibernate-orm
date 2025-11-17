/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.models.xml.complete;

import org.hibernate.annotations.Filter;
import org.hibernate.annotations.ResultCheckStyle;
import org.hibernate.annotations.SQLInsert;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.SqlFragmentAlias;
import org.hibernate.boot.model.process.spi.ManagedResources;
import org.hibernate.boot.model.source.internal.annotations.AdditionalManagedResourcesImpl;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.ClassDetailsRegistry;
import org.hibernate.models.spi.FieldDetails;
import org.hibernate.models.spi.ModelsContext;
import org.hibernate.orm.test.boot.models.xml.SimpleEntity;

import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Id;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.orm.test.boot.models.SourceModelTestHelper.createBuildingContext;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
public class SimpleCompleteXmlTests {
	@Test
	@ServiceRegistry
	void testSimpleCompleteEntity(ServiceRegistryScope registryScope) {
		final AdditionalManagedResourcesImpl.Builder managedResourcesBuilder = new AdditionalManagedResourcesImpl.Builder();
		managedResourcesBuilder.addXmlMappings( "mappings/models/complete/simple-complete.xml" );
		final ManagedResources managedResources = managedResourcesBuilder.build();

		final ModelsContext ModelsContext = createBuildingContext( managedResources, registryScope.getRegistry() );
		final ClassDetailsRegistry classDetailsRegistry = ModelsContext.getClassDetailsRegistry();

		final ClassDetails classDetails = classDetailsRegistry.getClassDetails( SimpleEntity.class.getName() );

		final FieldDetails idField = classDetails.findFieldByName( "id" );
		assertThat( idField.getDirectAnnotationUsage( Basic.class ) ).isNotNull();
		assertThat( idField.getDirectAnnotationUsage( Id.class ) ).isNotNull();
		final Column idColumnAnn = idField.getDirectAnnotationUsage( Column.class );
		assertThat( idColumnAnn ).isNotNull();
		assertThat( idColumnAnn.name() ).isEqualTo( "pk" );

		final FieldDetails nameField = classDetails.findFieldByName( "name" );
		assertThat( nameField.getDirectAnnotationUsage( Basic.class ) ).isNotNull();
		final Column nameColumnAnn = nameField.getDirectAnnotationUsage( Column.class );
		assertThat( nameColumnAnn ).isNotNull();
		assertThat( nameColumnAnn.name() ).isEqualTo( "description" );

		final SQLRestriction sqlRestriction = classDetails.getDirectAnnotationUsage( SQLRestriction.class );
		assertThat( sqlRestriction ).isNotNull();
		assertThat( sqlRestriction.value() ).isEqualTo( "name is not null" );

		validateSqlInsert( classDetails.getDirectAnnotationUsage( SQLInsert.class ));

		validateFilterUsage( classDetails.getAnnotationUsage( Filter.class, ModelsContext ) );
	}

	private void validateFilterUsage(Filter filter) {
		assertThat( filter ).isNotNull();
		assertThat( filter.name() ).isEqualTo( "name_filter" );
		assertThat( filter.condition() ).isEqualTo( "{t}.name = :name" );
		final SqlFragmentAlias[] aliases = filter.aliases();
		assertThat( aliases ).hasSize( 1 );
		assertThat( aliases[0].alias() ).isEqualTo( "t" );
		assertThat( aliases[0].table() ).isEqualTo( "SimpleEntity" );
		assertThat( aliases[0].entity().getName() ).isEqualTo( SimpleEntity.class.getName() );
	}

	private void validateSqlInsert(SQLInsert sqlInsert) {
		assertThat( sqlInsert ).isNotNull();
		assertThat( sqlInsert.sql() ).isEqualTo( "insertSimpleEntity(?)" );
		assertThat( sqlInsert.callable() ).isTrue();
		assertThat( sqlInsert.check() ).isEqualTo( ResultCheckStyle.COUNT );
		assertThat( sqlInsert.table() ).isEqualTo( "SimpleEntity" );
	}
}
