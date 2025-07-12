/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.jaxb.internal;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.Serializable;

import org.hibernate.boot.MappingException;
import org.hibernate.boot.jaxb.Origin;
import org.hibernate.boot.jaxb.SourceType;
import org.hibernate.boot.jaxb.spi.Binder;
import org.hibernate.boot.jaxb.spi.Binding;
import org.hibernate.boot.jaxb.spi.XmlSource;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.SerializationHelper;
import org.hibernate.type.SerializationException;

/**
 * @author Steve Ebersole
 */
public class CacheableFileXmlSource extends XmlSource {
	private static final CoreMessageLogger log = CoreLogging.messageLogger( CacheableFileXmlSource.class );

	private final File xmlFile;
	private final File serFile;
	private final boolean strict;

	public CacheableFileXmlSource(Origin origin, File xmlFile, File cachedFileDir, boolean strict) {
		super( origin );
		this.xmlFile = xmlFile;
		this.strict = strict;

		this.serFile = new File( cachedFileDir, xmlFile.getName() + ".bin" );

		if ( strict ) {
			if ( !serFile.exists() ) {
				throw new MappingException(
						String.format( "Cached file [%s] could not be found", origin.getName() ),
						origin
				);
			}
			if ( isSerfileObsolete() ) {
				throw new MappingException(
						String.format( "Cached file [%s] could not be used as the mapping file is newer", origin.getName() ),
						origin
				);
			}
		}
	}

	public static File determineCachedFile(File xmlFile) {
		return new File( xmlFile.getAbsolutePath() + ".bin" );
	}

	@Override
	public <T> Binding<T> doBind(Binder<T> binder) {
		if ( strict ) {
			try {
				return new Binding<>( readSerFile(), getOrigin() );
			}
			catch ( SerializationException e ) {
				throw new MappingException(
						String.format( "Unable to deserialize from cached file [%s]", getOrigin().getName() ) ,
						e,
						getOrigin()
				);
			}
			catch ( FileNotFoundException e ) {
				throw new MappingException(
						String.format( "Unable to locate cached file [%s]", getOrigin().getName() ) ,
						e,
						getOrigin()
				);
			}
		}
		else {
			if ( !isSerfileObsolete() ) {
				try {
					return new Binding<>( readSerFile(), getOrigin() );
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
			final Binding<T> binding = FileXmlSource.doBind( binder, xmlFile, getOrigin() );

			writeSerFile( binding );

			return binding;
		}
	}

	private <T> T readSerFile() throws SerializationException, FileNotFoundException {
		log.readingCachedMappings( serFile );
		return SerializationHelper.deserialize( new FileInputStream( serFile ) );
	}

	private void writeSerFile(Object binding) {
		writeSerFile( (Serializable) binding, xmlFile, serFile );
	}

	private static void writeSerFile(Serializable binding, File xmlFile, File serFile) {
		if ( binding instanceof Binding<?> bindingWrapper ) {
			binding = (Serializable) bindingWrapper.getRoot();
		}
		try ( FileOutputStream fos = new FileOutputStream( serFile ) ) {
			if ( log.isTraceEnabled() ) {
				log.tracef( "Writing cache file for: %s to: %s", xmlFile.getAbsolutePath(), serFile.getAbsolutePath() );
			}
			SerializationHelper.serialize( binding, fos );
			boolean success = serFile.setLastModified( System.currentTimeMillis() );
			if ( !success ) {
				log.warn( "Could not update cacheable hbm.xml bin file timestamp" );
			}
		}
		catch ( Exception e ) {
			log.unableToWriteCachedFile( serFile.getAbsolutePath(), e.getMessage() );
		}
	}

	public static void createSerFile(File xmlFile, Binder binder) {
		createSerFile( xmlFile, determineCachedFile( xmlFile ), binder );
	}

	public static void createSerFile(File xmlFile, File outputFile, Binder binder) {
		final Origin origin = new Origin( SourceType.FILE, xmlFile.getAbsolutePath() );
		writeSerFile(
				FileXmlSource.doBind( binder, xmlFile, origin ),
				xmlFile,
				outputFile
		);
	}

	private boolean isSerfileObsolete() {
		return xmlFile.exists() && serFile.exists() && xmlFile.lastModified() > serFile.lastModified();
	}

}
