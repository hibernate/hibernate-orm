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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.function.Consumer;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import static org.hibernate.boot.jaxb.JaxbLogger.JAXB_LOGGER;

/**
 * Support for creating a mapping {@linkplain Binding binding} from a JAR file entry.
 *
 * @see MappingBinder
 *
 * @author Steve Ebersole
 */
public class JarFileEntryXmlSource {

	/**
	 * Create a mapping {@linkplain Binding binding} for each entry in a JAR file that
	 * is a {@code hbm.xml} mapping file.  This binding is then handed back to the given
	 * consumer.
	 *
	 * @apiNote Assumes that any file named {@code *.hbm.xml} is a mapping document.
	 * Does not support {@code orm.xml} files.
	 */
	public static void fromJar(
			File jar,
			MappingBinder mappingBinder,
			Consumer<Binding<? extends JaxbBindableMappingDescriptor>> consumer) {
		JAXB_LOGGER.tracef( "Seeking mapping documents in jar file: %s", jar.getName() );

		final Origin origin = new Origin( SourceType.JAR, jar.getAbsolutePath() );

		try ( JarFile jarFile = new JarFile(jar) ) {
			final Enumeration<JarEntry> entries = jarFile.entries();
			while ( entries.hasMoreElements() ) {
				final JarEntry jarEntry = entries.nextElement();
				if ( jarEntry.getName().endsWith(".hbm.xml") ) {
					JAXB_LOGGER.tracef( "Found 'hbm.xml' mapping in jar: %s", jarEntry.getName() );
					consumer.accept( fromJarEntry( jarFile, jarEntry, origin, mappingBinder ) );
				}
			}
		}
		catch ( IOException e ) {
			throw new MappingNotFoundException( e, origin );
		}
	}

	/**
	 * Create a mapping {@linkplain Binding binding} from a JAR file entry.
	 */
	public static Binding<? extends JaxbBindableMappingDescriptor> fromJarEntry(
			JarFile jarFile,
			ZipEntry jarFileEntry,
			Origin origin,
			MappingBinder mappingBinder) {
		final InputStream stream;
		try {
			stream = jarFile.getInputStream( jarFileEntry );
		}
		catch (IOException e) {
			throw new MappingException(
					String.format(
							"Unable to open InputStream for jar file entry [%s : %s]",
							jarFile.getName(),
							jarFileEntry.getName()
					),
					e,
					origin
			);
		}

		return InputStreamXmlSource.fromStream( stream, origin, true, mappingBinder );
	}
}
