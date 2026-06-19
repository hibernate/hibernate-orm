/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.models.bind.embeddable;

import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.Join;
import org.hibernate.mapping.ManyToOne;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.boot.mapping.internal.model.AttributeDeclarationBinding;
import org.hibernate.boot.mapping.internal.model.AttributeUsageBinding;
import org.hibernate.boot.mapping.internal.model.BasicValueIntent;
import org.hibernate.boot.mapping.internal.model.CollectionValueIntent;
import org.hibernate.boot.mapping.internal.model.ComponentMemberBinding;
import org.hibernate.boot.mapping.internal.model.EmbeddableAttributeDeclarationBinding;
import org.hibernate.boot.mapping.internal.model.EmbeddedValueIntent;
import org.hibernate.boot.mapping.internal.model.ToOneValueIntent;
import org.hibernate.boot.mapping.internal.sources.ComponentSource;
import org.hibernate.orm.test.boot.models.bind.BindingTestingHelper;

import org.hibernate.annotations.Collate;
import org.hibernate.annotations.EmbeddedTable;
import org.hibernate.annotations.Formula;
import org.hibernate.boot.models.AttributeNature;
import org.hibernate.metamodel.CollectionClassification;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.AssociationOverride;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.SecondaryTable;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Steve Ebersole
 */
public class EmbeddableBindingTests {
	@Test
	@ServiceRegistry
	void testComponentMemberBindingFacts(ServiceRegistryScope scope) {
		BindingTestingHelper.checkDomainModel(
				(context) -> {
					final var contribution = context.getBindingState().getBootBindingModel()
							.embeddableContributions()
							.stream()
							.filter( (candidate) -> candidate.componentType().getName().equals( ComponentFacts.class.getName() ) )
							.findFirst()
							.orElseThrow();

					final ComponentMemberBinding city = componentMember( contribution.members(), "city" );
					assertThat( city.nature() ).isEqualTo( AttributeNature.BASIC );
					assertThat( city ).isInstanceOf( AttributeUsageBinding.class );
					assertThat( city ).isNotInstanceOf( AttributeDeclarationBinding.class );
					assertThat( city.declaration() ).isInstanceOf( EmbeddableAttributeDeclarationBinding.class );
					assertThat( city.declaration().attributeName() ).isEqualTo( "city" );
					assertThat( city.valueIntent() ).isSameAs( city.basicValueIntent() );
					assertThat( city.basicValueIntent().columnSource().name() ).isEqualTo( "home_city" );

					final ComponentMemberBinding code = componentMember( contribution.members(), "code" );
					assertThat( code.basicValueIntent().conversion() ).isNotNull();

					final ComponentMemberBinding formula = componentMember( contribution.members(), "cityFormula" );
					assertThat( formula.basicValueIntent().isFormula() ).isTrue();
					assertThat( formula.basicValueIntent().formulaExpression() ).isEqualTo( "upper(city)" );

					final ComponentMemberBinding collated = componentMember( contribution.members(), "collated" );
					assertThat( collated.collation() ).isEqualTo( "ucs_basic" );

					final ComponentMemberBinding country = componentMember( contribution.members(), "country" );
					assertThat( country.nature() ).isEqualTo( AttributeNature.TO_ONE );
					assertThat( country.associationOverride() ).isNotNull();
					assertThat( country.valueIntent() ).isSameAs( country.toOneValueIntent() );
					assertThat( country.toOneValueIntent().memberType().determineRawClass().toJavaClass() )
							.isEqualTo( Country.class );
					assertThat( country.toOneValueIntent().path() ).isEqualTo( "country" );
					assertThat( country.toOneValueIntent().fullPath() ).isEqualTo( "facts.country" );
					assertThat( country.toOneValueIntent().associationOverride() ).isNotNull();

					final PersistentClass entityBinding = context.getMetadataCollector()
							.getEntityBinding( ComponentFactsEntity.class.getName() );
					final Component facts = (Component) entityBinding.getProperty( "facts" ).getValue();
					assertThat( facts.getProperty( "city" ).getValue().getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "home_city" );
					assertThat( ( (BasicValue) facts.getProperty( "code" ).getValue() )
							.getJpaAttributeConverterDescriptor() ).isNotNull();
					assertThat( ( (BasicValue) facts.getProperty( "cityFormula" ).getValue() )
							.getSelectables().get( 0 ).getText() ).isEqualTo( "upper(city)" );
					assertThat( ( (ManyToOne) facts.getProperty( "country" ).getValue() )
							.getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "country_fk" );
				},
				scope.getRegistry(),
				Country.class,
				ComponentFactsEntity.class
		);
	}

