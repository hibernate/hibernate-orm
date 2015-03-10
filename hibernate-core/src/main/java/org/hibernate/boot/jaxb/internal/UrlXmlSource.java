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
import java.net.URL;
import java.net.UnknownHostException;

import org.hibernate.boot.MappingException;
import org.hibernate.boot.MappingNotFoundException;
import org.hibernate.boot.jaxb.Origin;
import org.hibernate.boot.jaxb.spi.Binder;
import org.hibernate.boot.jaxb.spi.Binding;
import org.hibernate.boot.jaxb.spi.XmlSource;

/**
 * @author Steve Ebersole
 */
public class UrlXmlSource extends XmlSource {
	private final URL url;

	public UrlXmlSource(Origin origin, URL url) {
		super( origin );
		this.url = url;
	}

	@Override
	public Binding doBind(Binder binder) {
		try {
			InputStream stream = url.openStream();
			return InputStreamXmlSource.doBind( binder, stream, getOrigin(), true );
		}
		catch (UnknownHostException e) {
			throw new MappingNotFoundException( "Invalid URL", e, getOrigin() );
		}
		catch (IOException e) {
			throw new MappingException( "Unable to open URL InputStream", e, getOrigin() );
		}
	}
}
