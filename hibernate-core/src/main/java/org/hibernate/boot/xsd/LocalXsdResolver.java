/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.xsd;

import java.io.IOException;
import java.net.URL;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.hibernate.internal.util.xml.XsdException;

import org.xml.sax.SAXException;

import static javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI;
import static org.hibernate.boot.jaxb.JaxbLogger.JAXB_LOGGER;

/**
 * When Hibernate loads an XSD we fully expect that to be resolved from our
 * jar file via ClassLoader resource look-up.  This class simplifies
 * the steps needed to achieve those goals explicitly using its own
 * ClassLoader for the look-ups.
 *
 * @author Steve Ebersole
 */
public class LocalXsdResolver {

	public static String latestJpaVerison() {
		return "3.2";
	}

	public static boolean isValidJpaVersion(String version) {
		return switch ( version ) {
			case "1.0", "2.0", "2.1", "2.2", "3.0", "3.1", "3.2" -> true;
			default -> false;
		};
	}

	public static URL resolveLocalXsdUrl(String resourceName) {
		try {
			final URL url = LocalXsdResolver.class.getClassLoader().getResource( resourceName );
			if ( url != null ) {
				return url;
			}
		}
		catch (Exception ignore) {
		}

		if ( resourceName.startsWith( "/" ) ) {
			resourceName = resourceName.substring( 1 );

			try {
				final URL url = LocalXsdResolver.class.getClassLoader().getResource( resourceName );
				if ( url != null ) {
					return url;
				}
			}
			catch (Exception ignore) {
			}
		}

		// Last: we try name as a URL
		try {
			return new URL( resourceName );
		}
		catch (Exception ignore) {
		}

		return null;
	}


	public static Schema resolveLocalXsdSchema(String schemaResourceName) {
		final URL url = resolveLocalXsdUrl( schemaResourceName );
		if ( url == null ) {
			throw new XsdException( "Unable to locate schema [" + schemaResourceName + "] via classpath", schemaResourceName );
		}
		try {
			final var schemaStream = url.openStream();
			try {
				return SchemaFactory.newInstance( W3C_XML_SCHEMA_NS_URI )
						.newSchema( new StreamSource( url.openStream() ) );
			}
			catch ( SAXException | IOException e ) {
				throw new XsdException( "Unable to load schema [" + schemaResourceName + "]", e, schemaResourceName );
			}
			finally {
				try {
					schemaStream.close();
				}
				catch ( IOException e ) {
					JAXB_LOGGER.problemClosingSchemaStream( e.toString() );
				}
			}
		}
		catch ( IOException e ) {
			throw new XsdException( "Stream error handling schema url [" + url.toExternalForm() + "]", schemaResourceName );
		}
	}

	public static XsdDescriptor buildXsdDescriptor(String resourceName, String version, String namespaceUri) {
		return new XsdDescriptor( resourceName, resolveLocalXsdSchema( resourceName ), version, namespaceUri );
	}
}
