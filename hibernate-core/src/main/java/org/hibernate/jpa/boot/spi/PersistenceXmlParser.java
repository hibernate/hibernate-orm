/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.boot.spi;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import javax.xml.transform.stream.StreamSource;

import org.hibernate.boot.archive.internal.ArchiveHelper;
import org.hibernate.boot.jaxb.Origin;
import org.hibernate.boot.jaxb.SourceType;
import org.hibernate.boot.jaxb.configuration.spi.JaxbPersistenceImpl;
import org.hibernate.boot.jaxb.configuration.spi.JaxbPersistenceImpl.JaxbPersistenceUnitImpl;
import org.hibernate.boot.jaxb.configuration.spi.JaxbPersistenceImpl.JaxbPersistenceUnitImpl.JaxbPropertiesImpl;
import org.hibernate.boot.jaxb.configuration.spi.JaxbPersistenceImpl.JaxbPersistenceUnitImpl.JaxbPropertiesImpl.JaxbPropertyImpl;
import org.hibernate.boot.jaxb.internal.ConfigurationBinder;
import org.hibernate.boot.jaxb.spi.Binding;
import org.hibernate.boot.registry.classloading.internal.ClassLoaderServiceImpl;
import org.hibernate.boot.registry.classloading.internal.TcclLookupPrecedence;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.log.DeprecationLogger;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.jpa.boot.internal.ParsedPersistenceXmlDescriptor;
import org.hibernate.jpa.internal.util.ConfigurationHelper;

import jakarta.persistence.PersistenceException;
import jakarta.persistence.PersistenceUnitTransactionType;
import org.hibernate.jpa.internal.util.PersistenceUnitTransactionTypeHelper;

import static jakarta.persistence.PersistenceUnitTransactionType.JTA;
import static jakarta.persistence.PersistenceUnitTransactionType.RESOURCE_LOCAL;
import static org.hibernate.internal.util.StringHelper.isEmpty;

/**
 * Used by Hibernate to parse {@code persistence.xml} files in SE environments.
 *
 * @author Steve Ebersole
 */
public final class PersistenceXmlParser {

	private static final CoreMessageLogger log = CoreLogging.messageLogger( PersistenceXmlParser.class );

	/**
	 * @return A {@link PersistenceXmlParser} using no settings at all.
	 */
	public static PersistenceXmlParser create() {
		return new PersistenceXmlParser( Map.of(), null, null );
	}

	/**
	 * @param integration The Map of integration settings
	 * @return A {@link PersistenceXmlParser} using the provided settings.
	 */
	public static PersistenceXmlParser create(Map<?,?> integration) {
		return new PersistenceXmlParser( integration, null, null );
	}

	/**
	 * @param integration The Map of integration settings
	 * @param providedClassLoader A class loader to use.
	 * Resources will be retrieved from this classloader in priority,
	 * before looking into classloaders mentioned in integration settings.
	 * @param providedClassLoaderService A class loader service to use.
	 * Takes precedence over classloading settings in {@code integration}
	 * and over {@code providedClassLoader}.
	 * @return A {@link PersistenceXmlParser} using the provided settings and classloading configuration.
	 */
	public static PersistenceXmlParser create(Map<?,?> integration, ClassLoader providedClassLoader,
			ClassLoaderService providedClassLoaderService) {
		return new PersistenceXmlParser( integration, providedClassLoader, providedClassLoaderService );
	}

	private final Map<?, ?> integration;
	private final ClassLoaderService classLoaderService;

	private PersistenceXmlParser(Map<?, ?> integration, ClassLoader providedClassLoader,
			ClassLoaderService providedClassLoaderService) {
		this.integration = integration;
		if ( providedClassLoaderService != null ) {
			classLoaderService = providedClassLoaderService;
		}
		else {
			final List<ClassLoader> providedClassLoaders = new ArrayList<>();
			if ( providedClassLoader != null ) {
				providedClassLoaders.add( providedClassLoader );
			}

			@SuppressWarnings("unchecked")
			final Collection<ClassLoader> classLoaders =
					(Collection<ClassLoader>) integration.get( AvailableSettings.CLASSLOADERS );
			if ( classLoaders != null ) {
				providedClassLoaders.addAll( classLoaders );
			}

			classLoaderService = new ClassLoaderServiceImpl(
					providedClassLoaders,
					TcclLookupPrecedence.from( integration, TcclLookupPrecedence.AFTER )
			);
		}
	}

