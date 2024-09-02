/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.java;

import java.lang.reflect.Array;

import org.hibernate.MappingException;
import org.hibernate.dialect.Dialect;
import org.hibernate.tool.schema.extract.spi.ColumnTypeInformation;
import org.hibernate.type.descriptor.converter.internal.ArrayConverter;
import org.hibernate.type.BasicArrayType;
import org.hibernate.type.BasicPluralType;
import org.hibernate.type.BasicType;
import org.hibernate.type.ConvertedBasicArrayType;
import org.hibernate.type.descriptor.converter.spi.BasicValueConverter;
import org.hibernate.type.descriptor.java.spi.UnknownBasicJavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeIndicators;
import org.hibernate.type.spi.TypeConfiguration;

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
		return indicators.getTypeConfiguration().getJdbcTypeRegistry().resolveTypeConstructorDescriptor(
				indicators.getPreferredSqlTypeCodeForArray(),
				indicators.getTypeConfiguration().getBasicTypeRegistry().resolve(
						componentJavaType, componentJavaType.getRecommendedJdbcType( indicators ) ),
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
		final Class<?> elementJavaTypeClass = elementType.getJavaTypeDescriptor().getJavaTypeClass();
		if ( elementType instanceof BasicPluralType<?, ?>
				|| elementJavaTypeClass != null && elementJavaTypeClass.isArray() ) {
			return null;
		}
		final BasicValueConverter<E, ?> valueConverter = elementType.getValueConverter();
		return valueConverter == null
				? resolveType( typeConfiguration, dialect, this, elementType, columnTypeInformation, stdIndicators )
				: createTypeUsingConverter( typeConfiguration, dialect, elementType, columnTypeInformation, stdIndicators, valueConverter );
	}

	<F> BasicType<T> createTypeUsingConverter(
			TypeConfiguration typeConfiguration,
			Dialect dialect,
			BasicType<E> elementType,
			ColumnTypeInformation columnTypeInformation,
			JdbcTypeIndicators stdIndicators,
			BasicValueConverter<E, F> valueConverter) {
		final Class<F> convertedElementClass = valueConverter.getRelationalJavaType().getJavaTypeClass();
		final Class<?> convertedArrayClass = Array.newInstance( convertedElementClass, 0 ).getClass();
		final JavaType<?> relationalJavaType = typeConfiguration.getJavaTypeRegistry().getDescriptor( convertedArrayClass );
		return new ConvertedBasicArrayType<>(
				elementType,
				typeConfiguration.getJdbcTypeRegistry().resolveTypeConstructorDescriptor(
						stdIndicators.getExplicitJdbcTypeCode(),
						elementType,
						columnTypeInformation
				),
				this,
				new ArrayConverter<>( valueConverter, this, relationalJavaType )
		);
	}

	BasicType<T> resolveType(
			TypeConfiguration typeConfiguration,
			Dialect dialect,
			AbstractArrayJavaType<T,E> arrayJavaType,
			BasicType<E> elementType,
			ColumnTypeInformation columnTypeInformation,
			JdbcTypeIndicators stdIndicators) {
		final JdbcType arrayJdbcType = typeConfiguration.getJdbcTypeRegistry().resolveTypeConstructorDescriptor(
				stdIndicators.getExplicitJdbcTypeCode(),
				elementType,
				columnTypeInformation
		);
		return typeConfiguration.getBasicTypeRegistry().resolve(
				arrayJavaType,
				arrayJdbcType,
				() -> new BasicArrayType<>( elementType, arrayJdbcType, arrayJavaType )
		);
	}

}
