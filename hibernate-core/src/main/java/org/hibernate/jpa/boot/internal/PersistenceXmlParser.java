/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.boot.internal;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import javax.xml.transform.stream.StreamSource;

import org.hibernate.boot.archive.internal.ArchiveHelper;
import org.hibernate.boot.jaxb.Origin;
import org.hibernate.boot.jaxb.SourceType;
import org.hibernate.boot.jaxb.configuration.spi.JaxbPersistenceImpl;
import org.hibernate.boot.jaxb.configuration.spi.JaxbPersistenceImpl.JaxbPersistenceUnitImpl.JaxbPropertiesImpl;
import org.hibernate.boot.jaxb.configuration.spi.JaxbPersistenceImpl.JaxbPersistenceUnitImpl.JaxbPropertiesImpl.JaxbPropertyImpl;
import org.hibernate.boot.jaxb.internal.ConfigurationBinder;
import org.hibernate.boot.jaxb.spi.Binding;
import org.hibernate.boot.registry.classloading.internal.ClassLoaderServiceImpl;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.internal.EntityManagerMessageLogger;
import org.hibernate.internal.log.DeprecationLogger;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.jpa.internal.util.ConfigurationHelper;

import jakarta.persistence.PersistenceException;
import jakarta.persistence.spi.PersistenceUnitTransactionType;

import static org.hibernate.internal.HEMLogging.messageLogger;

/**
 * Used by Hibernate to parse {@code persistence.xml} files in SE environments.
 *
 * @author Steve Ebersole
 */
public class PersistenceXmlParser {

	private static final EntityManagerMessageLogger LOG = messageLogger( PersistenceXmlParser.class );

	/**
	 * Find all persistence-units from all accessible {@code META-INF/persistence.xml} resources
	 *
	 * @param integration The Map of integration settings
	 *
	 * @return List of descriptors for all discovered persistence-units.
	 */
	@SuppressWarnings({ "removal", "deprecation" })
	public static List<ParsedPersistenceXmlDescriptor> locatePersistenceUnits(Map<?,?> integration) {
		final PersistenceXmlParser parser = new PersistenceXmlParser(
				ClassLoaderServiceImpl.fromConfigSettings( integration ),
				PersistenceUnitTransactionType.RESOURCE_LOCAL
		);
		parser.doResolve( integration );
		return new ArrayList<>( parser.persistenceUnits.values() );
	}

	/**
	 * Parse a specific {@code persistence.xml} with the assumption that it defines a single
	 * persistence-unit.
	 *
	 * @param persistenceXmlUrl The {@code persistence.xml} URL
	 *
	 * @return The single persistence-unit descriptor
	 */
	public static ParsedPersistenceXmlDescriptor locateIndividualPersistenceUnit(URL persistenceXmlUrl) {
		return locateIndividualPersistenceUnit( persistenceXmlUrl, Collections.emptyMap() );
	}

	/**
	 * Parse a specific {@code persistence.xml} with the assumption that it defines a single
	 * persistence-unit.
	 *
	 * @param persistenceXmlUrl The {@code persistence.xml} URL
	 * @param integration The Map of integration settings
	 *
	 * @return The single persistence-unit descriptor
	 */
	@SuppressWarnings("removal")
	public static ParsedPersistenceXmlDescriptor locateIndividualPersistenceUnit(URL persistenceXmlUrl, Map<?,?> integration) {
		return locateIndividualPersistenceUnit( persistenceXmlUrl, PersistenceUnitTransactionType.RESOURCE_LOCAL, integration );
	}

	/**
	 * Parse a specific {@code persistence.xml} with the assumption that it defines a single
	 * persistence-unit.
	 *
	 * @param persistenceXmlUrl The {@code persistence.xml} URL
	 * @param transactionType The specific PersistenceUnitTransactionType to incorporate into the persistence-unit descriptor
	 * @param integration The Map of integration settings
	 *
	 * @return The single persistence-unit descriptor
	 */
	@SuppressWarnings({ "removal", "deprecation" })
	public static ParsedPersistenceXmlDescriptor locateIndividualPersistenceUnit(
			URL persistenceXmlUrl,
			PersistenceUnitTransactionType transactionType,
			Map<?,?> integration) {
		final PersistenceXmlParser parser = new PersistenceXmlParser(
				ClassLoaderServiceImpl.fromConfigSettings( integration ),
				transactionType
		);

		parser.parsePersistenceXml( persistenceXmlUrl, integration );

		assert parser.persistenceUnits.size() == 1;

		return parser.persistenceUnits.values().iterator().next();
	}