	private static ComponentMemberBinding componentMember(
			java.util.List<ComponentMemberBinding> members,
			String attributeName) {
		return members.stream()
				.filter( (member) -> member.attributeName().equals( attributeName ) )
				.findFirst()
				.orElseThrow();
	}

	@Test
	@ServiceRegistry
	void testComponentPluralMemberValueIntents(ServiceRegistryScope scope) {
		BindingTestingHelper.checkDomainModel(
				(context) -> {
					final var contribution = context.getBindingState().getBootBindingModel()
							.embeddableContributions()
							.stream()
							.filter( (candidate) -> candidate.componentType().getName().equals( ComponentPluralFacts.class.getName() ) )
							.findFirst()
							.orElseThrow();

					final ComponentMemberBinding labels = componentMember( contribution.members(), "labels" );
					assertThat( labels.nature() ).isEqualTo( AttributeNature.ELEMENT_COLLECTION );
					assertThat( labels.valueIntent() ).isSameAs( labels.collectionValueIntent() );
					assertThat( labels.collectionValueIntent().classification() ).isEqualTo( CollectionClassification.LIST );
					assertThat( labels.collectionValueIntent().elementIntent() ).isInstanceOf( BasicValueIntent.class );
					assertThat( labels.collectionValueIntent().indexIntent() ).isInstanceOf( BasicValueIntent.class );
					assertThat( labels.collectionValueIntent().sourceRole() ).isEqualTo( "facts.labels" );
					assertThat( labels.collectionValueIntent().attributePath() ).isEqualTo( "labels" );

					final ComponentMemberBinding parts = componentMember( contribution.members(), "parts" );
					assertThat( parts.nature() ).isEqualTo( AttributeNature.ELEMENT_COLLECTION );
					assertThat( parts.collectionValueIntent().elementIntent() ).isInstanceOf( EmbeddedValueIntent.class );

					final ComponentMemberBinding children = componentMember( contribution.members(), "children" );
					assertThat( children.nature() ).isEqualTo( AttributeNature.ONE_TO_MANY );
					assertThat( children.collectionValueIntent().elementIntent() ).isInstanceOf( ToOneValueIntent.class );

					final ComponentMemberBinding tags = componentMember( contribution.members(), "tags" );
					final CollectionValueIntent tagsIntent = tags.collectionValueIntent();
					assertThat( tags.nature() ).isEqualTo( AttributeNature.MANY_TO_MANY );
					assertThat( tagsIntent.elementIntent() ).isInstanceOf( ToOneValueIntent.class );
					assertThat( tagsIntent.classification() ).isEqualTo( CollectionClassification.SET );
				},
				scope.getRegistry(),
				ComponentPluralOwner.class,
				ComponentPluralChild.class,
				ComponentPluralTag.class
		);
	}

	@Test
	@ServiceRegistry
	void testEmbeddedIdentifierRejectsPluralMember(ServiceRegistryScope scope) {
		assertThatThrownBy( () -> BindingTestingHelper.checkDomainModel(
				(context) -> {
				},
				scope.getRegistry(),
				EmbeddedIdentifierWithPluralEntity.class
		) )
				.isInstanceOf( org.hibernate.AnnotationException.class )
				.hasMessageContaining( "embeddables used as entity identifiers may not contain plural attributes" );
	}

	@Test
	@ServiceRegistry
	void testGenericMappedSuperclassMemberResolvesAtEmbeddedSite(ServiceRegistryScope scope) {
		BindingTestingHelper.checkDomainModel(
				(context) -> {
					final var contribution = context.getBindingState().getBootBindingModel()
							.embeddableContributions()
							.stream()
							.filter( (candidate) -> candidate.componentType().getName().equals( GenericAmount.class.getName() ) )
							.findFirst()
							.orElseThrow();

					final ComponentMemberBinding amount = componentMember( contribution.members(), "amount" );
					assertThat( amount ).isInstanceOf( AttributeUsageBinding.class );
					assertThat( amount ).isNotInstanceOf( AttributeDeclarationBinding.class );
					assertThat( amount.declaration().attributeName() ).isEqualTo( "amount" );
					assertThat( amount.resolvedType().determineRawClass().toJavaClass() )
							.isEqualTo( java.math.BigDecimal.class );
					assertThat( amount.path() ).isEqualTo( "amount" );
					assertThat( amount.fullPath() ).isEqualTo( "price.amount" );
				},
				scope.getRegistry(),
				GenericAmountEntity.class
		);
	}

