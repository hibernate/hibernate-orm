/*
 * Copyright (c) 2009, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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

import org.hibernate.cfg.EJB3DTDEntityResolver;
import org.hibernate.ejb.HibernatePersistence;
import org.hibernate.ejb.util.ConfigurationHelper;
import org.hibernate.util.StringHelper;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Persistence.xml handler
 *
 * @author <a href="mailto:bill@jboss.org">Bill Burke</a>
 * @author Emmanuel Bernard
 */
public final class PersistenceXmlLoader {
	private static final Logger log = LoggerFactory.getLogger( PersistenceXmlLoader.class );

	private PersistenceXmlLoader() {
	}

	private static Document loadURL(URL configURL, EntityResolver resolver) throws Exception {
		InputStream is = null;
		if (configURL != null) {
			URLConnection conn = configURL.openConnection();
			conn.setUseCaches( false ); //avoid JAR locking on Windows and Tomcat
			is = conn.getInputStream();
		}
		if ( is == null ) {
			throw new IOException( "Failed to obtain InputStream from url: " + configURL );
		}
		List errors = new ArrayList();
		DocumentBuilderFactory docBuilderFactory = null;
		docBuilderFactory = DocumentBuilderFactory.newInstance();
		docBuilderFactory.setValidating( true );
		docBuilderFactory.setNamespaceAware( true );
		try {
			//otherwise Xerces fails in validation
			docBuilderFactory.setAttribute( "http://apache.org/xml/features/validation/schema", true );
		}
		catch (IllegalArgumentException e) {
			docBuilderFactory.setValidating( false );
			docBuilderFactory.setNamespaceAware( false );
		}
		InputSource source = new InputSource( is );
		DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
		docBuilder.setEntityResolver( resolver );
		docBuilder.setErrorHandler( new ErrorLogger( "XML InputStream", errors, resolver ) );
		Document doc = docBuilder.parse( source );
		if ( errors.size() != 0 ) {
			throw new PersistenceException( "invalid persistence.xml", (Throwable) errors.get( 0 ) );
		}
		return doc;
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
		NodeList children = top.getChildNodes();
		ArrayList<PersistenceMetadata> units = new ArrayList<PersistenceMetadata>();
		for ( int i = 0; i < children.getLength() ; i++ ) {
			if ( children.item( i ).getNodeType() == Node.ELEMENT_NODE ) {
				Element element = (Element) children.item( i );
				String tag = element.getTagName();
				if ( tag.equals( "persistence-unit" ) ) {
					PersistenceMetadata metadata = parsePersistenceUnit( element );
					//override properties of metadata if needed
					if ( overrides.containsKey( HibernatePersistence.PROVIDER ) ) {
						String provider = (String) overrides.get( HibernatePersistence.PROVIDER );
						metadata.setProvider( provider );
					}
					if ( overrides.containsKey( HibernatePersistence.TRANSACTION_TYPE ) ) {
						String transactionType = (String) overrides.get( HibernatePersistence.TRANSACTION_TYPE );
						metadata.setTransactionType( PersistenceXmlLoader.getTransactionType( transactionType ) );
					}
					if ( overrides.containsKey( HibernatePersistence.JTA_DATASOURCE ) ) {
						String dataSource = (String) overrides.get( HibernatePersistence.JTA_DATASOURCE );
						metadata.setJtaDatasource( dataSource );
					}
					if ( overrides.containsKey( HibernatePersistence.NON_JTA_DATASOURCE ) ) {
						String dataSource = (String) overrides.get( HibernatePersistence.NON_JTA_DATASOURCE );
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
		private String file;
		private List errors;
		private EntityResolver resolver;

		ErrorLogger(String file, List errors, EntityResolver resolver) {
			this.file = file;
			this.errors = errors;
			this.resolver = resolver;
		}

		public void error(SAXParseException error) {
			if ( resolver instanceof EJB3DTDEntityResolver ) {
				if ( ( (EJB3DTDEntityResolver) resolver ).isResolved() == false ) return;
			}
			log.error( "Error parsing XML: {}({}) {}", new Object[] { file, error.getLineNumber(), error.getMessage() } );
			errors.add( error );
		}

		public void fatalError(SAXParseException error) {
			log.error( "Error parsing XML: {}({}) {}", new Object[] { file, error.getLineNumber(), error.getMessage() } );
			errors.add( error );
		}

		public void warning(SAXParseException warn) {
			log.warn( "Warning parsing XML: {}({}) {}", new Object[] { file, warn.getLineNumber(), warn.getMessage() } );
		}
	}

}
