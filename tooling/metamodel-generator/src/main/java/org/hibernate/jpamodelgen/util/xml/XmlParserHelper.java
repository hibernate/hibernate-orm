/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpamodelgen.util.xml;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.hibernate.jpamodelgen.Context;
import org.hibernate.jpamodelgen.util.Constants;
import org.hibernate.jpamodelgen.xml.jaxb.ObjectFactory;

import org.xml.sax.SAXException;

/**
 * Provides common functionality used for XML parsing.
 *
 * @author Gunnar Morling
 * @author Hardy Ferentschik
 */
public class XmlParserHelper {
	/**
	 * Path separator used for resource loading
	 */
	private static final String RESOURCE_PATH_SEPARATOR = "/";

	/**
	 * The expected number of XML schemas managed by this class. Used to set the
	 * initial cache size.
	 */
	private static final int NUMBER_OF_SCHEMAS = 4;

	private static final XMLInputFactory XML_INPUT_FACTORY = XMLInputFactory.newInstance();

	private static final ConcurrentMap<String, Schema> SCHEMA_CACHE = new ConcurrentHashMap<String, Schema>(
			NUMBER_OF_SCHEMAS
	);

	private final Context context;

	public XmlParserHelper(Context context) {
		this.context = context;
	}

	/**
	 * Returns an input stream for the specified resource. First an attempt is made to load the resource
	 * via the {@code Filer} API and if that fails {@link Class#getResourceAsStream}  is used.
	 *
	 * @param resource the resource to load
	 *
	 * @return an input stream for the specified resource or {@code null} in case resource cannot be loaded
	 */
	public InputStream getInputStreamForResource(String resource) {
		// METAGEN-75
		if ( !resource.startsWith( RESOURCE_PATH_SEPARATOR ) ) {
			resource = RESOURCE_PATH_SEPARATOR + resource;
		}

		String pkg = getPackage( resource );
		String name = getRelativeName( resource );
		InputStream ormStream;
		try {
			FileObject fileObject = context.getProcessingEnvironment()
					.getFiler()
					.getResource( StandardLocation.CLASS_OUTPUT, pkg, name );
			ormStream = fileObject.openInputStream();
		}
		catch ( IOException e1 ) {
			// TODO - METAGEN-12
			// unfortunately, the Filer.getResource API seems not to be able to load from /META-INF. One gets a
			// FilerException with the message with "Illegal name /META-INF". This means that we have to revert to
			// using the classpath. This might mean that we find a persistence.xml which is 'part of another jar.
			// Not sure what else we can do here
			ormStream = this.getClass().getResourceAsStream( resource );
		}
		return ormStream;
	}

	public Schema getSchema(String schemaResource) throws XmlParsingException {
		Schema schema = SCHEMA_CACHE.get( schemaResource );

		if ( schema != null ) {
			return schema;
		}

		schema = loadSchema( schemaResource );
		Schema previous = SCHEMA_CACHE.putIfAbsent( schemaResource, schema );

		return previous != null ? previous : schema;
	}

	public <T> T getJaxbRoot(InputStream stream, Class<T> clazz, Schema schema)
			throws XmlParsingException {

		XMLEventReader staxEventReader;
		try {
			staxEventReader = createXmlEventReader( stream );
		}
		catch ( XMLStreamException e ) {
			throw new XmlParsingException( "Unable to create stax reader", e );
		}

		ContextProvidingValidationEventHandler handler = new ContextProvidingValidationEventHandler();
		try {
			staxEventReader = new JpaNamespaceTransformingEventReader( staxEventReader );
			JAXBContext jaxbContext = JAXBContext.newInstance( ObjectFactory.class );
			Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
			unmarshaller.setSchema( schema );
			unmarshaller.setEventHandler( handler );
			return clazz.cast( unmarshaller.unmarshal( staxEventReader ) );
		}
		catch ( JAXBException e ) {
			StringBuilder builder = new StringBuilder();
			builder.append( "Unable to perform unmarshalling at line number " );
			builder.append( handler.getLineNumber() );
			builder.append( " and column " );
			builder.append( handler.getColumnNumber() );
			builder.append( ". Message: " );
			builder.append( handler.getMessage() );
			throw new XmlParsingException( builder.toString(), e );
		}
	}

	private synchronized XMLEventReader createXmlEventReader(InputStream xmlStream) throws XMLStreamException {
		return XML_INPUT_FACTORY.createXMLEventReader( xmlStream );
	}

	private String getPackage(String resourceName) {
		if ( !resourceName.contains( Constants.PATH_SEPARATOR ) ) {
			return "";
		}
		else {
			return resourceName.substring( 0, resourceName.lastIndexOf( Constants.PATH_SEPARATOR ) );
		}
	}

	private String getRelativeName(String resourceName) {
		if ( !resourceName.contains( Constants.PATH_SEPARATOR ) ) {
			return resourceName;
		}
		else {
			return resourceName.substring( resourceName.lastIndexOf( Constants.PATH_SEPARATOR ) + 1 );
		}
	}

	private Schema loadSchema(String schemaName) throws XmlParsingException {
		Schema schema = null;
		URL schemaUrl = this.getClass().getClassLoader().getResource( schemaName );
		if ( schemaUrl == null ) {
			return schema;
		}

		SchemaFactory sf = SchemaFactory.newInstance( javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI );
		try {
			schema = sf.newSchema( schemaUrl );
		}
		catch ( SAXException e ) {
			throw new XmlParsingException( "Unable to create schema for " + schemaName + ": " + e.getMessage(), e );
		}
		return schema;
	}
}