	@Test
	@ServiceRegistry
	void testExplicitEmbedded(ServiceRegistryScope scope) {
		BindingTestingHelper.checkDomainModel(
				(context) -> {
					final PersistentClass entityBinding = context.getMetadataCollector()
							.getEntityBinding( ExplicitEmbeddedEntity.class.getName() );
					final org.hibernate.mapping.Property property = entityBinding.getProperty( "address" );
					assertThat( property.getValue() ).isInstanceOf( Component.class );
					final Component component = (Component) property.getValue();

					assertThat( component.isFlattened() ).isFalse();
					assertThat( component.getComponentClassName() ).isEqualTo( Address.class.getName() );
					assertThat( component.getProperties() )
							.extracting( org.hibernate.mapping.Property::getName )
							.containsExactly( "line1", "zipCode" );
					assertThat( component.getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "line1", "zipCode" );

					final var contributions = context.getBindingState().getBootBindingModel()
							.embeddableContributions();
					assertThat( contributions ).hasSize( 1 );
					final var contribution = contributions.get( 0 );
					assertThat( contribution.kind() ).isEqualTo( ComponentSource.Kind.EMBEDDED_ATTRIBUTE );
					assertThat( contribution.sourceMember().resolveAttributeName() ).isEqualTo( "address" );
					assertThat( contribution.componentType().toJavaClass() ).isEqualTo( Address.class );
					assertThat( contribution.members() )
							.extracting( ComponentMemberBinding::attributeName )
							.containsExactly( "line1", "zipCode" );
				},
				scope.getRegistry(),
				ExplicitEmbeddedEntity.class
		);
	}

	@Test
	@ServiceRegistry
	void testImplicitEmbedded(ServiceRegistryScope scope) {
		BindingTestingHelper.checkDomainModel(
				(context) -> {
					final PersistentClass entityBinding = context.getMetadataCollector()
							.getEntityBinding( ImplicitEmbeddedEntity.class.getName() );
					final org.hibernate.mapping.Property property = entityBinding.getProperty( "address" );
					assertThat( property.getValue() ).isInstanceOf( Component.class );
					final Component component = (Component) property.getValue();

					assertThat( component.getComponentClassName() ).isEqualTo( Address.class.getName() );
					assertThat( component.getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "line1", "zipCode" );
				},
				scope.getRegistry(),
				ImplicitEmbeddedEntity.class
		);
	}

	@Test
	@ServiceRegistry
	void testAttributeOverride(ServiceRegistryScope scope) {
		BindingTestingHelper.checkDomainModel(
				(context) -> {
					final PersistentClass entityBinding = context.getMetadataCollector()
							.getEntityBinding( OverrideEmbeddedEntity.class.getName() );
					final org.hibernate.mapping.Property property = entityBinding.getProperty( "address" );
					final Component component = (Component) property.getValue();

					assertThat( component.getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "street", "postal_code" );
				},
				scope.getRegistry(),
				OverrideEmbeddedEntity.class
		);
	}

	@Test
	@ServiceRegistry
	void testEmbeddedOnSecondaryTable(ServiceRegistryScope scope) {
		BindingTestingHelper.checkDomainModel(
				(context) -> {
					final PersistentClass entityBinding = context.getMetadataCollector()
							.getEntityBinding( SecondaryTableEmbeddedEntity.class.getName() );
					assertThat( entityBinding.getJoins() ).hasSize( 1 );
					final Join join = entityBinding.getJoins().get( 0 );
					assertThat( entityBinding.getUnjoinedProperties() )
							.extracting( org.hibernate.mapping.Property::getName )
							.isEmpty();
					assertThat( join.getProperties() )
							.extracting( org.hibernate.mapping.Property::getName )
							.containsExactly( "address" );

					final Component component = (Component) join.getProperties().get( 0 ).getValue();
					assertThat( component.getTable() ).isSameAs( join.getTable() );
					assertThat( component.getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "street", "postal_code" );
				},
				scope.getRegistry(),
				SecondaryTableEmbeddedEntity.class
		);
	}

