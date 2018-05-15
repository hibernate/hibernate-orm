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

import org.hibernate.engine.jdbc.CharacterStream;
import org.hibernate.engine.jdbc.internal.CharacterStreamImpl;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.descriptor.java.spi.AbstractBasicJavaDescriptor;
import org.hibernate.type.descriptor.java.spi.ArrayMutabilityPlan;
import org.hibernate.type.descriptor.spi.JdbcRecommendedSqlTypeMappingContext;
import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;

/**
 * Descriptor for {@code char[]} handling.
 *
 * @author Steve Ebersole
 */
public class PrimitiveCharacterArrayJavaDescriptor extends AbstractBasicJavaDescriptor<char[]> {
	public static final PrimitiveCharacterArrayJavaDescriptor INSTANCE = new PrimitiveCharacterArrayJavaDescriptor();

	@SuppressWarnings({ "unchecked" })
	protected PrimitiveCharacterArrayJavaDescriptor() {
		super( char[].class, ArrayMutabilityPlan.INSTANCE );
	}

	public String toString(char[] value) {
		return new String( value );
	}

	public char[] fromString(String string) {
		return string.toCharArray();
	}

	@Override
	public boolean areEqual(char[] one, char[] another) {
		return one == another
				|| ( one != null && another != null && Arrays.equals( one, another ) );
	}

	@Override
	public SqlTypeDescriptor getJdbcRecommendedSqlType(JdbcRecommendedSqlTypeMappingContext context) {
		return CharacterArrayJavaDescriptor.INSTANCE.getJdbcRecommendedSqlType( context );
	}

	@Override
	public int extractHashCode(char[] chars) {
		int hashCode = 1;
		for ( char aChar : chars ) {
			hashCode = 31 * hashCode + aChar;
		}
		return hashCode;
	}

	@SuppressWarnings({ "unchecked" })
	public <X> X unwrap(char[] value, Class<X> type, SharedSessionContractImplementor session) {
		if ( value == null ) {
			return null;
		}
		if ( char[].class.isAssignableFrom( type ) ) {
			return (X) value;
		}
		if ( String.class.isAssignableFrom( type ) ) {
			return (X) new String( value );
		}
		if ( Clob.class.isAssignableFrom( type ) ) {
			return (X) session.getLobCreator().createClob( new String( value ) );
		}
		if ( Reader.class.isAssignableFrom( type ) ) {
			return (X) new StringReader( new String( value ) );
		}
		if ( CharacterStream.class.isAssignableFrom( type ) ) {
			return (X) new CharacterStreamImpl( new String( value ) );
		}
		throw unknownUnwrap( type );
	}

	public <X> char[] wrap(X value, SharedSessionContractImplementor session) {
		if ( value == null ) {
			return null;
		}
		if ( char[].class.isInstance( value ) ) {
			return (char[]) value;
		}
		if ( String.class.isInstance( value ) ) {
			return ( (String) value ).toCharArray();
		}
		if ( Clob.class.isInstance( value ) ) {
			return LobStreamDataHelper.extractString( ( (Clob) value ) ).toCharArray();
		}
		if ( Reader.class.isInstance( value ) ) {
			return LobStreamDataHelper.extractString( ( (Reader) value ) ).toCharArray();
		}
		throw unknownWrap( value.getClass() );
	}
}
