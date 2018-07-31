/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.boot.xsd;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.hibernate.internal.util.xml.XsdException;

import org.jboss.logging.Logger;

import org.xml.sax.SAXException;

/**
 * When Hibernate loads an XSD we fully expect that to be resolved from our
 * jar file via ClassLoader resource look-up.  This class simplifies
 * the steps needed to achieve those goals explicitly using its own
 * ClassLoader for the look-ups.
 *
 * @author Steve Ebersole
 */
@SuppressWarnings("WeakerAccess")
public class LocalXsdResolver {

	public static String latestJpaVerison() {
		return "2.2";
	}

	public static boolean isValidJpaVersion(String version) {
		switch ( version ) {
			case "1.0":
			case "2.0":
			case "2.1":
			case "2.2":
				return true;
			default:
				return false;
		}
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
			InputStream schemaStream = url.openStream();
			try {
				StreamSource source = new StreamSource( url.openStream() );
				SchemaFactory schemaFactory = SchemaFactory.newInstance( XMLConstants.W3C_XML_SCHEMA_NS_URI );
				return schemaFactory.newSchema( source );
			}
			catch ( SAXException | IOException e ) {
				throw new XsdException( "Unable to load schema [" + schemaResourceName + "]", e, schemaResourceName );
			}
			finally {
				try {
					schemaStream.close();
				}
				catch ( IOException e ) {
					Logger.getLogger( LocalXsdResolver.class ).debugf( "Problem closing schema stream [%s]", e.toString() );
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