	/**
	 * @return The {@link ClassLoaderService} used by this parser.
	 * Useful to retrieve URLs of persistence.xml files.
	 */
	public ClassLoaderService getClassLoaderService() {
		return classLoaderService;
	}

	/**
	 * Generic method to parse specified {@code persistence.xml} files and return a Map of descriptors
	 * for all discovered persistence-units keyed by the PU name.
	 *
	 * @param persistenceXmlUrls The URL of the {@code persistence.xml} files to parse
	 *
	 * @return Map of persistence-unit descriptors keyed by the PU name
	 */
	public Map<String, PersistenceUnitDescriptor> parse(List<URL> persistenceXmlUrls) {
		final Map<String, PersistenceUnitDescriptor> persistenceUnits = new HashMap<>();
		parsePersistenceXml( persistenceUnits, persistenceXmlUrls, RESOURCE_LOCAL );
		return persistenceUnits;
	}

	/**
	 * Generic method to parse specified {@code persistence.xml} files and return a Map of descriptors
	 * for all discovered persistence-units keyed by the PU name.
	 *
	 * @param persistenceXmlUrls The URLs of the {@code persistence.xml} files to parse
	 * @param transactionType The specific PersistenceUnitTransactionType to incorporate into the persistence-unit descriptor
	 *
	 * @return Map of persistence-unit descriptors keyed by the PU name
	 */
	public Map<String, PersistenceUnitDescriptor> parse(
			List<URL> persistenceXmlUrls,
			PersistenceUnitTransactionType transactionType) {
		Map<String, PersistenceUnitDescriptor> persistenceUnits = new HashMap<>();
		parsePersistenceXml( persistenceUnits, persistenceXmlUrls, transactionType );
		return persistenceUnits;
	}

	private void parsePersistenceXml(Map<String, PersistenceUnitDescriptor> persistenceUnits,
			List<URL> xmlUrls,
			PersistenceUnitTransactionType defaultTransactionType) {
		for ( URL xmlUrl : xmlUrls ) {
			parsePersistenceXml( persistenceUnits, xmlUrl, defaultTransactionType );
		}
	}

	private void parsePersistenceXml(Map<String, PersistenceUnitDescriptor> persistenceUnits,
			URL xmlUrl, PersistenceUnitTransactionType defaultTransactionType) {
		if ( log.isTraceEnabled() ) {
			log.tracef( "Attempting to parse persistence.xml file : %s", xmlUrl.toExternalForm() );
		}

		final URL persistenceUnitRootUrl = ArchiveHelper.getJarURLFromURLEntry( xmlUrl, "/META-INF/persistence.xml" );

		final JaxbPersistenceImpl jaxbPersistence = loadUrlWithJaxb( xmlUrl );
		final List<JaxbPersistenceUnitImpl> jaxbPersistenceUnits = jaxbPersistence.getPersistenceUnit();

		for ( int i = 0; i < jaxbPersistenceUnits.size(); i++ ) {
			final JaxbPersistenceUnitImpl jaxbPersistenceUnit = jaxbPersistenceUnits.get( i );

			if ( persistenceUnits.containsKey( jaxbPersistenceUnit.getName() ) ) {
				log.duplicatedPersistenceUnitName( jaxbPersistenceUnit.getName() );
				continue;
			}

			final ParsedPersistenceXmlDescriptor persistenceUnitDescriptor =
					new ParsedPersistenceXmlDescriptor( persistenceUnitRootUrl );
			bindPersistenceUnit( jaxbPersistenceUnit, persistenceUnitDescriptor );

			// per JPA spec, any settings passed in to PersistenceProvider bootstrap methods should override
			// values found in persistence.xml
			applyIntegrationOverrides( integration, defaultTransactionType, persistenceUnitDescriptor );

			persistenceUnits.put( persistenceUnitDescriptor.getName(), persistenceUnitDescriptor );
		}
	}

