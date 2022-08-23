/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import java.lang.reflect.Array;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.hibernate.metamodel.model.convert.spi.BasicValueConverter;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.JdbcLiteralFormatter;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeIndicators;
import org.hibernate.type.descriptor.jdbc.internal.JdbcLiteralFormatterArray;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * A type that maps between {@link java.sql.Types#ARRAY ARRAY} and {@code T[]}
 *
 * @author Jordan Gigov
 * @author Christian Beikov
 */
public class BasicArrayType<T>
		extends AbstractSingleColumnStandardBasicType<T[]>
		implements AdjustableBasicType<T[]>, BasicPluralType<T[], T> {

	private final BasicType<T> baseDescriptor;
	private final String name;
	private final ValueBinder<T[]> jdbcValueBinder;
	private final ValueExtractor<T[]> jdbcValueExtractor;
	private final JdbcLiteralFormatter<T[]> jdbcLiteralFormatter;

	public BasicArrayType(BasicType<T> baseDescriptor, JdbcType arrayJdbcType, JavaType<T[]> arrayTypeDescriptor) {
		super( arrayJdbcType, arrayTypeDescriptor );
		this.baseDescriptor = baseDescriptor;
		this.name = baseDescriptor.getName() + "[]";
		final ValueBinder<T[]> jdbcValueBinder = super.getJdbcValueBinder();
		final ValueExtractor<T[]> jdbcValueExtractor = super.getJdbcValueExtractor();
		final JdbcLiteralFormatter jdbcLiteralFormatter = super.getJdbcLiteralFormatter();
		//noinspection unchecked
		final BasicValueConverter<T, Object> valueConverter = (BasicValueConverter<T, Object>) baseDescriptor.getValueConverter();
		if ( valueConverter != null ) {
			this.jdbcValueBinder = new ValueBinder<T[]>() {
				@Override
				public void bind(PreparedStatement st, T[] value, int index, WrapperOptions options)
						throws SQLException {
					jdbcValueBinder.bind( st, getValue( value, valueConverter, options ), index, options );
				}

				@Override
				public void bind(CallableStatement st, T[] value, String name, WrapperOptions options)
						throws SQLException {
					jdbcValueBinder.bind( st, getValue( value, valueConverter, options ), name, options );
				}

				private T[] getValue(
						T[] value,
						BasicValueConverter<T, Object> valueConverter,
						WrapperOptions options) {
					if ( value == null ) {
						return null;
					}
					final JdbcType elementJdbcType = baseDescriptor.getJdbcType();
					final TypeConfiguration typeConfiguration = options.getSessionFactory().getTypeConfiguration();
					final JdbcType underlyingJdbcType = typeConfiguration.getJdbcTypeRegistry()
							.getDescriptor( elementJdbcType.getDefaultSqlTypeCode() );
					final Class<?> preferredJavaTypeClass = underlyingJdbcType.getPreferredJavaTypeClass( options );
					final Class<?> elementJdbcJavaTypeClass;
					if ( preferredJavaTypeClass == null ) {
						elementJdbcJavaTypeClass = underlyingJdbcType.getJdbcRecommendedJavaTypeMapping(
								null,
								null,
								typeConfiguration
						).getJavaTypeClass();
					}
					else {
						elementJdbcJavaTypeClass = preferredJavaTypeClass;
					}

					if ( value.getClass().getComponentType() == elementJdbcJavaTypeClass ) {
						return value;
					}
					final Object[] array = (Object[]) Array.newInstance( elementJdbcJavaTypeClass, value.length );
					for ( int i = 0; i < value.length; i++ ) {
						array[i] = valueConverter.getRelationalJavaType().unwrap(
								valueConverter.toRelationalValue( value[i] ),
								elementJdbcJavaTypeClass,
								options
						);
					}
					//noinspection unchecked
					return (T[]) array;
				}
			};
			this.jdbcValueExtractor = new ValueExtractor<T[]>() {
				@Override
				public T[] extract(ResultSet rs, int paramIndex, WrapperOptions options) throws SQLException {
					return getValue( jdbcValueExtractor.extract( rs, paramIndex, options ), valueConverter );
				}

				@Override
				public T[] extract(CallableStatement statement, int paramIndex, WrapperOptions options)
						throws SQLException {
					return getValue( jdbcValueExtractor.extract( statement, paramIndex, options ), valueConverter );
				}

				@Override
				public T[] extract(CallableStatement statement, String paramName, WrapperOptions options)
						throws SQLException {
					return getValue( jdbcValueExtractor.extract( statement, paramName, options ), valueConverter );
				}

				private T[] getValue(T[] value, BasicValueConverter<T, Object> valueConverter) {
					if ( value == null ) {
						return null;
					}
					if ( value.getClass().getComponentType() == valueConverter.getDomainJavaType().getJavaTypeClass() ) {
						return value;
					}
					//noinspection unchecked
					final T[] array = (T[]) Array.newInstance(
							valueConverter.getDomainJavaType().getJavaTypeClass(),
							value.length
					);
					for ( int i = 0; i < value.length; i++ ) {
						array[i] = valueConverter.toDomainValue( value[i] );
					}
					return array;
				}
			};
			this.jdbcLiteralFormatter = new JdbcLiteralFormatterArray(
					baseDescriptor.getJavaTypeDescriptor(),
					jdbcLiteralFormatter
			);
		}
		else {
			this.jdbcValueBinder = jdbcValueBinder;
			this.jdbcValueExtractor = jdbcValueExtractor;
			this.jdbcLiteralFormatter = jdbcLiteralFormatter;
		}
	}

	@Override
	public BasicType<T> getElementType() {
		return baseDescriptor;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	protected boolean registerUnderJavaType() {
		return true;
	}

	@Override
	public ValueExtractor<T[]> getJdbcValueExtractor() {
		return jdbcValueExtractor;
	}

	@Override
	public ValueBinder<T[]> getJdbcValueBinder() {
		return jdbcValueBinder;
	}

	@Override
	public JdbcLiteralFormatter getJdbcLiteralFormatter() {
		return jdbcLiteralFormatter;
	}

	@Override
	public <X> BasicType<X> resolveIndicatedType(JdbcTypeIndicators indicators, JavaType<X> domainJtd) {
		// TODO: maybe fallback to some encoding by default if the DB doesn't support arrays natively?
		//  also, maybe move that logic into the ArrayJdbcType
		//noinspection unchecked
		return (BasicType<X>) this;
	}
}