	@Test
	@ServiceRegistry
	void testEmbeddedTable(ServiceRegistryScope scope) {
		BindingTestingHelper.checkDomainModel(
				(context) -> {
					final PersistentClass entityBinding = context.getMetadataCollector()
							.getEntityBinding( EmbeddedTableEntity.class.getName() );
					assertThat( entityBinding.getJoins() ).hasSize( 1 );
					final Join join = entityBinding.getJoins().get( 0 );
					assertThat( entityBinding.getUnjoinedProperties() )
							.extracting( org.hibernate.mapping.Property::getName )
							.isEmpty();
					assertThat( join.getProperties() )
							.extracting( org.hibernate.mapping.Property::getName )
							.containsExactly( "address" );

					final Component component = (Component) join.getProperties().get( 0 ).getValue();
					assertThat( component.getTable() ).isSameAs( join.getTable() );
					assertThat( component.getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "line1", "zipCode" );
				},
				scope.getRegistry(),
				EmbeddedTableEntity.class
		);
	}

	@Test
	@ServiceRegistry
	void testNestedEmbedded(ServiceRegistryScope scope) {
		BindingTestingHelper.checkDomainModel(
				(context) -> {
					final PersistentClass entityBinding = context.getMetadataCollector()
							.getEntityBinding( NestedEmbeddedEntity.class.getName() );
					final Component address = (Component) entityBinding.getProperty( "address" ).getValue();
					assertThat( address.getProperties() )
							.extracting( org.hibernate.mapping.Property::getName )
							.containsExactly( "line1", "location", "zipCode" );

					final Component location = (Component) address.getProperty( "location" ).getValue();
					assertThat( location.getComponentClassName() ).isEqualTo( Location.class.getName() );
					assertThat( location.getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "city", "country" );
					assertThat( address.getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "line1", "city", "country", "zipCode" );

					final var contributions = context.getBindingState().getBootBindingModel()
							.embeddableContributions();
					assertThat( contributions )
							.extracting( (contribution) -> contribution.componentType().getName() )
							.containsExactly( AddressWithLocation.class.getName(), Location.class.getName() );
					final ComponentMemberBinding locationMember = componentMember( contributions.get( 0 ).members(), "location" );
					assertThat( locationMember.nature() ).isEqualTo( AttributeNature.EMBEDDED );
					assertThat( locationMember.valueIntent() ).isSameAs( locationMember.embeddedValueIntent() );
					assertThat( locationMember.path() ).isEqualTo( "location" );
					assertThat( locationMember.namingPath().getFullPath() ).isEqualTo( "address.location" );
					assertThat( locationMember.fullPath() ).isEqualTo( "address.location" );
					assertThat( locationMember.embeddedValueIntent().memberType().determineRawClass().toJavaClass() )
							.isEqualTo( Location.class );
					assertThat( locationMember.embeddedValueIntent().path() ).isEqualTo( "location" );
					assertThat( locationMember.embeddedValueIntent().fullPath() ).isEqualTo( "address.location" );
					assertThat( contributions.get( 1 ).pathPrefix() ).isEqualTo( "location." );
					assertThat( contributions.get( 1 ).namingPathPrefix() ).isEqualTo( "address.location." );
				},
				scope.getRegistry(),
				NestedEmbeddedEntity.class
		);
	}

	@Test
	@ServiceRegistry
	void testNestedAttributeOverride(ServiceRegistryScope scope) {
		BindingTestingHelper.checkDomainModel(
				(context) -> {
					final PersistentClass entityBinding = context.getMetadataCollector()
							.getEntityBinding( NestedOverrideEmbeddedEntity.class.getName() );
					final Component address = (Component) entityBinding.getProperty( "address" ).getValue();
					final Component location = (Component) address.getProperty( "location" ).getValue();

					assertThat( location.getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "home_city", "home_country" );
					assertThat( address.getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "line1", "home_city", "home_country", "zipCode" );
				},
				scope.getRegistry(),
				NestedOverrideEmbeddedEntity.class
		);
	}

