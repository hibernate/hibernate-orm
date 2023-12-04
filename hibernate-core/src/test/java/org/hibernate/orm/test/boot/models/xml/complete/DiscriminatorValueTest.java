/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.orm.test.boot.models.xml.complete;

import java.util.Set;

import org.hibernate.annotations.DiscriminatorFormula;
import org.hibernate.boot.internal.BootstrapContextImpl;
import org.hibernate.boot.internal.MetadataBuilderImpl;
import org.hibernate.boot.model.process.spi.ManagedResources;
import org.hibernate.boot.models.categorize.spi.CategorizedDomainModel;
import org.hibernate.boot.models.categorize.spi.EntityHierarchy;
import org.hibernate.boot.models.categorize.spi.EntityTypeMetadata;
import org.hibernate.boot.models.categorize.spi.IdentifiableTypeMetadata;
import org.hibernate.orm.test.boot.models.process.ManagedResourcesImpl;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.models.spi.AnnotationUsage;

import org.junit.jupiter.api.Test;

import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.DiscriminatorValue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.boot.models.categorize.spi.ManagedResourcesProcessor.processManagedResources;

public class DiscriminatorValueTest {
	@Test
	void testDiscriminatorValue() {
		final ManagedResources managedResources = new ManagedResourcesImpl.Builder()
				.addXmlMappings( "mappings/models/complete/discriminator-value.xml" )
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

			for ( EntityHierarchy entityHierarchy : entityHierarchies ) {
				final EntityTypeMetadata root = entityHierarchy.getRoot();

				final String entityName = root.getClassDetails().getName();
				if ( entityName.equals( "org.hibernate.orm.test.boot.models.xml.complete.Root" ) ) {

					final AnnotationUsage<DiscriminatorValue> rootDiscriminatorValueAnnotationUsage = root.getClassDetails()
							.getAnnotationUsage( DiscriminatorValue.class );
					assertThat( rootDiscriminatorValueAnnotationUsage ).isNull();

					final AnnotationUsage<DiscriminatorColumn> discriminatorColumnAnnotationUsage = root.getClassDetails()
							.getAnnotationUsage( DiscriminatorColumn.class );

					assertThat( discriminatorColumnAnnotationUsage ).isNotNull();

					final String discriminatorColumName = discriminatorColumnAnnotationUsage.getString( "name" );
					assertThat( discriminatorColumName ).isEqualTo( "TYPE_COLUMN" );

					final DiscriminatorType discriminatorColumnType = discriminatorColumnAnnotationUsage
							.getEnum( "discriminatorType" );
					assertThat( discriminatorColumnType ).isEqualTo( DiscriminatorType.INTEGER );

					final Iterable<IdentifiableTypeMetadata> subTypes = root.getSubTypes();
					assertThat( subTypes ).hasSize( 1 );

					final IdentifiableTypeMetadata subType = subTypes.iterator().next();
					final AnnotationUsage<DiscriminatorValue> subTypeDiscriminatorValueAnnotationUsage = subType.getClassDetails()
							.getAnnotationUsage( DiscriminatorValue.class );
					assertThat( subTypeDiscriminatorValueAnnotationUsage ).isNotNull();
					String discriminatorValue = subTypeDiscriminatorValueAnnotationUsage.getString( "value" );
					assertThat( discriminatorValue ).isEqualTo( "R" );

					final AnnotationUsage<DiscriminatorFormula> discriminatorFortmulaAnnotationUsage = root.getClassDetails()
							.getAnnotationUsage( DiscriminatorFormula.class );
					assertThat( discriminatorFortmulaAnnotationUsage ).isNull();
				}
				else if ( entityName.equals( "org.hibernate.orm.test.boot.models.xml.complete.SimplePerson" ) ) {
					final AnnotationUsage<DiscriminatorValue> rootDiscriminatorValueAnnotationUsage = root.getClassDetails()
							.getAnnotationUsage( DiscriminatorValue.class );
					assertThat( rootDiscriminatorValueAnnotationUsage ).isNull();

					final AnnotationUsage<DiscriminatorColumn> discriminatorColumnAnnotationUsage = root.getClassDetails()
							.getAnnotationUsage( DiscriminatorColumn.class );
					assertThat( discriminatorColumnAnnotationUsage ).isNotNull();

					final String discriminatorColumName = discriminatorColumnAnnotationUsage.getString( "name" );
					assertThat( discriminatorColumName ).isEqualTo( "DTYPE" );

					final DiscriminatorType discriminatorColumnType = discriminatorColumnAnnotationUsage
							.getEnum( "discriminatorType" );
					assertThat( discriminatorColumnType ).isEqualTo( DiscriminatorType.STRING );

					final AnnotationUsage<DiscriminatorFormula> discriminatorFortmulaAnnotationUsage = root.getClassDetails()
							.getAnnotationUsage( DiscriminatorFormula.class );
					assertThat( discriminatorFortmulaAnnotationUsage ).isNull();
				}
				else {
					assertThat( entityName ).isEqualTo( "org.hibernate.orm.test.boot.models.xml.SimpleEntity" );

					final AnnotationUsage<DiscriminatorValue> rootDiscriminatorValueAnnotationUsage = root.getClassDetails()
							.getAnnotationUsage( DiscriminatorValue.class );
					assertThat( rootDiscriminatorValueAnnotationUsage ).isNull();

					final AnnotationUsage<DiscriminatorColumn> discriminatorColumnAnnotationUsage = root.getClassDetails()
							.getAnnotationUsage( DiscriminatorColumn.class );
					assertThat( discriminatorColumnAnnotationUsage ).isNull();

					final AnnotationUsage<DiscriminatorFormula> discriminatorFortmulaAnnotationUsage = root.getClassDetails()
							.getAnnotationUsage( DiscriminatorFormula.class );
					assertThat( discriminatorFortmulaAnnotationUsage ).isNotNull();

					final String formula = discriminatorFortmulaAnnotationUsage.getString( "value" );
					assertThat( formula ).isEqualTo( "CASE WHEN VALUE1 IS NOT NULL THEN 1 WHEN VALUE2 IS NOT NULL THEN 2 END" );
				}
			}

		}
	}
}
