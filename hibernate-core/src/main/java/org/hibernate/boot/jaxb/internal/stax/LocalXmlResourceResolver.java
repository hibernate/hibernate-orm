/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat Inc. or third-party contributors as
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
package org.hibernate.boot.jaxb.internal.stax;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import javax.xml.stream.XMLStreamException;

import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.ConfigHelper;

/**
 * @author Steve Ebersole
 */
public class LocalXmlResourceResolver implements javax.xml.stream.XMLResolver {
	private static final CoreMessageLogger log = CoreLogging.messageLogger( LocalXmlResourceResolver.class );

	public static final LocalXmlResourceResolver INSTANCE = new LocalXmlResourceResolver();

	public static final String CLASSPATH_EXTENSION_URL_BASE = "classpath://";

	@Override
	public Object resolveEntity(String publicID, String systemID, String baseURI, String namespace) throws XMLStreamException {
		log.tracef( "In resolveEntity(%s, %s, %s, %s)", publicID, systemID, baseURI, namespace );

		if ( namespace != null ) {
			log.debugf( "Interpreting namespace : %s", namespace );
			if ( INITIAL_JPA_XSD_MAPPING.matches( namespace ) ) {
				return openUrlStream( INITIAL_JPA_XSD_MAPPING.getMappedLocalUrl() );
			}
			else if ( JPA_XSD_MAPPING.matches( namespace ) ) {
				return openUrlStream( JPA_XSD_MAPPING.getMappedLocalUrl() );
			}
			else if ( HBM_XSD_MAPPING.matches( namespace ) ) {
				return openUrlStream( HBM_XSD_MAPPING.getMappedLocalUrl() );
			}
			else if ( HBM_XSD_MAPPING2.matches( namespace ) ) {
				return openUrlStream( HBM_XSD_MAPPING2.getMappedLocalUrl() );
			}
			else if ( CFG_XSD_MAPPING.matches( namespace ) ) {
				return openUrlStream( CFG_XSD_MAPPING.getMappedLocalUrl() );
			}
		}

		if ( publicID != null || systemID != null ) {
			log.debugf( "Interpreting public/system identifier : [%s] - [%s]", publicID, systemID );
			if ( HBM_DTD_MAPPING.matches( publicID, systemID ) ) {
				log.debug(
						"Recognized hibernate-mapping identifier; attempting to resolve on classpath under org/hibernate/"
				);
				return openUrlStream( HBM_DTD_MAPPING.getMappedLocalUrl() );
			}
			else if ( LEGACY_HBM_DTD_MAPPING.matches( publicID, systemID ) ) {
				log.recognizedObsoleteHibernateNamespace(
						LEGACY_HBM_DTD_MAPPING.getIdentifierBase(),
						HBM_DTD_MAPPING.getIdentifierBase()
				);
				log.debug(
						"Recognized legacy hibernate-mapping identifier; attempting to resolve on classpath under org/hibernate/"
				);
				return openUrlStream( HBM_DTD_MAPPING.getMappedLocalUrl() );
			}
			else if ( LEGACY2_HBM_DTD_MAPPING.matches( publicID, systemID ) ) {
				log.recognizedObsoleteHibernateNamespace(
						LEGACY2_HBM_DTD_MAPPING.getIdentifierBase(),
						HBM_DTD_MAPPING.getIdentifierBase()
				);
				log.debug(
						"Recognized legacy hibernate-mapping identifier; attempting to resolve on classpath under org/hibernate/"
				);
				return openUrlStream( HBM_DTD_MAPPING.getMappedLocalUrl() );
			}
			else if ( CFG_DTD_MAPPING.matches( publicID, systemID ) ) {
				log.debug(
						"Recognized hibernate-configuration identifier; attempting to resolve on classpath under org/hibernate/"
				);
				return openUrlStream( CFG_DTD_MAPPING.getMappedLocalUrl() );
			}
		}

		if ( systemID != null ) {
			// technically, "classpath://..." identifiers should only be declared as SYSTEM identifiers
			if ( systemID.startsWith( CLASSPATH_EXTENSION_URL_BASE ) ) {
				log.debugf( "Recognized `classpath:` identifier; attempting to resolve on classpath [%s]", systemID );
				final String path = systemID.substring( CLASSPATH_EXTENSION_URL_BASE.length() );
				// todo : for this to truly work consistently, we need access to ClassLoaderService
				final InputStream stream = resolveInLocalNamespace( path );
				if ( stream == null ) {
					log.debugf( "Unable to resolve [%s] on classpath", systemID );
				}
				else {
					log.debugf( "Resolved [%s] on classpath", systemID );
				}
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

	private InputStream resolveInLocalNamespace(String path) {
		try {
			return ConfigHelper.getUserResourceAsStream( path );
		}
		catch ( Throwable t ) {
			return null;
		}
	}

	/**
	 * Maps the namespace for the orm.xml xsd for jpa 1.0 and 2.0
	 */
	public static final NamespaceSchemaMapping INITIAL_JPA_XSD_MAPPING = new NamespaceSchemaMapping(
			"http://java.sun.com/xml/ns/persistence/orm",
			"org/hibernate/jpa/orm_2_0.xsd"
	);

	/**
	 * Maps the namespace for the orm.xml xsd for jpa 2.1+
	 */
	public static final NamespaceSchemaMapping JPA_XSD_MAPPING = new NamespaceSchemaMapping(
			"http://xmlns.jcp.org/xml/ns/persistence/orm",
			"org/hibernate/jpa/orm_2_1.xsd"
	);

	public static final NamespaceSchemaMapping HBM_XSD_MAPPING = new NamespaceSchemaMapping(
			"http://www.hibernate.org/xsd/orm/hbm",
			"org/hibernate/xsd/mapping/legacy-mapping-4.0.xsd"
	);

	public static final NamespaceSchemaMapping HBM_XSD_MAPPING2 = new NamespaceSchemaMapping(
			"http://www.hibernate.org/xsd/hibernate-mapping",
			"org/hibernate/hibernate-mapping-4.0.xsd"
	);

	public static final NamespaceSchemaMapping CFG_XSD_MAPPING = new NamespaceSchemaMapping(
			"http://www.hibernate.org/xsd/orm/cfg",
			"org/hibernate/xsd/cfg/legacy-configuration-4.0.xsd"
	);

	public static final DtdMapping HBM_DTD_MAPPING = new DtdMapping(
			"http://www.hibernate.org/dtd/hibernate-mapping",
			"org/hibernate/hibernate-mapping-3.0.dtd"
	);

	public static final DtdMapping LEGACY_HBM_DTD_MAPPING = new DtdMapping(
			"http://www.hibernate.org/dtd/hibernate-mapping",
			"org/hibernate/hibernate-mapping-3.0.dtd"
	);

	public static final DtdMapping LEGACY2_HBM_DTD_MAPPING = new DtdMapping(
			"http://hibernate.sourceforge.net/hibernate-mapping",
			"org/hibernate/hibernate-mapping-3.0.dtd"
	);

	public static final DtdMapping CFG_DTD_MAPPING = new DtdMapping(
			"http://www.hibernate.org/dtd/hibernate-configuration",
			"org/hibernate/hibernate-configuration-3.0.dtd"
	);


	public static class NamespaceSchemaMapping {
		private final String namespace;
		private final URL localSchemaUrl;

		public NamespaceSchemaMapping(String namespace, String resourceName) {
			this.namespace = namespace;
			this.localSchemaUrl = LocalSchemaLocator.resolveLocalSchemaUrl( resourceName );
		}

		public boolean matches(String namespace) {
			return this.namespace.equals( namespace );
		}

		public URL getMappedLocalUrl() {
			return localSchemaUrl;
		}
	}

	public static class DtdMapping {
		private final String identifierBase;
		private final URL localSchemaUrl;

		public DtdMapping(String identifierBase, String resourceName) {
			this.identifierBase = identifierBase;
			this.localSchemaUrl = LocalSchemaLocator.resolveLocalSchemaUrl( resourceName );
		}

		public String getIdentifierBase() {
			return identifierBase;
		}

		public boolean matches(String publicId, String systemId) {
			if ( publicId != null ) {
				if ( publicId.startsWith( identifierBase ) ) {
					return true;
				}
			}

			if ( systemId != null ) {
				if ( systemId.startsWith( identifierBase ) ) {
					return true;
				}
			}

			return false;
		}

		public URL getMappedLocalUrl() {
			return localSchemaUrl;
		}
	}
}
