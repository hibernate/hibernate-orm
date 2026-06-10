/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.spi;

import org.hibernate.boot.archive.spi.InputStreamAccess;
import org.hibernate.boot.jaxb.internal.FileXmlSource;
import org.hibernate.boot.jaxb.internal.InputStreamAccessXmlSource;
import org.hibernate.boot.jaxb.internal.InputStreamXmlSource;
import org.hibernate.boot.jaxb.internal.MappingBinder;
import org.hibernate.boot.jaxb.internal.UrlXmlSource;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityMappingsImpl;
import org.hibernate.boot.jaxb.spi.Binding;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.service.ServiceRegistry;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.function.Function;

/**
 * Holds the XML binder and a classloader used for binding mappings, as well
 * as access to methods to perform binding of sources of mapping XML.
 *
 * @apiNote This class is very poorly named.
 *
 * @author Steve Ebersole
 */
public class XmlMappingBinderAccess {
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
	 *
	 * @see UrlXmlSource#fromUrl
	 */
	public Binding<JaxbEntityMappingsImpl> bind(String resource) {
		return UrlXmlSource.fromResource( resource, classLoaderService, getMappingBinder() );
	}

	/**
	 * Create a {@linkplain Binding binding} from a File reference
	 *
	 * @see FileXmlSource#fromFile
	 */
	public Binding<JaxbEntityMappingsImpl> bind(File file) {
		return FileXmlSource.fromFile( file, getMappingBinder() );
	}

	/**
	 * Create a {@linkplain Binding binding} from an input stream
	 *
	 * @see InputStreamAccessXmlSource#fromStreamAccess
	 */
	public Binding<JaxbEntityMappingsImpl> bind(InputStreamAccess xmlInputStreamAccess) {
		return InputStreamAccessXmlSource.fromStreamAccess( xmlInputStreamAccess, getMappingBinder() );
	}

	/**
	 * Create a {@linkplain Binding binding} from an input stream
	 *
	 * @see InputStreamXmlSource#fromStream
	 */
	public Binding<JaxbEntityMappingsImpl> bind(InputStream xmlInputStream) {
		return InputStreamXmlSource.fromStream( xmlInputStream, getMappingBinder() );
	}

	/**
	 * Create a {@linkplain Binding binding} from a URL
	 *
	 * @see UrlXmlSource#fromUrl
	 */
	public Binding<JaxbEntityMappingsImpl> bind(URL url) {
		return UrlXmlSource.fromUrl( url, getMappingBinder() );
	}
}
