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
import java.util.List;
import java.util.Map;
import java.util.Properties;
import javax.persistence.PersistenceException;
import javax.persistence.spi.PersistenceUnitTransactionType;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.hibernate.boot.archive.internal.ArchiveHelper;
import org.hibernate.boot.registry.classloading.internal.ClassLoaderServiceImpl;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.xml.XsdException;
import org.hibernate.jpa.AvailableSettings;
import org.hibernate.jpa.internal.EntityManagerMessageLogger;
import org.hibernate.jpa.internal.util.ConfigurationHelper;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import static org.hibernate.jpa.internal.HEMLogging.messageLogger;

/**
 * Used by Hibernate to parse {@code persistence.xml} files in SE environments.
 *
 * @author Steve Ebersole
 */
public class PersistenceXmlParser {
	private static final EntityManagerMessageLogger LOG = messageLogger( PersistenceXmlParser.class );

	private final ClassLoaderService classLoaderService;
	private final PersistenceUnitTransactionType defaultTransactionType;

	public static List<ParsedPersistenceXmlDescriptor> locatePersistenceUnits(Map integration) {
		final PersistenceXmlParser parser = new PersistenceXmlParser(
				ClassLoaderServiceImpl.fromConfigSettings( integration ),
				PersistenceUnitTransactionType.RESOURCE_LOCAL
		);

		return parser.doResolve( integration );
	}


	public PersistenceXmlParser(ClassLoaderService classLoaderService, PersistenceUnitTransactionType defaultTransactionType) {
		this.classLoaderService = classLoaderService;
		this.defaultTransactionType = defaultTransactionType;
	}

	public List<ParsedPersistenceXmlDescriptor> doResolve(Map integration) {
		final List<ParsedPersistenceXmlDescriptor> persistenceUnits = new ArrayList<ParsedPersistenceXmlDescriptor>();

		final List<URL> xmlUrls = classLoaderService.locateResources( "META-INF/persistence.xml" );
		if ( xmlUrls.isEmpty() ) {
			LOG.unableToFindPersistenceXmlInClasspath();
		}
		else {
			for ( URL xmlUrl : xmlUrls ) {
				persistenceUnits.addAll( parsePersistenceXml( xmlUrl, integration ) );
			}
		}

		return persistenceUnits;
	}

