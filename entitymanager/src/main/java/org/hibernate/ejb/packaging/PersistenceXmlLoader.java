/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.ejb.packaging;

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
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.hibernate.ejb.AvailableSettings;
import org.hibernate.ejb.util.ConfigurationHelper;
import org.hibernate.util.StringHelper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXParseException;

/**
 * Handler for persistence.xml files.
 *
 * @author <a href="mailto:bill@jboss.org">Bill Burke</a>
 * @author Emmanuel Bernard
 */
public final class PersistenceXmlLoader {
	private static final Logger log = LoggerFactory.getLogger( PersistenceXmlLoader.class );

	private PersistenceXmlLoader() {
	}

	private static Document loadURL(URL configURL, EntityResolver resolver) throws Exception {
		/*
		 * try and parse the document:
		 *  - try and validate the document with persistence_2_0.xsd
		  * - if it fails because of the version attribute mismatch, try and validate the document with persistence_1_0.xsd
		 */
		InputStream is = null;
		if (configURL != null) {
			URLConnection conn = configURL.openConnection();
			conn.setUseCaches( false ); //avoid JAR locking on Windows and Tomcat
			is = conn.getInputStream();
		}
		if ( is == null ) {
			throw new IOException( "Failed to obtain InputStream while reading persistence.xml file: " + configURL );
		}

		DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
		docBuilderFactory.setNamespaceAware( true );
		final Schema v2Schema = SchemaFactory.newInstance( javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI )
				.newSchema( new StreamSource( getStreamFromClasspath( "persistence_2_0.xsd" ) ) );
		final Validator v2Validator = v2Schema.newValidator();
		final Schema v1Schema = SchemaFactory.newInstance( javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI )
				.newSchema( new StreamSource( getStreamFromClasspath( "persistence_1_0.xsd" ) ) );
		final Validator v1Validator = v1Schema.newValidator();

		InputSource source = new InputSource( is );
		DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
		docBuilder.setEntityResolver( resolver );
		List<SAXParseException> errors = new ArrayList<SAXParseException>();
		Document doc = null;

		//first sparse document and collect syntaxic errors
		try {
			doc = docBuilder.parse( source );
		}
		catch ( SAXParseException e ) {
			errors.add( e );
		}

		if (errors.size() == 0) {
			v2Validator.setErrorHandler( new ErrorLogger( errors ) );
			log.trace("Validate with persistence_2_0.xsd schema on file {}", configURL);
			v2Validator.validate( new DOMSource( doc ) );
			boolean isV1Schema = false;
			if ( errors.size() != 0 ) {
				//v2 fails, it could be because the file is v1.
				log.trace("Found error with persistence_2_0.xsd schema on file {}", configURL);
				SAXParseException exception = errors.get( 0 );
				final String errorMessage = exception.getMessage();
				//is it a validation error due to a v1 schema validated by a v2
				isV1Schema = errorMessage.contains("1.0")
						&& errorMessage.contains("2.0")
						&& errorMessage.contains("version");

			}
			if (isV1Schema) {
				log.trace("Validate with persistence_1_0.xsd schema on file {}", configURL);
				errors.clear();
				v1Validator.setErrorHandler( new ErrorLogger( errors ) );
				v1Validator.validate( new DOMSource( doc ) );
			}
		}
		if ( errors.size() != 0 ) {
			//report all errors in the exception
			StringBuilder errorMessage = new StringBuilder( );
			for (SAXParseException error : errors) {
				errorMessage.append("Error parsing XML (line")
							.append(error.getLineNumber())
							.append(" : column ")
							.append(error.getColumnNumber())
							.append("): ")
							.append(error.getMessage())
							.append("\n");
			}
			throw new PersistenceException( "Invalid persistence.xml.\n" + errorMessage.toString() );
		}
		return doc;
	}

	private static InputStream getStreamFromClasspath(String fileName) {
		String path = "org/hibernate/ejb/" + fileName;
		InputStream dtdStream = PersistenceXmlLoader.class.getClassLoader().getResourceAsStream( path );
		return dtdStream;
	}

