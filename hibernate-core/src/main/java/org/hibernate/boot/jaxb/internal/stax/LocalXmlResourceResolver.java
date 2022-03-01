/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.jaxb.internal.stax;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import javax.xml.stream.XMLStreamException;

import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.xsd.ConfigXsdSupport;
import org.hibernate.boot.xsd.MappingXsdSupport;

import org.jboss.logging.Logger;

import static org.hibernate.internal.log.DeprecationLogger.DEPRECATION_LOGGER;

/**
 * @author Steve Ebersole
 */
public class LocalXmlResourceResolver implements javax.xml.stream.XMLResolver {
	private static final Logger log = Logger.getLogger( LocalXmlResourceResolver.class );

	public static final String CLASSPATH_EXTENSION_URL_BASE = "classpath://";

	private final ClassLoaderService classLoaderService;

	public LocalXmlResourceResolver(ClassLoaderService classLoaderService) {
		this.classLoaderService = classLoaderService;
	}

	@Override
	public Object resolveEntity(String publicID, String systemID, String baseURI, String namespace) throws XMLStreamException {
		log.tracef( "In resolveEntity(%s, %s, %s, %s)", publicID, systemID, baseURI, namespace );

		if ( namespace != null ) {
			log.debugf( "Interpreting namespace : %s", namespace );
			if ( MappingXsdSupport.jpa10.getNamespaceUri().matches( namespace ) ) {
				// JPA 1.0 and 2.0 share the same namespace URI
				return openUrlStream( LocalSchemaLocator.resolveLocalSchemaUrl( MappingXsdSupport.jpa10.getLocalResourceName() ) );
			}
			else if ( MappingXsdSupport.jpa21.getNamespaceUri().matches( namespace ) ) {
				// JPA 2.1 and 2.2 share the same namespace URI
				return openUrlStream( LocalSchemaLocator.resolveLocalSchemaUrl( MappingXsdSupport.jpa21.getLocalResourceName() ) );
			}
			else if ( MappingXsdSupport.jpa30.getNamespaceUri().matches( namespace ) ) {
				return openUrlStream( LocalSchemaLocator.resolveLocalSchemaUrl( MappingXsdSupport.jpa30.getLocalResourceName() ) );
			}
			else if ( ConfigXsdSupport.getJPA10().getNamespaceUri().matches( namespace ) ) {
				// JPA 1.0 and 2.0 share the same namespace URI
				return openUrlStream( LocalSchemaLocator.resolveLocalSchemaUrl( ConfigXsdSupport.getJPA10().getLocalResourceName() ) );
			}
			else if ( ConfigXsdSupport.getJPA21().getNamespaceUri().matches( namespace ) ) {
				// JPA 2.1 and 2.2 share the same namespace URI
				return openUrlStream( LocalSchemaLocator.resolveLocalSchemaUrl( ConfigXsdSupport.getJPA21().getLocalResourceName() ) );
			}
			else if ( ConfigXsdSupport.getJPA30().getNamespaceUri().matches( namespace ) ) {
				return openUrlStream( LocalSchemaLocator.resolveLocalSchemaUrl( ConfigXsdSupport.getJPA30().getLocalResourceName() ) );
			}
			else if ( MappingXsdSupport.hibernateMappingXml.getNamespaceUri().matches( namespace ) ) {
				return openUrlStream( LocalSchemaLocator.resolveLocalSchemaUrl( MappingXsdSupport.hibernateMappingXml.getLocalResourceName() ) );
			}
			else if ( MappingXsdSupport.hbmXml.getNamespaceUri().matches( namespace ) ) {
				return openUrlStream( LocalSchemaLocator.resolveLocalSchemaUrl( MappingXsdSupport.hbmXml.getLocalResourceName() ) );
			}
			else if ( ConfigXsdSupport.cfgXsd().getNamespaceUri().matches( namespace ) ) {
				return openUrlStream( LocalSchemaLocator.resolveLocalSchemaUrl( ConfigXsdSupport.cfgXsd().getLocalResourceName() ) );
			}
		}

		if ( publicID != null || systemID != null ) {
			log.debugf( "Checking public/system identifiers `%s`/`%s` as DTD references", publicID, systemID );

			if ( MAPPING_DTD.matches( publicID, systemID ) ) {
				return openUrlStream( MAPPING_DTD.localSchemaUrl );
			}

			if ( LEGACY_MAPPING_DTD.matches( publicID, systemID ) ) {
				DEPRECATION_LOGGER.recognizedObsoleteHibernateNamespace( LEGACY_MAPPING_DTD.getIdentifierBase(), MAPPING_DTD.getIdentifierBase() );
				return openUrlStream( MAPPING_DTD.localSchemaUrl );
			}

			if ( CFG_DTD.matches( publicID, systemID ) ) {
				return openUrlStream( CFG_DTD.localSchemaUrl );
			}

			if ( LEGACY_CFG_DTD.matches( publicID, systemID ) ) {
				DEPRECATION_LOGGER.recognizedObsoleteHibernateNamespace( LEGACY_CFG_DTD.getIdentifierBase(), CFG_DTD.getIdentifierBase() );
				return openUrlStream( CFG_DTD.localSchemaUrl );
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
			return classLoaderService.locateResourceStream( path );
		}
		catch ( Throwable t ) {
			return null;
		}
	}

	public static final DtdDescriptor MAPPING_DTD = new DtdDescriptor(
			"www.hibernate.org/dtd/hibernate-mapping",
			"org/hibernate/hibernate-mapping-3.0.dtd"
	);

	public static final DtdDescriptor LEGACY_MAPPING_DTD = new DtdDescriptor(
			"hibernate.sourceforge.net/hibernate-mapping",
			"org/hibernate/hibernate-mapping-3.0.dtd"
	);

	public static final DtdDescriptor CFG_DTD = new DtdDescriptor(
			"www.hibernate.org/dtd/hibernate-configuration",
			"org/hibernate/hibernate-configuration-3.0.dtd"
	);

	public static final DtdDescriptor LEGACY_CFG_DTD = new DtdDescriptor(
			"hibernate.sourceforge.net/hibernate-configuration",
			"org/hibernate/hibernate-configuration-3.0.dtd"
	);


	public static class DtdDescriptor {
		private final String httpBase;
		private final String httpsBase;
		private final URL localSchemaUrl;

		public DtdDescriptor(String identifierBase, String resourceName) {
			this.httpBase = "http://" + identifierBase;
			this.httpsBase = "https://" + identifierBase;
			this.localSchemaUrl = LocalSchemaLocator.resolveLocalSchemaUrl( resourceName );
		}

		public String getIdentifierBase() {
			return httpBase;
		}

		public boolean matches(String publicId, String systemId) {
			if ( publicId != null ) {
				if ( publicId.startsWith( httpBase )
						|| publicId.matches( httpsBase ) ) {
					return true;
				}
			}

			if ( systemId != null ) {
				if ( systemId.startsWith( httpBase )
						|| systemId.matches( httpsBase ) ) {
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
