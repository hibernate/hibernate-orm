/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.java;

import org.hibernate.dialect.Dialect;
import org.hibernate.internal.util.CharSequenceHelper;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.converter.spi.BasicValueConverter;
import org.hibernate.type.descriptor.java.spi.PrimitiveJavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;

/**
 * Descriptor for {@link Boolean} handling.
 *
 * @see org.hibernate.cfg.AvailableSettings#PREFERRED_BOOLEAN_JDBC_TYPE
 *
 * @author Steve Ebersole
 */
public class BooleanJavaType extends AbstractClassJavaType<Boolean> implements
		PrimitiveJavaType<Boolean> {
	public static final BooleanJavaType INSTANCE = new BooleanJavaType();

	private final char characterValueTrue;
	private final char characterValueFalse;

	private final char characterValueTrueLC;

	private final String stringValueTrue;
	private final String stringValueFalse;

	public BooleanJavaType() {
		this( 'Y', 'N' );
	}

	public BooleanJavaType(char characterValueTrue, char characterValueFalse) {
		super( Boolean.class );
		this.characterValueTrue = Character.toUpperCase( characterValueTrue );
		this.characterValueFalse = Character.toUpperCase( characterValueFalse );

		characterValueTrueLC = Character.toLowerCase( characterValueTrue );

		stringValueTrue = String.valueOf( characterValueTrue );
		stringValueFalse = String.valueOf( characterValueFalse );
	}

	@Override
	public boolean useObjectEqualsHashCode() {
		return true;
	}

	@Override
	public String toString(Boolean value) {
		return value == null ? null : value.toString();
	}

	@Override
	public Boolean fromString(CharSequence string) {
		return Boolean.valueOf( string.toString() );
	}

	@Override
	public boolean isInstance(Object value) {
		return value instanceof Boolean;
	}

	@Override
	public Boolean fromEncodedString(CharSequence charSequence, int start, int end) {
		switch ( charSequence.charAt( start ) ) {
			case 't':
			case 'T':
				return CharSequenceHelper.regionMatchesIgnoreCase( charSequence, start + 1, "rue", 0, 3 );
			case 'y':
			case 'Y':
				return end == start + 1;
		}
		return false;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <X> X unwrap(Boolean value, Class<X> type, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}
		if ( Boolean.class.isAssignableFrom( type ) || type == Object.class ) {
			return (X) value;
		}
		if ( Byte.class.isAssignableFrom( type ) ) {
			return (X) toByte( value );
		}
		if ( Short.class.isAssignableFrom( type ) ) {
			return (X) toShort( value );
		}
		if ( Integer.class.isAssignableFrom( type ) ) {
			return (X) toInteger( value );
		}
		if ( Long.class.isAssignableFrom( type ) ) {
			return (X) toLong( value );
		}
		if ( Character.class.isAssignableFrom( type ) ) {
			return (X) Character.valueOf( value ? characterValueTrue : characterValueFalse );
		}
		if ( String.class.isAssignableFrom( type ) ) {
			return (X) (value ? stringValueTrue : stringValueFalse);
		}
		throw unknownUnwrap( type );
	}

	@Override
	public <X> Boolean wrap(X value, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}
		if (value instanceof Boolean booleanValue) {
			return booleanValue;
		}
		if (value instanceof Number number) {
			return number.intValue() != 0;
		}
		if (value instanceof Character character) {
			return isTrue( character );
		}
		if (value instanceof String string) {
			return isTrue( string );
		}
		throw unknownWrap( value.getClass() );
	}

	private boolean isTrue(String strValue) {
		return strValue != null && !strValue.isEmpty() && isTrue( strValue.charAt(0) );
	}

	private boolean isTrue(char charValue) {
		return charValue == characterValueTrue || charValue == characterValueTrueLC;
	}

	public int toInt(Boolean value) {
		return value ? 1 : 0;
	}

	public Byte toByte(Boolean value) {
		return (byte) toInt( value );
	}

	public Short toShort(Boolean value) {
		return (short) toInt( value );
	}

	public Integer toInteger(Boolean value) {
		return toInt( value );
	}

	public Long toLong(Boolean value) {
		return (long) toInt( value );
	}

	@Override
	public Class<?> getPrimitiveClass() {
		return boolean.class;
	}

	@Override
	public Class<Boolean[]> getArrayClass() {
		return Boolean[].class;
	}

	@Override
	public Class<?> getPrimitiveArrayClass() {
		return boolean[].class;
	}

	@Override
	public Boolean getDefaultValue() {
		return false;
	}

	@Override
	public long getDefaultSqlLength(Dialect dialect, JdbcType jdbcType) {
		return 1;
	}

	@Override
	public int getDefaultSqlPrecision(Dialect dialect, JdbcType jdbcType) {
		return 1;
	}

	@Override
	public int getDefaultSqlScale(Dialect dialect, JdbcType jdbcType) {
		return 0;
	}

	@Override
	public String getCheckCondition(String columnName, JdbcType jdbcType, BasicValueConverter<Boolean, ?> converter, Dialect dialect) {
		if ( converter != null ) {
			if ( jdbcType.isString() ) {
				final Object falseValue = converter.toRelationalValue( false );
				final Object trueValue = converter.toRelationalValue( true );
				final String[] values = getPossibleStringValues( converter, falseValue, trueValue );
				return dialect.getCheckCondition( columnName, values );
			}
			else if ( jdbcType.isInteger() ) {
				@SuppressWarnings("unchecked")
				final BasicValueConverter<Boolean, ? extends Number> numericConverter =
						(BasicValueConverter<Boolean, ? extends Number>) converter;
				final Number falseValue = numericConverter.toRelationalValue( false );
				final Number trueValue = numericConverter.toRelationalValue( true );
				final Long[] values = getPossibleNumericValues( numericConverter, falseValue, trueValue );
				return dialect.getCheckCondition( columnName, values );
			}
		}
		return null;
	}

	private static Long[] getPossibleNumericValues(
			BasicValueConverter<Boolean, ? extends Number> numericConverter,
			Number falseValue,
			Number trueValue) {
		Number nullValue = null;
		try {
			nullValue = numericConverter.toRelationalValue( null );
		}
		catch ( NullPointerException ignored ) {
		}
		final Long[] values = new Long[nullValue != null ? 3 : 2];
		values[0] = falseValue != null ? falseValue.longValue() : null;
		values[1] = trueValue != null ? trueValue.longValue() : null;
		if ( nullValue != null ) {
			values[2] = nullValue.longValue();
		}
		return values;
	}

	private static String[] getPossibleStringValues(
			BasicValueConverter<Boolean, ?> stringConverter,
			Object falseValue,
			Object trueValue) {
		Object nullValue = null;
		try {
			nullValue =  stringConverter.toRelationalValue( null);
		}
		catch ( NullPointerException ignored ) {
		}
		final String[] values = new String[nullValue != null ? 3 : 2];
		values[0] = falseValue != null ? falseValue.toString() : null;
		values[1] = trueValue != null ? trueValue.toString() : null;
		if ( nullValue != null ) {
			values[2] =  nullValue.toString();
		}
		return values;
	}
}
