/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.orm.test.boot.models.xml.attr;

import java.util.List;
import java.util.stream.Collectors;

import org.hibernate.annotations.Any;
import org.hibernate.annotations.AnyDiscriminator;
import org.hibernate.annotations.AnyDiscriminatorValue;
import org.hibernate.boot.internal.BootstrapContextImpl;
import org.hibernate.boot.internal.MetadataBuilderImpl;
import org.hibernate.boot.model.process.spi.ManagedResources;
import org.hibernate.boot.internal.AnyKeyType;
import org.hibernate.boot.models.categorize.spi.CategorizedDomainModel;
import org.hibernate.boot.models.categorize.spi.EntityHierarchy;
import org.hibernate.boot.model.source.internal.annotations.AdditionalManagedResourcesImpl;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.models.spi.AnnotationUsage;
import org.hibernate.models.spi.FieldDetails;

import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.boot.models.categorize.spi.ManagedResourcesProcessor.processManagedResources;

/**
 * @author Steve Ebersole
 */
@ServiceRegistry
public class AnyTests {
	@Test
	@SuppressWarnings("JUnitMalformedDeclaration")
	void testSimpleAnyAttribute(ServiceRegistryScope scope) {
		final StandardServiceRegistry serviceRegistry = scope.getRegistry();
		final ManagedResources managedResources = new AdditionalManagedResourcesImpl.Builder()
				.addLoadedClasses( Entity1.class )
				.addLoadedClasses( Entity2.class )
				.addXmlMappings( "mappings/models/attr/any/simple.xml" )
				.build();

		final BootstrapContextImpl bootstrapContext = new BootstrapContextImpl(
				serviceRegistry,
				new MetadataBuilderImpl.MetadataBuildingOptionsImpl( serviceRegistry )
		);
		final CategorizedDomainModel categorizedDomainModel = processManagedResources(
				managedResources,
				bootstrapContext
		);

		assertThat( categorizedDomainModel.getEntityHierarchies() ).hasSize( 3 );
		final EntityHierarchy entity3Hierarchy = categorizedDomainModel.getEntityHierarchies()
				.stream()
				.filter( hierarchy -> hierarchy.getRoot().getEntityName().endsWith( "Entity3" ) )
				.findFirst()
				.orElse( null );
		assertThat( entity3Hierarchy ).isNotNull();

		final FieldDetails associationField = entity3Hierarchy.getRoot().getClassDetails().findFieldByName( "association" );
		assertThat( associationField ).isNotNull();
		assertThat( associationField.getAnnotationUsage( Any.class ) ).isNotNull();

		final AnnotationUsage<AnyDiscriminator> discrimAnn = associationField.getAnnotationUsage( AnyDiscriminator.class );
		assertThat( discrimAnn ).isNotNull();
		assertThat( discrimAnn.<DiscriminatorType>getEnum( "value" ) ).isEqualTo( DiscriminatorType.INTEGER );

		final List<AnnotationUsage<AnyDiscriminatorValue>> discriminatorMappings = associationField.getRepeatedAnnotationUsages( AnyDiscriminatorValue.class );
		assertThat( discriminatorMappings ).hasSize( 2 );

		final List<String> mappedEntityNames = discriminatorMappings.stream()
				.map( (valueAnn) -> valueAnn.getClassDetails( "entity" ).getName() )
				.collect( Collectors.toList() );
		assertThat( mappedEntityNames ).containsExactly( Entity1.class.getName(), Entity2.class.getName() );

		final AnnotationUsage<AnyKeyType> keyTypeAnn = associationField.getAnnotationUsage( AnyKeyType.class );
		assertThat( keyTypeAnn ).isNotNull();
		assertThat( keyTypeAnn.getString( "value" ) ).isEqualTo( "integer" );

		final AnnotationUsage<JoinColumn> keyColumn = associationField.getAnnotationUsage( JoinColumn.class );
		assertThat( keyColumn ).isNotNull();
		assertThat( keyColumn.getString( "name" ) ).isEqualTo( "association_fk" );
	}

	@Entity(name="Entity1")
	@Table(name="Entity1")
	public static class Entity1 {
		@Id
		private Integer id;
		private String name;
	}

	@Entity(name="Entity2")
	@Table(name="Entity2")
	public static class Entity2 {
		@Id
		private Integer id;
		private String name;
	}

	@Entity(name="Entity3")
	@Table(name="Entity3")
	public static class Entity3 {
		@Id
		private Integer id;
		private String name;

		@Any
		@AnyDiscriminator(DiscriminatorType.INTEGER)
		@AnyDiscriminatorValue( discriminator = "1", entity = Entity1.class )
		@AnyDiscriminatorValue( discriminator = "2", entity = Entity2.class )
		private Object association;

	}
}
