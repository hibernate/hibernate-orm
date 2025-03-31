/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
	public <T> Binding<T> doBind(Binder<T> binder) {
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
