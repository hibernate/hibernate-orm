/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.converted.converter.elementCollection;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Convert;
import jakarta.persistence.Converter;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.SimpleValue;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.hamcrest.CoreMatchers;

import static org.junit.Assert.assertThat;

/**
 * Similar to {@link CollectionCompositeElementConversionTest} except here we have an
 * explicit {@code @Convert} defined on the converted attribute
 *
 * @author Steve Ebersole
 */
@JiraKey( value = "HHH-10277" )
@ServiceRegistry
public class CollectionCompositeElementExplicitConversionTest {
	private Field simpleValueAttributeConverterDescriptorField;

	@BeforeAll
	public void setUp() throws Exception {
		simpleValueAttributeConverterDescriptorField = ReflectHelper.findField( SimpleValue.class, "attributeConverterDescriptor" );
	}

	@Test
	public void testCollectionOfEmbeddablesWithConvertedAttributes(ServiceRegistryScope scope) throws Exception {
		final MetadataImplementor metadata = (MetadataImplementor) new MetadataSources( scope.getRegistry() )
				.addAnnotatedClass( Disguise.class )
				.addAnnotatedClass( Traits.class )
				.buildMetadata();

		metadata.orderColumns( false );
		metadata.validate();

		final PersistentClass entityBinding = metadata.getEntityBinding( Disguise.class.getName() );

		// first check the singular composite...
		final Property singularTraitsProperty = entityBinding.getProperty( "singularTraits" );
		checkComposite( (Component) singularTraitsProperty.getValue() );

		// then check the plural composite...
		final Property pluralTraitsProperty = entityBinding.getProperty( "pluralTraits" );
		checkComposite( (Component) ( (org.hibernate.mapping.Set) pluralTraitsProperty.getValue() ).getElement() );

	}

	private void checkComposite(Component composite) throws Exception {
		// check `eyeColor`
		final Property eyeColorProperty = composite.getProperty( "eyeColor" );
		final SimpleValue eyeColorValueMapping = (SimpleValue) eyeColorProperty.getValue();
		assertThat( simpleValueAttributeConverterDescriptorField.get( eyeColorValueMapping ), CoreMatchers.notNullValue() );

		// check `hairColor`
		final Property hairColorProperty = composite.getProperty( "hairColor" );
		final SimpleValue hairColorValueMapping = (SimpleValue) hairColorProperty.getValue();
		assertThat( simpleValueAttributeConverterDescriptorField.get( hairColorValueMapping ), CoreMatchers.notNullValue() );

	}

	@Entity( name = "Disguise" )
	@Table( name = "DISGUISE" )
	public static class Disguise {
		@Id
		private Integer id;

		private Traits singularTraits;

		@ElementCollection(fetch = FetchType.EAGER)
		@CollectionTable(
				name = "DISGUISE_TRAIT",
				joinColumns = @JoinColumn(name = "DISGUISE_FK", nullable = false)
		)
		private Set<Traits> pluralTraits = new HashSet<Traits>();

		public Disguise() {
		}

		public Disguise(Integer id) {
			this.id = id;
		}
	}

	@Embeddable
	public static class Traits {
		@Convert(converter = ColorTypeConverter.class)
		public ColorType eyeColor;
		@Convert(converter = ColorTypeConverter.class)
		public ColorType hairColor;

		public Traits() {
		}

		public Traits(ColorType eyeColor, ColorType hairColor) {
			this.eyeColor = eyeColor;
			this.hairColor = hairColor;
		}
	}

	public static class ColorType implements Serializable {
		public static ColorType BLUE = new ColorType( "blue" );
		public static ColorType RED = new ColorType( "red" );
		public static ColorType YELLOW = new ColorType( "yellow" );

		private final String color;

		public ColorType(String color) {
			this.color = color;
		}

		public String toExternalForm() {
			return color;
		}

		public static ColorType fromExternalForm(String color) {
			if ( BLUE.color.equals( color ) ) {
				return BLUE;
			}
			else if ( RED.color.equals( color ) ) {
				return RED;
			}
			else if ( YELLOW.color.equals( color ) ) {
				return YELLOW;
			}
			else {
				throw new RuntimeException( "Unknown color : " + color );
			}
		}
	}

	@Converter( autoApply = false )
	public static class ColorTypeConverter implements AttributeConverter<ColorType, String> {
		@Override
		public String convertToDatabaseColumn(ColorType attribute) {
			return attribute == null ? null : attribute.toExternalForm();
		}

		@Override
		public ColorType convertToEntityAttribute(String dbData) {
			return ColorType.fromExternalForm( dbData );
		}
	}

}
