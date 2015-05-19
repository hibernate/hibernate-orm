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

import org.hibernate.engine.jdbc.CharacterStream;
import org.hibernate.engine.jdbc.internal.CharacterStreamImpl;
import org.hibernate.type.descriptor.WrapperOptions;

/**
 * Descriptor for {@link String} handling.
 *
 * @author Steve Ebersole
 */
public class StringTypeDescriptor extends AbstractTypeDescriptor<String> {
	public static final StringTypeDescriptor INSTANCE = new StringTypeDescriptor();

	public StringTypeDescriptor() {
		super( String.class );
	}

	public String toString(String value) {
		return value;
	}

	public String fromString(String string) {
		return string;
	}

	@SuppressWarnings({ "unchecked" })
	public <X> X unwrap(String value, Class<X> type, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}
		if ( String.class.isAssignableFrom( type ) ) {
			return (X) value;
		}
		if ( Reader.class.isAssignableFrom( type ) ) {
			return (X) new StringReader( value );
		}
		if ( CharacterStream.class.isAssignableFrom( type ) ) {
			return (X) new CharacterStreamImpl( value );
		}
		if ( Clob.class.isAssignableFrom( type ) ) {
			return (X) options.getLobCreator().createClob( value );
		}
		if ( DataHelper.isNClob( type ) ) {
			return (X) options.getLobCreator().createNClob( value );
		}

		throw unknownUnwrap( type );
	}

	public <X> String wrap(X value, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}
		if ( String.class.isInstance( value ) ) {
			return (String) value;
		}
		if ( Reader.class.isInstance( value ) ) {
			return DataHelper.extractString( (Reader) value );
		}
		if ( Clob.class.isInstance( value ) ) {
			return DataHelper.extractString( (Clob) value );
		}

		throw unknownWrap( value.getClass() );
	}
}
