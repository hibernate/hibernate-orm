/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.spi;

import org.hibernate.Remove;
import org.hibernate.Internal;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.service.spi.ServiceRegistryImplementor;

import static org.hibernate.cfg.MappingSettings.XML_MAPPING_ENABLED;
import static org.hibernate.engine.config.spi.StandardConverters.BOOLEAN;
import org.hibernate.boot.archive.spi.InputStreamAccess;
import org.hibernate.boot.jaxb.internal.FileXmlSource;
import org.hibernate.boot.jaxb.internal.InputStreamAccessXmlSource;
import org.hibernate.boot.jaxb.internal.InputStreamXmlSource;
import org.hibernate.boot.jaxb.internal.MappingBinder;
import org.hibernate.boot.jaxb.internal.UrlXmlSource;
import org.hibernate.boot.jaxb.spi.Binding;
import org.hibernate.boot.jaxb.spi.JaxbBindableMappingDescriptor;
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
@Remove
public class XmlMappingBinderAccess {
	private final ClassLoaderService classLoaderService;
	private final MappingBinder mappingBinder;
	private final Function<String, Object> configAccess;

	public XmlMappingBinderAccess(ServiceRegistry serviceRegistry) {
		this( serviceRegistry, settingName -> {
			final var configurationService = serviceRegistry instanceof ServiceRegistryImplementor implementor
					? implementor.fromRegistryOrChildren( ConfigurationService.class )
					: serviceRegistry.getService( ConfigurationService.class );
			return configurationService == null ? null : configurationService.getSettings().get( settingName );
		} );
	}

	public XmlMappingBinderAccess(ServiceRegistry serviceRegistry, Function<String, Object> configAccess) {
		this.classLoaderService = serviceRegistry.getService( ClassLoaderService.class );
		this.mappingBinder = new MappingBinder( classLoaderService, configAccess );
		this.configAccess = configAccess;
	}

	/// Whether mapping XML may be accessed and bound using the current settings.
	@Internal
	public boolean isXmlMappingEnabled() {
		final var setting = configAccess == null ? null : configAccess.apply( XML_MAPPING_ENABLED );
		return setting == null || BOOLEAN.convert( setting );
	}

	public MappingBinder getMappingBinder() {
		return mappingBinder;
	}

	/**
	 * Create a {@linkplain Binding binding} from a named URL resource
	 *
	 * @see UrlXmlSource#fromUrl
	 */
	public Binding<JaxbBindableMappingDescriptor> bind(String resource) {
		//noinspection unchecked,rawtypes
		return (Binding) UrlXmlSource.fromResource( resource, classLoaderService, getMappingBinder() );
	}

	/**
	 * Create a {@linkplain Binding binding} from a File reference
	 *
	 * @see FileXmlSource#fromFile
	 */
	public Binding<JaxbBindableMappingDescriptor> bind(File file) {
		//noinspection unchecked,rawtypes
		return (Binding) FileXmlSource.fromFile( file, getMappingBinder() );
	}

	/**
	 * Create a {@linkplain Binding binding} from an input stream
	 *
	 * @see InputStreamAccessXmlSource#fromStreamAccess
	 */
	public Binding<JaxbBindableMappingDescriptor> bind(InputStreamAccess xmlInputStreamAccess) {
		//noinspection unchecked,rawtypes
		return (Binding) InputStreamAccessXmlSource.fromStreamAccess( xmlInputStreamAccess, getMappingBinder() );
	}

	/**
	 * Create a {@linkplain Binding binding} from an input stream
	 *
	 * @see InputStreamXmlSource#fromStream
	 */
	public Binding<JaxbBindableMappingDescriptor> bind(InputStream xmlInputStream) {
		//noinspection unchecked,rawtypes
		return (Binding) InputStreamXmlSource.fromStream( xmlInputStream, getMappingBinder() );
	}

	/**
	 * Create a {@linkplain Binding binding} from a URL
	 *
	 * @see UrlXmlSource#fromUrl
	 */
	public Binding<JaxbBindableMappingDescriptor> bind(URL url) {
		//noinspection unchecked,rawtypes
		return (Binding) UrlXmlSource.fromUrl( url, getMappingBinder() );
	}
}
