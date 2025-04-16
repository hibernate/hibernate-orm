/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.models.xml.dynamic;


import org.hibernate.annotations.TenantId;
import org.hibernate.boot.model.process.spi.ManagedResources;
import org.hibernate.boot.model.source.internal.annotations.AdditionalManagedResourcesImpl;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.ClassDetailsRegistry;
import org.hibernate.models.spi.FieldDetails;
import org.hibernate.models.spi.ModelsContext;

import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.FetchType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.orm.test.boot.models.SourceModelTestHelper.createBuildingContext;


@SuppressWarnings("JUnitMalformedDeclaration")
public class TenantIdTest {
	@Test
	@ServiceRegistry
	void testSimpleDynamicModel(ServiceRegistryScope registryScope) {
		final ManagedResources managedResources = new AdditionalManagedResourcesImpl.Builder()
				.addXmlMappings( "mappings/models/dynamic/dynamic-tenantid.xml" )
				.build();
		final ModelsContext ModelsContext = createBuildingContext( managedResources, registryScope.getRegistry() );
		final ClassDetailsRegistry classDetailsRegistry = ModelsContext.getClassDetailsRegistry();

		final ClassDetails classDetails = classDetailsRegistry.getClassDetails( "EntityWithTenantId" );
		final FieldDetails tenantIdField = classDetails.findFieldByName( "tenantId" );

		final TenantId tenantId = tenantIdField.getDirectAnnotationUsage( TenantId.class );
		assertThat( tenantId ).isNotNull();

		final Basic basic = tenantIdField.getDirectAnnotationUsage( Basic.class );
		assertThat( basic ).isNotNull();
		assertThat( basic.fetch() ).isEqualTo( FetchType.EAGER );
		assertThat( basic.optional() ).isTrue();

		final Column column = tenantIdField.getDirectAnnotationUsage( Column.class );
		assertThat( column ).isNotNull();
		assertThat( column.name() ).isEqualTo( "TENANT_ID" );
		assertThat( column.insertable() ).isFalse();
	}
}