	/**
	 * Parse a specific {@code persistence.xml} and return the descriptor for the persistence-unit with matching name
	 *
	 * @param persistenceXmlUrl The {@code persistence.xml} URL
	 * @param name The PU name to match
	 *
	 * @return The matching persistence-unit descriptor
	 */
	public static ParsedPersistenceXmlDescriptor locateNamedPersistenceUnit(URL persistenceXmlUrl, String name) {
		return locateNamedPersistenceUnit( persistenceXmlUrl, name, Collections.emptyMap() );
	}

	/**
	 * Parse a specific {@code persistence.xml} and return the descriptor for the persistence-unit with matching name
	 *
	 * @param persistenceXmlUrl The {@code persistence.xml} URL
	 * @param name The PU name to match
	 * @param integration The Map of integration settings
	 *
	 * @return The matching persistence-unit descriptor
	 */
	@SuppressWarnings("removal")
	public static ParsedPersistenceXmlDescriptor locateNamedPersistenceUnit(URL persistenceXmlUrl, String name, Map<?,?> integration) {
		return locateNamedPersistenceUnit( persistenceXmlUrl, name, PersistenceUnitTransactionType.RESOURCE_LOCAL, integration );
	}

	/**
	 * Parse a specific {@code persistence.xml} and return the descriptor for the persistence-unit with matching name
	 *
	 * @param persistenceXmlUrl The {@code persistence.xml} URL
	 * @param name The PU name to match
	 * @param transactionType The specific PersistenceUnitTransactionType to incorporate into the persistence-unit descriptor
	 * @param integration The Map of integration settings
	 *
	 * @return The matching persistence-unit descriptor
	 */
	@SuppressWarnings({ "removal", "deprecation" })
	public static ParsedPersistenceXmlDescriptor locateNamedPersistenceUnit(
			URL persistenceXmlUrl,
			String name,
			PersistenceUnitTransactionType transactionType,
			Map<?,?> integration) {
		assert StringHelper.isNotEmpty( name );

		final PersistenceXmlParser parser = new PersistenceXmlParser(
				ClassLoaderServiceImpl.fromConfigSettings( integration ),
				transactionType
		);

		parser.parsePersistenceXml( persistenceXmlUrl, integration );
		assert parser.persistenceUnits.containsKey( name );

		return parser.persistenceUnits.get( name );
	}

	/**
	 * Intended only for use by Hibernate tests!
	 * <p>
	 * Parses a specific persistence.xml file...
	 */
	@SuppressWarnings("removal")
	public static Map<String, ParsedPersistenceXmlDescriptor> parse(
			URL persistenceXmlUrl,
			PersistenceUnitTransactionType transactionType) {
		return parse( persistenceXmlUrl, transactionType, Collections.emptyMap() );
	}

	/**
	 * Generic method to parse a specified {@code persistence.xml} and return a Map of descriptors
	 * for all discovered persistence-units keyed by the PU name.
	 *
	 * @param persistenceXmlUrl The URL of the {@code persistence.xml} to parse
	 * @param transactionType The specific PersistenceUnitTransactionType to incorporate into the persistence-unit descriptor
	 * @param integration The Map of integration settings
	 *
	 * @return Map of persistence-unit descriptors keyed by the PU name
	 */
	@SuppressWarnings({ "removal", "deprecation" })
	public static Map<String, ParsedPersistenceXmlDescriptor> parse(
			URL persistenceXmlUrl,
			PersistenceUnitTransactionType transactionType,
			Map<?,?> integration) {
		PersistenceXmlParser parser = new PersistenceXmlParser(
				ClassLoaderServiceImpl.fromConfigSettings( integration ),
				transactionType
		);

		parser.parsePersistenceXml( persistenceXmlUrl, integration );
		return parser.persistenceUnits;
	}

