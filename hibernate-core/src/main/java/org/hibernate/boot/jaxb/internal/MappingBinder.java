/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.jaxb.internal;

import java.util.Map;
import java.util.function.Supplier;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.events.StartElement;
import javax.xml.transform.stax.StAXSource;
import javax.xml.validation.Schema;
import javax.xml.validation.Validator;

import org.hibernate.Internal;
import org.hibernate.boot.MappingException;
import org.hibernate.boot.ResourceStreamLocator;
import org.hibernate.boot.UnsupportedOrmXsdVersionException;
import org.hibernate.boot.jaxb.Origin;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmHibernateMapping;
import org.hibernate.boot.jaxb.hbm.transform.UnsupportedFeatureHandling;
import org.hibernate.boot.jaxb.internal.stax.HbmEventReader;
import org.hibernate.boot.jaxb.internal.stax.JpaOrmXmlEventReader;
import org.hibernate.boot.jaxb.internal.stax.MappingEventReader;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityMappingsImpl;
import org.hibernate.boot.jaxb.spi.Binding;
import org.hibernate.boot.jaxb.spi.JaxbBindableMappingDescriptor;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.xsd.MappingXsdSupport;
import org.hibernate.boot.xsd.XmlValidationMode;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.internal.util.config.ConfigurationException;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.spi.ServiceRegistryImplementor;

import org.jboss.logging.Logger;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * Responsible for coordinating binding of mapping XML documents into
 * JAXB representations, producing {@link Binding} references.
 *
 * @author Steve Ebersole
 */
public class MappingBinder extends AbstractBinder<JaxbBindableMappingDescriptor> {
	private static final Logger log = Logger.getLogger( MappingBinder.class );

	private final XMLEventFactory xmlEventFactory = XMLEventFactory.newInstance();

	private final Supplier<Options> optionsAccess;

	private JAXBContext hbmJaxbContext;
	private JAXBContext entityMappingsJaxbContext;

	public interface Options {
		XmlValidationMode validationMode();
	}

	public static final Options NON_VALIDATING = () -> XmlValidationMode.DISABLED;

	public static final Options DEFAULT_VALIDATING = () -> XmlValidationMode.EXTENDED;

	public static final Options STRICT_VALIDATING = () -> XmlValidationMode.STRICT;

	/**
	 * Full constructor
	 */
	public MappingBinder(
			ResourceStreamLocator resourceStreamLocator,
			Supplier<Options> optionsAccess,
			Supplier<UnsupportedFeatureHandling> unsupportedHandlingAccess) {
		super( resourceStreamLocator );
		this.optionsAccess = optionsAccess;
	}

	/**
	 * Full non-lazy constructor
	 */
	private MappingBinder(
			ResourceStreamLocator resourceStreamLocator,
			Options options,
			UnsupportedFeatureHandling unsupportedHandling) {
		this( resourceStreamLocator, () -> options, () -> unsupportedHandling );
	}

	public MappingBinder(
			ResourceStreamLocator resourceStreamLocator,
			@Nullable Supplier<Map<String, Object>> settingsAccess) {
		super( resourceStreamLocator == null ? MappingBinder.class.getClassLoader()::getResourceAsStream : resourceStreamLocator );

		if ( settingsAccess == null ) {
			this.optionsAccess = () -> DEFAULT_VALIDATING;
		}
		else {
			this.optionsAccess = () -> (Options) () -> ConfigurationHelper.resolveXmlValidationMode( settingsAccess.get() );
		}
	}

	public MappingBinder(ServiceRegistry serviceRegistry) {
		this(
				serviceRegistry.getService( ClassLoaderService.class ),
				(Supplier<Map<String, Object>>) () -> {
					final ConfigurationService configurationService =
							serviceRegistry instanceof ServiceRegistryImplementor serviceRegistryImplementor
									? serviceRegistryImplementor.fromRegistryOrChildren( ConfigurationService.class )
									: serviceRegistry.getService( ConfigurationService.class );
					return configurationService == null ? null : configurationService.getSettings();
				}
		);
	}

	/**
	 * Constructor used by the Gradle plugin
	 */
	public MappingBinder(ResourceStreamLocator resourceStreamLocator, UnsupportedFeatureHandling unsupportedHandling) {
		this(
				resourceStreamLocator,
				MappingBinder.NON_VALIDATING,
				unsupportedHandling
		);
	}

	/**
	 * Constructor used everywhere else
	 */
	public MappingBinder(ResourceStreamLocator resourceStreamLocator, Options options) {
		this( resourceStreamLocator, options, UnsupportedFeatureHandling.ERROR );
	}

	protected XmlValidationMode getXmlValidationMode() {
		return optionsAccess.get().validationMode();
	}

