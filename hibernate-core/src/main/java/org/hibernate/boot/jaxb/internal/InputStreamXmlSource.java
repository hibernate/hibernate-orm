/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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