	@Test
	@ServiceRegistry
	void testNestedConvert(ServiceRegistryScope scope) {
		BindingTestingHelper.checkDomainModel(
				(context) -> {
					final PersistentClass entityBinding = context.getMetadataCollector()
							.getEntityBinding( NestedConvertEmbeddedEntity.class.getName() );
					final Component address = (Component) entityBinding.getProperty( "address" ).getValue();
					final Component location = (Component) address.getProperty( "location" ).getValue();
					final BasicValue city = (BasicValue) location.getProperty( "city" ).getValue();
					final BasicValue country = (BasicValue) location.getProperty( "country" ).getValue();

					assertThat( city.getJpaAttributeConverterDescriptor() ).isNotNull();
					assertThat( country.getJpaAttributeConverterDescriptor() ).isNotNull();
				},
				scope.getRegistry(),
				NestedConvertEmbeddedEntity.class
		);
	}

	@Test
	@ServiceRegistry
	void testEmbeddedManyToOne(ServiceRegistryScope scope) {
		BindingTestingHelper.checkDomainModel(
				(context) -> {
					final PersistentClass entityBinding = context.getMetadataCollector()
							.getEntityBinding( EmbeddedAssociationEntity.class.getName() );
					final Component address = (Component) entityBinding.getProperty( "address" ).getValue();
					final ManyToOne country = (ManyToOne) address.getProperty( "country" ).getValue();

					assertThat( country.getReferencedEntityName() ).isEqualTo( Country.class.getName() );
					assertThat( country.getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "country_id" );
				},
				scope.getRegistry(),
				Country.class,
				EmbeddedAssociationEntity.class
		);
	}

	@Test
	@ServiceRegistry
	void testNestedAssociationOverride(ServiceRegistryScope scope) {
		BindingTestingHelper.checkDomainModel(
				(context) -> {
					final PersistentClass entityBinding = context.getMetadataCollector()
							.getEntityBinding( NestedAssociationOverrideEntity.class.getName() );
					final Component address = (Component) entityBinding.getProperty( "address" ).getValue();
					final Component location = (Component) address.getProperty( "location" ).getValue();
					final ManyToOne country = (ManyToOne) location.getProperty( "country" ).getValue();

					assertThat( country.getReferencedEntityName() ).isEqualTo( Country.class.getName() );
					assertThat( country.getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "home_country_id" );
				},
				scope.getRegistry(),
				Country.class,
				NestedAssociationOverrideEntity.class
		);
	}

	@Test
	@ServiceRegistry
	void testNestedCompositeAssociationOverride(ServiceRegistryScope scope) {
		BindingTestingHelper.checkDomainModel(
				(context) -> {
					final PersistentClass entityBinding = context.getMetadataCollector()
							.getEntityBinding( NestedCompositeAssociationOverrideEntity.class.getName() );
					final Component address = (Component) entityBinding.getProperty( "address" ).getValue();
					final Component location = (Component) address.getProperty( "location" ).getValue();
					final ManyToOne country = (ManyToOne) location.getProperty( "country" ).getValue();

					assertThat( country.getReferencedEntityName() ).isEqualTo( CompositeCountry.class.getName() );
					assertThat( country.getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "home_country_code", "home_country_region" );
				},
				scope.getRegistry(),
				CompositeCountry.class,
				NestedCompositeAssociationOverrideEntity.class
		);
	}

	@Test
	@ServiceRegistry
	void testAssociationOverrideJoinTable(ServiceRegistryScope scope) {
		BindingTestingHelper.checkDomainModel(
				(context) -> {
					final PersistentClass entityBinding = context.getMetadataCollector()
							.getEntityBinding( AssociationOverrideJoinTableEntity.class.getName() );
					final Component address = (Component) entityBinding.getProperty( "address" ).getValue();
					final ManyToOne country = (ManyToOne) address.getProperty( "country" ).getValue();
					final Join join = entityBinding.getJoins().get( 0 );

					assertThat( join.getTable().getName() ).isEqualTo( "embedded_country_links" );
					assertThat( join.getKey().getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "owner_id" );
					assertThat( country.getTable() ).isSameAs( join.getTable() );
					assertThat( country.getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "country_id" );
				},
				scope.getRegistry(),
				Country.class,
				AssociationOverrideJoinTableEntity.class
		);
	}

