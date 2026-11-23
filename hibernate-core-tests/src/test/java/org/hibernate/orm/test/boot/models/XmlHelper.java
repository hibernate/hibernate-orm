package org.hibernate.orm.test.boot.models;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.hibernate.boot.ResourceStreamLocator;
import org.hibernate.boot.jaxb.Origin;
import org.hibernate.boot.jaxb.SourceType;
import org.hibernate.boot.jaxb.internal.MappingBinder;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityMappingsImpl;
import org.hibernate.boot.jaxb.spi.Binding;
import org.hibernate.boot.jaxb.spi.JaxbBindableMappingDescriptor;
import org.hibernate.boot.models.xml.XmlResourceException;
import org.hibernate.models.spi.ClassLoading;

import static org.hibernate.boot.jaxb.internal.MappingBinder.NON_VALIDATING;

/**
 * @author Steve Ebersole
 */
public class XmlHelper {
	public static Binding<JaxbEntityMappingsImpl> bindMapping(String resourceName, ClassLoading classLoadingAccess) {
		final JaxbEntityMappingsImpl jaxbRoot = loadMapping( resourceName, classLoadingAccess );
		return new Binding<>( jaxbRoot, new Origin( SourceType.RESOURCE, resourceName ) );
	}

	public static JaxbEntityMappingsImpl loadMapping(String resourceName, ClassLoading classLoadingAccess) {
		final ResourceStreamLocatorImpl resourceStreamLocator = new ResourceStreamLocatorImpl( classLoadingAccess );
		final MappingBinder mappingBinder = new MappingBinder( resourceStreamLocator, NON_VALIDATING );
		final var stream = resourceStreamLocator.locateResourceStream( resourceName );
		if ( stream == null ) {
			throw new XmlResourceException( "Could not locate XML mapping resource - " + resourceName );
		}
		final Binding<JaxbBindableMappingDescriptor> binding = mappingBinder.bind(
				stream,
				new Origin( SourceType.RESOURCE, resourceName )
		);
		return (JaxbEntityMappingsImpl) binding.getRoot();
	}

	private record ResourceStreamLocatorImpl(ClassLoading classLoadingAccess) implements ResourceStreamLocator {
		@Override
		@Nullable
		public InputStream locateResourceStream(@Nonnull String resourceName) {
			final URL resource = classLoadingAccess.locateResource( resourceName );
			try {
				return resource == null ? null : resource.openStream();
			}
			catch (IOException e) {
				throw new XmlResourceException( "Could not open XML mapping resource stream - " + resourceName, e );
			}
		}
	}
}
