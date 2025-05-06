/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.models.xml.complete;

import org.hibernate.annotations.DiscriminatorFormula;
import org.hibernate.boot.model.process.spi.ManagedResources;
import org.hibernate.boot.model.source.internal.annotations.AdditionalManagedResourcesImpl;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.ClassDetailsRegistry;
import org.hibernate.models.spi.ModelsContext;
import org.hibernate.orm.test.boot.models.xml.SimpleEntity;

import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.DiscriminatorValue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.orm.test.boot.models.SourceModelTestHelper.createBuildingContext;

public class DiscriminatorValueTest {
	@Test
	@ServiceRegistry
	void testDiscriminatorValue(ServiceRegistryScope registryScope) {
		final ManagedResources managedResources = new AdditionalManagedResourcesImpl.Builder()
				.addXmlMappings( "mappings/models/complete/discriminator-value.xml" )
				.build();

		final ModelsContext ModelsContext = createBuildingContext(
				managedResources,
				registryScope.getRegistry()
		);
		final ClassDetailsRegistry classDetailsRegistry = ModelsContext.getClassDetailsRegistry();

		{
			final ClassDetails rootClassDetails = classDetailsRegistry.getClassDetails( Root.class.getName() );
			assertThat( rootClassDetails.hasDirectAnnotationUsage( DiscriminatorValue.class ) ).isTrue();
			assertThat( rootClassDetails.hasDirectAnnotationUsage( DiscriminatorFormula.class ) ).isFalse();

			final DiscriminatorColumn discriminatorColumn = rootClassDetails.getDirectAnnotationUsage(
					DiscriminatorColumn.class );
			assertThat( discriminatorColumn ).isNotNull();
			assertThat( discriminatorColumn.name() ).isEqualTo( "TYPE_COLUMN" );
			assertThat( discriminatorColumn.discriminatorType() ).isEqualTo( DiscriminatorType.CHAR );

			final ClassDetails subClassDetails = classDetailsRegistry.getClassDetails( Sub.class.getName() );
			assertThat( subClassDetails.hasDirectAnnotationUsage( DiscriminatorColumn.class ) ).isFalse();
			assertThat( subClassDetails.hasDirectAnnotationUsage( DiscriminatorFormula.class ) ).isFalse();

			final DiscriminatorValue discriminatorValue = subClassDetails.getDirectAnnotationUsage(
					DiscriminatorValue.class );
			assertThat( discriminatorValue.value() ).isEqualTo( "R" );
		}

		{
			final ClassDetails simplePersonClassDetails = classDetailsRegistry.getClassDetails( SimplePerson.class.getName() );
			assertThat( simplePersonClassDetails.hasDirectAnnotationUsage( DiscriminatorValue.class ) ).isFalse();
			assertThat( simplePersonClassDetails.hasDirectAnnotationUsage( DiscriminatorFormula.class ) ).isFalse();
			final DiscriminatorColumn discriminatorColumn = simplePersonClassDetails.getDirectAnnotationUsage(
					DiscriminatorColumn.class );
			assertThat( discriminatorColumn ).isNotNull();
			assertThat( discriminatorColumn.name() ).isEqualTo( "DTYPE" );
			assertThat( discriminatorColumn.discriminatorType() ).isEqualTo( DiscriminatorType.STRING );
		}

		{
			final ClassDetails simpleEntityClassDetails = classDetailsRegistry.getClassDetails( SimpleEntity.class.getName() );

			final DiscriminatorValue discriminatorValue = simpleEntityClassDetails.getDirectAnnotationUsage(
					DiscriminatorValue.class );
			assertThat( discriminatorValue ).isNull();

			final DiscriminatorColumn discriminatorColumn = simpleEntityClassDetails.getDirectAnnotationUsage(
					DiscriminatorColumn.class );
			assertThat( discriminatorColumn ).isNull();

			final DiscriminatorFormula discriminatorFormula = simpleEntityClassDetails.getDirectAnnotationUsage(
					DiscriminatorFormula.class );
			assertThat( discriminatorFormula ).isNotNull();
			assertThat( discriminatorFormula.value() ).isEqualTo(
					"CASE WHEN VALUE1 IS NOT NULL THEN 1 WHEN VALUE2 IS NOT NULL THEN 2 END" );
			assertThat( discriminatorFormula.discriminatorType() ).isEqualTo( DiscriminatorType.STRING );
		}

	}
}
