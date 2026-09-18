/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.pipeline.internal.source;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.function.Consumer;

import org.hibernate.Internal;
import org.hibernate.InvalidMappingException;
import org.hibernate.boot.archive.spi.InputStreamAccess;
import org.hibernate.boot.jaxb.Origin;
import org.hibernate.boot.jaxb.SourceType;
import org.hibernate.boot.jaxb.internal.CacheableFileXmlSource;
import org.hibernate.boot.jaxb.internal.InputStreamAccessXmlSource;
import org.hibernate.boot.jaxb.internal.InputStreamXmlSource;
import org.hibernate.boot.jaxb.internal.MappingBinder;
import org.hibernate.boot.jaxb.internal.UrlXmlSource;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityMappingsImpl;
import org.hibernate.boot.jaxb.spi.Binding;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;

/// A lazy XML mapping declaration, with its diagnostic origin and discovery identity.
/// Explicit declarations retain their multiplicity; only discovered declarations are deduplicated.
///
/// @since 9.0
/// @author Steve Ebersole
@Internal
public interface XmlMappingSource {
	void bind(
			MappingBinder mappingBinder,
			ClassLoaderService classLoaderService,
			Consumer<Binding<JaxbEntityMappingsImpl>> bindingConsumer);

	default Origin origin() {
		return new Origin( SourceType.OTHER, null );
	}

	/// Resource identity, when available without consuming an input stream.
	default URI identity(ClassLoaderService classLoading) {
		return null;
	}

	default boolean discovered() {
		return false;
	}

	/// Preserve the historical resource, URI, URL, then stream/cache binding order.
	default int bindingOrder() {
		return 3;
	}

	static XmlMappingSource fromResource(String name) {
		return new Resource( name );
	}

	static XmlMappingSource fromUri(URI uri) {
		return new Uri( uri, false );
	}

	static XmlMappingSource discoveredUri(URI uri) {
		return new Uri( uri, true );
	}

	static XmlMappingSource fromUrl(URL url) {
		return new Url( url );
	}

	static XmlMappingSource fromInputStream(InputStream inputStream) {
		return new StreamSource( new Origin( SourceType.INPUT_STREAM, null ), null, false,
				(mappingBinder, classLoaderService, consumer) ->
						consumer.accept( InputStreamXmlSource.fromStream( inputStream, mappingBinder ) ) );
	}

	static XmlMappingSource fromInputStreamAccess(InputStreamAccess access) {
		return new StreamSource( new Origin( SourceType.INPUT_STREAM, access.getStreamName() ), null, false,
				(binder, loading, consumer) -> consumer.accept( InputStreamAccessXmlSource.fromStreamAccess( access, binder ) ) );
	}

	static XmlMappingSource fromArchiveEntry(org.hibernate.boot.archive.spi.ArchiveEntry entry) {
		final var origin = new Origin( SourceType.URL, entry.getUri().toString() );
		return new StreamSource( origin, entry.getUri(), true,
				(binder, loading, consumer) -> consumer.accept(
						InputStreamAccessXmlSource.fromStreamAccess( entry.getStreamAccess(), origin, binder ) ) );
	}

	static XmlMappingSource fromCacheableFile(File xmlFile, File cacheDirectory, boolean strict) {
		return new StreamSource( new Origin( SourceType.FILE, xmlFile.getPath() ), xmlFile.toURI(), false,
				(binder, loading, consumer) -> consumer.accept(
						CacheableFileXmlSource.fromCacheableFile( xmlFile, cacheDirectory, strict, binder ) ) );
	}

	/// A classloader resource declaration.
	record Resource(String name) implements XmlMappingSource {
		@Override
		public Origin origin() {
			return new Origin( SourceType.RESOURCE, name );
		}

		@Override
		public URI identity(ClassLoaderService loading) {
			final var url = loading.locateResource( name );
			return url == null ? null : URI.create( url.toExternalForm() ).normalize();
		}

		@Override
		public int bindingOrder() {
			return 0;
		}

		@Override
		public void bind(MappingBinder binder, ClassLoaderService loading, Consumer<Binding<JaxbEntityMappingsImpl>> consumer) {
			final var origin = origin();
			try (var stream = loading.locateResourceStream( name )) {
				consumer.accept( binder.bind( stream, origin ) );
			}
			catch (org.hibernate.boot.MappingException e) {
				throw new InvalidMappingException( "Could not parse mapping document: " + name,
						origin.getType().getLegacyTypeText(), origin.getName(), e );
			}
			catch (IOException e) {
				throw new RuntimeException( "Error accessing mapping resource - " + name, e );
			}
		}
	}

	/// A URI declaration, optionally contributed by discovery.
	record Uri(URI uri, boolean discovered) implements XmlMappingSource {
		@Override
		public Origin origin() {
			return new Origin( SourceType.URL, uri.toString() );
		}

		@Override
		public URI identity(ClassLoaderService loading) {
			return uri.normalize();
		}

		@Override
		public int bindingOrder() {
			return 1;
		}

		@Override
		public void bind(MappingBinder binder, ClassLoaderService loading, Consumer<Binding<JaxbEntityMappingsImpl>> consumer) {
			try {
				consumer.accept( UrlXmlSource.fromUrl( uri.toURL(), binder ) );
			}
			catch (MalformedURLException e) {
				throw new RuntimeException( "Error accessing mapping file - " + uri, e );
			}
		}
	}

	/// A URL declaration retaining its URL handler.
	record Url(URL url) implements XmlMappingSource {
		@Override
		public Origin origin() {
			return new Origin( SourceType.URL, url.toExternalForm() );
		}

		@Override
		public URI identity(ClassLoaderService loading) {
			return URI.create( url.toExternalForm() ).normalize();
		}

		@Override
		public int bindingOrder() {
			return 2;
		}

		@Override
		public void bind(MappingBinder binder, ClassLoaderService loading, Consumer<Binding<JaxbEntityMappingsImpl>> consumer) {
			consumer.accept( UrlXmlSource.fromUrl( url, binder ) );
		}
	}

	/// Stream-backed declarations retain the ownership semantics of their binder.
	record StreamSource(Origin origin, URI resourceIdentity, boolean discovered, XmlMappingSource delegate)
			implements XmlMappingSource {
		@Override
		public URI identity(ClassLoaderService loading) {
			return resourceIdentity == null ? null : resourceIdentity.normalize();
		}

		@Override
		public int bindingOrder() {
			return discovered ? 2 : 3;
		}

		@Override
		public void bind(MappingBinder binder, ClassLoaderService loading, Consumer<Binding<JaxbEntityMappingsImpl>> consumer) {
			delegate.bind( binder, loading, consumer );
		}
	}
}
