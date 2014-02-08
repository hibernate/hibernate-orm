/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.xml.internal.stax;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import javax.xml.stream.XMLStreamException;

import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.ConfigHelper;

/**
 * @author Steve Ebersole
 *
 * @deprecated No longer used.  "resolvers" to stop remote lookups are no longer needed given the
 * way we now process the XML.
 */
@Deprecated
public class LocalXmlResourceResolver implements javax.xml.stream.XMLResolver {
	private static final CoreMessageLogger log = CoreLogging.messageLogger( LocalXmlResourceResolver.class );

	public static final LocalXmlResourceResolver INSTANCE = new LocalXmlResourceResolver();

	/**
	 * Namespace for the orm.xml xsd for jpa 1.0 and 2.0
	 */
	public static final String INITIAL_JPA_ORM_NS = "http://java.sun.com/xml/ns/persistence/orm";

	/**
	 * Namespace for the orm.xml xsd for jpa 2.1
	 */
	public static final String SECOND_JPA_ORM_NS = "http://xmlns.jcp.org/xml/ns/persistence/orm";

	public static final String HIBERNATE_MAPPING_DTD_URL_BASE = "http://www.hibernate.org/dtd/";
	public static final String LEGACY_HIBERNATE_MAPPING_DTD_URL_BASE = "http://hibernate.sourceforge.net/";
	public static final String CLASSPATH_EXTENSION_URL_BASE = "classpath://";

	@Override
	public Object resolveEntity(String publicID, String systemID, String baseURI, String namespace) throws XMLStreamException {
		log.tracef( "In resolveEntity(%s, %s, %s, %s)", publicID, systemID, baseURI, namespace );

		if ( namespace != null ) {
			log.debugf( "Interpreting namespace : %s", namespace );
			if ( INITIAL_JPA_ORM_NS.equals( namespace ) ) {
				return openUrlStream( SupportedOrmXsdVersion.ORM_2_0.getSchemaUrl() );
			}
			else if ( SECOND_JPA_ORM_NS.equals( namespace ) ) {
				return openUrlStream( SupportedOrmXsdVersion.ORM_2_1.getSchemaUrl() );
			}
		}

		if ( systemID != null ) {
			log.debugf( "Interpreting systemID : %s", namespace );
			InputStream stream = null;
			if ( systemID.startsWith( HIBERNATE_MAPPING_DTD_URL_BASE ) ) {
				log.debug( "Recognized hibernate namespace; attempting to resolve on classpath under org/hibernate/" );
				stream = resolveOnClassPath( systemID, HIBERNATE_MAPPING_DTD_URL_BASE );
			}
			else if ( systemID.startsWith( LEGACY_HIBERNATE_MAPPING_DTD_URL_BASE ) ) {
				log.recognizedObsoleteHibernateNamespace( LEGACY_HIBERNATE_MAPPING_DTD_URL_BASE, HIBERNATE_MAPPING_DTD_URL_BASE );
				log.debug( "Attempting to resolve on classpath under org/hibernate/" );
				stream = resolveOnClassPath( systemID, LEGACY_HIBERNATE_MAPPING_DTD_URL_BASE );
			}
			else if ( systemID.startsWith( CLASSPATH_EXTENSION_URL_BASE ) ) {
				log.debug( "Recognized local namespace; attempting to resolve on classpath" );
				final String path = systemID.substring( CLASSPATH_EXTENSION_URL_BASE.length() );
				stream = resolveInLocalNamespace( path );
				if ( stream == null ) {
					log.debugf( "Unable to resolve [%s] on classpath", systemID );
				}
				else {
					log.debugf( "Resolved [%s] on classpath", systemID );
				}
			}

			if ( stream != null ) {
				return stream;
			}
		}

		return null;
	}

	private InputStream openUrlStream(URL url) {
		try {
			return url.openStream();
		}
		catch (IOException e) {
			throw new XmlInfrastructureException( "Could not open url stream : " + url.toExternalForm(), e );
		}
	}

	private InputStream resolveOnClassPath(String systemID, String namespace) {
		final String relativeResourceName = systemID.substring( namespace.length() );
		final String path = "org/hibernate/" + relativeResourceName;
		InputStream dtdStream = resolveInHibernateNamespace( path );
		if ( dtdStream == null ) {
			log.debugf( "Unable to locate [%s] on classpath", systemID );
			if ( relativeResourceName.contains( "2.0" ) ) {
				log.usingOldDtd();
			}
			return null;
		}
		else {
			log.debugf( "Located [%s] in classpath", systemID );
			return dtdStream;
		}
	}

	private InputStream resolveInHibernateNamespace(String path) {
		return this.getClass().getClassLoader().getResourceAsStream( path );
	}

	private InputStream resolveInLocalNamespace(String path) {
		try {
			return ConfigHelper.getUserResourceAsStream( path );
		}
		catch ( Throwable t ) {
			return null;
		}
	}
}
