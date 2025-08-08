/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.jaxb.internal;

import org.hibernate.boot.InvalidMappingException;
import org.hibernate.boot.jaxb.Origin;
import org.hibernate.boot.jaxb.SourceType;
import org.hibernate.boot.jaxb.spi.Binding;
import org.hibernate.boot.jaxb.spi.JaxbBindableMappingDescriptor;

import java.io.IOException;
import java.io.InputStream;

import static org.hibernate.boot.jaxb.JaxbLogger.JAXB_LOGGER;

/**
 * Support for processing mapping XML from a {@linkplain InputStream} reference.
 *
 * @see MappingBinder
 *
 * @author Steve Ebersole
 */
public class InputStreamXmlSource {
	/**
	 * Create a mapping {@linkplain Binding binding} from an input stream.
	 *
	 * @apiNote This method does not close the given {@code inputStream}.
	 */
	public static Binding<? extends JaxbBindableMappingDescriptor> fromStream(
			InputStream inputStream,
			MappingBinder mappingBinder) {
		JAXB_LOGGER.trace( "reading mappings from InputStream" );

		final Origin origin = new Origin( SourceType.INPUT_STREAM, null );
		return fromStream( inputStream, origin, false, mappingBinder );
	}

	/**
	 * Utility form to create a {@linkplain Binding binding} from an input source.
	 *
	 * @param stream The stream from which to read the mappings
	 * @param origin Description of the source from which the stream came
	 * @param autoClose Whether to {@linkplain InputStream#close() close} the stream after we have processed it
	 * @param binder The JAXB binder to use
	 */
	public static Binding<? extends JaxbBindableMappingDescriptor> fromStream(
			InputStream stream,
			Origin origin,
			boolean autoClose,
			MappingBinder binder) {
		try {
			return binder.bind( stream, origin );
		}
		catch ( Exception e ) {
			throw new InvalidMappingException( origin, e );
		}
		finally {
			if ( autoClose ) {
				try {
					stream.close();
				}
				catch ( IOException ignore ) {
					JAXB_LOGGER.trace( "Was unable to close input stream" );
				}
			}
		}
	}
}
