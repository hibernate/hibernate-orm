/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.orm.test.boot.models.xml.dynamic;

import java.util.Set;

import org.hibernate.annotations.RowId;
import org.hibernate.boot.internal.BootstrapContextImpl;
import org.hibernate.boot.internal.MetadataBuilderImpl;
import org.hibernate.boot.model.process.spi.ManagedResources;
import org.hibernate.boot.models.categorize.spi.CategorizedDomainModel;
import org.hibernate.boot.models.categorize.spi.EntityHierarchy;
import org.hibernate.boot.models.categorize.spi.EntityTypeMetadata;
import org.hibernate.boot.model.source.internal.annotations.AdditionalManagedResourcesImpl;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.models.spi.AnnotationUsage;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.boot.models.categorize.spi.ManagedResourcesProcessor.processManagedResources;

public class RowIdTest {
	@Test
	void testSimpleDynamicModel() {
		final ManagedResources managedResources = new AdditionalManagedResourcesImpl.Builder()
				.addXmlMappings( "mappings/models/dynamic/dynamic-rowid.xml" )
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
			assertThat( entityHierarchies ).hasSize( 3 );

			entityHierarchies.forEach(
					entityHierarchy -> {
						final EntityTypeMetadata root = entityHierarchy.getRoot();
						final AnnotationUsage<RowId> rowIdAnnotationUsage = root.getClassDetails().getAnnotationUsage(
								RowId.class );
						final String entityName = root.getEntityName();
						if ( entityName.equals( "EntityWithoutRowId" ) ) {
							assertThat( rowIdAnnotationUsage ).isNull();
						}
						else {
							assertThat( rowIdAnnotationUsage ).isNotNull();
							final String value = rowIdAnnotationUsage.getString( "value" );
							if ( entityName.equals( "EntityWithRowIdNoValue" ) ) {
								assertThat( value ).isEmpty();
							}
							else {
								assertThat( value ).isEqualTo( "ROW_ID" );
							}
						}
					}
			);
		}
	}
}
