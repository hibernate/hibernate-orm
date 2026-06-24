/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.models.xml.attr;

import java.util.Map;

import org.hibernate.annotations.MapKeyJavaType;
import org.hibernate.annotations.MapKeyJdbcType;
import org.hibernate.annotations.MapKeyJdbcTypeCode;
import org.hibernate.boot.model.process.spi.ManagedResources;
import org.hibernate.boot.model.source.internal.annotations.AdditionalManagedResourcesImpl;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.ClassDetailsRegistry;
import org.hibernate.models.spi.FieldDetails;
import org.hibernate.models.spi.ModelsContext;
import org.hibernate.type.descriptor.java.StringJavaType;
import org.hibernate.type.descriptor.jdbc.VarcharJdbcType;

import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.orm.test.boot.models.SourceModelTestHelper.createBuildingContext;

@ServiceRegistry
public class ElementCollectionTests {
	@Test
	@SuppressWarnings("JUnitMalformedDeclaration")
	void testMapKeyJavaType(ServiceRegistryScope scope) {
		final StandardServiceRegistry serviceRegistry = scope.getRegistry();
		final ManagedResources managedResources = new AdditionalManagedResourcesImpl.Builder()
				.addXmlMappings( "mappings/models/attr/element-collection/map-key-type.xml" )
				.build();

		final ModelsContext modelsContext = createBuildingContext( managedResources, serviceRegistry );
		final ClassDetailsRegistry classDetailsRegistry = modelsContext.getClassDetailsRegistry();

		final ClassDetails classDetails = classDetailsRegistry.getClassDetails( SimpleEntity.class.getName() );

		final FieldDetails dataField = classDetails.findFieldByName( "data" );

		final MapKeyJavaType mapKeyJavaTypeAnn = dataField.getDirectAnnotationUsage( MapKeyJavaType.class );
		assertThat( mapKeyJavaTypeAnn ).isNotNull();
		assertThat( mapKeyJavaTypeAnn.value() ).isEqualTo( StringJavaType.class );
	}

	@Test
	@SuppressWarnings("JUnitMalformedDeclaration")
	void testMapKeyJdbcType(ServiceRegistryScope scope) {
		final StandardServiceRegistry serviceRegistry = scope.getRegistry();
		final ManagedResources managedResources = new AdditionalManagedResourcesImpl.Builder()
				.addXmlMappings( "mappings/models/attr/element-collection/map-key-jdbc-type.xml" )
				.build();

		final ModelsContext modelsContext = createBuildingContext( managedResources, serviceRegistry );
		final ClassDetailsRegistry classDetailsRegistry = modelsContext.getClassDetailsRegistry();

		final ClassDetails classDetails = classDetailsRegistry.getClassDetails( SimpleEntity.class.getName() );

		final FieldDetails dataField = classDetails.findFieldByName( "data" );

		final MapKeyJdbcType mapKeyJdbcTypeAnn = dataField.getDirectAnnotationUsage( MapKeyJdbcType.class );
		assertThat( mapKeyJdbcTypeAnn ).isNotNull();
		assertThat( mapKeyJdbcTypeAnn.value() ).isEqualTo( VarcharJdbcType.class );
	}

	@Test
	@SuppressWarnings("JUnitMalformedDeclaration")
	void testMapKeyJdbcTypeCode(ServiceRegistryScope scope) {
		final StandardServiceRegistry serviceRegistry = scope.getRegistry();
		final ManagedResources managedResources = new AdditionalManagedResourcesImpl.Builder()
				.addXmlMappings( "mappings/models/attr/element-collection/map-key-jdbc-type-code.xml" )
				.build();

		final ModelsContext modelsContext = createBuildingContext( managedResources, serviceRegistry );
		final ClassDetailsRegistry classDetailsRegistry = modelsContext.getClassDetailsRegistry();

		final ClassDetails classDetails = classDetailsRegistry.getClassDetails( SimpleEntity.class.getName() );

		final FieldDetails dataField = classDetails.findFieldByName( "data" );

		final MapKeyJdbcTypeCode mapKeyJdbcTypeCodeAnn = dataField.getDirectAnnotationUsage( MapKeyJdbcTypeCode.class );
		assertThat( mapKeyJdbcTypeCodeAnn ).isNotNull();
		assertThat( mapKeyJdbcTypeCodeAnn.value() ).isEqualTo( java.sql.Types.VARCHAR );
	}

	@SuppressWarnings("unused")
	@Entity(name = "SimpleEntity")
	@Table(name = "SimpleEntity")
	public static class SimpleEntity {
		@Id
		private Integer id;
		private Map<String, String> data;
	}
}