	private void bindPersistenceUnit(
			JaxbPersistenceUnitImpl jaxbPersistenceUnit,
			ParsedPersistenceXmlDescriptor persistenceUnitDescriptor) {
		final String name = jaxbPersistenceUnit.getName();
		if ( StringHelper.isNotEmpty( name ) ) {
			log.tracef( "Persistence unit name from persistence.xml : %s", name );
			persistenceUnitDescriptor.setName( name );
		}

		setTransactionType( jaxbPersistenceUnit, persistenceUnitDescriptor );

		persistenceUnitDescriptor.setProviderClassName( jaxbPersistenceUnit.getProvider() );
		persistenceUnitDescriptor.setNonJtaDataSource( jaxbPersistenceUnit.getNonJtaDataSource() );
		persistenceUnitDescriptor.setJtaDataSource( jaxbPersistenceUnit.getJtaDataSource() );
		persistenceUnitDescriptor.setSharedCacheMode( jaxbPersistenceUnit.getSharedCacheMode() );
		persistenceUnitDescriptor.setValidationMode( jaxbPersistenceUnit.getValidationMode() );
		persistenceUnitDescriptor.setExcludeUnlistedClasses( handleBoolean( jaxbPersistenceUnit.isExcludeUnlistedClasses() ) );
		persistenceUnitDescriptor.addClasses( jaxbPersistenceUnit.getClasses() );
		persistenceUnitDescriptor.addMappingFiles( jaxbPersistenceUnit.getMappingFiles() );
		persistenceUnitDescriptor.addJarFileUrls( jaxbPersistenceUnit.getJarFiles() );

		final JaxbPropertiesImpl propertyContainer = jaxbPersistenceUnit.getPropertyContainer();
		if ( propertyContainer != null ) {
			for ( JaxbPropertyImpl property : propertyContainer.getProperties() ) {
				persistenceUnitDescriptor.getProperties()
						.put( property.getName(), property.getValue() );
			}
		}
	}

	@SuppressWarnings("removal")
	private static void setTransactionType(
			JaxbPersistenceUnitImpl jaxbPersistenceUnit,
			ParsedPersistenceXmlDescriptor persistenceUnitDescriptor) {
		final jakarta.persistence.spi.PersistenceUnitTransactionType transactionType =
				jaxbPersistenceUnit.getTransactionType();
		if ( transactionType != null ) {
			persistenceUnitDescriptor.setTransactionType(
					PersistenceUnitTransactionTypeHelper.toNewForm( transactionType ) );
		}
	}

	private boolean handleBoolean(Boolean incoming) {
		if ( incoming != null ) {
			return incoming;
		}
		return false;
	}