	/**
     * Method used by JBoss AS 4.0.5 for parsing
     */
	public static List<PersistenceMetadata> deploy(URL url, Map overrides, EntityResolver resolver) throws Exception {
        return deploy(url, overrides, resolver, PersistenceUnitTransactionType.JTA);
    }

    /**
     * Method used by JBoss EJB3 (4.2 and above) for parsing
     */
    public static List<PersistenceMetadata> deploy(URL url, Map overrides, EntityResolver resolver,
												   PersistenceUnitTransactionType defaultTransactionType) throws Exception {
		Document doc = loadURL( url, resolver );
		Element top = doc.getDocumentElement();
		//version is mandatory
		final String version = top.getAttribute( "version" );

		NodeList children = top.getChildNodes();
		ArrayList<PersistenceMetadata> units = new ArrayList<PersistenceMetadata>();
		for ( int i = 0; i < children.getLength() ; i++ ) {
			if ( children.item( i ).getNodeType() == Node.ELEMENT_NODE ) {
				Element element = (Element) children.item( i );
				String tag = element.getTagName();
				if ( tag.equals( "persistence-unit" ) ) {
					PersistenceMetadata metadata = parsePersistenceUnit( element );
					metadata.setVersion(version);
					//override properties of metadata if needed
					if ( overrides.containsKey( AvailableSettings.PROVIDER ) ) {
						String provider = (String) overrides.get( AvailableSettings.PROVIDER );
						metadata.setProvider( provider );
					}
					if ( overrides.containsKey( AvailableSettings.TRANSACTION_TYPE ) ) {
						String transactionType = (String) overrides.get( AvailableSettings.TRANSACTION_TYPE );
						metadata.setTransactionType( PersistenceXmlLoader.getTransactionType( transactionType ) );
					}
					if ( overrides.containsKey( AvailableSettings.JTA_DATASOURCE ) ) {
						String dataSource = (String) overrides.get( AvailableSettings.JTA_DATASOURCE );
						metadata.setJtaDatasource( dataSource );
					}
					if ( overrides.containsKey( AvailableSettings.NON_JTA_DATASOURCE ) ) {
						String dataSource = (String) overrides.get( AvailableSettings.NON_JTA_DATASOURCE );
						metadata.setNonJtaDatasource( dataSource );
					}
					/*
					 * if explicit => use it
					 * if JTA DS => JTA transaction
					 * if non JTA DA => RESOURCE_LOCAL transaction
					 * else default JavaSE => RESOURCE_LOCAL
					 */
					PersistenceUnitTransactionType transactionType = metadata.getTransactionType();
					Boolean isJTA = null;
					if ( StringHelper.isNotEmpty( metadata.getJtaDatasource() ) ) {
						isJTA = Boolean.TRUE;
					}
					else if ( StringHelper.isNotEmpty( metadata.getNonJtaDatasource() ) ) {
						isJTA = Boolean.FALSE;
					}
					if (transactionType == null) {
						if (isJTA == Boolean.TRUE) {
							transactionType = PersistenceUnitTransactionType.JTA;
						}
						else if (isJTA == Boolean.FALSE) {
							transactionType = PersistenceUnitTransactionType.RESOURCE_LOCAL;
						}
						else {
							transactionType = defaultTransactionType;
						}
					}
					metadata.setTransactionType( transactionType );
					Properties properties = metadata.getProps();
					ConfigurationHelper.overrideProperties( properties, overrides );
					units.add( metadata );
				}
			}
		}
		return units;
	}

