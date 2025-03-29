/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.util.xml;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.hibernate.boot.jaxb.cfg.spi.ObjectFactory;

import org.hibernate.processor.Context;
import org.hibernate.processor.util.NullnessUtil;

import org.checkerframework.checker.nullness.qual.Nullable;
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

	private static final ConcurrentMap<String, Schema> SCHEMA_CACHE = new ConcurrentHashMap<>(
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
	public @Nullable InputStream getInputStreamForResource(String resource) {
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

			// Class.getClassLoader() may return null if the class was loaded by the bootstrap class loader,
			// but since we don't expect the annotation processor to be loaded by that class loader,
			// we expect the return to be non-null and hence cast
			ClassLoader cl = NullnessUtil.castNonNull( ObjectFactory.class.getClassLoader() );
			String packageName = NullnessUtil.castNonNull( ObjectFactory.class.getPackage() ).getName();
			JAXBContext jaxbContext = JAXBContext.newInstance( packageName, cl );

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
		if ( !resourceName.contains("/") ) {
			return "";
		}
		else {
			return resourceName.substring( 0, resourceName.lastIndexOf("/") );
		}
	}

	private String getRelativeName(String resourceName) {
		if ( !resourceName.contains("/") ) {
			return resourceName;
		}
		else {
			return resourceName.substring( resourceName.lastIndexOf("/") + 1 );
		}
	}

	private Schema loadSchema(String schemaName) throws XmlParsingException {
		URL schemaUrl = NullnessUtil.castNonNull( this.getClass().getClassLoader() ).getResource( schemaName );
		if ( schemaUrl == null ) {
			throw new IllegalArgumentException( "Couldn't find schema on classpath: " + schemaName );
		}

		SchemaFactory sf = SchemaFactory.newInstance( javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI );
		try {
			return sf.newSchema( schemaUrl );
		}
		catch ( SAXException e ) {
			throw new XmlParsingException( "Unable to create schema for " + schemaName + ": " + e.getMessage(), e );
		}
	}
}
