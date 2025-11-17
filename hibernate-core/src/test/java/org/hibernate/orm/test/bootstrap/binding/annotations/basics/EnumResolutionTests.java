/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bootstrap.binding.annotations.basics;

import java.sql.Types;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.type.descriptor.converter.spi.JpaAttributeConverter;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.spi.TypeConfiguration;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.junit.jupiter.api.Test;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import static jakarta.persistence.EnumType.ORDINAL;
import static jakarta.persistence.EnumType.STRING;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Steve Ebersole
 */
@ServiceRegistry
@DomainModel( annotatedClasses = EnumResolutionTests.EntityWithEnums.class )
public class EnumResolutionTests {

	@Test
	public void testRawEnumResolution(DomainModelScope scope) {
		final PersistentClass entityBinding = scope
				.getDomainModel()
				.getEntityBinding( EntityWithEnums.class.getName() );

		verifyEnumResolution(
				entityBinding.getProperty( "rawEnum" ),
				Types.TINYINT,
				Byte.class,
				null
		);
	}

	@Test
	public void testUnspecifiedMappingEnumResolution(DomainModelScope scope) {
		final PersistentClass entityBinding = scope
				.getDomainModel()
				.getEntityBinding( EntityWithEnums.class.getName() );

		verifyEnumResolution(
				entityBinding.getProperty( "unspecifiedMappingEnum" ),
				Types.TINYINT,
				Byte.class,
				null
		);
	}

	@Test
	public void testOrdinalEnumResolution(DomainModelScope scope) {
		final PersistentClass entityBinding = scope
				.getDomainModel()
				.getEntityBinding( EntityWithEnums.class.getName() );

		verifyEnumResolution(
				entityBinding.getProperty( "ordinalEnum" ),
				Types.TINYINT,
				Byte.class,
				null
		);
	}

	@Test
	public void testNamedEnumResolution(DomainModelScope scope) {
		final PersistentClass entityBinding = scope
				.getDomainModel()
				.getEntityBinding( EntityWithEnums.class.getName() );

		verifyEnumResolution(
				entityBinding.getProperty( "namedEnum" ),
				Types.VARCHAR,
				String.class,
				null
		);
	}

	@Test
	public void testConvertedEnumResolution(DomainModelScope scope) {
		final PersistentClass entityBinding = scope
				.getDomainModel()
				.getEntityBinding( EntityWithEnums.class.getName() );

		verifyEnumResolution(
				entityBinding.getProperty( "convertedEnum" ),
				Types.INTEGER,
				Integer.class,
				ConverterImpl.class
		);
	}

	@Test
	public void testExplicitEnumResolution(DomainModelScope scope) {
		final PersistentClass entityBinding = scope
				.getDomainModel()
				.getEntityBinding( EntityWithEnums.class.getName() );

		verifyEnumResolution(
				entityBinding.getProperty( "explicitEnum" ),
				Types.SMALLINT,
				Short.class,
				null
		);
	}

	@Test
	public void testSingleCharEnumResolution(DomainModelScope scope) {
		final PersistentClass entityBinding = scope
				.getDomainModel()
				.getEntityBinding( EntityWithEnums.class.getName() );

		verifyEnumResolution(
				entityBinding.getProperty( "singleCharEnum" ),
				Types.CHAR,
				Character.class,
				null
		);
	}

	private void verifyEnumResolution(
			Property property,
			int jdbcCode,
			Class<?> javaType,
			Class<? extends AttributeConverter<?,?>> converterClass) {
		final BasicValue.Resolution<?> resolution = ( (BasicValue) property.getValue() ).resolve();
		final TypeConfiguration typeConfiguration = ( (BasicValue) property.getValue() ).getTypeConfiguration();
		final JdbcType jdbcType = typeConfiguration.getJdbcTypeRegistry().getDescriptor( jdbcCode );

		// verify the interpretations used for reading
//		assertThat( resolution.getJdbcType(), is( jdbcType ) );
//		assertThat( resolution.getRelationalJavaType().getJavaTypeClass(), equalTo( javaType ) );
		assertThat( resolution.getDomainJavaType().getJavaTypeClass(), equalTo( Values.class ) );

		final JdbcMapping jdbcMapping = resolution.getJdbcMapping();
		assertThat( jdbcMapping.getJdbcType(), equalTo( resolution.getJdbcType() ) );
		assertThat( jdbcMapping.getJdbcJavaType(), equalTo( resolution.getRelationalJavaType() ) );

		if ( converterClass == null ) {
			assertThat( resolution.getValueConverter(), nullValue() );
		}
		else {
			assertThat( ((JpaAttributeConverter<?,?>) resolution.getValueConverter())
							.getConverterJavaType().getJavaTypeClass(),
					equalTo( converterClass ) );
		}
	}

	@Entity( name = "EntityWithEnums" )
	@Table( name = "entity_with_enums")
	@SuppressWarnings("unused")
	public static class EntityWithEnums {
		@Id
		private Integer id;

		private Values rawEnum;

		@Convert( converter = ConverterImpl.class )
		private Values convertedEnum;

		@Enumerated
		private Values unspecifiedMappingEnum;

		@Enumerated( ORDINAL )
		private Values ordinalEnum;

		@Enumerated( STRING )
		private Values namedEnum;

		@Enumerated( ORDINAL )
		@JdbcTypeCode( Types.SMALLINT )
		private Values explicitEnum;

		@Enumerated( STRING )
		@Column( length = 1 )
		private Values singleCharEnum;
	}

	enum Values { FIRST, SECOND }

	public static class ConverterImpl implements AttributeConverter<Values,Integer> {
		@Override
		public Integer convertToDatabaseColumn(Values attribute) {
			return null;
		}

		@Override
		public Values convertToEntityAttribute(Integer dbData) {
			return null;
		}
	}
}