	private static PersistenceMetadata parsePersistenceUnit(Element top)
			throws Exception {
		PersistenceMetadata metadata = new PersistenceMetadata();
		String puName = top.getAttribute( "name" );
		if ( StringHelper.isNotEmpty( puName ) ) {
			log.trace( "Persistent Unit name from persistence.xml: {}", puName );
			metadata.setName( puName );
		}
		NodeList children = top.getChildNodes();
		for ( int i = 0; i < children.getLength() ; i++ ) {
			if ( children.item( i ).getNodeType() == Node.ELEMENT_NODE ) {
				Element element = (Element) children.item( i );
				String tag = element.getTagName();
//				if ( tag.equals( "name" ) ) {
//					String puName = XmlHelper.getElementContent( element );
//					log.trace( "FOUND PU NAME: " + puName );
//					metadata.setName( puName );
//				}
//				else
				if ( tag.equals( "non-jta-data-source" ) ) {
					metadata.setNonJtaDatasource( XmlHelper.getElementContent( element ) );
				}
				else if ( tag.equals( "jta-data-source" ) ) {
					metadata.setJtaDatasource( XmlHelper.getElementContent( element ) );
				}
				else if ( tag.equals( "provider" ) ) {
					metadata.setProvider( XmlHelper.getElementContent( element ) );
				}
				else if ( tag.equals( "class" ) ) {
					metadata.getClasses().add( XmlHelper.getElementContent( element ) );
				}
				else if ( tag.equals( "mapping-file" ) ) {
					metadata.getMappingFiles().add( XmlHelper.getElementContent( element ) );
				}
				else if ( tag.equals( "jar-file" ) ) {
					metadata.getJarFiles().add( XmlHelper.getElementContent( element ) );
				}
				else if ( tag.equals( "exclude-unlisted-classes" ) ) {
					metadata.setExcludeUnlistedClasses( true );
				}
				else if ( tag.equals( "delimited-identifiers" ) ) {
					metadata.setUseQuotedIdentifiers( true );
				}
				else if ( tag.equals( "validation-mode" ) ) {
					metadata.setValidationMode( XmlHelper.getElementContent( element ) );
				}
				else if ( tag.equals( "shared-cache-mode" ) ) {
					metadata.setSharedCacheMode( XmlHelper.getElementContent( element ) );
				}
				else if ( tag.equals( "properties" ) ) {
					NodeList props = element.getChildNodes();
					for ( int j = 0; j < props.getLength() ; j++ ) {
						if ( props.item( j ).getNodeType() == Node.ELEMENT_NODE ) {
							Element propElement = (Element) props.item( j );
							if ( !"property".equals( propElement.getTagName() ) ) continue;
							String propName = propElement.getAttribute( "name" ).trim();
							String propValue = propElement.getAttribute( "value" ).trim();
							if ( StringHelper.isEmpty( propValue ) ) {
								//fall back to the natural (Hibernate) way of description
								propValue = XmlHelper.getElementContent( propElement, "" );
							}
							metadata.getProps().put( propName, propValue );
						}
					}

				}
			}
		}
		PersistenceUnitTransactionType transactionType = getTransactionType( top.getAttribute( "transaction-type" ) );
		if (transactionType != null) metadata.setTransactionType( transactionType );

		return metadata;
	}

	public static PersistenceUnitTransactionType getTransactionType(String elementContent) {
		if ( StringHelper.isEmpty( elementContent ) ) {
			return null; //PersistenceUnitTransactionType.JTA;
		}
		else if ( elementContent.equalsIgnoreCase( "JTA" ) ) {
			return PersistenceUnitTransactionType.JTA;
		}
		else if ( elementContent.equalsIgnoreCase( "RESOURCE_LOCAL" ) ) {
			return PersistenceUnitTransactionType.RESOURCE_LOCAL;
		}
		else {
			throw new PersistenceException( "Unknown TransactionType: " + elementContent );
		}
	}

	public static class ErrorLogger implements ErrorHandler {
		private List<SAXParseException> errors;

		ErrorLogger(List<SAXParseException> errors) {
			this.errors = errors;
		}

		public void error(SAXParseException error) {
			//what was this commented code about?
//			if ( resolver instanceof EJB3DTDEntityResolver ) {
//				if ( ( (EJB3DTDEntityResolver) resolver ).isResolved() == false ) return;
//			}
			errors.add( error );
		}

		public void fatalError(SAXParseException error) {
			errors.add( error );
		}

		public void warning(SAXParseException warn) {
		}
	}

}
