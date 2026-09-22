/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.namingstrategy;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.boot.internal.SessionFactoryOptionsCollector;
import org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl;
import org.hibernate.boot.model.naming.spi.DiscriminatorColumnNamingInput;
import org.hibernate.boot.model.naming.spi.EmbeddableDiscriminatorColumnNamingInput;
import org.hibernate.boot.model.naming.spi.ImplicitNamingContext;
import org.hibernate.boot.model.naming.spi.PhysicalNamingContext;
import org.hibernate.boot.model.naming.spi.StandardImplicitNamingStrategy;
import org.hibernate.boot.pipeline.internal.SessionFactoryPipeline;
import org.hibernate.boot.pipeline.internal.source.MappingSources;
import org.hibernate.mapping.Component;
import org.hibernate.orm.test.boot.MetadataBuildingTestHelper;
import org.hibernate.relational.naming.spi.LogicalName;
import org.hibernate.relational.naming.spi.PhysicalName;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.boot.model.naming.spi.EmbeddableDiscriminatorColumnNamingInput.Kind.COLLECTION_ELEMENT;
import static org.hibernate.boot.model.naming.spi.EmbeddableDiscriminatorColumnNamingInput.Kind.EMBEDDED_ATTRIBUTE;

/// Verifies embeddable discriminator defaults, source precedence, naming inputs,
/// physical finalization, and runtime subtype loading.
///
/// @author Steve Ebersole
@BaseUnitTest
class ImplicitEmbeddableDiscriminatorNamingTest {
	private static MappingSources sources() {
		return new MappingSources().addManagedClasses( DirectOwner.class, NestedOwner.class,
				CollectionOwner.class, NestedCollectionOwner.class, Pet.class, Dog.class, Home.class );
	}

	@ParameterizedTest
	@MethodSource("org.hibernate.orm.test.namingstrategy.ImplicitNamingStrategyMatrixTest#cases")
	void suppliedStrategiesPreserveDefaults(ImplicitNamingStrategyMatrixTest.Case test) {
		try (var registry = ServiceRegistryUtil.serviceRegistry()) {
			final var metadata = MetadataBuildingTestHelper.buildMetadataWithNaming( registry, sources(),
					test.strategy().implementation, new ImplicitNamingStrategyMatrixTest.PhysicalStrategy( test.prefix() ) );
			assertThat( discriminator( component( metadata, DirectOwner.class, "pet" ) ).getName() ).isEqualTo( test.physical( "pet_DTYPE" ) );
			final var home = component( metadata, NestedOwner.class, "home" );
			assertThat( home.getDiscriminator() ).isNull();
			assertThat( discriminator( (Component) home.getProperty( "pet" ).getValue() ).getName() ).isEqualTo( test.physical( "pet_DTYPE" ) );
			final var pets = (Component) metadata.getCollectionBinding( CollectionOwner.class.getName() + ".pets" ).getElement();
			assertThat( discriminator( pets ).getName() ).isEqualTo( test.physical( "element_DTYPE" ) );
			final var homes = (Component) metadata.getCollectionBinding( NestedCollectionOwner.class.getName() + ".homes" ).getElement();
			assertThat( discriminator( (Component) homes.getProperty( "pet" ).getValue() ).getName() ).isEqualTo( test.physical( "pet_DTYPE" ) );
		}
	}

