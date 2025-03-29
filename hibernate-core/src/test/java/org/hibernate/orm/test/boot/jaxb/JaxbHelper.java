/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.jaxb;

import java.io.InputStream;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.validation.Schema;

import org.hibernate.boot.jaxb.internal.ContextProvidingValidationEventHandler;
import org.hibernate.boot.jaxb.internal.stax.BufferedXMLEventReader;
import org.hibernate.boot.jaxb.internal.stax.LocalXmlResourceResolver;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;

/**
 * @author Steve Ebersole
 */
public class JaxbHelper {
	public static final JaxbHelper INSTANCE = new JaxbHelper( false );
	public static final JaxbHelper VALIDATING = new JaxbHelper( true );

	private final boolean validationEnabled;

	public JaxbHelper(boolean validationEnabled) {
		this.validationEnabled = validationEnabled;
	}

	public boolean isValidationEnabled() {
		return validationEnabled;
	}

	public static <T> T withStaxEventReader(InputStream inputStream, ClassLoaderService cls, Function<XMLEventReader,T> action) {
		final XMLEventReader reader = createReader( inputStream, cls );
		final T applied = action.apply( reader );
		try {
			reader.close();
		}
		catch (XMLStreamException ignore) {
		}
		return applied;
	}

	public static void withStaxEventReader(InputStream inputStream, ClassLoaderService cls, Consumer<XMLEventReader> action) {
		final XMLEventReader reader = createReader( inputStream, cls );
		action.accept( reader );
		try {
			reader.close();
		}
		catch (XMLStreamException ignore) {
		}
	}

	private static XMLEventReader createReader(InputStream stream, ClassLoaderService cls) {
		final XMLInputFactory staxFactory = XMLInputFactory.newInstance();
		staxFactory.setXMLResolver( new LocalXmlResourceResolver( cls ) );

		try {
			// create a standard StAX reader
			final XMLEventReader staxReader = staxFactory.createXMLEventReader( stream );
			// and wrap it in a buffered reader (keeping 100 element sized buffer)
			return new BufferedXMLEventReader( staxReader, 100 );
		}
		catch (XMLStreamException e) {
			throw new RuntimeException( "Unable to create StAX reader", e );
		}
	}

	@SuppressWarnings("unchecked")
	public <T> T jaxb(XMLEventReader reader, Schema xsd, JAXBContext jaxbContext) throws JAXBException {
		final ContextProvidingValidationEventHandler handler = new ContextProvidingValidationEventHandler();

		final Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
		unmarshaller.setEventHandler( handler );

		if ( isValidationEnabled() ) {
			unmarshaller.setSchema( xsd );
		}
		else {
			unmarshaller.setSchema( null );
		}

		return (T) unmarshaller.unmarshal( reader );
	}
}
