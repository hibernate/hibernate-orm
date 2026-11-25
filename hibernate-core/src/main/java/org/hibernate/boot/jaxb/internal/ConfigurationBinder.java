/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.jaxb.internal;

import java.util.function.Function;
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
import org.hibernate.cfg.MappingSettings;
import org.hibernate.internal.util.config.ConfigurationException;

import jakarta.annotation.Nullable;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;

import static org.hibernate.engine.config.spi.StandardConverters.BOOLEAN;

/**
 * @author Steve Ebersole
 */
public class ConfigurationBinder extends AbstractBinder<JaxbPersistenceImpl> {
	private final XMLEventFactory xmlEventFactory = XMLEventFactory.newInstance();
	/**
	 * Access to the settings available while parsing {@code persistence.xml}.
	 * {@code null} means no settings, in which case the secure defaults apply.
	 */
	private final @Nullable Function<String, Object> settingsAccess;
	private JAXBContext jaxbContext;

	public ConfigurationBinder(ResourceStreamLocator resourceStreamLocator) {
		this( resourceStreamLocator, null );
	}

	public ConfigurationBinder(
			ResourceStreamLocator resourceStreamLocator,
			@Nullable Function<String, Object> settingsAccess) {
		super( resourceStreamLocator );
		this.settingsAccess = settingsAccess;
	}

	@Override
	public boolean isValidationEnabled() {
		return false;
	}

	// persistence.xml is parsed before the persistence unit properties are known, so
	// only the settings supplied through settingsAccess can enable DTD processing here.

	@Override
	public boolean isDtdAndExternalEntitiesAllowed() {
		if ( settingsAccess == null ) {
			return false;
		}
		final Object setting = settingsAccess.apply( MappingSettings.XML_ALLOW_DTD_AND_EXTERNAL_ENTITIES );
		return setting != null && BOOLEAN.convert( setting );
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
