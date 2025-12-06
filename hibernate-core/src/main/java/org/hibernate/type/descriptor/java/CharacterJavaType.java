/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.java;

import org.hibernate.dialect.Dialect;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.spi.PrimitiveJavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;

/**
 * Descriptor for {@link Character} handling.
 *
 * @author Steve Ebersole
 */
public class CharacterJavaType extends AbstractClassJavaType<Character> implements
		PrimitiveJavaType<Character> {
	public static final CharacterJavaType INSTANCE = new CharacterJavaType();

	public CharacterJavaType() {
		super( Character.class );
	}

	@Override
	public boolean useObjectEqualsHashCode() {
		return true;
	}

	@Override
	public String toString(Character value) {
		return value.toString();
	}

	@Override
	public Character fromString(CharSequence string) {
		if ( string.length() != 1 ) {
			throw new CoercionException( "value must contain exactly one character: '" + string + "'" );
		}
		return string.charAt( 0 );
	}

	@Override
	public boolean isInstance(Object value) {
		return value instanceof Character;
	}

	@Override
	public Character cast(Object value) {
		return (Character) value;
	}

	@Override
	public <X> X unwrap(Character value, Class<X> type, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}
		if ( Character.class.isAssignableFrom( type ) || type == Object.class ) {
			return type.cast( value );
		}
		if ( String.class.isAssignableFrom( type ) ) {
			return type.cast( value.toString() );
		}
		if ( Number.class.isAssignableFrom( type ) ) {
			return type.cast( (short) value.charValue() );
		}
		throw unknownUnwrap( type );
	}

	@Override
	public <X> Character wrap(X value, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}
		else if (value instanceof Character character) {
			return character;
		}
		else if (value instanceof String string) {
			switch ( string.length() ) {
				case 1:
					return string.charAt( 0 );
				case 0:
					if ( options.getDialect().stripsTrailingSpacesFromChar() ) {
						// we previously stored char values in char(1) columns on MySQL
						// but MySQL strips trailing spaces from the value when read
						return ' ';
					}
					else {
						throw new CoercionException( "value does not contain a character: '" + string + "'" );
					}
				default:
					throw new CoercionException( "value contains more than one character: '" + string + "'" );
			}
		}
		else if (value instanceof Number number) {
			return (char) number.shortValue();
		}
		else {
			throw unknownWrap( value.getClass() );
		}
	}

	@Override
	public Class<?> getPrimitiveClass() {
		return char.class;
	}

	@Override
	public Class<Character[]> getArrayClass() {
		return Character[].class;
	}

	@Override
	public Class<?> getPrimitiveArrayClass() {
		return char[].class;
	}

	@Override
	public Character getDefaultValue() {
		return 0;
	}

	@Override
	public long getDefaultSqlLength(Dialect dialect, JdbcType jdbcType) {
		return 1;
	}

	@Override
	public int getDefaultSqlPrecision(Dialect dialect, JdbcType jdbcType) {
		return 3;
	}

	@Override
	public int getDefaultSqlScale(Dialect dialect, JdbcType jdbcType) {
		return 0;
	}

}
