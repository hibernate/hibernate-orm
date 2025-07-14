/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.spi;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.function.Function;

import org.hibernate.boot.MappingNotFoundException;
import org.hibernate.boot.archive.spi.InputStreamAccess;
import org.hibernate.boot.jaxb.Origin;
import org.hibernate.boot.jaxb.SourceType;
import org.hibernate.boot.jaxb.internal.FileXmlSource;
import org.hibernate.boot.jaxb.internal.InputStreamXmlSource;
import org.hibernate.boot.jaxb.internal.MappingBinder;
import org.hibernate.boot.jaxb.internal.UrlXmlSource;
import org.hibernate.boot.jaxb.spi.JaxbBindableMappingDescriptor;
import org.hibernate.boot.jaxb.spi.Binding;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.service.ServiceRegistry;

import org.jboss.logging.Logger;

/**
 * Holds the XML binder and a classloader used for binding mappings.
 *
 * @apiNote This class is very poorly named.
 *
 * @author Steve Ebersole
 */
public class XmlMappingBinderAccess {
	private static final Logger LOG = Logger.getLogger( XmlMappingBinderAccess.class );

	private final ClassLoaderService classLoaderService;
	private final MappingBinder mappingBinder;

	public XmlMappingBinderAccess(ServiceRegistry serviceRegistry) {
		this.classLoaderService = serviceRegistry.getService( ClassLoaderService.class );
		this.mappingBinder = new MappingBinder( serviceRegistry );
	}

	public XmlMappingBinderAccess(ServiceRegistry serviceRegistry, Function<String, Object> configAccess) {
		this.classLoaderService = serviceRegistry.getService( ClassLoaderService.class );
		this.mappingBinder = new MappingBinder( classLoaderService, configAccess );
	}

	public MappingBinder getMappingBinder() {
		return mappingBinder;
	}

	/**
	 * Create a {@linkplain Binding binding} from a named URL resource
	 */
	public Binding<JaxbBindableMappingDescriptor> bind(String resource) {
		LOG.tracef( "Reading mappings from resource: %s", resource );
		final Origin origin = new Origin( SourceType.RESOURCE, resource );
		final URL url = classLoaderService.locateResource( resource );
		if ( url == null ) {
			throw new MappingNotFoundException( origin );
		}
		return new UrlXmlSource( origin, url ).doBind( getMappingBinder() );
	}

	/**
	 * Create a {@linkplain Binding binding} from a File reference
	 */
	public Binding<JaxbBindableMappingDescriptor> bind(File file) {
		final Origin origin = new Origin( SourceType.FILE, file.getPath() );
		LOG.tracef( "Reading mappings from file: %s", origin.getName() );
		if ( !file.exists() ) {
			throw new MappingNotFoundException( origin );
		}
		return new FileXmlSource( origin, file ).doBind( getMappingBinder() );
	}

	/**
	 * Create a {@linkplain Binding binding} from an input stream
	 */
	public Binding<JaxbBindableMappingDescriptor> bind(InputStreamAccess xmlInputStreamAccess) {
		LOG.tracef( "Reading mappings from InputStreamAccess: %s", xmlInputStreamAccess.getStreamName() );
		final Origin origin = new Origin( SourceType.INPUT_STREAM, xmlInputStreamAccess.getStreamName() );
		final InputStream xmlInputStream = xmlInputStreamAccess.accessInputStream();
		try {
			return new InputStreamXmlSource( origin, xmlInputStream, false ).doBind( mappingBinder );
		}
		finally {
			try {
				xmlInputStream.close();
			}
			catch (IOException e) {
				LOG.debugf( "Unable to close InputStream obtained from InputStreamAccess : %s", xmlInputStreamAccess.getStreamName() );
			}
		}
	}

	/**
	 * Create a {@linkplain Binding binding} from an input stream
	 */
	public Binding<JaxbBindableMappingDescriptor> bind(InputStream xmlInputStream) {
		LOG.trace( "Reading mappings from InputStream" );
		final Origin origin = new Origin( SourceType.INPUT_STREAM, null );
		return new InputStreamXmlSource( origin, xmlInputStream, false ).doBind( getMappingBinder() );
	}

	/**
	 * Create a {@linkplain Binding binding} from a URL
	 */
	public Binding<JaxbBindableMappingDescriptor> bind(URL url) {
		final String urlExternalForm = url.toExternalForm();
		LOG.tracef( "Reading mapping document from URL: %s", urlExternalForm );
		final Origin origin = new Origin( SourceType.URL, urlExternalForm );
		return new UrlXmlSource( origin, url ).doBind( getMappingBinder() );
	}
}