	@Override
	protected <X extends JaxbBindableMappingDescriptor> Binding<X> doBind(
			JaxbBindingSource jaxbBindingSource,
			StartElement rootElementStartEvent) {
		final String rootElementLocalName = rootElementStartEvent.getName().getLocalPart();
		final XMLEventReader staxEventReader = jaxbBindingSource.getEventReader();
		final Origin origin = jaxbBindingSource.getOrigin();
		if ( "hibernate-mapping".equals( rootElementLocalName ) ) {
			if ( log.isTraceEnabled() ) {
				log.tracef( "Performing JAXB binding of hbm.xml document: %s", origin.toString() );
			}

			final XMLEventReader hbmReader = new HbmEventReader( staxEventReader, xmlEventFactory );

			final Schema xsd;
			if ( getXmlValidationMode() == XmlValidationMode.EXTENDED ) {
				xsd = MappingXsdSupport.INSTANCE.hbmXsd().getSchema();
			}
			else {
				xsd = null;
			}

			final JaxbHbmHibernateMapping hbmBindings = jaxb(
					hbmReader,
					xsd,
					hbmJaxbContext(),
					origin
			);

			//noinspection unchecked
			return new Binding<>( (X) hbmBindings, origin );
		}
		else {
			assert "entity-mappings".equals( rootElementLocalName );
			try {
				log.tracef( "Performing JAXB binding of orm.xml document: %s", origin.toString() );

				final XMLEventReader reader = new MappingEventReader( staxEventReader, xmlEventFactory );

				final Schema xsd;
				final XmlValidationMode validationMode = getXmlValidationMode();
				if ( validationMode == XmlValidationMode.STRICT ) {
					// deals with StrictValidationErrorHandler, etc
					doStrictValidation( jaxbBindingSource );
					xsd = null;
				}
				else if ( validationMode == XmlValidationMode.EXTENDED ) {
					// extended validation
					xsd = MappingXsdSupport.latestDescriptor().getSchema();
				}
				else {
					xsd = null;
				}

				final JaxbEntityMappingsImpl bindingRoot = jaxb(
						reader,
						xsd,
						mappingJaxbContext(),
						origin
				);

				//noinspection unchecked
				return new Binding<>( (X) bindingRoot, origin );
			}
			catch (JpaOrmXmlEventReader.BadVersionException e) {
				throw new UnsupportedOrmXsdVersionException( e.getRequestedVersion(), origin );
			}
		}
	}

	private void doStrictValidation(JaxbBindingSource jaxbBindingSource) {
		StrictValidationErrorHandler errorHandler = new StrictValidationErrorHandler();
		try {
			Validator validator = MappingXsdSupport.latestJpaDescriptor().getSchema()
					.newValidator();
			validator.setErrorHandler( errorHandler );
			// We need 'clean' access to the InputStream at this point, using the staxEventReader leads to errors
			validator.validate( new StAXSource(jaxbBindingSource.getEventReader()) );
		}
		catch (Exception e) {
			throw new MappingException(
					"Strict validation failure: " + errorHandler.getMessage(),
					e,
					jaxbBindingSource.getOrigin() );
		}
	}

	private JAXBContext hbmJaxbContext() {
		if ( hbmJaxbContext == null ) {
			try {
				hbmJaxbContext = JAXBContext.newInstance( JaxbHbmHibernateMapping.class );
			}
			catch (JAXBException e) {
				throw new ConfigurationException( "Unable to build hbm.xml JAXBContext", e );
			}
		}
		return hbmJaxbContext;
	}

	@Internal
	public JAXBContext mappingJaxbContext() {
		if ( entityMappingsJaxbContext == null ) {
			try {
				entityMappingsJaxbContext = JAXBContext.newInstance( JaxbEntityMappingsImpl.class );
			}
			catch (JAXBException e) {
				throw new ConfigurationException( "Unable to build orm.xml JAXBContext", e );
			}
		}
		return entityMappingsJaxbContext;
	}

	private static final class StrictValidationErrorHandler implements ErrorHandler {
		private int lineNumber;
		private int columnNumber;
		private String message;

		@Override
		public void warning(SAXParseException exception) {}

		@Override
		// Capture validation errors
		public void error(SAXParseException exception) throws SAXException {
			lineNumber = exception.getLineNumber();
			columnNumber = exception.getColumnNumber();
			message = exception.toString();
			throw new SAXException( getMessage() );
		}

		@Override
		public void fatalError(SAXParseException exception) {
		}

		public String getMessage() {
			StringBuilder sb = new StringBuilder();
			sb.append( message ).append( lineNumber != -1 && columnNumber != -1 ? " at line number " + lineNumber + " and column number " + columnNumber : "" );
			return sb.toString();
		}
	}

}
