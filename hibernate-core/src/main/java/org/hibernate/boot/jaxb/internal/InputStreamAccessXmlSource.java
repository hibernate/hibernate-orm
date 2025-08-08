/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.jaxb.internal;

import org.hibernate.boot.archive.spi.InputStreamAccess;
import org.hibernate.boot.jaxb.Origin;
import org.hibernate.boot.jaxb.SourceType;
import org.hibernate.boot.jaxb.spi.Binding;
import org.hibernate.boot.jaxb.spi.JaxbBindableMappingDescriptor;

import static org.hibernate.boot.jaxb.JaxbLogger.JAXB_LOGGER;

/**
 * Support for processing mapping XML from a {@linkplain InputStreamAccess} reference.
 *
 * @author Steve Ebersole
 *
 * @see MappingBinder
 */
public class InputStreamAccessXmlSource {
	/**
	 * Create a mapping {@linkplain Binding binding} from an input stream.
	 *
	 * @apiNote This method does not close the given {@code inputStream}.
	 */
	public static Binding<? extends JaxbBindableMappingDescriptor> fromStreamAccess(
			InputStreamAccess inputStreamAccess,
			MappingBinder mappingBinder) {
		return fromStreamAccess(
				inputStreamAccess,
				new Origin( SourceType.INPUT_STREAM, inputStreamAccess.getStreamName() ),
				mappingBinder
		);
	}

	/**
	 * Create a mapping {@linkplain Binding binding} from an input stream.
	 *
	 * @apiNote This method does not close the given {@code inputStream}.
	 */
	public static Binding<? extends JaxbBindableMappingDescriptor> fromStreamAccess(
			InputStreamAccess inputStreamAccess,
			Origin origin,
			MappingBinder mappingBinder) {
		JAXB_LOGGER.trace( "reading mappings from InputStreamAccess" );

		return inputStreamAccess.fromStream( (stream) ->
				InputStreamXmlSource.fromStream( stream, origin, false, mappingBinder )
		);
	}
}
