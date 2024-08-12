/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.java;

import java.net.MalformedURLException;
import java.net.URL;

import org.hibernate.HibernateException;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeIndicators;

/**
 * Descriptor for {@link URL} handling.
 *
 * @author Steve Ebersole
 */
public class UrlJavaType extends AbstractClassJavaType<URL> {
	public static final UrlJavaType INSTANCE = new UrlJavaType();

	public UrlJavaType() {
		super( URL.class );
	}

	@Override
	public JdbcType getRecommendedJdbcType(JdbcTypeIndicators context) {
		return context.getJdbcType( SqlTypes.VARCHAR );
	}

	@Override
	public boolean useObjectEqualsHashCode() {
		return true;
	}

	public String toString(URL value) {
		return value.toExternalForm();
	}

	public URL fromString(CharSequence string) {
		try {
			return new URL( string.toString() );
		}
		catch ( MalformedURLException e ) {
			throw new HibernateException( "Unable to convert string [" + string + "] to URL : " + e );
		}
	}

	@SuppressWarnings("unchecked")
	public <X> X unwrap(URL value, Class<X> type, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}
		if ( URL.class.isAssignableFrom( type ) ) {
			return (X) value;
		}
		if ( String.class.isAssignableFrom( type ) ) {
			return (X) toString( value );
		}
		throw unknownUnwrap( type );
	}

	public <X> URL wrap(X value, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}
		if (value instanceof URL) {
			return (URL) value;
		}
		if (value instanceof CharSequence) {
			return fromString( (CharSequence) value );
		}
		throw unknownWrap( value.getClass() );
	}

}
