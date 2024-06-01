/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.java;

import java.io.Reader;
import java.io.StringReader;
import java.sql.Clob;
import java.sql.NClob;
import java.util.Arrays;

import org.hibernate.engine.jdbc.CharacterStream;
import org.hibernate.engine.jdbc.internal.CharacterStreamImpl;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.jdbc.AdjustableJdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeIndicators;
import org.hibernate.type.descriptor.jdbc.JdbcTypeJavaClassMappings;

/**
 * Descriptor for {@code Character[]} handling, which disallows {@code null} elements.
 * This {@link JavaType} is useful if the domain model uses {@code Character[]} and wants to map to {@link SqlTypes#VARCHAR}.
 *
 * @author Steve Ebersole
 */
public class CharacterArrayJavaType extends AbstractClassJavaType<Character[]> {
	public static final CharacterArrayJavaType INSTANCE = new CharacterArrayJavaType();

	@SuppressWarnings("unchecked")
	public CharacterArrayJavaType() {
		super( Character[].class, ArrayMutabilityPlan.INSTANCE, IncomparableComparator.INSTANCE );
	}

	@Override
	public String toString(Character[] value) {
		return new String( unwrapChars( value ) );
	}

	@Override
	public Character[] fromString(CharSequence string) {
		return wrapChars( string.toString().toCharArray() );
	}

	@Override
	public boolean areEqual(Character[] one, Character[] another) {
		return one == another
				|| ( one != null && another != null && Arrays.equals( one, another ) );
	}

	@Override
	public int extractHashCode(Character[] chars) {
		int hashCode = 1;
		for ( Character aChar : chars ) {
			hashCode = 31 * hashCode + aChar;
		}
		return hashCode;
	}

	@Override
	public JdbcType getRecommendedJdbcType(JdbcTypeIndicators indicators) {
		// match legacy behavior
		final JdbcType descriptor = indicators.getJdbcType( indicators.resolveJdbcTypeCode( SqlTypes.VARCHAR ) );
		return descriptor instanceof AdjustableJdbcType
				? ( (AdjustableJdbcType) descriptor ).resolveIndicatedType( indicators, this )
				: descriptor;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <X> X unwrap(Character[] value, Class<X> type, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}
		if ( Character[].class.isAssignableFrom( type ) ) {
			return (X) value;
		}
		if ( String.class.isAssignableFrom( type ) ) {
			return (X) new String( unwrapChars( value ) );
		}
		if ( NClob.class.isAssignableFrom( type ) ) {
			return (X) options.getLobCreator().createNClob( new String( unwrapChars( value ) ) );
		}
		if ( Clob.class.isAssignableFrom( type ) ) {
			return (X) options.getLobCreator().createClob( new String( unwrapChars( value ) ) );
		}
		if ( Reader.class.isAssignableFrom( type ) ) {
			return (X) new StringReader( new String( unwrapChars( value ) ) );
		}
		if ( CharacterStream.class.isAssignableFrom( type ) ) {
			return (X) new CharacterStreamImpl( new String( unwrapChars( value ) ) );
		}
		throw unknownUnwrap( type );
	}
	@Override
	public <X> Character[] wrap(X value, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}
		if (value instanceof Character[]) {
			return (Character[]) value;
		}
		if (value instanceof String) {
			return wrapChars( ( (String) value ).toCharArray() );
		}
		if (value instanceof Clob) {
			return wrapChars( DataHelper.extractString( ( (Clob) value ) ).toCharArray() );
		}
		if (value instanceof Reader) {
			return wrapChars( DataHelper.extractString( (Reader) value ).toCharArray() );
		}
		throw unknownWrap( value.getClass() );
	}

	private Character[] wrapChars(char[] chars) {
		if ( chars == null ) {
			return null;
		}
		final Character[] result = new Character[chars.length];
		for ( int i = 0; i < chars.length; i++ ) {
			result[i] = chars[i];
		}
		return result;
	}

	private char[] unwrapChars(Character[] chars) {
		if ( chars == null ) {
			return null;
		}
		final char[] result = new char[chars.length];
		for ( int i = 0; i < chars.length; i++ ) {
			result[i] = chars[i];
		}
		return result;
	}
}
