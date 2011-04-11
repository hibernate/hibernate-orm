/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.metamodel.source.util.xml;

import java.io.InputStream;
import java.net.URL;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

/**
 * @author Hardy Ferentschik
 */
public class XmlHelper {
	private static final Logger log = LoggerFactory.getLogger( XmlHelper.class );

	private XmlHelper() {
	}

	public static <T> JaxbRoot<T> unmarshallXml(String fileName, String schemaName, Class<T> clazz)
			throws JAXBException {
		Schema schema = getMappingSchema( schemaName );
		InputStream in = getInputStreamForPath( fileName );
		JAXBContext jc = JAXBContext.newInstance( clazz );
		Unmarshaller unmarshaller = jc.createUnmarshaller();
		unmarshaller.setSchema( schema );
		StreamSource stream = new StreamSource( in );
		JAXBElement<T> elem = unmarshaller.unmarshal( stream, clazz );
		Origin origin = new OriginImpl( "", fileName );
		return new JaxbRootImpl<T>( elem.getValue(), origin );
	}

	private static Schema getMappingSchema(String schemaVersion) {
		// todo - think about class loading. does this have to go via the class loader service?
		ClassLoader loader = XmlHelper.class.getClassLoader();
		URL schemaUrl = loader.getResource( schemaVersion );
		SchemaFactory sf = SchemaFactory.newInstance( javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI );
		Schema schema = null;
		try {
			schema = sf.newSchema( schemaUrl );
		}
		catch ( SAXException e ) {
			log.debug( "Unable to create schema for {}: {}", schemaVersion, e.getMessage() );
		}
		return schema;
	}

	private static InputStream getInputStreamForPath(String path) {
		// try the context class loader first
		InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream( path );

		// try the current class loader
		if ( inputStream == null ) {
			inputStream = XmlHelper.class.getResourceAsStream( path );
		}
		return inputStream;
	}
}