	@Test
	@ServiceRegistry
	void testNestedAssociationOverrideJoinTable(ServiceRegistryScope scope) {
		BindingTestingHelper.checkDomainModel(
				(context) -> {
					final PersistentClass entityBinding = context.getMetadataCollector()
							.getEntityBinding( NestedAssociationOverrideJoinTableEntity.class.getName() );
					final Component address = (Component) entityBinding.getProperty( "address" ).getValue();
					final Component location = (Component) address.getProperty( "location" ).getValue();
					final ManyToOne country = (ManyToOne) location.getProperty( "country" ).getValue();
					final Join join = entityBinding.getJoins().get( 0 );

					assertThat( join.getTable().getName() ).isEqualTo( "nested_embedded_country_links" );
					assertThat( join.getKey().getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "owner_id" );
					assertThat( country.getTable() ).isSameAs( join.getTable() );
					assertThat( country.getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "country_id" );
				},
				scope.getRegistry(),
				Country.class,
				NestedAssociationOverrideJoinTableEntity.class
		);
	}

	@Test
	@ServiceRegistry
	void testNestedCompositeAssociationOverrideJoinTable(ServiceRegistryScope scope) {
		BindingTestingHelper.checkDomainModel(
				(context) -> {
					final PersistentClass entityBinding = context.getMetadataCollector()
							.getEntityBinding( NestedCompositeAssociationOverrideJoinTableEntity.class.getName() );
					final Component address = (Component) entityBinding.getProperty( "address" ).getValue();
					final Component location = (Component) address.getProperty( "location" ).getValue();
					final ManyToOne country = (ManyToOne) location.getProperty( "country" ).getValue();
					final Join join = entityBinding.getJoins().get( 0 );

					assertThat( join.getTable().getName() ).isEqualTo( "nested_embedded_composite_country_links" );
					assertThat( join.getKey().getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "owner_id" );
					assertThat( country.getTable() ).isSameAs( join.getTable() );
					assertThat( country.getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "country_code", "country_region" );
				},
				scope.getRegistry(),
				CompositeCountry.class,
				NestedCompositeAssociationOverrideJoinTableEntity.class
		);
	}

	@Entity(name = "ExplicitEmbeddedEntity")
	@Table(name = "explicit_embedded")
	public static class ExplicitEmbeddedEntity {
		@Id
		private Integer id;
		@Embedded
		private Address address;
	}

	@Entity(name = "ImplicitEmbeddedEntity")
	@Table(name = "implicit_embedded")
	public static class ImplicitEmbeddedEntity {
		@Id
		private Integer id;
		private Address address;
	}

	@Entity(name = "OverrideEmbeddedEntity")
	@Table(name = "override_embedded")
	public static class OverrideEmbeddedEntity {
		@Id
		private Integer id;
		@Embedded
		@AttributeOverride(name = "line1", column = @Column(name = "street"))
		@AttributeOverride(name = "zipCode", column = @Column(name = "postal_code"))
		private Address address;
	}

	@Entity(name = "SecondaryTableEmbeddedEntity")
	@Table(name = "secondary_table_embedded")
	@SecondaryTable(name = "secondary_table_embedded_details")
	public static class SecondaryTableEmbeddedEntity {
		@Id
		private Integer id;
		@Embedded
		@AttributeOverride(name = "line1", column = @Column(name = "street", table = "secondary_table_embedded_details"))
		@AttributeOverride(name = "zipCode", column = @Column(name = "postal_code", table = "secondary_table_embedded_details"))
		private Address address;
	}

	@Entity(name = "EmbeddedTableEntity")
	@Table(name = "embedded_table_entities")
	@SecondaryTable(name = "embedded_table_entity_details")
	public static class EmbeddedTableEntity {
		@Id
		private Integer id;
		@Embedded
		@EmbeddedTable("embedded_table_entity_details")
		private Address address;
	}

	@Entity(name = "NestedEmbeddedEntity")
	@Table(name = "nested_embedded")
	public static class NestedEmbeddedEntity {
		@Id
		private Integer id;
		@Embedded
		private AddressWithLocation address;
	}

