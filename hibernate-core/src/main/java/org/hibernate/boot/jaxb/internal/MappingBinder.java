/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.jaxb.internal;

import java.util.function.Function;
import java.util.function.Supplier;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.events.StartElement;

import org.hibernate.Internal;
import org.hibernate.boot.ResourceStreamLocator;
import org.hibernate.boot.UnsupportedOrmXsdVersionException;
import org.hibernate.boot.jaxb.JaxbLogger;
import org.hibernate.boot.jaxb.Origin;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmHibernateMapping;
import org.hibernate.boot.jaxb.hbm.transform.HbmXmlTransformer;
import org.hibernate.boot.jaxb.hbm.transform.UnsupportedFeatureHandling;
import org.hibernate.boot.jaxb.internal.stax.HbmEventReader;
import org.hibernate.boot.jaxb.internal.stax.JpaOrmXmlEventReader;
import org.hibernate.boot.jaxb.internal.stax.MappingEventReader;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityMappingsImpl;
import org.hibernate.boot.jaxb.spi.JaxbBindableMappingDescriptor;
import org.hibernate.boot.jaxb.spi.Binding;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.xsd.MappingXsdSupport;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.internal.log.DeprecationLogger;
import org.hibernate.internal.util.config.ConfigurationException;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.spi.ServiceRegistryImplementor;

import org.jboss.logging.Logger;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;

import static org.hibernate.engine.config.spi.StandardConverters.BOOLEAN;

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
	private final Supplier<UnsupportedFeatureHandling> unsupportedHandlingAccess;

	private JAXBContext hbmJaxbContext;
	private JAXBContext entityMappingsJaxbContext;

	public interface Options {
		boolean validateMappings();

		boolean transformHbmMappings();
	}

	public static final Options VALIDATING = new Options() {
		@Override
		public boolean validateMappings() {
			return true;
		}

		@Override
		public boolean transformHbmMappings() {
			return false;
		}
	};

	public static final Options NON_VALIDATING = new Options() {
		@Override
		public boolean validateMappings() {
			return true;
		}

		@Override
		public boolean transformHbmMappings() {
			return false;
		}
	};

	/**
	 * Full constructor
	 */
	public MappingBinder(
			ResourceStreamLocator resourceStreamLocator,
			Supplier<Options> optionsAccess,
			Supplier<UnsupportedFeatureHandling> unsupportedHandlingAccess) {
		super( resourceStreamLocator );
		this.optionsAccess = optionsAccess;
		this.unsupportedHandlingAccess = unsupportedHandlingAccess;
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
			Function<String, Object> settingsAccess) {
		super( resourceStreamLocator == null ? MappingBinder.class.getClassLoader()::getResourceAsStream : resourceStreamLocator );

		if ( settingsAccess == null ) {
			this.optionsAccess = () -> VALIDATING;
			this.unsupportedHandlingAccess = () -> UnsupportedFeatureHandling.ERROR;
		}
		else {
			this.optionsAccess = () -> new Options() {
				@Override
				public boolean validateMappings() {
					final Object setting = settingsAccess.apply( AvailableSettings.VALIDATE_XML );
					if ( setting == null ) {
						return false;
					}
					return BOOLEAN.convert( setting );
				}

				@Override
				public boolean transformHbmMappings() {
					final Object setting = settingsAccess.apply( AvailableSettings.TRANSFORM_HBM_XML );
					if ( setting == null ) {
						return false;
					}
					return BOOLEAN.convert( setting );
				}
			};

			this.unsupportedHandlingAccess = () -> {
				final Object setting = settingsAccess.apply( AvailableSettings.TRANSFORM_HBM_XML_FEATURE_HANDLING );
				return UnsupportedFeatureHandling.fromSetting( setting, UnsupportedFeatureHandling.ERROR );
			};
		}
	}

	public MappingBinder(ServiceRegistry serviceRegistry) {
		this(
				serviceRegistry.getService( ClassLoaderService.class ),
				(settingName) -> {
					final ConfigurationService configurationService;
					if ( serviceRegistry instanceof ServiceRegistryImplementor ) {
						final ServiceRegistryImplementor serviceRegistryImplementor = (ServiceRegistryImplementor) serviceRegistry;
						configurationService = serviceRegistryImplementor.fromRegistryOrChildren( ConfigurationService.class );
					}
					else {
						configurationService = serviceRegistry.getService( ConfigurationService.class );
					}

					return configurationService == null ? null : configurationService.getSettings().get( settingName );
				}
		);
	}

	/**
	 * Constructor used by the Gradle plugin
	 */
	public MappingBinder(ResourceStreamLocator resourceStreamLocator, UnsupportedFeatureHandling unsupportedHandling) {
		this(
				resourceStreamLocator,
				new Options() {
					@Override
					public boolean validateMappings() {
						return false;
					}

					@Override
					public boolean transformHbmMappings() {
						return false;
					}
				},
				unsupportedHandling
		);
	}

	/**
	 * Constructor used everywhere else
	 */
	public MappingBinder(ResourceStreamLocator resourceStreamLocator, Options options) {
		this( resourceStreamLocator, options, UnsupportedFeatureHandling.ERROR );
	}

	@Override
	public boolean isValidationEnabled() {
		return optionsAccess.get().validateMappings();
	}

	@Override
	protected <X extends JaxbBindableMappingDescriptor> Binding<X> doBind(
			XMLEventReader staxEventReader,
			StartElement rootElementStartEvent,
			Origin origin) {
		final String rootElementLocalName = rootElementStartEvent.getName().getLocalPart();
		if ( "hibernate-mapping".equals( rootElementLocalName ) ) {
			if ( log.isDebugEnabled() ) {
				log.debugf( "Performing JAXB binding of hbm.xml document : %s", origin.toString() );
			}

			final XMLEventReader hbmReader = new HbmEventReader( staxEventReader, xmlEventFactory );
			final JaxbHbmHibernateMapping hbmBindings = jaxb(
					hbmReader,
					MappingXsdSupport.INSTANCE.hbmXsd().getSchema(),
					hbmJaxbContext(),
					origin
			);

			if ( optionsAccess.get().transformHbmMappings() ) {
				JaxbLogger.JAXB_LOGGER.tracef( "Performing on-the-fly hbm.xml -> mapping.xml transformation - %s ", origin );
				//noinspection unchecked
				return new Binding<>( (X) HbmXmlTransformer.transform( hbmBindings, origin, unsupportedHandlingAccess::get ), origin );
			}

			DeprecationLogger.DEPRECATION_LOGGER.logDeprecatedHbmXmlProcessing( origin.getType(), origin.getName() );
			//noinspection unchecked
			return new Binding<>( (X) hbmBindings, origin );
		}
		else {
			assert "entity-mappings".equals( rootElementLocalName );
			try {
				log.debugf( "Performing JAXB binding of orm.xml document : %s", origin.toString() );

				final XMLEventReader reader = new MappingEventReader( staxEventReader, xmlEventFactory );
				final JaxbEntityMappingsImpl bindingRoot = jaxb(
						reader,
						MappingXsdSupport.latestDescriptor().getSchema(),
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
}
