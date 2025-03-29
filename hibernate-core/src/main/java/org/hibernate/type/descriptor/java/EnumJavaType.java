/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.java;

import java.util.Set;

import org.hibernate.boot.model.process.internal.EnumeratedValueConverter;
import org.hibernate.dialect.Dialect;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.converter.spi.BasicValueConverter;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeIndicators;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;

import jakarta.persistence.EnumType;

import static jakarta.persistence.EnumType.ORDINAL;
import static org.hibernate.internal.util.collections.CollectionHelper.setOfSize;
import static org.hibernate.type.SqlTypes.CHAR;
import static org.hibernate.type.SqlTypes.ENUM;
import static org.hibernate.type.SqlTypes.NAMED_ENUM;
import static org.hibernate.type.SqlTypes.ORDINAL_ENUM;
import static org.hibernate.type.SqlTypes.NAMED_ORDINAL_ENUM;
import static org.hibernate.type.SqlTypes.NCHAR;
import static org.hibernate.type.SqlTypes.NVARCHAR;
import static org.hibernate.type.SqlTypes.SMALLINT;
import static org.hibernate.type.SqlTypes.TINYINT;
import static org.hibernate.type.SqlTypes.VARCHAR;

/**
 * Describes a Java {@code enum} type.
 *
 * @author Steve Ebersole
 */
public class EnumJavaType<T extends Enum<T>> extends AbstractClassJavaType<T> {

	public EnumJavaType(Class<T> type) {
		super( type, ImmutableMutabilityPlan.instance() );
	}

	@Override
	public JdbcType getRecommendedJdbcType(JdbcTypeIndicators context) {
		return context.getTypeConfiguration().getJdbcTypeRegistry().getDescriptor( sqlType( context ) );
	}

	private int sqlType(JdbcTypeIndicators context) {
		final EnumType enumType = context.getEnumeratedType();
		final boolean preferNativeEnumTypes = context.isPreferNativeEnumTypesEnabled();
		final JdbcTypeRegistry jdbcTypeRegistry = context.getTypeConfiguration().getJdbcTypeRegistry();
		return switch ( enumType == null ? ORDINAL : enumType ) {
			case ORDINAL:
				if ( preferNativeEnumTypes && jdbcTypeRegistry.hasRegisteredDescriptor( ORDINAL_ENUM ) ) {
					yield ORDINAL_ENUM;
				}
				else if ( preferNativeEnumTypes && jdbcTypeRegistry.hasRegisteredDescriptor( NAMED_ORDINAL_ENUM ) ) {
					yield NAMED_ORDINAL_ENUM;
				}
				else {
					yield hasManyValues() ? SMALLINT : TINYINT;
				}
			case STRING:
				if ( jdbcTypeRegistry.hasRegisteredDescriptor( ENUM ) ) {
					yield ENUM;
				}
				else if ( preferNativeEnumTypes && jdbcTypeRegistry.hasRegisteredDescriptor( NAMED_ENUM ) ) {
					yield NAMED_ENUM;
				}
				else if ( context.getColumnLength() == 1 ) {
					yield context.isNationalized() ? NCHAR : CHAR;
				}
				else {
					yield context.isNationalized() ? NVARCHAR : VARCHAR;
				}
		};
	}

	public boolean hasManyValues() {
		// a bit arbitrary, but gives us some headroom
		return getJavaTypeClass().getEnumConstants().length > 128;
	}

	@Override
	public boolean useObjectEqualsHashCode() {
		return true;
	}

	@Override
	public String toString(T value) {
		return value == null ? "<null>" : value.name();
	}