	@Entity(name = "NestedOverrideEmbeddedEntity")
	@Table(name = "nested_override_embedded")
	public static class NestedOverrideEmbeddedEntity {
		@Id
		private Integer id;
		@Embedded
		@AttributeOverride(name = "location.city", column = @Column(name = "home_city"))
		@AttributeOverride(name = "location.country", column = @Column(name = "home_country"))
		private AddressWithLocation address;
	}

	@Entity(name = "NestedConvertEmbeddedEntity")
	@Table(name = "nested_convert_embedded")
	public static class NestedConvertEmbeddedEntity {
		@Id
		private Integer id;
		@Embedded
		@Convert(attributeName = "location.city", converter = CityConverter.class)
		private AddressWithConvertedLocation address;
	}

	@Entity(name = "EmbeddedAssociationEntity")
	@Table(name = "embedded_association")
	public static class EmbeddedAssociationEntity {
		@Id
		private Integer id;
		@Embedded
		private AddressWithCountry address;
	}

	@Entity(name = "NestedAssociationOverrideEntity")
	@Table(name = "nested_association_override")
	public static class NestedAssociationOverrideEntity {
		@Id
		private Integer id;
		@Embedded
		@AssociationOverride(name = "location.country", joinColumns = @JoinColumn(name = "home_country_id"))
		private AddressWithAssociationLocation address;
	}

	@Entity(name = "NestedCompositeAssociationOverrideEntity")
	@Table(name = "nested_composite_association_override")
	public static class NestedCompositeAssociationOverrideEntity {
		@Id
		private Integer id;
		@Embedded
		@AssociationOverride(name = "location.country", joinColumns = {
				@JoinColumn(name = "home_country_region", referencedColumnName = "region"),
				@JoinColumn(name = "home_country_code", referencedColumnName = "code")
		})
		private AddressWithCompositeAssociationLocation address;
	}

	@Entity(name = "AssociationOverrideJoinTableEntity")
	@Table(name = "association_override_join_table")
	public static class AssociationOverrideJoinTableEntity {
		@Id
		private Integer id;
		@Embedded
		@AssociationOverride(name = "country", joinTable = @JoinTable(
				name = "embedded_country_links",
				joinColumns = @JoinColumn(name = "owner_id", referencedColumnName = "id"),
				inverseJoinColumns = @JoinColumn(name = "country_id", referencedColumnName = "id")
		))
		private AddressWithCountry address;
	}

	@Entity(name = "NestedAssociationOverrideJoinTableEntity")
	@Table(name = "nested_association_override_join_table")
	public static class NestedAssociationOverrideJoinTableEntity {
		@Id
		private Integer id;
		@Embedded
		@AssociationOverride(name = "location.country", joinTable = @JoinTable(
				name = "nested_embedded_country_links",
				joinColumns = @JoinColumn(name = "owner_id", referencedColumnName = "id"),
				inverseJoinColumns = @JoinColumn(name = "country_id", referencedColumnName = "id")
		))
		private AddressWithAssociationLocation address;
	}

	@Entity(name = "NestedCompositeAssociationOverrideJoinTableEntity")
	@Table(name = "nested_composite_association_override_join_table")
	public static class NestedCompositeAssociationOverrideJoinTableEntity {
		@Id
		private Integer id;
		@Embedded
		@AssociationOverride(name = "location.country", joinTable = @JoinTable(
				name = "nested_embedded_composite_country_links",
				joinColumns = @JoinColumn(name = "owner_id", referencedColumnName = "id"),
				inverseJoinColumns = {
						@JoinColumn(name = "country_region", referencedColumnName = "region"),
						@JoinColumn(name = "country_code", referencedColumnName = "code")
				}
		))
		private AddressWithCompositeAssociationLocation address;
	}

	@Entity(name = "ComponentFactsEntity")
	@Table(name = "component_facts")
	public static class ComponentFactsEntity {
		@Id
		private Integer id;
		@Embedded
		@AttributeOverride(name = "city", column = @Column(name = "home_city"))
		@AssociationOverride(name = "country", joinColumns = @JoinColumn(name = "country_fk"))
		private ComponentFacts facts;
	}

	@Entity(name = "GenericAmountEntity")
	@Table(name = "generic_amount_entities")
	public static class GenericAmountEntity {
		@Id
		private Integer id;
		@Embedded
		private GenericAmount price;
	}

