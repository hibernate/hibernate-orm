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
import jakarta.persistence.PersistenceException;
import jakarta.persistence.spi.PersistenceUnitTransactionType;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.validation.Validator;

import org.hibernate.boot.archive.internal.ArchiveHelper;
import org.hibernate.boot.registry.classloading.internal.ClassLoaderServiceImpl;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.xsd.ConfigXsdSupport;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Environment;
import org.hibernate.cfg.MappingSettings;
import org.hibernate.internal.EntityManagerMessageLogger;
import org.hibernate.internal.log.DeprecationLogger;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.jpa.internal.util.ConfigurationHelper;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.SAXParseException;

import static org.hibernate.engine.config.spi.StandardConverters.BOOLEAN;
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
	public static List<ParsedPersistenceXmlDescriptor> locatePersistenceUnits(Map integration) {
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
	public static ParsedPersistenceXmlDescriptor locateIndividualPersistenceUnit(URL persistenceXmlUrl, Map integration) {
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
	public static ParsedPersistenceXmlDescriptor locateIndividualPersistenceUnit(
			URL persistenceXmlUrl,
			PersistenceUnitTransactionType transactionType,
			Map integration) {
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
	public static ParsedPersistenceXmlDescriptor locateNamedPersistenceUnit(URL persistenceXmlUrl, String name, Map integration) {
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
	public static ParsedPersistenceXmlDescriptor locateNamedPersistenceUnit(
			URL persistenceXmlUrl,
			String name,
			PersistenceUnitTransactionType transactionType,
			Map integration) {
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
	public static Map<String, ParsedPersistenceXmlDescriptor> parse(
			URL persistenceXmlUrl,
			PersistenceUnitTransactionType transactionType,
			Map integration) {
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

	protected PersistenceXmlParser(ClassLoaderService classLoaderService, PersistenceUnitTransactionType defaultTransactionType) {
		this.classLoaderService = classLoaderService;
		this.defaultTransactionType = defaultTransactionType;
		this.persistenceUnits = new ConcurrentHashMap<>();
	}

	protected List<ParsedPersistenceXmlDescriptor> getResolvedPersistenceUnits() {
		return new ArrayList<>(persistenceUnits.values());
	}

	private void doResolve(Map integration) {
		final List<URL> xmlUrls = classLoaderService.locateResources( "META-INF/persistence.xml" );
		if ( xmlUrls.isEmpty() ) {
			LOG.unableToFindPersistenceXmlInClasspath();
		}
		else {
			parsePersistenceXml( xmlUrls, integration );
		}
	}

	private void parsePersistenceXml(List<URL> xmlUrls, Map integration) {
		for ( URL xmlUrl : xmlUrls ) {
			parsePersistenceXml( xmlUrl, integration );
		}
	}

	protected void parsePersistenceXml(URL xmlUrl, Map integration) {
		if ( LOG.isTraceEnabled() ) {
			LOG.tracef( "Attempting to parse persistence.xml file : %s", xmlUrl.toExternalForm() );
		}

		final Document doc = loadUrl( xmlUrl, integration );
		final Element top = doc.getDocumentElement();

		final NodeList children = top.getChildNodes();
		for ( int i = 0; i < children.getLength() ; i++ ) {
			if ( children.item( i ).getNodeType() == Node.ELEMENT_NODE ) {
				final Element element = (Element) children.item( i );
				final String tag = element.getTagName();
				if ( tag.equals( "persistence-unit" ) ) {
					final URL puRootUrl = ArchiveHelper.getJarURLFromURLEntry( xmlUrl, "/META-INF/persistence.xml" );
					ParsedPersistenceXmlDescriptor persistenceUnit = new ParsedPersistenceXmlDescriptor( puRootUrl );
					bindPersistenceUnit( persistenceUnit, element );

					if ( persistenceUnits.containsKey( persistenceUnit.getName() ) ) {
						LOG.duplicatedPersistenceUnitName( persistenceUnit.getName() );
						continue;
					}

					// per JPA spec, any settings passed in to PersistenceProvider bootstrap methods should override
					// values found in persistence.xml
					if ( integration.containsKey( AvailableSettings.JAKARTA_PERSISTENCE_PROVIDER ) ) {
						persistenceUnit.setProviderClassName( (String) integration.get( AvailableSettings.JAKARTA_PERSISTENCE_PROVIDER ) );
					}
					else if ( integration.containsKey( AvailableSettings.JPA_PERSISTENCE_PROVIDER ) ) {
						DeprecationLogger.DEPRECATION_LOGGER.deprecatedSetting(
								AvailableSettings.JPA_PERSISTENCE_PROVIDER,
								AvailableSettings.JAKARTA_PERSISTENCE_PROVIDER
						);
						persistenceUnit.setProviderClassName( (String) integration.get( AvailableSettings.JPA_PERSISTENCE_PROVIDER ) );
					}

					if ( integration.containsKey( AvailableSettings.JPA_TRANSACTION_TYPE ) ) {
						DeprecationLogger.DEPRECATION_LOGGER.deprecatedSetting(
								AvailableSettings.JPA_TRANSACTION_TYPE,
								AvailableSettings.JAKARTA_TRANSACTION_TYPE
						);
						String transactionType = (String) integration.get( AvailableSettings.JPA_TRANSACTION_TYPE );
						persistenceUnit.setTransactionType( parseTransactionType( transactionType ) );
					}
					else if ( integration.containsKey( AvailableSettings.JAKARTA_TRANSACTION_TYPE ) ) {
						String transactionType = (String) integration.get( AvailableSettings.JAKARTA_TRANSACTION_TYPE );
						persistenceUnit.setTransactionType( parseTransactionType( transactionType ) );
					}

					if ( integration.containsKey( AvailableSettings.JPA_JTA_DATASOURCE ) ) {
						DeprecationLogger.DEPRECATION_LOGGER.deprecatedSetting(
								AvailableSettings.JPA_JTA_DATASOURCE,
								AvailableSettings.JAKARTA_JTA_DATASOURCE
						);
						persistenceUnit.setJtaDataSource( integration.get( AvailableSettings.JPA_JTA_DATASOURCE ) );
					}
					else if ( integration.containsKey( AvailableSettings.JAKARTA_JTA_DATASOURCE ) ) {
						persistenceUnit.setJtaDataSource( integration.get( AvailableSettings.JAKARTA_JTA_DATASOURCE ) );
					}

					if ( integration.containsKey( AvailableSettings.JPA_NON_JTA_DATASOURCE ) ) {
						DeprecationLogger.DEPRECATION_LOGGER.deprecatedSetting(
								AvailableSettings.JPA_NON_JTA_DATASOURCE,
								AvailableSettings.JAKARTA_NON_JTA_DATASOURCE
						);
						persistenceUnit.setNonJtaDataSource( integration.get( AvailableSettings.JPA_NON_JTA_DATASOURCE ) );
					}
					else if ( integration.containsKey( AvailableSettings.JAKARTA_NON_JTA_DATASOURCE ) ) {
						persistenceUnit.setNonJtaDataSource( integration.get( AvailableSettings.JAKARTA_NON_JTA_DATASOURCE ) );
					}

					decodeTransactionType( persistenceUnit );

					Properties properties = persistenceUnit.getProperties();
					ConfigurationHelper.overrideProperties( properties, integration );

					persistenceUnits.put( persistenceUnit.getName(), persistenceUnit );
				}
			}
		}
	}

	private void decodeTransactionType(ParsedPersistenceXmlDescriptor persistenceUnit) {
		// if transaction type is set already
		// 		use that value
		// else
		//		if JTA DS
		//			use JTA
		//		else if NOT JTA DS
		//			use RESOURCE_LOCAL
		//		else
		//			use defaultTransactionType
		if ( persistenceUnit.getTransactionType() != null ) {
			return;
		}

		if ( persistenceUnit.getJtaDataSource() != null ) {
			persistenceUnit.setTransactionType( PersistenceUnitTransactionType.JTA );
		}
		else if ( persistenceUnit.getNonJtaDataSource() != null ) {
			persistenceUnit.setTransactionType( PersistenceUnitTransactionType.RESOURCE_LOCAL );
		}
		else {
			persistenceUnit.setTransactionType( defaultTransactionType );
		}
	}

	private void bindPersistenceUnit(ParsedPersistenceXmlDescriptor persistenceUnit, Element persistenceUnitElement) {
		final String name = persistenceUnitElement.getAttribute( "name" );
		if ( StringHelper.isNotEmpty( name ) ) {
			LOG.tracef( "Persistence unit name from persistence.xml : %s", name );
			persistenceUnit.setName( name );
		}

		final PersistenceUnitTransactionType transactionType = parseTransactionType(
				persistenceUnitElement.getAttribute( "transaction-type" )
		);
		if ( transactionType != null ) {
			persistenceUnit.setTransactionType( transactionType );
		}


		NodeList children = persistenceUnitElement.getChildNodes();
		for ( int i = 0; i < children.getLength() ; i++ ) {
			if ( children.item( i ).getNodeType() == Node.ELEMENT_NODE ) {
				Element element = (Element) children.item( i );
				String tag = element.getTagName();
				if ( tag.equals( "non-jta-data-source" ) ) {
					persistenceUnit.setNonJtaDataSource( extractContent( element ) );
				}
				else if ( tag.equals( "jta-data-source" ) ) {
					persistenceUnit.setJtaDataSource( extractContent( element ) );
				}
				else if ( tag.equals( "provider" ) ) {
					persistenceUnit.setProviderClassName( extractContent( element ) );
				}
				else if ( tag.equals( "class" ) ) {
					persistenceUnit.addClasses( extractContent( element ) );
				}
				else if ( tag.equals( "mapping-file" ) ) {
					persistenceUnit.addMappingFiles( extractContent( element ) );
				}
				else if ( tag.equals( "jar-file" ) ) {
					persistenceUnit.addJarFileUrl( ArchiveHelper.getURLFromPath( extractContent( element ) ) );
				}
				else if ( tag.equals( "exclude-unlisted-classes" ) ) {
					persistenceUnit.setExcludeUnlistedClasses( extractBooleanContent(element, true) );
				}
				else if ( tag.equals( "delimited-identifiers" ) ) {
					persistenceUnit.setUseQuotedIdentifiers( true );
				}
				else if ( tag.equals( "validation-mode" ) ) {
					persistenceUnit.setValidationMode( extractContent( element ) );
				}
				else if ( tag.equals( "shared-cache-mode" ) ) {
					persistenceUnit.setSharedCacheMode( extractContent( element ) );
				}
				else if ( tag.equals( "properties" ) ) {
					NodeList props = element.getChildNodes();
					for ( int j = 0; j < props.getLength() ; j++ ) {
						if ( props.item( j ).getNodeType() == Node.ELEMENT_NODE ) {
							Element propElement = (Element) props.item( j );
							if ( !"property".equals( propElement.getTagName() ) ) {
								continue;
							}
							String propName = propElement.getAttribute( "name" ).trim();
							String propValue = propElement.getAttribute( "value" ).trim();
							if ( propValue.isEmpty() ) {
								//fall back to the natural (Hibernate) way of description
								propValue = extractContent( propElement, "" );
							}
							persistenceUnit.getProperties().put( propName, propValue );
						}
					}
				}
			}
		}
	}

	private static String extractContent(Element element) {
		return extractContent( element, null );
	}

	private static String extractContent(Element element, String defaultStr) {
		if ( element == null ) {
			return defaultStr;
		}

		NodeList children = element.getChildNodes();
		StringBuilder result = new StringBuilder();
		for ( int i = 0; i < children.getLength() ; i++ ) {
			if ( children.item( i ).getNodeType() == Node.TEXT_NODE ||
					children.item( i ).getNodeType() == Node.CDATA_SECTION_NODE ) {
				result.append( children.item( i ).getNodeValue() );
			}
		}
		return result.toString().trim();
	}

	private static boolean extractBooleanContent(Element element, boolean defaultBool) {
		String content = extractContent( element );
		if (content != null && content.length() > 0) {
			return Boolean.valueOf(content);
		}
		return defaultBool;
	}

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

	/**
	 * Settings that affect parsing of {@code persistence.xml} itself. Properties declared inside the
	 * file cannot apply to its own parsing, so only integration settings and
	 * {@code hibernate.properties}/system properties are consulted, in that order.
	 */
	private static Object getXmlParsingSetting(Map integration, String settingName) {
		if ( integration != null && integration.containsKey( settingName ) ) {
			return integration.get( settingName );
		}
		return Environment.getProperties().get( settingName );
	}

	private static boolean isDtdAndExternalEntitiesAllowed(Map integration) {
		final Object setting = getXmlParsingSetting( integration, MappingSettings.XML_ALLOW_DTD_AND_EXTERNAL_ENTITIES );
		return setting != null && BOOLEAN.convert( setting );
	}

	private Document loadUrl(URL xmlUrl, Map integration) {
		final String resourceName = xmlUrl.toExternalForm();
		final boolean allowDtdAndExternalEntities = isDtdAndExternalEntitiesAllowed( integration );
		try {
			URLConnection conn = xmlUrl.openConnection();
			conn.setUseCaches( false ); //avoid JAR locking on Windows and Tomcat
			try {
				try (InputStream inputStream = conn.getInputStream()) {
					final InputSource inputSource = new InputSource( inputStream );
					try {
						DocumentBuilder documentBuilder = documentBuilderFactory( allowDtdAndExternalEntities ).newDocumentBuilder();
						try {
							Document document = documentBuilder.parse( inputSource );
							validate( document );
							return document;
						}
						catch (SAXException | IOException e) {
							if ( !allowDtdAndExternalEntities && e.getMessage() != null && e.getMessage().contains( "DOCTYPE" ) ) {
								throw new PersistenceException(
										"The persistence.xml [" + resourceName + "] declares a DTD, but DTDs and external entities are disabled by default for security reasons;"
												+ " to re-enable them, set '" + MappingSettings.XML_ALLOW_DTD_AND_EXTERNAL_ENTITIES
												+ "' to 'true' at your own risk",
										e
								);
							}
							throw new PersistenceException( "Unexpected error parsing [" + resourceName + "]", e );
						}
					}
					catch (ParserConfigurationException e) {
						throw new PersistenceException(
								"Unable to generate javax.xml.parsers.DocumentBuilder instance",
								e
						);
					}
				}
			}
			catch (IOException e) {
				throw new PersistenceException( "Unable to obtain input stream from [" + resourceName + "]", e );
			}
		}
		catch (IOException e) {
			throw new PersistenceException( "Unable to access [" + resourceName + "]", e );
		}
	}

	/**
	 * Validate the document using the
	 */
	private void validate(Document document) {
		// todo : add ability to disable validation...

		final String version = document.getDocumentElement().getAttribute( "version" );
		final Validator validator = new ConfigXsdSupport().jpaXsd( version ).getSchema().newValidator();

		List<SAXException> errors = new ArrayList<>();
		try {
			validator.setProperty( XMLConstants.ACCESS_EXTERNAL_DTD, "" );
			validator.setProperty( XMLConstants.ACCESS_EXTERNAL_SCHEMA, "" );
		}
		catch (SAXNotRecognizedException | SAXNotSupportedException ignore) {
			// the Validator implementation does not support JAXP 1.5 access restrictions
		}
		validator.setErrorHandler( new ErrorHandlerImpl( errors ) );
		try {
			validator.validate( new DOMSource( document ) );
		}
		catch (SAXException e) {
			errors.add( e );
		}
		catch (IOException e) {
			throw new PersistenceException( "Unable to validate persistence.xml", e );
		}

		if ( errors.size() != 0 ) {
			//report all errors in the exception
			StringBuilder errorMessage = new StringBuilder( );
			for ( SAXException error : errors ) {
				errorMessage.append( extractInfo( error ) ).append( '\n' );
			}
			throw new PersistenceException( "Invalid persistence.xml.\n" + errorMessage.toString() );
		}
	}

	private DocumentBuilderFactory documentBuilderFactory;
	private boolean documentBuilderFactoryAllowsDtd;

	private DocumentBuilderFactory documentBuilderFactory(boolean allowDtdAndExternalEntities) {
		if ( documentBuilderFactory == null || documentBuilderFactoryAllowsDtd != allowDtdAndExternalEntities ) {
			documentBuilderFactory = buildDocumentBuilderFactory( allowDtdAndExternalEntities );
			documentBuilderFactoryAllowsDtd = allowDtdAndExternalEntities;
		}
		return documentBuilderFactory;
	}

	private static DocumentBuilderFactory buildDocumentBuilderFactory(boolean allowDtdAndExternalEntities) {
		final DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
		documentBuilderFactory.setNamespaceAware( true );
		if ( !allowDtdAndExternalEntities ) {
			setFeature( documentBuilderFactory, "http://apache.org/xml/features/disallow-doctype-decl", true );
			setFeature( documentBuilderFactory, "http://xml.org/sax/features/external-general-entities", false );
			setFeature( documentBuilderFactory, "http://xml.org/sax/features/external-parameter-entities", false );
			setFeature( documentBuilderFactory, "http://apache.org/xml/features/nonvalidating/load-external-dtd", false );
			documentBuilderFactory.setXIncludeAware( false );
			documentBuilderFactory.setExpandEntityReferences( false );
			try {
				documentBuilderFactory.setAttribute( XMLConstants.ACCESS_EXTERNAL_DTD, "" );
				documentBuilderFactory.setAttribute( XMLConstants.ACCESS_EXTERNAL_SCHEMA, "" );
			}
			catch (IllegalArgumentException ignore) {
				// the DocumentBuilderFactory implementation does not support JAXP 1.5 access restrictions
			}
		}
		return documentBuilderFactory;
	}

	private static void setFeature(DocumentBuilderFactory documentBuilderFactory, String feature, boolean value) {
		try {
			documentBuilderFactory.setFeature( feature, value );
		}
		catch (ParserConfigurationException ignore) {
			// the DocumentBuilderFactory implementation does not support this feature
		}
	}

	public static class ErrorHandlerImpl implements ErrorHandler {
		private List<SAXException> errors;

		ErrorHandlerImpl(List<SAXException> errors) {
			this.errors = errors;
		}

		public void error(SAXParseException error) {
			errors.add( error );
		}

		public void fatalError(SAXParseException error) {
			errors.add( error );
		}

		public void warning(SAXParseException warn) {
			if ( LOG.isTraceEnabled() ) {
				LOG.trace( extractInfo( warn ) );
			}
		}
	}

	private static String extractInfo(SAXException error) {
		if ( error instanceof SAXParseException ) {
			return "Error parsing XML [line : " + ( (SAXParseException) error ).getLineNumber()
					+ ", column : " + ( (SAXParseException) error ).getColumnNumber()
					+ "] : " + error.getMessage();
		}
		else {
			return "Error parsing XML : " + error.getMessage();
		}
	}
}
