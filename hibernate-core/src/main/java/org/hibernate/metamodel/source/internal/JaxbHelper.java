/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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

package org.hibernate.metamodel.source.internal;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.jboss.logging.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import org.hibernate.HibernateException;
import org.hibernate.cfg.EJB3DTDEntityResolver;
import org.hibernate.internal.util.xml.ErrorLogger;
import org.hibernate.metamodel.source.MappingException;
import org.hibernate.metamodel.source.Origin;
import org.hibernate.metamodel.source.SourceType;
import org.hibernate.metamodel.source.XsdException;
import org.hibernate.metamodel.source.annotation.xml.EntityMappings;
import org.hibernate.metamodel.source.hbm.xml.mapping.HibernateMapping;
import org.hibernate.service.classloading.spi.ClassLoaderService;

/**
 * @author Steve Ebersole
 */
class JaxbHelper {
	private static final Logger log = Logger.getLogger( JaxbHelper.class );

	public static final String ASSUMED_ORM_XSD_VERSION = "2.0";

	private final MetadataImpl metadata;

	JaxbHelper(MetadataImpl metadata) {
		this.metadata = metadata;
	}

	public JaxbRoot unmarshal(InputSource source, Origin origin) {
		ErrorLogger errorHandler = new ErrorLogger();

		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setValidating( false ); // we will validate separately
		try {
			DocumentBuilder builder = factory.newDocumentBuilder();
			builder.setEntityResolver( metadata.getEntityResolver() );
			builder.setErrorHandler( errorHandler );
			return unmarshal( builder.parse( source ), origin );
        }
		catch ( HibernateException e ) {
			throw e;
		}
		catch (Exception e) {
			throw new MappingException( "Unable to read DOM via JAXP", e, origin );
		}
	}

	@SuppressWarnings( {"unchecked"})
	public JaxbRoot unmarshal(Document document, Origin origin) {
		Element rootElement = document.getDocumentElement();
		if ( rootElement ==  null ) {
			throw new MappingException( "No root element found", origin );
		}

		final Schema validationSchema;
		final Class jaxbTarget;

		if ( "entity-mappings".equals( rootElement.getLocalName() ) ) {
			final String explicitVersion = rootElement.getAttribute( "version" );
			validationSchema = resolveSupportedOrmXsd( explicitVersion );
			jaxbTarget = EntityMappings.class;
		}
		else {
			validationSchema = hbmSchema();
			jaxbTarget = HibernateMapping.class;
		}

		try {
			validationSchema.newValidator().validate( new DOMSource( rootElement.getOwnerDocument() ) );
		}
		catch ( SAXException e ) {
			throw new MappingException( "Validation problem", e, origin );
		}
		catch ( IOException e ) {
			throw new MappingException( "Validation problem", e, origin );
		}

		final Object target;
		try {
			JAXBContext jaxbContext = JAXBContext.newInstance( jaxbTarget );
			Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
			target = unmarshaller.unmarshal( document );
		}
		catch (JAXBException e) {
			throw new MappingException( "Unable to perform unmarshalling", e, origin );
		}

		return new JaxbRoot( target, origin );
	}

	private Schema resolveSupportedOrmXsd(String explicitVersion) {
		final String xsdVersionString = explicitVersion == null ? ASSUMED_ORM_XSD_VERSION : explicitVersion;
		if ( "1.0".equals( xsdVersionString ) ) {
			return orm1Schema();
		}
		else if ( "2.0".equals( xsdVersionString ) ) {
			return orm1Schema();
		}
		throw new IllegalArgumentException( "Unsupported orm.xml XSD version encountered [" + xsdVersionString + "]" );
	}

	public static final String HBM_SCHEMA_NAME = "/org/hibernate/hibernate-mapping-4.0.xsd";
	public static final String ORM_1_SCHEMA_NAME = "org/hibernate/ejb/orm_1_0.xsd";
	public static final String ORM_2_SCHEMA_NAME = "org/hibernate/ejb/orm_2_0.xsd";

	private Schema hbmSchema;

	private Schema hbmSchema() {
		if ( hbmSchema == null ) {
			hbmSchema = resolveLocalSchema( HBM_SCHEMA_NAME );
		}
		return hbmSchema;
	}

	private Schema orm1Schema;

	private Schema orm1Schema() {
		if ( orm1Schema == null ) {
			orm1Schema = resolveLocalSchema( ORM_1_SCHEMA_NAME );
		}
		return orm1Schema;
	}

	private Schema orm2Schema;

	private Schema orm2Schema() {
		if ( orm2Schema == null ) {
			orm2Schema = resolveLocalSchema( ORM_2_SCHEMA_NAME );
		}
		return orm2Schema;
	}

	private Schema resolveLocalSchema(String schemaName) {
		return resolveLocalSchema( schemaName, XMLConstants.W3C_XML_SCHEMA_NS_URI );
	}

	private Schema resolveLocalSchema(String schemaName, String schemaLanguage) {
        URL url = metadata.getServiceRegistry().getService( ClassLoaderService.class ).locateResource( schemaName );
		if ( url == null ) {
			throw new XsdException( "Unable to locate schema [" + schemaName + "] via classpath", schemaName );
		}
		try {
			InputStream schemaStream = url.openStream();
			try {
				StreamSource source = new StreamSource(url.openStream());
				SchemaFactory schemaFactory = SchemaFactory.newInstance( schemaLanguage );
				return schemaFactory.newSchema(source);
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
					log.debugf( "Problem closing schema stream [%s]", e.toString() );
				}
			}
		}
		catch ( IOException e ) {
			throw new XsdException( "Stream error handling schema url [" + url.toExternalForm() + "]", schemaName );
		}
	}


}
