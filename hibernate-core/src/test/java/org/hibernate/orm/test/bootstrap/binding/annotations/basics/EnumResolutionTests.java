/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.bootstrap.binding.annotations.basics;

import java.sql.Types;
import java.util.function.Consumer;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.model.convert.internal.NamedEnumValueConverter;
import org.hibernate.metamodel.model.convert.internal.OrdinalEnumValueConverter;
import org.hibernate.metamodel.model.convert.spi.BasicValueConverter;
import org.hibernate.metamodel.model.convert.spi.JpaAttributeConverter;
import org.hibernate.type.BasicType;
import org.hibernate.type.CustomType;
import org.hibernate.type.EnumType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.internal.ConvertedBasicTypeImpl;
import org.hibernate.type.spi.TypeConfiguration;
import org.hibernate.usertype.UserType;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.junit.jupiter.api.Test;

import static jakarta.persistence.EnumType.ORDINAL;
import static jakarta.persistence.EnumType.STRING;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
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
				Types.SMALLINT,
				Integer.class,
				OrdinalEnumValueConverter.class,
				true
		);
	}

	@Test
	public void testUnspecifiedMappingEnumResolution(DomainModelScope scope) {
		final PersistentClass entityBinding = scope
				.getDomainModel()
				.getEntityBinding( EntityWithEnums.class.getName() );

		verifyEnumResolution(
				entityBinding.getProperty( "unspecifiedMappingEnum" ),
				Types.SMALLINT,
				Integer.class,
				OrdinalEnumValueConverter.class,
				true
		);
	}

	@Test
	public void testOrdinalEnumResolution(DomainModelScope scope) {
		final PersistentClass entityBinding = scope
				.getDomainModel()
				.getEntityBinding( EntityWithEnums.class.getName() );

		verifyEnumResolution(
				entityBinding.getProperty( "ordinalEnum" ),
				Types.SMALLINT,
				Integer.class,
				OrdinalEnumValueConverter.class,
				true
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
				NamedEnumValueConverter.class,
				false
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
				(converter) -> {
					assertThat( converter, notNullValue() );
					assertThat( converter, instanceOf( JpaAttributeConverter.class ) );
					//noinspection rawtypes
					final Class converterType = ( (JpaAttributeConverter) converter ).getConverterBean().getBeanClass();
					assertThat( converterType, equalTo( ConverterImpl.class ) );
				},
				(legacyResolution) -> {
					assertThat( legacyResolution, instanceOf( ConvertedBasicTypeImpl.class ) );
				}
		);
	}

	@Test
	public void testExplicitEnumResolution(DomainModelScope scope) {
		final PersistentClass entityBinding = scope
				.getDomainModel()
				.getEntityBinding( EntityWithEnums.class.getName() );

		verifyEnumResolution(
				entityBinding.getProperty( "explicitEnum" ),
				Types.TINYINT,
				Integer.class,
				OrdinalEnumValueConverter.class,
				true
		);
	}

	@SuppressWarnings("rawtypes")
	private void verifyEnumResolution(
			Property property,
			int jdbcCode,
			Class<?> jdbcJavaType,
			Class<? extends BasicValueConverter> converterClass,
			boolean isOrdinal) {
		verifyEnumResolution(
				property,
				jdbcCode,
				jdbcJavaType,
				valueConverter -> {
					assertThat( valueConverter, notNullValue() );
					assertThat( valueConverter, instanceOf( converterClass ) );
				},
				legacyResolvedType -> {
					assertThat( legacyResolvedType, instanceOf( CustomType.class ) );
					final UserType rawEnumUserType = ( (CustomType<Object>) legacyResolvedType ).getUserType();
					assertThat( rawEnumUserType, instanceOf( EnumType.class ) );
					final EnumType rawEnumEnumType = (EnumType) rawEnumUserType;
					assertThat( rawEnumEnumType.isOrdinal(), is( isOrdinal ) );
				}
		);
	}

	@SuppressWarnings("rawtypes")
	private void verifyEnumResolution(
			Property property,
			int jdbcCode,
			Class<?> javaType,
			Consumer<BasicValueConverter> converterChecker,
			Consumer<BasicType> legacyTypeChecker) {
		final BasicValue.Resolution<?> resolution = ( (BasicValue) property.getValue() ).resolve();
		final TypeConfiguration typeConfiguration = ( (BasicValue) property.getValue() ).getTypeConfiguration();
		final JdbcType jdbcType = typeConfiguration.getJdbcTypeRegistry().getDescriptor( jdbcCode );
		// verify the interpretations used for reading
		assertThat( resolution.getJdbcType(), is( jdbcType ) );
		assertThat( resolution.getRelationalJavaType().getJavaTypeClass(), equalTo( javaType ) );
		assertThat( resolution.getDomainJavaType().getJavaTypeClass(), equalTo( Values.class ) );

		final JdbcMapping jdbcMapping = resolution.getJdbcMapping();
		assertThat( jdbcMapping.getJdbcType(), equalTo( resolution.getJdbcType() ) );
		assertThat( jdbcMapping.getJdbcJavaType(), equalTo( resolution.getRelationalJavaType() ) );

		converterChecker.accept( resolution.getValueConverter() );

		// verify the (legacy) interpretations used for writing
		legacyTypeChecker.accept( resolution.getLegacyResolvedBasicType() );
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
		@JdbcTypeCode( Types.TINYINT )
		private Values explicitEnum;
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