	private final ClassLoaderService classLoaderService;
	private final PersistenceUnitTransactionType defaultTransactionType;
	private final Map<String, ParsedPersistenceXmlDescriptor> persistenceUnits;

	@SuppressWarnings("removal")
	protected PersistenceXmlParser(ClassLoaderService classLoaderService, PersistenceUnitTransactionType defaultTransactionType) {
		this.classLoaderService = classLoaderService;
		this.defaultTransactionType = defaultTransactionType;
		this.persistenceUnits = new ConcurrentHashMap<>();
	}

	protected List<ParsedPersistenceXmlDescriptor> getResolvedPersistenceUnits() {
		return new ArrayList<>(persistenceUnits.values());
	}

	private void doResolve(Map<?,?> integration) {
		final List<URL> xmlUrls = classLoaderService.locateResources( "META-INF/persistence.xml" );
		if ( xmlUrls.isEmpty() ) {
			LOG.unableToFindPersistenceXmlInClasspath();
		}
		else {
			parsePersistenceXml( xmlUrls, integration );
		}
	}

	private void parsePersistenceXml(List<URL> xmlUrls, Map<?,?> integration) {
		for ( URL xmlUrl : xmlUrls ) {
			parsePersistenceXml( xmlUrl, integration );
		}
	}

	protected void parsePersistenceXml(URL xmlUrl, Map<?,?> integration) {
		if ( LOG.isTraceEnabled() ) {
			LOG.tracef( "Attempting to parse persistence.xml file : %s", xmlUrl.toExternalForm() );
		}

		final URL persistenceUnitRootUrl = ArchiveHelper.getJarURLFromURLEntry( xmlUrl, "/META-INF/persistence.xml" );

		final JaxbPersistenceImpl jaxbPersistence = loadUrlWithJaxb( xmlUrl );
		final List<JaxbPersistenceImpl.JaxbPersistenceUnitImpl> jaxbPersistenceUnits = jaxbPersistence.getPersistenceUnit();

		for ( int i = 0; i < jaxbPersistenceUnits.size(); i++ ) {
			final JaxbPersistenceImpl.JaxbPersistenceUnitImpl jaxbPersistenceUnit = jaxbPersistenceUnits.get( i );

			if ( persistenceUnits.containsKey( jaxbPersistenceUnit.getName() ) ) {
				LOG.duplicatedPersistenceUnitName( jaxbPersistenceUnit.getName() );
				continue;
			}

			final ParsedPersistenceXmlDescriptor persistenceUnitDescriptor = new ParsedPersistenceXmlDescriptor(
					persistenceUnitRootUrl );
			bindPersistenceUnit( jaxbPersistenceUnit, persistenceUnitDescriptor );

			// per JPA spec, any settings passed in to PersistenceProvider bootstrap methods should override
			// values found in persistence.xml
			applyIntegrationOverrides( integration, persistenceUnitDescriptor );

			persistenceUnits.put( persistenceUnitDescriptor.getName(), persistenceUnitDescriptor );
		}
	}

	private void bindPersistenceUnit(
			JaxbPersistenceImpl.JaxbPersistenceUnitImpl jaxbPersistenceUnit,
			ParsedPersistenceXmlDescriptor persistenceUnitDescriptor) {
		final String name = jaxbPersistenceUnit.getName();
		if ( StringHelper.isNotEmpty( name ) ) {
			LOG.tracef( "Persistence unit name from persistence.xml : %s", name );
			persistenceUnitDescriptor.setName( name );
		}

		//noinspection removal
		final PersistenceUnitTransactionType transactionType = jaxbPersistenceUnit.getTransactionType();
		if ( transactionType != null ) {
			persistenceUnitDescriptor.setTransactionType( transactionType );
		}

		persistenceUnitDescriptor.setProviderClassName( jaxbPersistenceUnit.getProvider() );
		persistenceUnitDescriptor.setNonJtaDataSource( jaxbPersistenceUnit.getNonJtaDataSource() );
		persistenceUnitDescriptor.setJtaDataSource( jaxbPersistenceUnit.getJtaDataSource() );
		persistenceUnitDescriptor.setSharedCacheMode( jaxbPersistenceUnit.getSharedCacheMode() );
		persistenceUnitDescriptor.setValidationMode( jaxbPersistenceUnit.getValidationMode() );
		persistenceUnitDescriptor.setExcludeUnlistedClasses( handleBoolean( jaxbPersistenceUnit.isExcludeUnlistedClasses(), false ) );
		persistenceUnitDescriptor.addClasses( jaxbPersistenceUnit.getClasses() );
		persistenceUnitDescriptor.addMappingFiles( jaxbPersistenceUnit.getMappingFiles() );
		persistenceUnitDescriptor.addJarFileUrls( jaxbPersistenceUnit.getJarFiles() );

		final JaxbPropertiesImpl propertyContainer = jaxbPersistenceUnit.getPropertyContainer();
		if ( propertyContainer != null ) {
			for ( JaxbPropertyImpl property : propertyContainer.getProperties() ) {
				persistenceUnitDescriptor.getProperties().put(
						property.getName(),
						property.getValue()
				);
			}
		}
	}