	@Override
	public T fromString(CharSequence string) {
		return string == null ? null : Enum.valueOf( getJavaTypeClass(), string.toString() );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <X> X unwrap(T value, Class<X> type, WrapperOptions options) {
		if ( String.class.equals( type ) ) {
			return (X) toName( value );
		}
		else if ( Long.class.equals( type ) ) {
			return (X) toLong( value );
		}
		else if ( Integer.class.equals( type ) ) {
			return (X) toInteger( value );
		}
		else if ( Short.class.equals( type ) ) {
			return (X) toShort( value );
		}
		else if ( Byte.class.equals( type ) ) {
			return (X) toByte( value );
		}
		return (X) value;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <X> T wrap(X value, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}
		else if ( value instanceof String string ) {
			return fromName( string );
		}
		else if ( value instanceof Long longValue ) {
			return fromLong( longValue );
		}
		else if ( value instanceof Integer integerValue ) {
			return fromInteger( integerValue );
		}
		else if ( value instanceof Short shortValue ) {
			return fromShort( shortValue );
		}
		else if ( value instanceof Byte byteValue ) {
			return fromByte( byteValue );
		}
		else if ( value instanceof Number number ) {
			return fromLong( number.longValue() );
		}
		else {
		return (T) value;
		}
	}

	/**
	 * Convert a value of the enum type to its ordinal value
	 */
	public Byte toByte(T domainForm) {
		if ( domainForm == null ) {
			return null;
		}
		return (byte) domainForm.ordinal();
	}

	/**
	 * Convert a value of the enum type to its ordinal value
	 */
	public Short toShort(T domainForm) {
		if ( domainForm == null ) {
			return null;
		}
		return (short) domainForm.ordinal();
	}

	/**
	 * Convert a value of the enum type to its ordinal value
	 */
	public Integer toInteger(T domainForm) {
		if ( domainForm == null ) {
			return null;
		}
		return domainForm.ordinal();
	}

	/**
	 * Convert a value of the enum type to its ordinal value
	 */
	public Long toLong(T domainForm) {
		if ( domainForm == null ) {
			return null;
		}
		return (long) domainForm.ordinal();
	}

	/**
	 * Convert a value of the enum type to its ordinal value
	 */
	public Integer toOrdinal(T domainForm) {
		return toInteger( domainForm );
	}

	/**
	 * Interpret a numeric value as the ordinal of the enum type
	 */
	public T fromByte(Byte relationalForm) {
		if ( relationalForm == null ) {
			return null;
		}
		return getJavaTypeClass().getEnumConstants()[ relationalForm ];
	}

	/**
	 * Interpret a numeric value as the ordinal of the enum type
	 */
	public T fromShort(Short relationalForm) {
		if ( relationalForm == null ) {
			return null;
		}
		return getJavaTypeClass().getEnumConstants()[ relationalForm ];
	}

	/**
	 * Interpret a numeric value as the ordinal of the enum type
	 */
	public T fromInteger(Integer relationalForm) {
		if ( relationalForm == null ) {
			return null;
		}
		return getJavaTypeClass().getEnumConstants()[ relationalForm ];
	}

	/**
	 * Interpret a numeric value as the ordinal of the enum type
	 */
	public T fromLong(Long relationalForm) {
		if ( relationalForm == null ) {
			return null;
		}
		return getJavaTypeClass().getEnumConstants()[ relationalForm.intValue() ];
	}

	/**
	 * Interpret a numeric value as the ordinal of the enum type
	 */
	public T fromOrdinal(Integer relationalForm) {
		return fromInteger( relationalForm );
	}

	/**
	 * Convert a value of the enum type to its name value
	 */
	public String toName(T domainForm) {
		return domainForm == null ? null : domainForm.name();
	}

	/**
	 * Interpret a string value as the named value of the enum type
	 */
	public T fromName(String relationalForm) {
		return relationalForm == null ? null : Enum.valueOf( getJavaTypeClass(), relationalForm.trim() );
	}

	@Override
	public String getCheckCondition(String columnName, JdbcType jdbcType, BasicValueConverter<T, ?> converter, Dialect dialect) {
		if ( converter != null
				&& jdbcType.getDefaultSqlTypeCode() != NAMED_ENUM ) {
			return renderConvertedEnumCheckConstraint( columnName, jdbcType, converter, dialect );
		}
		else if ( jdbcType.isInteger() ) {
			int max = getJavaTypeClass().getEnumConstants().length - 1;
			return dialect.getCheckCondition( columnName, 0, max );
		}
		else if ( jdbcType.isString() ) {
			return dialect.getCheckCondition( columnName, getJavaTypeClass() );
		}
		else {
			return null;
		}
	}

	private String renderConvertedEnumCheckConstraint(
			String columnName,
			JdbcType jdbcType,
			BasicValueConverter<T, ?> converter,
			Dialect dialect) {
		final Set<?> valueSet = valueSet( jdbcType, converter );
		return valueSet == null ? null : dialect.getCheckCondition( columnName, valueSet, jdbcType );
	}

	private <R> Set<R> valueSet(JdbcType jdbcType, BasicValueConverter<T,R> converter) {
		// for `@EnumeratedValue` we already have the possible values...
		if ( converter instanceof EnumeratedValueConverter<T,R> enumeratedValueConverter ) {
			return enumeratedValueConverter.getRelationalValueSet();
		}
		else {
			if ( !SqlTypes.isIntegral( jdbcType.getJdbcTypeCode() )
					&& !SqlTypes.isCharacterType( jdbcType.getJdbcTypeCode() ) ) {
				// we only support adding check constraints for generalized conversions to
				// INTEGER, SMALLINT, TINYINT, (N)CHAR, (N)VARCHAR, LONG(N)VARCHAR
				return null;
			}
			else {
				final T[] enumConstants = getJavaTypeClass().getEnumConstants();
				final Set<R> valueSet = setOfSize( enumConstants.length );
				for ( T enumConstant : enumConstants ) {
					valueSet.add( converter.toRelationalValue( enumConstant ) );
				}
				return valueSet;
			}
		}
	}
}
