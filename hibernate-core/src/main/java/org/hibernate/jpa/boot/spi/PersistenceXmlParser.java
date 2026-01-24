/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.jpa.boot.spi;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.transform.stream.StreamSource;

import org.hibernate.boot.jaxb.Origin;
import org.hibernate.boot.jaxb.SourceType;
import org.hibernate.boot.jaxb.configuration.spi.JaxbPersistenceImpl;
import org.hibernate.boot.jaxb.configuration.spi.JaxbPersistenceImpl.JaxbPersistenceUnitImpl;
import org.hibernate.boot.jaxb.internal.ConfigurationBinder;
import org.hibernate.boot.registry.classloading.internal.ClassLoaderServiceImpl;
import org.hibernate.boot.registry.classloading.internal.TcclLookupPrecedence;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.jpa.boot.internal.ParsedPersistenceXmlDescriptor;

import jakarta.persistence.PersistenceException;
import jakarta.persistence.PersistenceUnitTransactionType;

import static jakarta.persistence.PersistenceUnitTransactionType.JTA;
import static jakarta.persistence.PersistenceUnitTransactionType.RESOURCE_LOCAL;
import static org.hibernate.boot.archive.internal.ArchiveHelper.getJarURLFromURLEntry;
import static org.hibernate.cfg.JdbcSettings.JAKARTA_JTA_DATASOURCE;
import static org.hibernate.cfg.JdbcSettings.JAKARTA_NON_JTA_DATASOURCE;
import static org.hibernate.cfg.JdbcSettings.JPA_JTA_DATASOURCE;
import static org.hibernate.cfg.JdbcSettings.JPA_NON_JTA_DATASOURCE;
import static org.hibernate.cfg.PersistenceSettings.JAKARTA_PERSISTENCE_PROVIDER;
import static org.hibernate.cfg.PersistenceSettings.JAKARTA_TRANSACTION_TYPE;
import static org.hibernate.cfg.PersistenceSettings.JPA_PERSISTENCE_PROVIDER;
import static org.hibernate.cfg.PersistenceSettings.JPA_TRANSACTION_TYPE;
import static org.hibernate.internal.log.DeprecationLogger.DEPRECATION_LOGGER;
import static org.hibernate.internal.util.StringHelper.isNotEmpty;
import static org.hibernate.jpa.internal.JpaLogger.JPA_LOGGER;
import static org.hibernate.internal.util.StringHelper.isEmpty;
import static org.hibernate.jpa.internal.util.ConfigurationHelper.overrideProperties;

/**
 * Used by Hibernate to parse {@code persistence.xml} files in SE environments.
 *
 * @author Steve Ebersole
 */
public final class PersistenceXmlParser {

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

			final var classLoaders =
					(Collection<?>)
							integration.get( AvailableSettings.CLASSLOADERS );
			if ( classLoaders != null ) {
				for ( var classLoader : classLoaders ) {
					providedClassLoaders.add( (ClassLoader) classLoader );
				}
			}

			classLoaderService =
					new ClassLoaderServiceImpl( providedClassLoaders,
							TcclLookupPrecedence.from( integration, TcclLookupPrecedence.AFTER ) );
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
		if ( JPA_LOGGER.isTraceEnabled() ) {
			JPA_LOGGER.attemptingToParsePersistenceXml( xmlUrl.toExternalForm() );
		}

