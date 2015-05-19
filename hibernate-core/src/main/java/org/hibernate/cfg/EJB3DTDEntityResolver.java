/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cfg;

import java.io.InputStream;

import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.xml.DTDEntityResolver;

import org.jboss.logging.Logger;

import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;

/**
 * Resolve JPA xsd files locally
 * Hibernate OGM uses this class, consider this some kind of exposed service at the SPI level
 *
 * @author Emmanuel Bernard
 */
public class EJB3DTDEntityResolver extends DTDEntityResolver {
	public static final EntityResolver INSTANCE = new EJB3DTDEntityResolver();

	private static final CoreMessageLogger LOG = Logger.getMessageLogger( CoreMessageLogger.class, EJB3DTDEntityResolver.class.getName() );

	boolean resolved = false;

	/**
	 * Persistence.xml has been resolved locally
	 * @return true if it has
	 */
	public boolean isResolved() {
		return resolved;
	}

	@Override
	public InputSource resolveEntity(String publicId, String systemId) {
		LOG.tracev( "Resolving XML entity {0} : {1}", publicId, systemId );
		if ( systemId != null ) {
			if ( systemId.endsWith( "orm_2_1.xsd" ) ) {
				InputStream dtdStream = getStreamFromClasspath( "orm_2_1.xsd" );
				final InputSource source = buildInputSource( publicId, systemId, dtdStream, false );
				if ( source != null ) {
					return source;
				}
			}
			else if ( systemId.endsWith( "orm_2_0.xsd" ) ) {
				InputStream dtdStream = getStreamFromClasspath( "orm_2_0.xsd" );
				final InputSource source = buildInputSource( publicId, systemId, dtdStream, false );
				if ( source != null ) {
					return source;
				}
			}
			else if ( systemId.endsWith( "orm_1_0.xsd" ) ) {
				InputStream dtdStream = getStreamFromClasspath( "orm_1_0.xsd" );
				final InputSource source = buildInputSource( publicId, systemId, dtdStream, false );
				if ( source != null ) {
					return source;
				}
			}
			else if ( systemId.endsWith( "persistence_2_1.xsd" ) ) {
				InputStream dtdStream = getStreamFromClasspath( "persistence_2_1.xsd" );
				final InputSource source = buildInputSource( publicId, systemId, dtdStream, true );
				if ( source != null ) {
					return source;
				}
			}
			else if ( systemId.endsWith( "persistence_2_0.xsd" ) ) {
				InputStream dtdStream = getStreamFromClasspath( "persistence_2_0.xsd" );
				final InputSource source = buildInputSource( publicId, systemId, dtdStream, true );
				if ( source != null ) {
					return source;
				}
			}
			else if ( systemId.endsWith( "persistence_1_0.xsd" ) ) {
				InputStream dtdStream = getStreamFromClasspath( "persistence_1_0.xsd" );
				final InputSource source = buildInputSource( publicId, systemId, dtdStream, true );
				if ( source != null ) {
					return source;
				}
			}
		}

		// because the old code did this too (in terms of setting resolved)
		InputSource source = super.resolveEntity( publicId, systemId );
		if ( source != null ) {
			resolved = true;
		}
		return source;
	}

	private InputSource buildInputSource(String publicId, String systemId, InputStream dtdStream, boolean resolved) {
		if ( dtdStream == null ) {
			LOG.tracev( "Unable to locate [{0}] on classpath", systemId );
			return null;
		}
		LOG.tracev( "Located [{0}] in classpath", systemId );
		InputSource source = new InputSource( dtdStream );
		source.setPublicId( publicId );
		source.setSystemId( systemId );
		this.resolved = resolved;
		return source;
	}

	private InputStream getStreamFromClasspath(String fileName) {
		LOG.trace( "Recognized JPA ORM namespace; attempting to resolve on classpath under org/hibernate/jpa" );
		String path = "org/hibernate/jpa/" + fileName;
		InputStream dtdStream = resolveInHibernateNamespace( path );
		return dtdStream;
	}
}
