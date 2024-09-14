/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.boot.models.xml.dynamic;

import org.hibernate.annotations.RowId;
import org.hibernate.boot.model.process.spi.ManagedResources;
import org.hibernate.boot.model.source.internal.annotations.AdditionalManagedResourcesImpl;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.ClassDetailsRegistry;
import org.hibernate.models.spi.SourceModelBuildingContext;

import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.orm.test.boot.models.SourceModelTestHelper.createBuildingContext;

@SuppressWarnings("JUnitMalformedDeclaration")
public class RowIdTest {
	@Test
	@ServiceRegistry
	void testSimpleDynamicModel(ServiceRegistryScope registryScope) {
		final ManagedResources managedResources = new AdditionalManagedResourcesImpl.Builder()
				.addXmlMappings( "mappings/models/dynamic/dynamic-rowid.xml" )
				.build();
		final SourceModelBuildingContext sourceModelBuildingContext = createBuildingContext(
				managedResources,
				registryScope.getRegistry()
		);
		final ClassDetailsRegistry classDetailsRegistry = sourceModelBuildingContext.getClassDetailsRegistry();

		{
			final ClassDetails classDetails = classDetailsRegistry.getClassDetails( "EntityWithoutRowId" );
			final RowId rowId = classDetails.getDirectAnnotationUsage( RowId.class );
			assertThat( rowId ).isNull();
		}

		{
			final ClassDetails classDetails = classDetailsRegistry.getClassDetails( "EntityWithRowIdNoValue" );
			final RowId rowId = classDetails.getDirectAnnotationUsage( RowId.class );
			assertThat( rowId.value() ).isEmpty();
		}

		{
			final ClassDetails classDetails = classDetailsRegistry.getClassDetails( "EntityWithRowId" );
			final RowId rowId = classDetails.getDirectAnnotationUsage( RowId.class );
			assertThat( rowId.value() ).isEqualTo( "ROW_ID" );
		}
	}
}
