/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.java;

import org.hibernate.HibernateException;
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
	public String toString(Character value) {
		return value.toString();
	}

	@Override
	public Character fromString(CharSequence string) {
		if ( string.length() != 1 ) {
			throw new HibernateException( "multiple or zero characters found parsing string" );
		}
		return string.charAt( 0 );
	}

	@SuppressWarnings("unchecked")
	@Override
	public <X> X unwrap(Character value, Class<X> type, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}
		if ( Character.class.isAssignableFrom( type ) || type == Object.class ) {
			return (X) value;
		}
		if ( String.class.isAssignableFrom( type ) ) {
			return (X) value.toString();
		}
		if ( Number.class.isAssignableFrom( type ) ) {
			return (X) Short.valueOf( (short) value.charValue() );
		}
		throw unknownUnwrap( type );
	}

	@Override
	public <X> Character wrap(X value, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}
		if (value instanceof Character) {
			return (Character) value;
		}
		if ( value instanceof String ) {
			if ( value.equals( "" ) ) {
				return ' ';
			}
			final String str = (String) value;
			return str.charAt( 0 );
		}
		if (value instanceof Number) {
			final Number nbr = (Number) value;
			return (char) nbr.shortValue();
		}
		throw unknownWrap( value.getClass() );
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