	@SuppressWarnings("deprecation")
	private void applyIntegrationOverrides(Map<?,?> integration, PersistenceUnitTransactionType defaultTransactionType,
			ParsedPersistenceXmlDescriptor persistenceUnitDescriptor) {
		if ( integration.containsKey( AvailableSettings.JAKARTA_PERSISTENCE_PROVIDER ) ) {
			persistenceUnitDescriptor.setProviderClassName( (String)
					integration.get( AvailableSettings.JAKARTA_PERSISTENCE_PROVIDER ) );
		}
		else if ( integration.containsKey( AvailableSettings.JPA_PERSISTENCE_PROVIDER ) ) {
			DeprecationLogger.DEPRECATION_LOGGER.deprecatedSetting(
					AvailableSettings.JPA_PERSISTENCE_PROVIDER,
					AvailableSettings.JAKARTA_PERSISTENCE_PROVIDER
			);
			persistenceUnitDescriptor.setProviderClassName( (String)
					integration.get( AvailableSettings.JPA_PERSISTENCE_PROVIDER ) );
		}

		if ( integration.containsKey( AvailableSettings.JPA_TRANSACTION_TYPE ) ) {
			DeprecationLogger.DEPRECATION_LOGGER.deprecatedSetting(
					AvailableSettings.JPA_TRANSACTION_TYPE,
					AvailableSettings.JAKARTA_TRANSACTION_TYPE
			);
			String transactionType = (String) integration.get( AvailableSettings.JPA_TRANSACTION_TYPE );
			persistenceUnitDescriptor.setTransactionType( parseTransactionType( transactionType ) );
		}
		else if ( integration.containsKey( AvailableSettings.JAKARTA_TRANSACTION_TYPE ) ) {
			String transactionType = (String) integration.get( AvailableSettings.JAKARTA_TRANSACTION_TYPE );
			persistenceUnitDescriptor.setTransactionType( parseTransactionType( transactionType ) );
		}

		if ( integration.containsKey( AvailableSettings.JPA_JTA_DATASOURCE ) ) {
			DeprecationLogger.DEPRECATION_LOGGER.deprecatedSetting(
					AvailableSettings.JPA_JTA_DATASOURCE,
					AvailableSettings.JAKARTA_JTA_DATASOURCE
			);
			persistenceUnitDescriptor.setJtaDataSource( integration.get( AvailableSettings.JPA_JTA_DATASOURCE ) );
		}
		else if ( integration.containsKey( AvailableSettings.JAKARTA_JTA_DATASOURCE ) ) {
			persistenceUnitDescriptor.setJtaDataSource( integration.get( AvailableSettings.JAKARTA_JTA_DATASOURCE ) );
		}

		if ( integration.containsKey( AvailableSettings.JPA_NON_JTA_DATASOURCE ) ) {
			DeprecationLogger.DEPRECATION_LOGGER.deprecatedSetting(
					AvailableSettings.JPA_NON_JTA_DATASOURCE,
					AvailableSettings.JAKARTA_NON_JTA_DATASOURCE
			);
			persistenceUnitDescriptor.setNonJtaDataSource( integration.get( AvailableSettings.JPA_NON_JTA_DATASOURCE ) );
		}
		else if ( integration.containsKey( AvailableSettings.JAKARTA_NON_JTA_DATASOURCE ) ) {
			persistenceUnitDescriptor.setNonJtaDataSource( integration.get( AvailableSettings.JAKARTA_NON_JTA_DATASOURCE ) );
		}

		applyTransactionTypeOverride( persistenceUnitDescriptor, defaultTransactionType );

		final Properties properties = persistenceUnitDescriptor.getProperties();
		ConfigurationHelper.overrideProperties( properties, integration );
	}

	private void applyTransactionTypeOverride(ParsedPersistenceXmlDescriptor persistenceUnitDescriptor,
			PersistenceUnitTransactionType defaultTransactionType) {
		// if transaction type is set already, use that value
		if ( persistenceUnitDescriptor.getPersistenceUnitTransactionType() == null ) {
			// else
			//		if JTA DS
			//			use JTA
			//		else if NOT JTA DS
			//			use RESOURCE_LOCAL
			//		else
			//			use defaultTransactionType
			if ( persistenceUnitDescriptor.getJtaDataSource() != null ) {
				persistenceUnitDescriptor.setTransactionType( JTA );
			}
			else if ( persistenceUnitDescriptor.getNonJtaDataSource() != null ) {
				persistenceUnitDescriptor.setTransactionType( RESOURCE_LOCAL );
			}
			else {
				persistenceUnitDescriptor.setTransactionType( defaultTransactionType );
			}
		}
	}

	private static PersistenceUnitTransactionType parseTransactionType(String value) {
		if ( isEmpty( value ) ) {
			return null;
		}
		else if ( JTA.name().equalsIgnoreCase( value ) ) {
			return JTA;
		}
		else if ( RESOURCE_LOCAL.name().equalsIgnoreCase( value ) ) {
			return RESOURCE_LOCAL;
		}
		else {
			throw new PersistenceException( "Unknown persistence unit transaction type : " + value );
		}
	}

	private JaxbPersistenceImpl loadUrlWithJaxb(URL xmlUrl) {
		final String resourceName = xmlUrl.toExternalForm();
		try {
			URLConnection conn = xmlUrl.openConnection();
			// avoid JAR locking on Windows and Tomcat
			conn.setUseCaches( false );

			try ( InputStream inputStream = conn.getInputStream() ) {
				final StreamSource inputSource = new StreamSource( inputStream );
				final ConfigurationBinder configurationBinder = new ConfigurationBinder( classLoaderService );
				final Binding<JaxbPersistenceImpl> binding =
						configurationBinder.bind( inputSource, new Origin( SourceType.URL, resourceName ) );
				return binding.getRoot();
			}
			catch (IOException e) {
				throw new PersistenceException( "Unable to obtain input stream from [" + resourceName + "]", e );
			}
		}
		catch (IOException e) {
			throw new PersistenceException( "Unable to access [" + resourceName + "]", e );
		}
	}

}
