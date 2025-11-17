/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.jaxb.internal;

import org.hibernate.boot.MappingNotFoundException;
import org.hibernate.boot.jaxb.Origin;
import org.hibernate.boot.jaxb.SourceType;
import org.hibernate.boot.jaxb.spi.Binding;
import org.hibernate.boot.jaxb.spi.JaxbBindableMappingDescriptor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import static org.hibernate.boot.jaxb.JaxbLogger.JAXB_LOGGER;

/**
 * Support for processing mapping XML from a {@linkplain File} reference.
 *
 * @see MappingBinder
 *
 * @author Steve Ebersole
 */
public class FileXmlSource {
	/**
	 * Create a mapping {@linkplain Binding binding} from a File reference.
	 */
	public static Binding<? extends JaxbBindableMappingDescriptor> fromFile(
			File file,
			MappingBinder mappingBinder) {
		final String filePath = file.getPath();
		JAXB_LOGGER.tracef( "Reading mappings from file: %s", filePath );

		final Origin origin = new Origin( SourceType.FILE, filePath );

		if ( !file.exists() ) {
			throw new MappingNotFoundException( origin );
		}

		final FileInputStream fis;
		try {
			fis = new FileInputStream( file );
		}
		catch ( FileNotFoundException e ) {
			throw new MappingNotFoundException( e, origin );
		}

		return InputStreamXmlSource.fromStream( fis, origin, true, mappingBinder );
	}
}
