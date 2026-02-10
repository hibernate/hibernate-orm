/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.java;

import org.hibernate.MappingException;
import org.hibernate.dialect.Dialect;
import org.hibernate.internal.build.AllowReflection;
import org.hibernate.tool.schema.extract.spi.ColumnTypeInformation;
import org.hibernate.type.BasicArrayType;
import org.hibernate.type.BasicPluralType;
import org.hibernate.type.BasicType;
import org.hibernate.type.ConvertedBasicArrayType;
import org.hibernate.type.descriptor.converter.internal.ArrayConverter;
import org.hibernate.type.descriptor.converter.spi.BasicValueConverter;
import org.hibernate.type.descriptor.java.spi.UnknownBasicJavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeIndicators;
import org.hibernate.type.spi.TypeConfiguration;


import static org.hibernate.internal.util.ReflectHelper.arrayClass;

@AllowReflection
public abstract class AbstractArrayJavaType<T, E> extends AbstractClassJavaType<T>
		implements BasicPluralJavaType<E> {

	private final JavaType<E> componentJavaType;

	public AbstractArrayJavaType(Class<T> clazz, JavaType<E> baseDescriptor, MutabilityPlan<T> mutabilityPlan) {
		super( clazz, mutabilityPlan );
		this.componentJavaType = baseDescriptor;
	}

	@Override
	public JavaType<E> getElementJavaType() {
		return componentJavaType;
	}

	@Override
	public JdbcType getRecommendedJdbcType(JdbcTypeIndicators indicators) {
		if ( componentJavaType instanceof UnknownBasicJavaType) {
			throw new MappingException("Basic array has element type '"
					+ componentJavaType.getTypeName()
					+ "' which is not a known basic type"
					+ " (attribute is not annotated '@ElementCollection', '@OneToMany', or '@ManyToMany')");
		}
		// Always determine the recommended type to make sure this is a valid basic java type
		final var recommendedComponentJdbcType = componentJavaType.getRecommendedJdbcType( indicators );
		final var typeConfiguration = indicators.getTypeConfiguration();
		return typeConfiguration.getJdbcTypeRegistry()
				.resolveTypeConstructorDescriptor(
						indicators.getPreferredSqlTypeCodeForArray( recommendedComponentJdbcType.getDefaultSqlTypeCode() ),
						typeConfiguration.getBasicTypeRegistry().resolve( componentJavaType, recommendedComponentJdbcType ),
						ColumnTypeInformation.EMPTY
				);
	}

	@Override
	public boolean isWider(JavaType<?> javaType) {
		// Support binding single element value
		return this == javaType || componentJavaType == javaType;
	}

	@Override
	public BasicType<?> resolveType(
			TypeConfiguration typeConfiguration,
			Dialect dialect,
			BasicType<E> elementType,
			ColumnTypeInformation columnTypeInformation,
			JdbcTypeIndicators stdIndicators) {
		final var elementJavaTypeClass = elementType.getJavaTypeDescriptor().getJavaTypeClass();
		if ( elementType instanceof BasicPluralType<?, ?>
				|| elementJavaTypeClass != null && elementJavaTypeClass.isArray() ) {
			return null;
		}
		final var valueConverter = elementType.getValueConverter();
		return valueConverter == null
				? resolveType( typeConfiguration, this, elementType, columnTypeInformation, stdIndicators )
				: createTypeUsingConverter( typeConfiguration, elementType, columnTypeInformation, stdIndicators, valueConverter );
	}

	private static JdbcType arrayJdbcType(
			TypeConfiguration typeConfiguration,
			BasicType<?> elementType,
			ColumnTypeInformation columnTypeInformation,
			JdbcTypeIndicators indicators) {
		final int arrayTypeCode =
				indicators.getPreferredSqlTypeCodeForArray( elementType.getJdbcType().getDefaultSqlTypeCode() );
		return typeConfiguration.getJdbcTypeRegistry()
				.resolveTypeConstructorDescriptor( arrayTypeCode, elementType, columnTypeInformation );
	}

	<F> BasicType<T> createTypeUsingConverter(
			TypeConfiguration typeConfiguration,
			BasicType<E> elementType,
			ColumnTypeInformation columnTypeInformation,
			JdbcTypeIndicators indicators,
			BasicValueConverter<E, F> valueConverter) {
		final var convertedArrayClass =
				arrayClass( valueConverter.getRelationalJavaType().getJavaTypeClass() );
		final var relationalJavaType =
				typeConfiguration.getJavaTypeRegistry()
					.resolveDescriptor( convertedArrayClass );
		return new ConvertedBasicArrayType<>(
				elementType,
				arrayJdbcType( typeConfiguration, elementType, columnTypeInformation, indicators ),
				this,
				new ArrayConverter<>( valueConverter, this, relationalJavaType, elementType )
		);
	}

	BasicType<T> resolveType(
			TypeConfiguration typeConfiguration,
			AbstractArrayJavaType<T,E> arrayJavaType,
			BasicType<E> elementType,
			ColumnTypeInformation columnTypeInformation,
			JdbcTypeIndicators indicators) {
		final var arrayJdbcType =
				arrayJdbcType( typeConfiguration, elementType, columnTypeInformation, indicators );
		return typeConfiguration.getBasicTypeRegistry()
				.resolve( arrayJavaType, arrayJdbcType,
						() -> new BasicArrayType<>( elementType, arrayJdbcType, arrayJavaType ) );
	}

	// Methods required to support Horrible hack around the fact
	// that java.sql.Timestamps in an array can be represented as
	// instances of java.util.Date (Why do we even allow this?)

	public T deepCopy(Object value) {
		return getMutabilityPlan().deepCopy( cast( value ) );
	}

	public boolean isEqual(Object one, Object another) {
		return areEqual( cast( one ), cast( another) );
	}
}
