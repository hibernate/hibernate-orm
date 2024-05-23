/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.orm.test.boot.models.xml.dynamic;


import org.hibernate.annotations.TenantId;
import org.hibernate.boot.internal.BootstrapContextImpl;
import org.hibernate.boot.internal.MetadataBuilderImpl;
import org.hibernate.boot.model.process.spi.ManagedResources;
import org.hibernate.boot.model.source.internal.annotations.AdditionalManagedResourcesImpl;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.ClassDetailsRegistry;
import org.hibernate.models.spi.FieldDetails;
import org.hibernate.models.spi.SourceModelBuildingContext;

import org.junit.jupiter.api.Test;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.FetchType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.orm.test.boot.models.SourceModelTestHelper.createBuildingContext;


public class TenantIdTest {
	@Test
	void testSimpleDynamicModel() {
		final ManagedResources managedResources = new AdditionalManagedResourcesImpl.Builder()
				.addXmlMappings( "mappings/models/dynamic/dynamic-tenantid.xml" )
				.build();
		try (StandardServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder().build()) {
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
			final ClassDetailsRegistry classDetailsRegistry = sourceModelBuildingContext.getClassDetailsRegistry();

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
}
