/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.jaxb.internal;

import java.io.IOException;
import java.io.InputStream;

import org.hibernate.boot.InvalidMappingException;
import org.hibernate.boot.jaxb.Origin;
import org.hibernate.boot.jaxb.spi.Binder;
import org.hibernate.boot.jaxb.spi.Binding;
import org.hibernate.boot.jaxb.spi.XmlSource;
import org.hibernate.internal.CoreLogging;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public class InputStreamXmlSource extends XmlSource {
	private static final Logger log = CoreLogging.logger( InputStreamXmlSource.class );

	private final InputStream inputStream;
	private final boolean autoClose;

	public InputStreamXmlSource(Origin origin, InputStream inputStream, boolean autoClose) {
		super( origin );
		this.inputStream = inputStream;
		this.autoClose = autoClose;
	}

	@Override
	public Binding doBind(Binder binder) {
		return doBind( binder, inputStream, getOrigin(), autoClose );
	}

	public static Binding doBind(Binder binder, InputStream inputStream, Origin origin, boolean autoClose) {
		try {
			return binder.bind( inputStream, origin );
		}
		catch ( Exception e ) {
			throw new InvalidMappingException( origin, e );
		}
		finally {
			if ( autoClose ) {
				try {
					inputStream.close();
				}
				catch ( IOException ignore ) {
					log.trace( "Was unable to close input stream" );
				}
			}
		}
	}
}
