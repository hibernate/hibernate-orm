/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.orm.test.boot.models.xml.attr;

import java.util.Arrays;
import java.util.List;

import org.hibernate.annotations.Any;
import org.hibernate.annotations.AnyDiscriminator;
import org.hibernate.annotations.AnyDiscriminatorValue;
import org.hibernate.boot.internal.AnyKeyType;
import org.hibernate.boot.internal.BootstrapContextImpl;
import org.hibernate.boot.internal.MetadataBuilderImpl;
import org.hibernate.boot.model.process.spi.ManagedResources;
import org.hibernate.boot.model.source.internal.annotations.AdditionalManagedResourcesImpl;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.ClassDetailsRegistry;
import org.hibernate.models.spi.FieldDetails;
import org.hibernate.models.spi.SourceModelBuildingContext;

import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.orm.test.boot.models.SourceModelTestHelper.createBuildingContext;

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

		final SourceModelBuildingContext sourceModelBuildingContext = createBuildingContext(
				managedResources,
				false,
				new MetadataBuilderImpl.MetadataBuildingOptionsImpl( bootstrapContext.getServiceRegistry() ),
				bootstrapContext
		);

		final ClassDetailsRegistry classDetailsRegistry = sourceModelBuildingContext.getClassDetailsRegistry();

		// Entity3 is mapped by XML
		final ClassDetails entity3ClassDetails = classDetailsRegistry.resolveClassDetails( Entity3.class.getName() );

		final FieldDetails associationField = entity3ClassDetails.findFieldByName( "association" );
		assertThat( associationField ).isNotNull();
		assertThat( associationField.getDirectAnnotationUsage( Any.class ) ).isNotNull();

		final AnyDiscriminator discrimAnn = associationField.getDirectAnnotationUsage( AnyDiscriminator.class );
		assertThat( discrimAnn ).isNotNull();
		assertThat( discrimAnn.value() ).isEqualTo( DiscriminatorType.INTEGER );

		final AnyDiscriminatorValue[] discriminatorMappings = associationField.getRepeatedAnnotationUsages( AnyDiscriminatorValue.class, sourceModelBuildingContext );
		assertThat( discriminatorMappings ).hasSize( 2 );

		final List<String> mappedEntityNames = Arrays.stream( discriminatorMappings )
				.map( (valueAnn) -> valueAnn.entity().getName() )
				.toList();
		assertThat( mappedEntityNames ).containsExactly( Entity1.class.getName(), Entity2.class.getName() );

		final AnyKeyType keyTypeAnn = associationField.getDirectAnnotationUsage( AnyKeyType.class );
		assertThat( keyTypeAnn ).isNotNull();
		assertThat( keyTypeAnn.value() ).isEqualTo( "integer" );

		final JoinColumn keyColumn = associationField.getAnnotationUsage( JoinColumn.class, sourceModelBuildingContext );
		assertThat( keyColumn ).isNotNull();
		assertThat( keyColumn.name() ).isEqualTo( "association_fk" );
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
