/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.jaxb.internal;

import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.events.StartElement;

import org.hibernate.Internal;
import org.hibernate.boot.ResourceStreamLocator;
import org.hibernate.boot.jaxb.Origin;
import org.hibernate.boot.jaxb.configuration.spi.JaxbPersistenceImpl;
import org.hibernate.boot.jaxb.internal.stax.ConfigurationEventReader;
import org.hibernate.boot.jaxb.spi.Binding;
import org.hibernate.boot.xsd.ConfigXsdSupport;
import org.hibernate.internal.util.config.ConfigurationException;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;

/**
 * @author Steve Ebersole
 */
public class ConfigurationBinder extends AbstractBinder<JaxbPersistenceImpl> {
	private final XMLEventFactory xmlEventFactory = XMLEventFactory.newInstance();
	private JAXBContext jaxbContext;

	public ConfigurationBinder(ResourceStreamLocator resourceStreamLocator) {
		super( resourceStreamLocator );
	}

	@Override
	public boolean isValidationEnabled() {
		return false;
	}

	@Override
	protected <X extends JaxbPersistenceImpl> Binding<X> doBind(
			XMLEventReader staxEventReader,
			StartElement rootElementStartEvent,
			Origin origin) {
		final XMLEventReader reader = new ConfigurationEventReader( staxEventReader, xmlEventFactory );
		final JaxbPersistenceImpl bindingRoot = jaxb(
				reader,
				ConfigXsdSupport.configurationXsd().getSchema(),
				jaxbContext(),
				origin
		);
		//noinspection unchecked
		return new Binding<>( (X) bindingRoot, origin );
	}

	@Internal
	public JAXBContext jaxbContext() {
		if ( jaxbContext == null ) {
			try {
				jaxbContext = JAXBContext.newInstance( JaxbPersistenceImpl.class );
			}
			catch (JAXBException e) {
				throw new ConfigurationException( "Unable to build configuration.xml JAXBContext", e );
			}
		}
		return jaxbContext;
	}
}