	@Entity(name = "ComponentPluralOwner")
	@Table(name = "component_plural_owners")
	public static class ComponentPluralOwner {
		@Id
		private Integer id;
		@Embedded
		private ComponentPluralFacts facts;
	}

	@Entity(name = "ComponentPluralChild")
	@Table(name = "component_plural_children")
	public static class ComponentPluralChild {
		@Id
		private Integer id;
	}

	@Entity(name = "ComponentPluralTag")
	@Table(name = "component_plural_tags")
	public static class ComponentPluralTag {
		@Id
		private Integer id;
	}

	@Entity(name = "Country")
	@Table(name = "countries")
	public static class Country {
		@Id
		private Integer id;
		private String name;
	}

	@Entity(name = "CompositeCountry")
	@Table(name = "composite_countries")
	public static class CompositeCountry {
		@EmbeddedId
		private CompositeCountryPk id;
		private String name;
	}

	@Embeddable
	public static class Address {
		private String line1;
		private String zipCode;
	}

	@Embeddable
	public static class AddressWithLocation {
		private String line1;
		private String zipCode;
		private Location location;
	}

	@Embeddable
	public static class Location {
		private String city;
		private String country;
	}

	@Embeddable
	public static class AddressWithConvertedLocation {
		private String line1;
		private String zipCode;
		private ConvertedLocation location;
	}

	@Embeddable
	public static class ConvertedLocation {
		private String city;
		@Convert(converter = CountryConverter.class)
		private String country;
	}

	@Embeddable
	public static class AddressWithCountry {
		private String line1;
		@jakarta.persistence.ManyToOne
		private Country country;
	}

	@Embeddable
	public static class AddressWithAssociationLocation {
		private String line1;
		private AssociationLocation location;
	}

	@Embeddable
	public static class AssociationLocation {
		private String city;
		@jakarta.persistence.ManyToOne
		private Country country;
	}

	@Embeddable
	public static class AddressWithCompositeAssociationLocation {
		private String line1;
		private CompositeAssociationLocation location;
	}

	@Embeddable
	public static class CompositeAssociationLocation {
		private String city;
		@jakarta.persistence.ManyToOne
		private CompositeCountry country;
	}

	@Embeddable
	public static class CompositeCountryPk {
		private String code;
		private String region;
	}

	@Embeddable
	public static class ComponentFacts {
		private String city;
		@Convert(converter = CountryConverter.class)
		private String code;
		@Formula("upper(city)")
		private String cityFormula;
		@Collate("ucs_basic")
		private String collated;
		@jakarta.persistence.ManyToOne
		private Country country;
	}

	@Embeddable
	public static class ComponentPluralFacts {
		@ElementCollection
		private java.util.List<String> labels;
		@ElementCollection
		private java.util.List<ComponentPluralPart> parts;
		@OneToMany
		private java.util.List<ComponentPluralChild> children;
		@ManyToMany
		private java.util.Set<ComponentPluralTag> tags;
	}

	@Embeddable
	public static class ComponentPluralPart {
		private String name;
	}

	@Entity(name = "EmbeddedIdentifierWithPluralEntity")
	@Table(name = "embedded_identifier_with_plural")
	public static class EmbeddedIdentifierWithPluralEntity {
		@EmbeddedId
		private EmbeddedIdentifierWithPlural id;
	}

	@Embeddable
	public static class EmbeddedIdentifierWithPlural {
		private Integer id;
		@ElementCollection
		private java.util.List<String> labels;
	}

	@MappedSuperclass
	public static class GenericAmountBase<T extends Number> {
		private T amount;
	}

	@Embeddable
	public static class GenericAmount extends GenericAmountBase<java.math.BigDecimal> {
	}

	public static class CityConverter implements AttributeConverter<String, String> {
		@Override
		public String convertToDatabaseColumn(String attribute) {
			return attribute;
		}

		@Override
		public String convertToEntityAttribute(String dbData) {
			return dbData;
		}
	}

	public static class CountryConverter implements AttributeConverter<String, String> {
		@Override
		public String convertToDatabaseColumn(String attribute) {
			return attribute;
		}

		@Override
		public String convertToEntityAttribute(String dbData) {
			return dbData;
		}
	}
}
