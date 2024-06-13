/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
	public String toString(Boolean value) {
		return value == null ? null : value.toString();
	}

	@Override
	public Boolean fromString(CharSequence string) {
		return Boolean.valueOf( string.toString() );
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
		if (value instanceof Boolean) {
			return (Boolean) value;
		}
		if (value instanceof Number) {
			final int intValue = ( (Number) value ).intValue();
			return intValue != 0;
		}
		if (value instanceof Character) {
			return isTrue( (Character) value );
		}
		if (value instanceof String) {
			return isTrue( (String) value );
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
	public String getCheckCondition(String columnName, JdbcType jdbcType, BasicValueConverter<?, ?> converter, Dialect dialect) {
		if ( converter != null ) {
			if ( jdbcType.isString() ) {
				@SuppressWarnings("unchecked")
				BasicValueConverter<Boolean, ?> stringConverter =
						(BasicValueConverter<Boolean, ?>) converter;
				final Object falseValue = stringConverter.toRelationalValue( false );
				final Object trueValue = stringConverter.toRelationalValue( true );
				String[] values = new String[] {
						falseValue != null ? falseValue.toString() : null,
						trueValue != null ? trueValue.toString() : null
				};
				return dialect.getCheckCondition( columnName, values );
			}
			else if ( jdbcType.isInteger() ) {
				@SuppressWarnings("unchecked")
				BasicValueConverter<Boolean, ? extends Number> numericConverter =
						(BasicValueConverter<Boolean, ? extends Number>) converter;
				final Number falseValue = numericConverter.toRelationalValue( false );
				final Number trueValue = numericConverter.toRelationalValue( true );
				Long[] values = new Long[] {
						falseValue != null ? Long.valueOf(falseValue.longValue()) : null,
						trueValue != null ? Long.valueOf(trueValue.longValue()) : null
				};
				return dialect.getCheckCondition( columnName, values );
			}
		}
		return null;
	}
}
