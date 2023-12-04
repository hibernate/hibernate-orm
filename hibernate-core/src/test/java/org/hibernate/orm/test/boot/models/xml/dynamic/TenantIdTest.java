/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.orm.test.boot.models.xml.dynamic;


import java.util.Set;

import org.hibernate.annotations.TenantId;
import org.hibernate.boot.internal.BootstrapContextImpl;
import org.hibernate.boot.internal.MetadataBuilderImpl;
import org.hibernate.boot.model.process.spi.ManagedResources;
import org.hibernate.boot.models.categorize.spi.CategorizedDomainModel;
import org.hibernate.boot.models.categorize.spi.EntityHierarchy;
import org.hibernate.boot.models.categorize.spi.EntityTypeMetadata;
import org.hibernate.orm.test.boot.models.process.ManagedResourcesImpl;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.models.spi.AnnotationUsage;
import org.hibernate.models.spi.FieldDetails;

import org.junit.jupiter.api.Test;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.FetchType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.boot.models.categorize.spi.ManagedResourcesProcessor.processManagedResources;


public class TenantIdTest {
	@Test
	void testSimpleDynamicModel() {
		final ManagedResources managedResources = new ManagedResourcesImpl.Builder()
				.addXmlMappings( "mappings/models/dynamic/dynamic-tenantid.xml" )
				.build();
		try (StandardServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder().build()) {
			final BootstrapContextImpl bootstrapContext = new BootstrapContextImpl(
					serviceRegistry,
					new MetadataBuilderImpl.MetadataBuildingOptionsImpl( serviceRegistry )
			);
			final CategorizedDomainModel categorizedDomainModel = processManagedResources(
					managedResources,
					bootstrapContext
			);

			final Set<EntityHierarchy> entityHierarchies = categorizedDomainModel.getEntityHierarchies();
			assertThat( entityHierarchies ).hasSize( 2 );

			entityHierarchies.forEach(
					entityHierarchy -> {
						final EntityTypeMetadata root = entityHierarchy.getRoot();
						final String entityName = root.getEntityName();

						final FieldDetails field = root.getClassDetails().findFieldByName( "tenantId" );

						if ( entityName.equals( "EntityWithoutTenantId" ) ) {
							assertThat( field ).isNull();
						}
						else {
							final AnnotationUsage<TenantId> tenantIdAnnotationUsage = field.getAnnotationUsage( TenantId.class );

							assertThat( tenantIdAnnotationUsage ).isNotNull();

							final AnnotationUsage<Basic> basicAnnotationUsage = field.getAnnotationUsage( Basic.class );
							assertThat( basicAnnotationUsage ).isNotNull();
							assertThat( basicAnnotationUsage.<FetchType>getAttributeValue( "fetch" ) )
									.isEqualTo( FetchType.EAGER );
							assertThat( basicAnnotationUsage.getBoolean( "optional" ) ).isTrue();

							final AnnotationUsage<Column> columnAnnotationUsage = field.getAnnotationUsage( Column.class );
							assertThat( basicAnnotationUsage ).isNotNull();
							assertThat( columnAnnotationUsage.getString( "name" ) ).isEqualTo( "TENANT_ID" );
							assertThat( columnAnnotationUsage.getBoolean( "insertable" ) ).isFalse();
						}
					}
			);
		}
	}
}
