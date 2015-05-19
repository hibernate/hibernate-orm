/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.jaxb.internal;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.Serializable;

import org.hibernate.boot.MappingException;
import org.hibernate.boot.jaxb.Origin;
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

	public CacheableFileXmlSource(Origin origin, File xmlFile, boolean strict) {
		super( origin );
		this.xmlFile = xmlFile;
		this.strict = strict;

		this.serFile = determineCachedFile( xmlFile );

		final boolean useCachedFile = xmlFile.exists()
				&& serFile.exists()
				&& xmlFile.lastModified() < serFile.lastModified();

		if ( strict && !useCachedFile ) {
			throw new MappingException(
					String.format( "Cached file [%s] could not be found or could not be used", origin.getName() ),
					origin
			);
		}

	}

	private static File determineCachedFile(File xmlFile) {
		return new File( xmlFile.getAbsolutePath() + ".bin" );
	}

	@Override
	@SuppressWarnings("unchecked")
	public Binding doBind(Binder binder) {
		if ( strict ) {
			try {
				return new Binding( readSerFile(), getOrigin() );
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
			try {
				return readSerFile();
			}
			catch ( SerializationException e ) {
				log.unableToDeserializeCache( serFile.getName(), e );
			}
			catch ( FileNotFoundException e ) {
				log.cachedFileNotFound( serFile.getName(), e );
			}

			log.readingMappingsFromFile( xmlFile.getPath() );
			final Object binding = FileXmlSource.doBind( binder, xmlFile, getOrigin() );

			writeSerFile( binding );
		}

		return null;
	}

	private <T> T readSerFile() throws SerializationException, FileNotFoundException {
		log.readingCachedMappings( serFile );
		return SerializationHelper.deserialize( new FileInputStream( serFile ) );
	}

	private void writeSerFile(Object binding) {
		try {
			log.debugf( "Writing cache file for: %s to: %s", xmlFile.getAbsolutePath(), serFile.getAbsolutePath() );
			SerializationHelper.serialize( (Serializable) binding, new FileOutputStream( serFile ) );
		}
		catch ( Exception e ) {
			log.unableToWriteCachedFile( serFile.getAbsolutePath(), e.getMessage() );
		}
	}


}
