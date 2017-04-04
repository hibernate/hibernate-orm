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
 * Helper for resolving XML Schema references locally.
 * <p/>
 * Note that *by design* we always use our ClassLoader to perform the lookups here.
 *
 * @author Steve Ebersole
 */
public class LocalSchemaLocator {
	private static final Logger log = Logger.getLogger( LocalSchemaLocator.class );

	/**
	 * Disallow direct instantiation
	 */
	private LocalSchemaLocator() {
	}

	/**
	 * Given the resource name of a schema, locate its URL reference via ClassLoader lookup.
	 *
	 * @param schemaResourceName The local resource name to the schema
	 *
	 * @return
	 */
	public static URL resolveLocalSchemaUrl(String schemaResourceName) {
		URL url = LocalSchemaLocator.class.getClassLoader().getResource( schemaResourceName );
		if ( url == null ) {
			throw new XmlInfrastructureException( "Unable to locate schema [" + schemaResourceName + "] via classpath" );
		}
		return url;
	}

	public static Schema resolveLocalSchema(String schemaName){
		return resolveLocalSchema( resolveLocalSchemaUrl( schemaName ) );
	}

	public static Schema resolveLocalSchema(URL schemaUrl) {
		try {
			InputStream schemaStream = schemaUrl.openStream();
			try {
				StreamSource source = new StreamSource(schemaUrl.openStream());
				SchemaFactory schemaFactory = SchemaFactory.newInstance( XMLConstants.W3C_XML_SCHEMA_NS_URI );
				return schemaFactory.newSchema(source);
			}
			catch ( Exception e ) {
				throw new XmlInfrastructureException( "Unable to load schema [" + schemaUrl.toExternalForm() + "]", e );
			}
			finally {
				try {
					schemaStream.close();
				}
				catch ( IOException e ) {
					log.debugf( "Problem closing schema stream - %s", e.toString() );
				}
			}
		}
		catch ( IOException e ) {
			throw new XmlInfrastructureException( "Stream error handling schema url [" + schemaUrl.toExternalForm() + "]" );
		}
	}
}
