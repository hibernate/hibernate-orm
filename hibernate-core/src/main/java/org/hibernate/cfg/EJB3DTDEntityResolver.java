/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cfg;

import java.io.InputStream;
import java.io.Serializable;

import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.ResourcesHelper;

import org.jboss.logging.Logger;

import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;

/**
 * Resolve JPA xsd files locally
 * Hibernate OGM uses this class, consider this some kind of exposed service at the SPI level
 *
 * @author Emmanuel Bernard
 */
@Deprecated
public class EJB3DTDEntityResolver implements EntityResolver, Serializable {
	public static final EntityResolver INSTANCE = new EJB3DTDEntityResolver();

	private static final CoreMessageLogger LOG = Logger.getMessageLogger( CoreMessageLogger.class, EJB3DTDEntityResolver.class.getName() );

	private static final String HIBERNATE_NAMESPACE = "http://www.hibernate.org/dtd/";
	private static final String OLD_HIBERNATE_NAMESPACE = "http://hibernate.sourceforge.net/";
	private static final String USER_NAMESPACE = "classpath://";

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
			if ( systemId.endsWith( "orm_3_0.xsd" ) ) {
				InputStream dtdStream = getStreamFromJpaClasspath( "orm_3_0.xsd" );
				final InputSource source = buildInputSource( publicId, systemId, dtdStream, false );
				if ( source != null ) {
					return source;
				}
			}
			else if ( systemId.endsWith( "orm_2_1.xsd" ) ) {
				InputStream dtdStream = getStreamFromJpaClasspath( "orm_2_1.xsd" );
				final InputSource source = buildInputSource( publicId, systemId, dtdStream, false );
				if ( source != null ) {
					return source;
				}
			}
			else if ( systemId.endsWith( "orm_2_2.xsd" ) ) {
				InputStream dtdStream = getStreamFromJpaClasspath( "orm_2_2.xsd" );
				final InputSource source = buildInputSource( publicId, systemId, dtdStream, false );
				if ( source != null ) {
					return source;
				}
			}
			else if ( systemId.endsWith( "orm_2_0.xsd" ) ) {
				InputStream dtdStream = getStreamFromJpaClasspath( "orm_2_0.xsd" );
				final InputSource source = buildInputSource( publicId, systemId, dtdStream, false );
				if ( source != null ) {
					return source;
				}
			}
			else if ( systemId.endsWith( "orm_1_0.xsd" ) ) {
				InputStream dtdStream = getStreamFromJpaClasspath( "orm_1_0.xsd" );
				final InputSource source = buildInputSource( publicId, systemId, dtdStream, false );
				if ( source != null ) {
					return source;
				}
			}
			else if ( systemId.endsWith( "persistence_3_0.xsd" ) ) {
				InputStream dtdStream = getStreamFromJpaClasspath( "persistence_3_0.xsd" );
				final InputSource source = buildInputSource( publicId, systemId, dtdStream, true );
				if ( source != null ) {
					return source;
				}
			}
			else if ( systemId.endsWith( "persistence_2_2.xsd" ) ) {
				InputStream dtdStream = getStreamFromJpaClasspath( "persistence_2_2.xsd" );
				final InputSource source = buildInputSource( publicId, systemId, dtdStream, true );
				if ( source != null ) {
					return source;
				}
			}
			else if ( systemId.endsWith( "persistence_2_1.xsd" ) ) {
				InputStream dtdStream = getStreamFromJpaClasspath( "persistence_2_1.xsd" );
				final InputSource source = buildInputSource( publicId, systemId, dtdStream, true );
				if ( source != null ) {
					return source;
				}
			}
			else if ( systemId.endsWith( "persistence_2_0.xsd" ) ) {
				InputStream dtdStream = getStreamFromJpaClasspath( "persistence_2_0.xsd" );
				final InputSource source = buildInputSource( publicId, systemId, dtdStream, true );
				if ( source != null ) {
					return source;
				}
			}
			else if ( systemId.endsWith( "persistence_1_0.xsd" ) ) {
				InputStream dtdStream = getStreamFromJpaClasspath( "persistence_1_0.xsd" );
				final InputSource source = buildInputSource( publicId, systemId, dtdStream, true );
				if ( source != null ) {
					return source;
				}
			}
			else if ( systemId.startsWith( HIBERNATE_NAMESPACE ) ) {
				LOG.debug( "Recognized hibernate namespace" );
				InputStream dtdStream = getStreamFromNonJpaClasspath( systemId.substring( HIBERNATE_NAMESPACE.length() ) );
				final InputSource source = buildInputSource( publicId, systemId, dtdStream, true );
				if ( source != null ) {
					return source;
				}
			}
			else if ( systemId.startsWith( OLD_HIBERNATE_NAMESPACE ) ) {
				LOG.recognizedObsoleteHibernateNamespace( OLD_HIBERNATE_NAMESPACE, HIBERNATE_NAMESPACE );
				InputStream dtdStream = getStreamFromNonJpaClasspath( systemId.substring( OLD_HIBERNATE_NAMESPACE.length() ) );
				final InputSource source = buildInputSource( publicId, systemId, dtdStream, true );
				if ( source != null ) {
					return source;
				}
			}
			else if ( systemId.startsWith( USER_NAMESPACE ) ) {
				LOG.debug( "Recognized local namespace; attempting to resolve on classpath" );
				InputStream dtdStream = resolveInLocalNamespace( systemId.substring( USER_NAMESPACE.length() ) );
				return buildInputSource( publicId, systemId, dtdStream, true );
			}
		}
		return null;
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

	private InputStream getStreamFromJpaClasspath(String fileName) {
		LOG.tracev( "Recognized JPA ORM namespace; attempting to resolve [{0}] on classpath under org/hibernate/jpa", fileName );
		String path = "org/hibernate/jpa/" + fileName;
		return ResourcesHelper.locateResourceAsStream( path, this.getClass().getClassLoader() );
	}

	private InputStream getStreamFromNonJpaClasspath(String fileName) {
		LOG.tracev( "Attempting to resolve [{0}] on classpath under org/hibernate/", fileName );
		String path = "org/hibernate/" + fileName;
		InputStream dtdStream = ResourcesHelper.locateResourceAsStream( path, this.getClass().getClassLoader() );
		if ( dtdStream == null ) {
			if ( fileName.indexOf( "2.0" ) > -1 ) {
				LOG.usingOldDtd();
			}
		}
		return dtdStream;
	}

	// Copied over from ConfigHelper that was removed
	private InputStream resolveInLocalNamespace(String path) {
		boolean hasLeadingSlash = path.startsWith( "/" );
		String stripped = hasLeadingSlash ? path.substring( 1 ) : path;

		InputStream stream = null;

		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		if ( classLoader != null ) {
			stream = classLoader.getResourceAsStream( path );
			if ( stream == null && hasLeadingSlash ) {
				stream = classLoader.getResourceAsStream( stripped );
			}
		}

		if ( stream == null ) {
			stream = Environment.class.getClassLoader().getResourceAsStream( path );
		}
		if ( stream == null && hasLeadingSlash ) {
			stream = Environment.class.getClassLoader().getResourceAsStream( stripped );
		}

		return stream;
	}
}
