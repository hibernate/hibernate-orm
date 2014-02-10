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
package org.hibernate.xml.internal.stax;

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
	MAPPING(
			"http://www.hibernate.org/xsd/orm/mapping",
			"org/hibernate/xsd/mapping/mapping-2.1.0.xsd",
			"2.1.0"
	),
	HBM(
			"http://www.hibernate.org/xsd/orm/hbm",
			"org/hibernate/xsd/mapping/legacy-mapping-4.0.xsd",
			"4.0"
	)
//	, CONFIGURATION( )
//	, CFG( )
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


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// todo : centralize this...
	// 		this is all a general case of wanting to resolve resources strictly
	// 		using the ClassLoader

	private static javax.xml.validation.Schema resolveLocalSchema(String schemaName) {
		return resolveLocalSchema( resolveLocalSchemaUrl( schemaName ) );
	}

	private static URL resolveLocalSchemaUrl(String schemaName) {
		URL url = LocalSchemaLocator.class.getClassLoader().getResource( schemaName );
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
