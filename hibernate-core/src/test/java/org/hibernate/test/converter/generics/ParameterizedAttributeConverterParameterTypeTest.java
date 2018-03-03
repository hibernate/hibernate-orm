/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.converter.generics;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AttributeConverterDefinition;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.type.descriptor.converter.AttributeConverterTypeAdapter;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.hibernate.testing.junit4.ExtraAssertions.assertTyping;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Test the ability to interpret and understand AttributeConverter impls which
 * use parameterized types as one of (typically the "attribute type") its parameter types.
 * 
 * @author Svein Baardsen
 * @author Steve Ebersole
 */
public class ParameterizedAttributeConverterParameterTypeTest extends BaseUnitTestCase {

	private static StandardServiceRegistry ssr;

	@BeforeClass
	public static void beforeClass() {
		ssr = new StandardServiceRegistryBuilder().build();
	}

	@AfterClass
	public static void afterClass() {
		if ( ssr != null ) {
			StandardServiceRegistryBuilder.destroy( ssr );
		}
	}

	public static class CustomAttributeConverter implements AttributeConverter<List<String>, Integer> {
		@Override
		public Integer convertToDatabaseColumn(List<String> attribute) {
			return attribute.size();
		}

		@Override
		public List<String> convertToEntityAttribute(Integer dbData) {
			return new ArrayList<String>(dbData);
		}
	}
	
	@Test
	@TestForIssue(jiraKey = "HHH-8804")
	public void testGenericTypeParameters() {
		AttributeConverterDefinition def = AttributeConverterDefinition.from( CustomAttributeConverter.class );
		assertEquals( List.class, def.getEntityAttributeType() );
	}

	@Test
	@TestForIssue( jiraKey = "HHH-10050" )
	public void testNestedTypeParameterAutoApplication() {
		final Metadata metadata = new MetadataSources( ssr )
				.addAnnotatedClass( SampleEntity.class )
				.getMetadataBuilder()
				.applyAttributeConverter( IntegerListConverter.class )
				.applyAttributeConverter( StringListConverter.class )
				.build();

		// lets make sure the auto-apply converters were applied properly...
		PersistentClass pc = metadata.getEntityBinding( SampleEntity.class.getName() );

		{
			Property prop = pc.getProperty( "someStrings" );
			AttributeConverterTypeAdapter type = assertTyping(
					AttributeConverterTypeAdapter.class,
					prop.getType()
			);

			assertTrue( StringListConverter.class.isAssignableFrom( type.getAttributeConverter().getConverterJavaTypeDescriptor().getJavaType() ) );
		}

		{
			Property prop = pc.getProperty( "someIntegers" );
			AttributeConverterTypeAdapter type = assertTyping(
					AttributeConverterTypeAdapter.class,
					prop.getType()
			);

			assertTrue( IntegerListConverter.class.isAssignableFrom( type.getAttributeConverter().getConverterJavaTypeDescriptor().getJavaType() ) );
		}
	}

	@Entity
	public static class SampleEntity {
		@Id
		private Integer id;
		private List<String> someStrings;
		private List<Integer> someIntegers;
	}

	@Converter( autoApply = true )
	public static class IntegerListConverter implements AttributeConverter<List<Integer>,String> {
		@Override
		public String convertToDatabaseColumn(List<Integer> attribute) {
			if ( attribute == null || attribute.isEmpty() ) {
				return null;
			}
			else {
				return StringHelper.join( ", ", attribute.iterator() );
			}
		}

		@Override
		public List<Integer> convertToEntityAttribute(String dbData) {
			if ( dbData == null ) {
				return null;
			}

			dbData = dbData.trim();
			if ( dbData.length() == 0 ) {
				return null;
			}

			final List<Integer> integers = new ArrayList<Integer>();
			final StringTokenizer tokens = new StringTokenizer( dbData, "," );

			while ( tokens.hasMoreTokens() ) {
				integers.add( Integer.valueOf( tokens.nextToken() ) );
			}

			return integers;
		}
	}

	@Converter( autoApply = true )
	public static class StringListConverter implements AttributeConverter<List<String>,String> {
		@Override
		public String convertToDatabaseColumn(List<String> attribute) {
			if ( attribute == null || attribute.isEmpty() ) {
				return null;
			}
			else {
				return String.join( ", ", attribute );
			}
		}

		@Override
		public List<String> convertToEntityAttribute(String dbData) {
			if ( dbData == null ) {
				return null;
			}

			dbData = dbData.trim();
			if ( dbData.length() == 0 ) {
				return null;
			}

			final List<String> strings = new ArrayList<String>();
			final StringTokenizer tokens = new StringTokenizer( dbData, "," );

			while ( tokens.hasMoreTokens() ) {
				strings.add( tokens.nextToken() );
			}

			return strings;
		}
	}


}
