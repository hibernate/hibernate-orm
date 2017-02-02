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
import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public enum LocalSchema {
	ORM(
			"http://www.hibernate.org/xsd/orm/mapping",
			"org/hibernate/jpa/orm_2_1.xsd",
			"2.1"
	),
	HBM(
			"http://www.hibernate.org/xsd/orm/hbm",
			"org/hibernate/xsd/mapping/legacy-mapping-4.0.xsd",
			"4.0"
	),
	CFG(
			"http://www.hibernate.org/xsd/orm/cfg",
			"org/hibernate/hibernate-configuration-4.0.xsd",
			"4.0"

	)
	;

	private static final Logger log = Logger.getLogger( LocalSchema.class );

	private final String namespaceUri;
	private final String localResourceName;
	private final String currentVersion;
	private final Schema schema;

	LocalSchema(String namespaceUri, String localResourceName, String currentVersion) {
		this.namespaceUri = namespaceUri;
		this.localResourceName = localResourceName;
		this.currentVersion = currentVersion;
		this.schema = resolveLocalSchema( localResourceName );
	}

	public String getNamespaceUri() {
		return namespaceUri;
	}

	public String getCurrentVersion() {
		return currentVersion;
	}

	public Schema getSchema() {
		return schema;
	}

	private static javax.xml.validation.Schema resolveLocalSchema(String schemaName) {
		return resolveLocalSchema( resolveLocalSchemaUrl( schemaName ) );
	}

	private static URL resolveLocalSchemaUrl(String schemaName) {
		URL url = LocalSchema.class.getClassLoader().getResource( schemaName );
		if ( url == null ) {
			throw new XmlInfrastructureException( "Unable to locate schema [" + schemaName + "] via classpath" );
		}
		return url;
	}

	private static javax.xml.validation.Schema resolveLocalSchema(URL schemaUrl) {
		try {
			InputStream schemaStream = schemaUrl.openStream();
			try {
				StreamSource source = new StreamSource( schemaUrl.openStream() );
				SchemaFactory schemaFactory = SchemaFactory.newInstance( XMLConstants.W3C_XML_SCHEMA_NS_URI );
				return schemaFactory.newSchema( source );
			}
			catch (Exception e) {
				throw new XmlInfrastructureException( "Unable to load schema [" + schemaUrl.toExternalForm() + "]", e );
			}
			finally {
				try {
					schemaStream.close();
				}
				catch (IOException e) {
					log.debugf( "Problem closing schema stream - %s", e.toString() );
				}
			}
		}
		catch (IOException e) {
			throw new XmlInfrastructureException( "Stream error handling schema url [" + schemaUrl.toExternalForm() + "]" );
		}
	}
}
