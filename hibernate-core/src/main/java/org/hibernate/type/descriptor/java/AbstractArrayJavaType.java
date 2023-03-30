/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.java;

import java.lang.reflect.Array;
import java.sql.Types;
import java.util.function.Function;

import org.hibernate.dialect.Dialect;
import org.hibernate.tool.schema.extract.spi.ColumnTypeInformation;
import org.hibernate.type.descriptor.converter.internal.ArrayConverter;
import org.hibernate.type.BasicArrayType;
import org.hibernate.type.BasicPluralType;
import org.hibernate.type.BasicType;
import org.hibernate.type.ConvertedBasicArrayType;
import org.hibernate.type.descriptor.converter.spi.BasicValueConverter;
import org.hibernate.type.descriptor.jdbc.ArrayJdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeIndicators;
import org.hibernate.type.spi.TypeConfiguration;

public abstract class AbstractArrayJavaType<T, E> extends AbstractClassJavaType<T>
		implements BasicPluralJavaType<E> {

	private final JavaType<E> componentJavaType;

	public AbstractArrayJavaType(Class<T> clazz, BasicType<E> baseDescriptor, MutabilityPlan<T> mutabilityPlan) {
		this( clazz, baseDescriptor.getJavaTypeDescriptor(), mutabilityPlan );
	}

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
		final int preferredSqlTypeCodeForArray = indicators.getPreferredSqlTypeCodeForArray();
		// Always determine the recommended type to make sure this is a valid basic java type
		final JdbcType recommendedComponentJdbcType = componentJavaType.getRecommendedJdbcType( indicators );
		final TypeConfiguration typeConfiguration = indicators.getTypeConfiguration();
		final JdbcType jdbcType = typeConfiguration.getJdbcTypeRegistry().getDescriptor( preferredSqlTypeCodeForArray );
		if ( jdbcType instanceof ArrayJdbcType ) {
			return ( (ArrayJdbcType) jdbcType ).resolveType(
					typeConfiguration,
					indicators.getDialect(),
					recommendedComponentJdbcType,
					ColumnTypeInformation.EMPTY
			);
		}
		return jdbcType;
	}

	@Override
	public BasicType<?> resolveType(
			TypeConfiguration typeConfiguration,
			Dialect dialect,
			BasicType<E> elementType,
			ColumnTypeInformation columnTypeInformation,
			JdbcTypeIndicators stdIndicators) {
		final Class<?> elementJavaTypeClass = elementType.getJavaTypeDescriptor().getJavaTypeClass();
		if ( elementType instanceof BasicPluralType<?, ?> || elementJavaTypeClass != null && elementJavaTypeClass.isArray() ) {
			return null;
		}
		final BasicValueConverter<E, ?> valueConverter = elementType.getValueConverter();
		if ( valueConverter == null ) {
			final Function<JavaType<T>, BasicType<T>> creator = javaType -> {
				JdbcType arrayJdbcType = typeConfiguration.getJdbcTypeRegistry().getDescriptor( Types.ARRAY );
				if ( arrayJdbcType instanceof ArrayJdbcType ) {
					arrayJdbcType = ( (ArrayJdbcType) arrayJdbcType ).resolveType(
							typeConfiguration,
							dialect,
							elementType,
							columnTypeInformation
					);
				}
				//noinspection unchecked,rawtypes
				return new BasicArrayType( elementType, arrayJdbcType, javaType );
			};
			if ( typeConfiguration.getBasicTypeRegistry().getRegisteredType( elementType.getName() ) == elementType ) {
				return typeConfiguration.standardBasicTypeForJavaType( getJavaType(), creator );
			}
			return creator.apply( this );
		}
		else {
			final JavaType<Object> relationalJavaType = typeConfiguration.getJavaTypeRegistry().getDescriptor(
					Array.newInstance( valueConverter.getRelationalJavaType().getJavaTypeClass(), 0 ).getClass()
			);

			JdbcType arrayJdbcType = typeConfiguration.getJdbcTypeRegistry().getDescriptor( Types.ARRAY );
			if ( arrayJdbcType instanceof ArrayJdbcType ) {
				arrayJdbcType = ( (ArrayJdbcType) arrayJdbcType ).resolveType(
						typeConfiguration,
						dialect,
						elementType,
						columnTypeInformation
				);
			}
			//noinspection unchecked,rawtypes
			return new ConvertedBasicArrayType(
					elementType,
					arrayJdbcType,
					this,
					new ArrayConverter( valueConverter, this, relationalJavaType )
			);
		}
	}
}