	@Test
	void customInputsDescribeEachUseAndPreserveQuoting() {
		try (var registry = ServiceRegistryUtil.serviceRegistry()) {
			final var implicit = new Strategy();
			final var physical = new Physical();
			final var metadata = MetadataBuildingTestHelper.buildMetadataWithNaming( registry, sources(), implicit, physical );
			assertThat( implicit.inputs ).extracting( EmbeddableDiscriminatorColumnNamingInput::attributePath )
					.containsExactlyInAnyOrder( "pet", "home.pet", "pets", "homes.pet" );
			assertThat( implicit.inputs ).allSatisfy( input -> {
				assertThat( input.embeddableTypeName() ).isEqualTo( Pet.class.getName() );
				assertThat( input.kind() ).isEqualTo( input.attributePath().equals( "pets" ) ? COLLECTION_ELEMENT : EMBEDDED_ATTRIBUTE );
				assertThat( input.declaration() ).isEqualTo( EmbeddableDiscriminatorColumnNamingInput.Declaration.ABSENT );
			} );
			assertThat( implicit.inputs ).filteredOn( input -> input.attributePath().equals( "home.pet" ) ).singleElement()
					.satisfies( input -> assertThat( input.entity().getClassName() ).isEqualTo( NestedOwner.class.getName() ) );
			assertThat( physical.inputs ).filteredOn( name -> name.getText().startsWith( "kind_" ) ).hasSize( 4 )
					.allSatisfy( name -> assertThat( name.isExplicit() ).isFalse() );
			final var column = discriminator( component( metadata, DirectOwner.class, "pet" ) );
			assertThat( column.getName() ).isEqualTo( "p_kind_pet" );
			assertThat( column.isQuoted() ).isTrue();
		}
	}

	@ParameterizedTest @ValueSource(booleans = { true, false })
	void annotationAndOverridePrecedence(boolean collection) {
		final Class<?> owner = collection ? CollectionPrecedenceOwner.class : PrecedenceOwner.class;
		try (var registry = ServiceRegistryUtil.serviceRegistry()) {
			final var implicit = new Defaults();
			final var physical = new Physical();
			final var metadata = MetadataBuildingTestHelper.buildMetadataWithNaming( registry,
					new MappingSources().addManagedClasses( owner, DefaultPet.class, DefaultDog.class,
							EmptyPet.class, EmptyDog.class, NamedPet.class, NamedDog.class ), implicit, physical );
			assertThat( implicit.inputs ).extracting( EmbeddableDiscriminatorColumnNamingInput::attributePath )
					.containsExactlyInAnyOrder( "empty", "overriddenEmpty" );
			assertThat( implicit.inputs ).filteredOn( input -> input.attributePath().equals( "empty" ) )
					.singleElement().satisfies( input -> assertThat( input.declaration() )
							.isEqualTo( EmbeddableDiscriminatorColumnNamingInput.Declaration.DISCRIMINATOR_COLUMN ) );
			assertThat( implicit.inputs ).filteredOn( input -> input.attributePath().equals( "overriddenEmpty" ) )
					.singleElement().satisfies( input -> assertThat( input.declaration() )
							.isEqualTo( EmbeddableDiscriminatorColumnNamingInput.Declaration.OVERRIDE ) );
			assertThat( discriminator( precedenceComponent( metadata, owner, collection, "defaulted" ) ).getName() ).isEqualTo( "p_DTYPE" );
			assertThat( discriminator( precedenceComponent( metadata, owner, collection, "empty" ) ).getName() ).isEqualTo( "p_DTYPE" );
			assertThat( discriminator( precedenceComponent( metadata, owner, collection, "named" ) ).getName() ).isEqualTo( "p_type_name" );
			assertThat( discriminator( precedenceComponent( metadata, owner, collection, "overridden" ) ).getName() ).isEqualTo( "p_override_name" );
			assertThat( discriminator( precedenceComponent( metadata, owner, collection, "overriddenEmpty" ) ).getName() ).isEqualTo( collection ? "p_element_DTYPE" : "p_overriddenEmpty_DTYPE" );
			assertThat( discriminator( precedenceComponent( metadata, owner, collection, "defaulted" ) ).getLength() ).isEqualTo( 64 );
			assertThat( physical.inputs ).filteredOn( name -> name.getText().equals( "DTYPE" ) )
					.extracting( LogicalName::isExplicit ).containsExactlyInAnyOrder( true, false );
		}
	}

