/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.jaxb.internal;

import org.hibernate.boot.MappingException;
import org.hibernate.boot.jaxb.Origin;
import org.hibernate.boot.jaxb.SourceType;
import org.hibernate.boot.jaxb.spi.Binding;
import org.hibernate.boot.jaxb.spi.JaxbBindableMappingDescriptor;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.SerializationHelper;
import org.hibernate.type.SerializationException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

/**
 * Support for creating a mapping {@linkplain Binding binding} from "cached" XML files.
 * <p/>
 * This is a legacy feature, caching a serialized form of the {@linkplain JaxbBindableMappingDescriptor JAXB model}
 * into a file for later use.  While not deprecated per se, its use is discouraged.
 *
 * @see MappingBinder
 *
 * @author Steve Ebersole
 */
public class CacheableFileXmlSource {
	private static final CoreMessageLogger log = CoreLogging.messageLogger( CacheableFileXmlSource.class );

	public static Binding<? extends JaxbBindableMappingDescriptor> fromCacheableFile(
			File xmlFile,
			File serLocation,
			boolean strict,
			MappingBinder binder) {
		final Origin origin = new Origin( SourceType.FILE, xmlFile.getAbsolutePath() );
		return fromCacheableFile( xmlFile, serLocation, origin, strict, binder );
	}

	public static Binding<? extends JaxbBindableMappingDescriptor> fromCacheableFile(
			File xmlFile,
			File serLocation,
			Origin origin,
			boolean strict,
			MappingBinder binder) {
		final File serFile = resolveSerFile( xmlFile, serLocation );

		if ( strict ) {
			try {
				return new Binding<>( readSerFile( serFile ), origin );
			}
			catch ( SerializationException e ) {
				throw new MappingException(
						String.format( "Unable to deserialize from cached file [%s]", origin.getName() ) ,
						e,
						origin
				);
			}
			catch ( FileNotFoundException e ) {
				throw new MappingException(
						String.format( "Unable to locate cached file [%s]", origin.getName() ) ,
						e,
						origin
				);
			}
		}
		else {
			if ( !isSerfileObsolete( xmlFile, serFile ) ) {
				try {
					return new Binding<>( readSerFile( serFile ), origin );
				}
				catch ( SerializationException e ) {
					log.unableToDeserializeCache( serFile.getName(), e );
				}
				catch ( FileNotFoundException e ) {
					log.cachedFileNotFound( serFile.getName(), e );
				}
			}
			else {
				log.cachedFileObsolete( serFile );
			}

			log.readingMappingsFromFile( xmlFile.getPath() );
			final Binding<? extends JaxbBindableMappingDescriptor> binding = FileXmlSource.fromFile( xmlFile, binder );

			writeSerFile( binding.getRoot(), xmlFile, serFile );

			return binding;
		}
	}

	/**
	 * Determine the ser file for a given mapping XML file.
	 *
	 * @param xmlFile The source mapping XML file
	 * @param serLocation The location details about the ser file.  Can be one of 3 things:<ul>
	 *     <li>{@code null} indicating we should {@linkplain #determineCachedFile(File) calculate} the File reference in the same directory as the {@code xmlFile}
	 *     <li>a {@linkplain File#isDirectory() directory} indicating we should {@linkplain #determineCachedFile(File,File) calculate} the File reference in the given directory
	 *     <il>the {@linkplain File#isFile() file} to use
	 * </ul>
	 *
	 * @return The ser file reference.
	 */
	public static File resolveSerFile(File xmlFile, File serLocation) {
		if ( serLocation == null ) {
			return determineCachedFile( xmlFile );
		}
		if ( serLocation.isDirectory() ) {
			return determineCachedFile( xmlFile, serLocation );
		}
		assert serLocation.isFile();
		return serLocation;
	}

	public static File determineCachedFile(File xmlFile) {
		return new File( xmlFile.getAbsolutePath() + ".bin" );
	}

	public static File determineCachedFile(File xmlFile, File serDirectory) {
		return new File( serDirectory, xmlFile.getName() + ".bin" );
	}

	private static <T extends JaxbBindableMappingDescriptor> T readSerFile(File serFile) throws SerializationException, FileNotFoundException {
		log.readingCachedMappings( serFile );
		return SerializationHelper.deserialize( new FileInputStream( serFile ) );
	}

	private static <T extends JaxbBindableMappingDescriptor> void writeSerFile(
			T jaxbModel,
			File xmlFile,
			File serFile) {
		try ( FileOutputStream fos = new FileOutputStream( serFile ) ) {
			if ( log.isTraceEnabled() ) {
				log.tracef( "Writing cache file for: %s to: %s", xmlFile.getAbsolutePath(), serFile.getAbsolutePath() );
			}
			SerializationHelper.serialize( jaxbModel, fos );
			boolean success = serFile.setLastModified( System.currentTimeMillis() );
			if ( !success ) {
				log.warn( "Could not update cacheable hbm.xml bin file timestamp" );
			}
		}
		catch ( Exception e ) {
			log.unableToWriteCachedFile( serFile.getAbsolutePath(), e.getMessage() );
		}
	}

	public static void createSerFile(File xmlFile, MappingBinder binder) {
		createSerFile( xmlFile, determineCachedFile( xmlFile ), binder );
	}

	public static void createSerFile(File xmlFile, File outputFile, MappingBinder binder) {
		final Binding<? extends JaxbBindableMappingDescriptor> binding = FileXmlSource.fromFile( xmlFile, binder );
		writeSerFile( binding.getRoot(), xmlFile, outputFile );
	}

	public static boolean isSerfileObsolete(File xmlFile, File serFile) {
		return xmlFile.exists()
			&& serFile.exists()
			&& xmlFile.lastModified() > serFile.lastModified();
	}

}