		final URL persistenceUnitRootUrl = getJarURLFromURLEntry( xmlUrl, "/META-INF/persistence.xml" );
		for ( var jaxbPersistenceUnit : loadUrlWithJaxb( xmlUrl ).getPersistenceUnit() ) {
			if ( persistenceUnits.containsKey( jaxbPersistenceUnit.getName() ) ) {
				JPA_LOGGER.duplicatedPersistenceUnitName( jaxbPersistenceUnit.getName() );
			}
			else {
				final var persistenceUnitDescriptor =
						new ParsedPersistenceXmlDescriptor( persistenceUnitRootUrl );
				bindPersistenceUnit( jaxbPersistenceUnit, persistenceUnitDescriptor );
				// per JPA spec, any settings passed in to PersistenceProvider
				// bootstrap methods should override values found in persistence.xml
				applyIntegrationOverrides( integration, defaultTransactionType, persistenceUnitDescriptor );
				persistenceUnits.put( persistenceUnitDescriptor.getName(), persistenceUnitDescriptor );
			}
		}
	}

	private void bindPersistenceUnit(
			JaxbPersistenceUnitImpl jaxbPersistenceUnit,
			ParsedPersistenceXmlDescriptor persistenceUnitDescriptor) {
		final String name = jaxbPersistenceUnit.getName();
		if ( isNotEmpty( name ) ) {
			JPA_LOGGER.persistenceUnitNameFromXml( name );
			persistenceUnitDescriptor.setName( name );
		}

		setTransactionType( jaxbPersistenceUnit, persistenceUnitDescriptor );

		persistenceUnitDescriptor.setProviderClassName( jaxbPersistenceUnit.getProvider() );
		persistenceUnitDescriptor.setDefaultToOneFetchType( jaxbPersistenceUnit.getDefaultToOneFetchType() );
		persistenceUnitDescriptor.setNonJtaDataSource( jaxbPersistenceUnit.getNonJtaDataSource() );
		persistenceUnitDescriptor.setJtaDataSource( jaxbPersistenceUnit.getJtaDataSource() );
		persistenceUnitDescriptor.setSharedCacheMode( jaxbPersistenceUnit.getSharedCacheMode() );
		persistenceUnitDescriptor.setValidationMode( jaxbPersistenceUnit.getValidationMode() );
		persistenceUnitDescriptor.setExcludeUnlistedClasses( handleBoolean( jaxbPersistenceUnit.isExcludeUnlistedClasses() ) );
		persistenceUnitDescriptor.addClasses( jaxbPersistenceUnit.getClasses() );
		persistenceUnitDescriptor.addMappingFiles( jaxbPersistenceUnit.getMappingFiles() );
		persistenceUnitDescriptor.addJarFileUrls( jaxbPersistenceUnit.getJarFiles() );

		final var propertyContainer = jaxbPersistenceUnit.getPropertyContainer();
		if ( propertyContainer != null ) {
			for ( var property : propertyContainer.getProperties() ) {
				persistenceUnitDescriptor.getProperties()
						.put( property.getName(), property.getValue() );
			}
		}
	}

	private static void setTransactionType(
			JaxbPersistenceUnitImpl jaxbPersistenceUnit,
			ParsedPersistenceXmlDescriptor persistenceUnitDescriptor) {
		final var transactionType = jaxbPersistenceUnit.getTransactionType();
		if ( transactionType != null ) {
			persistenceUnitDescriptor.setTransactionType( transactionType );
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
		if ( integration.containsKey( JAKARTA_PERSISTENCE_PROVIDER ) ) {
			persistenceUnitDescriptor.setProviderClassName( (String)
					integration.get( JAKARTA_PERSISTENCE_PROVIDER ) );
		}
		else if ( integration.containsKey( JPA_PERSISTENCE_PROVIDER ) ) {
			DEPRECATION_LOGGER.deprecatedSetting( JPA_PERSISTENCE_PROVIDER, JAKARTA_PERSISTENCE_PROVIDER );
			persistenceUnitDescriptor.setProviderClassName( (String)
					integration.get( JPA_PERSISTENCE_PROVIDER ) );
		}

		if ( integration.containsKey( JPA_TRANSACTION_TYPE ) ) {
			DEPRECATION_LOGGER.deprecatedSetting( JPA_TRANSACTION_TYPE, JAKARTA_TRANSACTION_TYPE );
			String transactionType = (String) integration.get( JPA_TRANSACTION_TYPE );
			persistenceUnitDescriptor.setTransactionType( parseTransactionType( transactionType ) );
		}
		else if ( integration.containsKey( JAKARTA_TRANSACTION_TYPE ) ) {
			String transactionType = (String) integration.get( JAKARTA_TRANSACTION_TYPE );
			persistenceUnitDescriptor.setTransactionType( parseTransactionType( transactionType ) );
		}

		if ( integration.containsKey( JPA_JTA_DATASOURCE ) ) {
			DEPRECATION_LOGGER.deprecatedSetting( JPA_JTA_DATASOURCE, JAKARTA_JTA_DATASOURCE );
			persistenceUnitDescriptor.setJtaDataSource( integration.get( JPA_JTA_DATASOURCE ) );
		}
		else if ( integration.containsKey( JAKARTA_JTA_DATASOURCE ) ) {
			persistenceUnitDescriptor.setJtaDataSource( integration.get( JAKARTA_JTA_DATASOURCE ) );
		}

		if ( integration.containsKey( JPA_NON_JTA_DATASOURCE ) ) {
			DEPRECATION_LOGGER.deprecatedSetting( JPA_NON_JTA_DATASOURCE, JAKARTA_NON_JTA_DATASOURCE );
			persistenceUnitDescriptor.setNonJtaDataSource( integration.get( JPA_NON_JTA_DATASOURCE ) );
		}
		else if ( integration.containsKey( JAKARTA_NON_JTA_DATASOURCE ) ) {
			persistenceUnitDescriptor.setNonJtaDataSource( integration.get( JAKARTA_NON_JTA_DATASOURCE ) );
		}

		applyTransactionTypeOverride( persistenceUnitDescriptor, defaultTransactionType );

		overrideProperties( persistenceUnitDescriptor.getProperties(), integration );
	}

	private void applyTransactionTypeOverride(
			ParsedPersistenceXmlDescriptor persistenceUnitDescriptor,
			PersistenceUnitTransactionType defaultTransactionType) {
		// if the transaction type is set already, use that value
		if ( persistenceUnitDescriptor.getPersistenceUnitTransactionType() == null ) {
			persistenceUnitDescriptor.setTransactionType(
					determineTransactionType( persistenceUnitDescriptor, defaultTransactionType ) );
		}
	}

	private static PersistenceUnitTransactionType determineTransactionType(
			ParsedPersistenceXmlDescriptor persistenceUnitDescriptor,
			PersistenceUnitTransactionType defaultTransactionType) {
		if ( persistenceUnitDescriptor.getJtaDataSource() != null ) {
			return JTA;
		}
		else if ( persistenceUnitDescriptor.getNonJtaDataSource() != null ) {
			return RESOURCE_LOCAL;
		}
		else {
			return defaultTransactionType;
		}
	}

	private static PersistenceUnitTransactionType parseTransactionType(String value) {
		if ( isEmpty( value ) ) {
			return null;
		}
		else {
			for ( var transactionType : PersistenceUnitTransactionType.values() ) {
				if ( transactionType.name().equalsIgnoreCase( value ) ) {
					return transactionType;
				}
			}
			throw new PersistenceException( "Unknown persistence unit transaction type : " + value );
		}
	}

	private JaxbPersistenceImpl loadUrlWithJaxb(URL xmlUrl) {
		final String resourceName = xmlUrl.toExternalForm();
		try {
			var connection = xmlUrl.openConnection();
			// avoid JAR locking on Windows and Tomcat
			connection.setUseCaches( false );
			try ( var inputStream = connection.getInputStream() ) {
				return new ConfigurationBinder( classLoaderService )
						.bind( new StreamSource( inputStream ),
								new Origin( SourceType.URL, resourceName ) )
						.getRoot();
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