	private List<ParsedPersistenceXmlDescriptor> parsePersistenceXml(URL xmlUrl, Map integration) {
		LOG.tracef( "Attempting to parse persistence.xml file : %s", xmlUrl.toExternalForm() );

		final Document doc = loadUrl( xmlUrl );
		final Element top = doc.getDocumentElement();

		final List<ParsedPersistenceXmlDescriptor> persistenceUnits = new ArrayList<ParsedPersistenceXmlDescriptor>();

		final NodeList children = top.getChildNodes();
		for ( int i = 0; i < children.getLength() ; i++ ) {
			if ( children.item( i ).getNodeType() == Node.ELEMENT_NODE ) {
				final Element element = (Element) children.item( i );
				final String tag = element.getTagName();
				if ( tag.equals( "persistence-unit" ) ) {
					final URL puRootUrl = ArchiveHelper.getJarURLFromURLEntry( xmlUrl, "/META-INF/persistence.xml" );
					ParsedPersistenceXmlDescriptor persistenceUnit = new ParsedPersistenceXmlDescriptor( puRootUrl );
					bindPersistenceUnit( persistenceUnit, element );

					// per JPA spec, any settings passed in to PersistenceProvider bootstrap methods should override
					// values found in persistence.xml
					if ( integration.containsKey( AvailableSettings.PROVIDER ) ) {
						persistenceUnit.setProviderClassName( (String) integration.get( AvailableSettings.PROVIDER ) );
					}
					if ( integration.containsKey( AvailableSettings.TRANSACTION_TYPE ) ) {
						String transactionType = (String) integration.get( AvailableSettings.TRANSACTION_TYPE );
						persistenceUnit.setTransactionType( parseTransactionType( transactionType ) );
					}
					if ( integration.containsKey( AvailableSettings.JTA_DATASOURCE ) ) {
						persistenceUnit.setJtaDataSource( integration.get( AvailableSettings.JTA_DATASOURCE ) );
					}
					if ( integration.containsKey( AvailableSettings.NON_JTA_DATASOURCE ) ) {
						persistenceUnit.setNonJtaDataSource( integration.get( AvailableSettings.NON_JTA_DATASOURCE ) );
					}

					decodeTransactionType( persistenceUnit );

					Properties properties = persistenceUnit.getProperties();
					ConfigurationHelper.overrideProperties( properties, integration );

					persistenceUnits.add( persistenceUnit );
				}
			}
		}
		return persistenceUnits;
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
							if ( StringHelper.isEmpty( propValue ) ) {
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
		StringBuilder result = new StringBuilder("");
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

	private Document loadUrl(URL xmlUrl) {
		final String resourceName = xmlUrl.toExternalForm();
		try {
			URLConnection conn = xmlUrl.openConnection();
			conn.setUseCaches( false ); //avoid JAR locking on Windows and Tomcat
			try {
				InputStream inputStream = conn.getInputStream();
				try {
					final InputSource inputSource = new InputSource( inputStream );
					try {
						DocumentBuilder documentBuilder = documentBuilderFactory().newDocumentBuilder();
						try {
							Document document = documentBuilder.parse( inputSource );
							validate( document );
							return document;
						}
						catch (SAXException e) {
							throw new PersistenceException( "Unexpected error parsing [" + resourceName + "]", e );
						}
						catch (IOException e) {
							throw new PersistenceException( "Unexpected error parsing [" + resourceName + "]", e );
						}
					}
					catch (ParserConfigurationException e) {
						throw new PersistenceException( "Unable to generate javax.xml.parsers.DocumentBuilder instance", e );
					}
				}
				finally {
					try {
						inputStream.close();
					}
					catch (Exception ignored) {
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

	private void validate(Document document) {
		// todo : add ability to disable validation...

		final Validator validator;
		final String version = document.getDocumentElement().getAttribute( "version" );
		if ( "2.1".equals( version ) ) {
			validator = v21Schema().newValidator();
		}
		else if ( "2.0".equals( version ) ) {
			validator = v2Schema().newValidator();
		}
		else if ( "1.0".equals(  version ) ) {
			validator = v1Schema().newValidator();
		}
		else {
			throw new PersistenceException( "Unrecognized persistence.xml version [" + version + "]" );
		}

		List<SAXException> errors = new ArrayList<SAXException>();
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

	private DocumentBuilderFactory documentBuilderFactory() {
		if ( documentBuilderFactory == null ) {
			documentBuilderFactory = buildDocumentBuilderFactory();
		}
		return documentBuilderFactory;
	}

	private DocumentBuilderFactory buildDocumentBuilderFactory() {
		DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
		documentBuilderFactory.setNamespaceAware( true );
		return documentBuilderFactory;
	}

	private Schema v21Schema;

	private Schema v21Schema() {
		if ( v21Schema == null ) {
			v21Schema = resolveLocalSchema( "org/hibernate/jpa/persistence_2_1.xsd" );
		}
		return v21Schema;
	}

	private Schema v2Schema;

	private Schema v2Schema() {
		if ( v2Schema == null ) {
			v2Schema = resolveLocalSchema( "org/hibernate/jpa/persistence_2_0.xsd" );
		}
		return v2Schema;
	}

	private Schema v1Schema;

	private Schema v1Schema() {
		if ( v1Schema == null ) {
			v1Schema = resolveLocalSchema( "org/hibernate/jpa/persistence_1_0.xsd" );
		}
		return v1Schema;
	}


	private Schema resolveLocalSchema(String schemaName) {
		// These XSD resources should be available on the Hibernate ClassLoader
		final URL url = classLoaderService.locateResource( schemaName );
		if ( url == null ) {
			throw new XsdException( "Unable to locate schema [" + schemaName + "] via classpath", schemaName );
		}
		try {
			InputStream schemaStream = url.openStream();
			try {
				StreamSource source = new StreamSource( url.openStream() );
				SchemaFactory schemaFactory = SchemaFactory.newInstance( XMLConstants.W3C_XML_SCHEMA_NS_URI );
				return schemaFactory.newSchema( source );
			}
			catch ( SAXException e ) {
				throw new XsdException( "Unable to load schema [" + schemaName + "]", e, schemaName );
			}
			catch ( IOException e ) {
				throw new XsdException( "Unable to load schema [" + schemaName + "]", e, schemaName );
			}
			finally {
				try {
					schemaStream.close();
				}
				catch ( IOException e ) {
					LOG.debugf( "Problem closing schema stream [%s]", e.toString() );
				}
			}
		}
		catch ( IOException e ) {
			throw new XsdException( "Stream error handling schema url [" + url.toExternalForm() + "]", schemaName );
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
			LOG.trace( extractInfo( warn ) );
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
