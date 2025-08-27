/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.jaxb.internal;

import org.hibernate.boot.MappingException;
import org.hibernate.boot.MappingNotFoundException;
import org.hibernate.boot.jaxb.Origin;
import org.hibernate.boot.jaxb.SourceType;
import org.hibernate.boot.jaxb.spi.Binding;
import org.hibernate.boot.jaxb.spi.JaxbBindableMappingDescriptor;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.UnknownHostException;

import static org.hibernate.boot.jaxb.JaxbLogger.JAXB_LOGGER;

/**
 * Support for processing mapping XML from a {@linkplain URL} reference.
 *
 * @see MappingBinder
 *
 * @author Steve Ebersole
 */
public class UrlXmlSource {
	/**
	 * Create a mapping {@linkplain Binding binding} from a classpath resource (via URL).
	 *
	 * @see #fromUrl(URL, Origin, MappingBinder)
	 */
	public static Binding<? extends JaxbBindableMappingDescriptor> fromResource(
			String resourceName,
			ClassLoaderService classLoaderService,
			MappingBinder mappingBinder) {
		JAXB_LOGGER.tracef( "Reading mappings from resource: %s", resourceName );

		final Origin origin = new Origin( SourceType.RESOURCE, resourceName );
		final URL url = classLoaderService.locateResource( resourceName );
		if ( url == null ) {
			throw new MappingNotFoundException( origin );
		}

		return fromUrl( url, origin, mappingBinder );
	}

	/**
	 * Create a mapping {@linkplain Binding binding} from a URL
	 *
	 * @see #fromUrl(URL, Origin, MappingBinder)
	 */
	public static Binding<? extends JaxbBindableMappingDescriptor> fromUrl(
			URL url,
			MappingBinder mappingBinder) {
		final Origin origin = new Origin( SourceType.URL, url.toExternalForm() );
		return fromUrl( url, origin, mappingBinder );
	}

	/**
	 * Create a mapping {@linkplain Binding binding} from a URL
	 *
	 * @param url The url from which to read the mapping information
	 * @param origin Description of the source from which the url came
	 * @param binder The JAXB binder to use
	 */
	public static Binding<? extends JaxbBindableMappingDescriptor> fromUrl(
			URL url,
			Origin origin,
			MappingBinder binder) {
		JAXB_LOGGER.tracef( "Reading mapping document from URL: %s", origin.getName() );

		try {
			InputStream stream = url.openStream();
			return InputStreamXmlSource.fromStream( stream, origin, true, binder );
		}
		catch (UnknownHostException e) {
			throw new MappingNotFoundException( "Invalid URL", e, origin );
		}
		catch (IOException e) {
			throw new MappingException( "Unable to open URL InputStream", e, origin );
		}
	}
}