	private boolean handleBoolean(Boolean incoming, boolean fallback) {
		if ( incoming != null ) {
			return incoming;
		}
		return fallback;
	}

	@SuppressWarnings("deprecation")
	private void applyIntegrationOverrides(Map<?,?> integration, ParsedPersistenceXmlDescriptor persistenceUnitDescriptor) {
		if ( integration.containsKey( AvailableSettings.JAKARTA_PERSISTENCE_PROVIDER ) ) {
			persistenceUnitDescriptor.setProviderClassName( (String) integration.get( AvailableSettings.JAKARTA_PERSISTENCE_PROVIDER ) );
		}
		else if ( integration.containsKey( AvailableSettings.JPA_PERSISTENCE_PROVIDER ) ) {
			DeprecationLogger.DEPRECATION_LOGGER.deprecatedSetting(
					AvailableSettings.JPA_PERSISTENCE_PROVIDER,
					AvailableSettings.JAKARTA_PERSISTENCE_PROVIDER
			);
			persistenceUnitDescriptor.setProviderClassName( (String) integration.get( AvailableSettings.JPA_PERSISTENCE_PROVIDER ) );
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

		applyTransactionTypeOverride( persistenceUnitDescriptor );

		final Properties properties = persistenceUnitDescriptor.getProperties();
		ConfigurationHelper.overrideProperties( properties, integration );
	}

	@SuppressWarnings("removal")
	private void applyTransactionTypeOverride(ParsedPersistenceXmlDescriptor persistenceUnitDescriptor) {
		// if transaction type is set already, use that value
		if ( persistenceUnitDescriptor.getTransactionType() != null ) {
			return;
		}

		// else
		//		if JTA DS
		//			use JTA
		//		else if NOT JTA DS
		//			use RESOURCE_LOCAL
		//		else
		//			use defaultTransactionType
		if ( persistenceUnitDescriptor.getJtaDataSource() != null ) {
			persistenceUnitDescriptor.setTransactionType( PersistenceUnitTransactionType.JTA );
		}
		else if ( persistenceUnitDescriptor.getNonJtaDataSource() != null ) {
			persistenceUnitDescriptor.setTransactionType( PersistenceUnitTransactionType.RESOURCE_LOCAL );
		}
		else {
			persistenceUnitDescriptor.setTransactionType( defaultTransactionType );
		}
	}

	@SuppressWarnings("removal")
	private static PersistenceUnitTransactionType parseTransactionType(String value) {
		if ( StringHelper.isEmpty( value ) ) {
			return null;
		}
		else if ( value.equalsIgnoreCase( "JTA" ) ) {
			return PersistenceUnitTransactionType.JTA;
		}
		else if ( value.equalsIgnoreCase( "RESOURCE_LOCAL" ) ) {
			return PersistenceUnitTransactionType.RESOURCE_LOCAL;
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

			try (InputStream inputStream = conn.getInputStream()) {
				final StreamSource inputSource = new StreamSource( inputStream );
				final ConfigurationBinder configurationBinder = new ConfigurationBinder( classLoaderService );
				final Binding<JaxbPersistenceImpl> binding = configurationBinder.bind(
						inputSource,
						new Origin( SourceType.URL, resourceName )
				);
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
