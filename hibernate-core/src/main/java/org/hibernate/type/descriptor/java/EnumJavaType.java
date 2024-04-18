/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.java;

import jakarta.persistence.EnumType;

import org.hibernate.AssertionFailure;
import org.hibernate.dialect.Dialect;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.converter.spi.BasicValueConverter;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeIndicators;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;

import static jakarta.persistence.EnumType.ORDINAL;
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
		final JdbcTypeRegistry jdbcTypeRegistry = context.getTypeConfiguration().getJdbcTypeRegistry();
		final EnumType type = context.getEnumeratedType();
		final boolean preferNativeEnumTypesEnabled = context.isPreferNativeEnumTypesEnabled();
		int sqlType;
		switch ( type == null ? ORDINAL : type ) {
			case ORDINAL:
				if ( preferNativeEnumTypesEnabled && jdbcTypeRegistry.hasRegisteredDescriptor( ORDINAL_ENUM ) ) {
					sqlType = ORDINAL_ENUM;
				}
				else if ( preferNativeEnumTypesEnabled && jdbcTypeRegistry.hasRegisteredDescriptor( NAMED_ORDINAL_ENUM ) ) {
					sqlType = NAMED_ORDINAL_ENUM;
				}
				else {
					sqlType = hasManyValues() ? SMALLINT : TINYINT;
				}
				break;
			case STRING:
				if ( jdbcTypeRegistry.hasRegisteredDescriptor( ENUM ) ) {
					sqlType = ENUM;
				}
				else if ( preferNativeEnumTypesEnabled && jdbcTypeRegistry.hasRegisteredDescriptor( NAMED_ENUM ) ) {
					sqlType = NAMED_ENUM;
				}
				else if ( context.getColumnLength() == 1 ) {
					sqlType = context.isNationalized() ? NCHAR : CHAR;
				}
				else {
					sqlType = context.isNationalized() ? NVARCHAR : VARCHAR;
				}
				break;
			default:
				throw new AssertionFailure("unknown EnumType");
		}
		return jdbcTypeRegistry.getDescriptor( sqlType );
	}

	public boolean hasManyValues() {
		// a bit arbitrary, but gives us some headroom
		return getJavaTypeClass().getEnumConstants().length > 128;
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
		else if ( value instanceof String ) {
			return fromName( (String) value );
		}
		else if ( value instanceof Long ) {
			return fromLong( (Long) value );
		}
		else if ( value instanceof Integer ) {
			return fromInteger( (Integer) value );
		}
		else if ( value instanceof Short ) {
			return fromShort( (Short) value );
		}
		else if ( value instanceof Byte ) {
			return fromByte( (Byte) value );
		}
		else if ( value instanceof Number ) {
			return fromLong( ((Number) value).longValue() );
		}

		return (T) value;
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
		if ( domainForm == null ) {
			return null;
		}
		return domainForm.name();
	}

	/**
	 * Interpret a string value as the named value of the enum type
	 */
	public T fromName(String relationalForm) {
		if ( relationalForm == null ) {
			return null;
		}
		return Enum.valueOf( getJavaTypeClass(), relationalForm.trim() );
	}

	@Override
	public String getCheckCondition(String columnName, JdbcType jdbcType, BasicValueConverter<?, ?> converter, Dialect dialect) {
		if ( converter != null ) {
			//TODO: actually convert the enum values to create the check constraint
			return null;
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
}
