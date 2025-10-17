/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.converted.converter.generics;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.model.convert.internal.ConverterDescriptors;
import org.hibernate.boot.model.convert.spi.ConverterDescriptor;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.type.descriptor.converter.spi.JpaAttributeConverter;
import org.hibernate.type.internal.ConvertedBasicTypeImpl;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.boot.BootstrapContextImpl;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.junit.jupiter.api.Test;

import static org.hibernate.testing.orm.junit.ExtraAssertions.assertTyping;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test the ability to interpret and understand AttributeConverter impls which
 * use parameterized types as one of (typically the "attribute type") its parameter types.
 *
 * @author Svein Baardsen
 * @author Steve Ebersole
 */
@ServiceRegistry
public class ParameterizedAttributeConverterParameterTypeTest {

	@Test
	@JiraKey(value = "HHH-8804")
	public void testGenericTypeParameters() {
		final BootstrapContextImpl bootstrapContext = new BootstrapContextImpl();
		try {
			final ConverterDescriptor<List<String>, Integer> converterDescriptor =
					ConverterDescriptors.of( CustomAttributeConverter.class,
							bootstrapContext.getClassmateContext() );
			assertEquals( List.class, converterDescriptor.getDomainValueResolvedType().getErasedType() );
		} finally {
			bootstrapContext.close();
		}
	}

	@Test
	@JiraKey( value = "HHH-10050" )
	public void testNestedTypeParameterAutoApplication(ServiceRegistryScope scope) {
		final Metadata metadata = new MetadataSources( scope.getRegistry() )
				.addAnnotatedClass( SampleEntity.class )
				.getMetadataBuilder()
				.applyAttributeConverter( IntegerListConverter.class )
				.applyAttributeConverter( StringListConverter.class )
				.build();

		// lets make sure the auto-apply converters were applied properly...
		PersistentClass pc = metadata.getEntityBinding( SampleEntity.class.getName() );

		{
			Property prop = pc.getProperty( "someStrings" );
			ConvertedBasicTypeImpl type = assertTyping(
					ConvertedBasicTypeImpl.class,
					prop.getType()
			);
			JpaAttributeConverter converter = (JpaAttributeConverter) type.getValueConverter();
			assertTrue( StringListConverter.class.isAssignableFrom( converter.getConverterJavaType().getJavaTypeClass() ) );
		}

		{
			Property prop = pc.getProperty( "someIntegers" );
			ConvertedBasicTypeImpl type = assertTyping(
					ConvertedBasicTypeImpl.class,
					prop.getType()
			);
			JpaAttributeConverter converter = (JpaAttributeConverter) type.getValueConverter();
			assertTrue( IntegerListConverter.class.isAssignableFrom( converter.getConverterJavaType().getJavaTypeClass() ) );
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


}