	@Test
	void xmlOverridesAndGlobalQuoting() {
		try (var registry = ServiceRegistryUtil.serviceRegistryBuilder()
				.applySetting( "hibernate.globally_quoted_identifiers", true ).build()) {
			final var implicit = new Defaults();
			final var metadata = MetadataBuildingTestHelper.buildMetadataWithNaming( registry,
					new MappingSources().addManagedClasses( DirectOwner.class, NestedOwner.class, Home.class, Pet.class, Dog.class )
							.addMappingResource( "org/hibernate/orm/test/namingstrategy/embeddable-discriminator.orm.xml" ),
					implicit, new Physical() );
			assertThat( implicit.inputs ).singleElement().satisfies( input ->
					assertThat( input.attributePath() ).isEqualTo( "home.pet" ) );
			final var direct = discriminator( component( metadata, DirectOwner.class, "pet" ) );
			assertThat( direct.getName() ).isEqualTo( "p_xml_kind" );
			assertThat( direct.isQuoted() ).isTrue();
			final var home = component( metadata, NestedOwner.class, "home" );
			final var nested = discriminator( (Component) home.getProperty( "pet" ).getValue() );
			assertThat( nested.getName() ).isEqualTo( "p_pet_DTYPE" );
			assertThat( nested.isQuoted() ).isTrue();
			assertThat( nested.getLength() ).isEqualTo( 255 );
		}
	}

	@Test
	void logicalDiscriminatorNameCanBeUsedByAnIndex() {
		try (var registry = ServiceRegistryUtil.serviceRegistry()) {
			final var metadata = MetadataBuildingTestHelper.buildMetadataWithNaming( registry,
					new MappingSources().addManagedClasses( IndexedOwner.class, Pet.class, Dog.class ), new Strategy(), new Physical() );
			final var table = (org.hibernate.mapping.PhysicalTable) metadata.getEntityBinding( IndexedOwner.class.getName() ).getTable();
			assertThat( table.getIndexes().get( "pet_kind_lookup" ).getSelectables() )
					.containsExactly( discriminator( component( metadata, IndexedOwner.class, "pet" ) ) );
		}
	}

	@ParameterizedTest @ValueSource(booleans = { true, false })
	void rejectsInvalidResults(boolean returnNull) {
		try (var registry = ServiceRegistryUtil.serviceRegistry()) {
			assertThatThrownBy( () -> MetadataBuildingTestHelper.buildMetadataWithImplicitNaming( registry, sources(), new StandardImplicitNamingStrategy() {
				@Override
				public LogicalName determineEmbeddableDiscriminatorColumnName(EmbeddableDiscriminatorColumnNamingInput input, ImplicitNamingContext context) {
					return returnNull ? null : new LogicalName( "invalid", false, true );
				}
			} ) ).hasMessageContaining( "non-null implicit name for embeddable discriminator column" );
		}
	}

	@Test
	void customQuotedNamesWorkAtRuntime() {
		try (var registry = ServiceRegistryUtil.serviceRegistryBuilder()
				.applySetting( "jakarta.persistence.schema-generation.database.action", "create-drop" ).build()) {
			final var metadata = MetadataBuildingTestHelper.buildMetadataWithNaming( registry, sources(), new Strategy(), new Physical() );
			try (var factory = SessionFactoryPipeline.build( metadata, new SessionFactoryOptionsCollector() )) {
				try (var session = factory.openSession()) {
					final var tx = session.beginTransaction();
					final var direct = new DirectOwner(); direct.id = 1; direct.pet = new Dog(); direct.pet.label = "direct";
					session.persist( direct );
					final var collection = new CollectionOwner(); collection.id = 2;
					final var pet = new Pet(); pet.label = "base";
					final var dog = new Dog(); dog.label = "dog";
					collection.pets.add( pet ); collection.pets.add( dog );
					session.persist( collection );
					tx.commit();
				}
				try (var session = factory.openSession()) {
					assertThat( session.find( DirectOwner.class, 1L ).pet ).isInstanceOf( Dog.class );
					assertThat( session.find( CollectionOwner.class, 2L ).pets ).extracting( Object::getClass )
							.containsExactlyInAnyOrder( Pet.class, Dog.class );
				}
			}
		}
	}

	private static Component precedenceComponent(org.hibernate.boot.Metadata metadata, Class<?> owner, boolean collection, String property) {
		return collection ? (Component) metadata.getCollectionBinding( owner.getName() + "." + property ).getElement()
				: component( metadata, owner, property );
	}

