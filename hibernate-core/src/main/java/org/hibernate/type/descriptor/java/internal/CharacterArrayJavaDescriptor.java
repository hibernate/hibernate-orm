/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.java.internal;

import java.io.Reader;
import java.io.StringReader;
import java.sql.Clob;
import java.util.Arrays;
import java.util.Comparator;

import org.hibernate.engine.jdbc.CharacterStream;
import org.hibernate.engine.jdbc.internal.CharacterStreamImpl;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.descriptor.java.spi.AbstractBasicJavaDescriptor;
import org.hibernate.type.descriptor.java.spi.ArrayMutabilityPlan;
import org.hibernate.type.descriptor.spi.IncomparableComparator;
import org.hibernate.type.descriptor.spi.JdbcRecommendedSqlTypeMappingContext;
import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;

/**
 * Descriptor for {@code Character[]} handling.
 *
 * @author Steve Ebersole
 */
public class CharacterArrayJavaDescriptor extends AbstractBasicJavaDescriptor<Character[]> {
	public static final CharacterArrayJavaDescriptor INSTANCE = new CharacterArrayJavaDescriptor();

	@SuppressWarnings({ "unchecked" })
	public CharacterArrayJavaDescriptor() {
		super( Character[].class, ArrayMutabilityPlan.INSTANCE );
	}

	@Override
	public String toString(Character[] value) {
		return new String( unwrapChars( value ) );
	}

	@Override
	public Character[] fromString(String string) {
		return wrapChars( string.toCharArray() );
	}

	@Override
	public boolean areEqual(Character[] one, Character[] another) {
		return one == another
				|| ( one != null && another != null && Arrays.equals( one, another ) );
	}

	@Override
	public SqlTypeDescriptor getJdbcRecommendedSqlType(JdbcRecommendedSqlTypeMappingContext context) {
		return StringJavaDescriptor.INSTANCE.getJdbcRecommendedSqlType( context );
	}

	@Override
	public Comparator<Character[]> getComparator() {
		return IncomparableComparator.INSTANCE;
	}

	@Override
	public int extractHashCode(Character[] chars) {
		int hashCode = 1;
		for ( Character aChar : chars ) {
			hashCode = 31 * hashCode + aChar;
		}
		return hashCode;
	}

	@SuppressWarnings({ "unchecked" })
	@Override
	public <X> X unwrap(Character[] value, Class<X> type, SharedSessionContractImplementor session) {
		if ( value == null ) {
			return null;
		}
		if ( Character[].class.isAssignableFrom( type ) ) {
			return (X) value;
		}
		if ( String.class.isAssignableFrom( type ) ) {
			return (X) new String( unwrapChars( value ) );
		}
		if ( Clob.class.isAssignableFrom( type ) ) {
			return (X) session.getLobCreator().createClob( new String( unwrapChars( value ) ) );
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
	public <X> Character[] wrap(X value, SharedSessionContractImplementor session) {
		if ( value == null ) {
			return null;
		}
		if ( Character[].class.isInstance( value ) ) {
			return (Character[]) value;
		}
		if ( String.class.isInstance( value ) ) {
			return wrapChars( ( (String) value ).toCharArray() );
		}
		if ( Clob.class.isInstance( value ) ) {
			return wrapChars( LobStreamDataHelper.extractString( ( (Clob) value ) ).toCharArray() );
		}
		if ( Reader.class.isInstance( value ) ) {
			return wrapChars( LobStreamDataHelper.extractString( (Reader) value ).toCharArray() );
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
