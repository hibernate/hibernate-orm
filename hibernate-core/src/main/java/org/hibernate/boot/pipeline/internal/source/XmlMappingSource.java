/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.pipeline.internal.source;

import java.io.File;
import java.io.InputStream;
import java.util.function.Consumer;

import org.hibernate.Internal;
import org.hibernate.boot.archive.spi.InputStreamAccess;
import org.hibernate.boot.jaxb.internal.CacheableFileXmlSource;
import org.hibernate.boot.jaxb.internal.InputStreamAccessXmlSource;
import org.hibernate.boot.jaxb.internal.InputStreamXmlSource;
import org.hibernate.boot.jaxb.internal.MappingBinder;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityMappingsImpl;
import org.hibernate.boot.jaxb.spi.Binding;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;

/// Lazy XML mapping source used by compatibility entry points for inputs that
/// cannot be represented as simple resource names, files, or URLs.
///
/// @since 9.0
/// @author Steve Ebersole
@Internal
public interface XmlMappingSource {
	void bind(
			MappingBinder mappingBinder,
			ClassLoaderService classLoaderService,
			Consumer<Binding<JaxbEntityMappingsImpl>> bindingConsumer);

	static XmlMappingSource fromInputStream(InputStream inputStream) {
		return (mappingBinder, classLoaderService, bindingConsumer) ->
				bindingConsumer.accept( InputStreamXmlSource.fromStream( inputStream, mappingBinder ) );
	}

	static XmlMappingSource fromInputStreamAccess(InputStreamAccess inputStreamAccess) {
		return (mappingBinder, classLoaderService, bindingConsumer) ->
				bindingConsumer.accept( InputStreamAccessXmlSource.fromStreamAccess( inputStreamAccess, mappingBinder ) );
	}

	static XmlMappingSource fromCacheableFile(File xmlFile, File cacheDirectory, boolean strict) {
		return (mappingBinder, classLoaderService, bindingConsumer) ->
				bindingConsumer.accept( CacheableFileXmlSource.fromCacheableFile(
						xmlFile,
						cacheDirectory,
						strict,
						mappingBinder
				) );
	}
}