	private static Component component(org.hibernate.boot.Metadata metadata, Class<?> owner, String property) {
		return (Component) metadata.getEntityBinding( owner.getName() ).getProperty( property ).getValue();
	}
	private static org.hibernate.mapping.Column discriminator(Component component) {
		return component.getDiscriminator().getColumns().get( 0 );
	}
	static class Defaults extends StandardImplicitNamingStrategy {
		final List<EmbeddableDiscriminatorColumnNamingInput> inputs = new ArrayList<>();
		@Override
		public LogicalName determineEmbeddableDiscriminatorColumnName(EmbeddableDiscriminatorColumnNamingInput input, ImplicitNamingContext context) {
			inputs.add( input );
			return super.determineEmbeddableDiscriminatorColumnName( input, context );
		}
		@Override
		public LogicalName determineDiscriminatorColumnName(DiscriminatorColumnNamingInput input, ImplicitNamingContext context) {
			throw new AssertionError( "Embeddable naming must not invoke entity discriminator naming" );
		}
	}
	static class Strategy extends Defaults {
		@Override
		public LogicalName determineEmbeddableDiscriminatorColumnName(EmbeddableDiscriminatorColumnNamingInput input, ImplicitNamingContext context) {
			super.determineEmbeddableDiscriminatorColumnName( input, context );
			return context.implicitName( "kind_" + input.attributePath().replace( '.', '_' ), true );
		}
	}
	static class Physical extends PhysicalNamingStrategyStandardImpl {
		final List<LogicalName> inputs = new ArrayList<>();
		@Override
		public PhysicalName toPhysicalColumnName(LogicalName name, PhysicalNamingContext context) {
			inputs.add( name );
			return context.getPhysicalNameFactory().create( "p_" + name.getText(), false );
		}
	}
	@Embeddable @DiscriminatorValue("pet") static class Pet { String label; }
	@Embeddable @DiscriminatorValue("dog") static class Dog extends Pet {}
	@Embeddable static class Home { @Embedded Pet pet; }
	@Entity @jakarta.persistence.Table(indexes = @jakarta.persistence.Index(name = "pet_kind_lookup", columnList = "`kind_pet`"))
	static class IndexedOwner { @Id long id; @Embedded Pet pet; }
	@Entity static class DirectOwner { @Id long id; @Embedded Pet pet; }
	@Entity static class NestedOwner { @Id long id; @Embedded Home home; }
	@Entity static class CollectionOwner { @Id long id; @ElementCollection List<Pet> pets = new ArrayList<>(); }
	@Entity static class NestedCollectionOwner { @Id long id; @ElementCollection List<Home> homes; }
	@Embeddable @DiscriminatorColumn(length = 64) static class DefaultPet { String label; }
	@Embeddable static class DefaultDog extends DefaultPet {}
	@Embeddable @DiscriminatorColumn(name = "") static class EmptyPet { String label; }
	@Embeddable static class EmptyDog extends EmptyPet {}
	@Embeddable @DiscriminatorColumn(name = "type_name") static class NamedPet { String label; }
	@Embeddable static class NamedDog extends NamedPet {}
	@Entity static class PrecedenceOwner {
		@Id long id;
		@Embedded DefaultPet defaulted;
		@Embedded EmptyPet empty;
		@Embedded NamedPet named;
		@Embedded @AttributeOverride(name = "{discriminator}", column = @Column(name = "override_name")) NamedPet overridden;
		@Embedded @AttributeOverride(name = "{discriminator}", column = @Column(name = "")) NamedPet overriddenEmpty;
	}
	@Entity static class CollectionPrecedenceOwner {
		@Id long id;
		@ElementCollection List<DefaultPet> defaulted;
		@ElementCollection List<EmptyPet> empty;
		@ElementCollection List<NamedPet> named;
		@ElementCollection @AttributeOverride(name = "{discriminator}", column = @Column(name = "override_name")) List<NamedPet> overridden;
		@ElementCollection @AttributeOverride(name = "{discriminator}", column = @Column(name = "")) List<NamedPet> overriddenEmpty;
	}

}
