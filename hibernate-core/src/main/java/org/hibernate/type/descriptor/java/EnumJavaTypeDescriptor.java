/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.java;

import java.sql.Types;
import jakarta.persistence.EnumType;

import org.hibernate.dialect.Dialect;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeDescriptorIndicators;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;

/**
 * Describes a Java Enum type.
 *
 * @author Steve Ebersole
 */
public class EnumJavaTypeDescriptor<T extends Enum<T>> extends AbstractClassJavaTypeDescriptor<T> {
	@SuppressWarnings("unchecked")
	public EnumJavaTypeDescriptor(Class<T> type) {
		super( type, ImmutableMutabilityPlan.INSTANCE );
	}

	@Override
	public JdbcType getRecommendedJdbcType(JdbcTypeDescriptorIndicators context) {
		JdbcTypeRegistry registry = context.getTypeConfiguration().getJdbcTypeDescriptorRegistry();
		if ( context.getEnumeratedType() != null && context.getEnumeratedType() == EnumType.STRING ) {
			if ( context.getColumnLength() == 1 ) {
				return context.isNationalized()
						? registry.getDescriptor( Types.NCHAR )
						: registry.getDescriptor( Types.CHAR );
			}

			return context.isNationalized()
					? registry.getDescriptor( Types.NVARCHAR )
					: registry.getDescriptor( Types.VARCHAR );
		}
		else {
			return registry.getDescriptor( Types.TINYINT );
		}
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
	 * Interpret a String value as the named value of the enum type
	 */
	public T fromName(String relationalForm) {
		if ( relationalForm == null ) {
			return null;
		}
		return Enum.valueOf( getJavaTypeClass(), relationalForm.trim() );
	}

	@Override
	public String getCheckCondition(String columnName, JdbcType jdbcType, Dialect dialect) {
		return dialect.getEnumCheckCondition( columnName, jdbcType.getJdbcTypeCode(), getJavaTypeClass() );
	}
}
