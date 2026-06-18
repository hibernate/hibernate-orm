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
import org.hibernate.boot.models.bind.internal.sources.ComponentSource;
import org.hibernate.orm.test.boot.models.bind.BindingTestingHelper;

import org.hibernate.annotations.EmbeddedTable;
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
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.SecondaryTable;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Steve Ebersole
 */
public class EmbeddableBindingTests {
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
							.extracting( ComponentSource.ComponentMember::attributeName )
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
