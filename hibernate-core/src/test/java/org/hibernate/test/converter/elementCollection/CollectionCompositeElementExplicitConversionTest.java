/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.converter.elementCollection;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.AttributeConverter;
import javax.persistence.CollectionTable;
import javax.persistence.Convert;
import javax.persistence.Converter;
import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Table;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.SimpleValue;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.hamcrest.CoreMatchers;

import static org.junit.Assert.assertThat;

/**
 * Similar to {@link CollectionCompositeElementConversionTest} except here we have an
 * explicit {@code @Convert} defined on the converted attribute
 *
 * @author Steve Ebersole
 */
@TestForIssue( jiraKey = "HHH-10277" )
public class CollectionCompositeElementExplicitConversionTest extends BaseUnitTestCase {
	private StandardServiceRegistry ssr;
	private Field simpleValueAttributeConverterDescriptorField;

	@Before
	public void setUp() throws Exception {
		ssr = new StandardServiceRegistryBuilder().build();
		simpleValueAttributeConverterDescriptorField = ReflectHelper.findField( SimpleValue.class, "attributeConverterDescriptor" );
	}

	@After
	public void tearDown() {
		if ( ssr != null ) {
			StandardServiceRegistryBuilder.destroy( ssr );
		}
	}

	@Test
	public void testCollectionOfEmbeddablesWithConvertedAttributes() throws Exception {
		final MetadataImplementor metadata = (MetadataImplementor) new MetadataSources( ssr )
				.addAnnotatedClass( Disguise.class )
				.addAnnotatedClass( Traits.class )
				.buildMetadata();

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
