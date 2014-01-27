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
package org.hibernate.jaxb.internal;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.XMLEvent;
import javax.xml.transform.dom.DOMSource;
import javax.xml.validation.Schema;

import org.jboss.logging.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.xml.LocalXmlResourceResolver;
import org.hibernate.internal.util.xml.MappingReader;
import org.hibernate.internal.util.xml.OriginImpl;
import org.hibernate.jaxb.spi.JaxbRoot;
import org.hibernate.jaxb.spi.Origin;
import org.hibernate.jaxb.spi.hbm.JaxbHibernateMapping;
import org.hibernate.jaxb.spi.orm.JaxbEntityMappings;
import org.hibernate.metamodel.spi.source.MappingException;
import org.hibernate.service.ServiceRegistry;

/**
 * Loads {@code hbm.xml} and {@code orm.xml} files and processes them using StAX and JAXB.
 *
 * @author Steve Ebersole
 * @author Hardy Ferentschik
 */
public class JaxbMappingProcessor extends AbstractJaxbProcessor{
	private static final Logger log = Logger.getLogger( JaxbMappingProcessor.class );
	public static final String HIBERNATE_MAPPING_URI = "http://www.hibernate.org/xsd/hibernate-mapping";

	public JaxbMappingProcessor(ServiceRegistry serviceRegistry) {
		this( serviceRegistry, true );
	}

	public JaxbMappingProcessor(ServiceRegistry serviceRegistry, boolean validateXml) {
		super(serviceRegistry, validateXml);
	}

	@Override
	protected JAXBContext getJaxbContext(XMLEvent event) throws JAXBException {
		final String elementName = event.asStartElement().getName().getLocalPart();
		final Class jaxbTarget;
		if ( "entity-mappings".equals( elementName ) ) {
			jaxbTarget = JaxbEntityMappings.class;
		}
		else {
			jaxbTarget = JaxbHibernateMapping.class;
		}
		return JAXBContext.newInstance( jaxbTarget );
	}

	@Override
	protected Schema getSchema(XMLEvent event, Origin origin) throws JAXBException {
		final String elementName = event.asStartElement().getName().getLocalPart();
		final Schema validationSchema;
		if ( "entity-mappings".equals( elementName ) ) {
			final Attribute attribute = event.asStartElement().getAttributeByName( ORM_VERSION_ATTRIBUTE_QNAME );
			final String explicitVersion = attribute == null ? null : attribute.getValue();
			validationSchema = validateXml ? resolveSupportedOrmXsd( explicitVersion, origin ) : null;
		}
		else {
			validationSchema = validateXml ? MappingReader.SupportedOrmXsdVersion.HBM_4_0.getSchema() : null;
		}
		return validationSchema;
	}

	@Override
	protected XMLEventReader wrapReader(XMLEventReader staxEventReader, XMLEvent event) {
		final String elementName = event.asStartElement().getName().getLocalPart();
		if ( "entity-mappings".equals( elementName ) ) {
			final Attribute attribute = event.asStartElement().getAttributeByName( ORM_VERSION_ATTRIBUTE_QNAME );
			final String explicitVersion = attribute == null ? null : attribute.getValue();
			if ( !"2.1".equals( explicitVersion ) ) {
				return new LegacyJPAEventReader(
						staxEventReader,
						LocalXmlResourceResolver.SECOND_JPA_ORM_NS
				);
			}
		}
		else {
			if ( !isNamespaced( event.asStartElement() ) ) {
				// if the elements are not namespaced, wrap the reader in a reader which will namespace them as pulled.
				log.debug( "HBM mapping document did not define namespaces; wrapping in custom event reader to introduce namespace information" );
				return  new NamespaceAddingEventReader( staxEventReader, HIBERNATE_MAPPING_URI );
			}
		}
		return super.wrapReader( staxEventReader, event );
	}

	private static final QName ORM_VERSION_ATTRIBUTE_QNAME = new QName( "version" );





	@SuppressWarnings( { "unchecked" })
	public JaxbRoot unmarshal(Document document, Origin origin) {
		Element rootElement = document.getDocumentElement();
		if ( rootElement == null ) {
			throw new MappingException( "No root element found", origin );
		}

		final Schema validationSchema;
		final Class jaxbTarget;

		if ( "entity-mappings".equals( rootElement.getNodeName() ) ) {
			final String explicitVersion = rootElement.getAttribute( "version" );
			validationSchema = validateXml ? resolveSupportedOrmXsd( explicitVersion, origin ) : null;
			jaxbTarget = JaxbEntityMappings.class;
		}
		else {
			validationSchema = validateXml ? MappingReader.SupportedOrmXsdVersion.HBM_4_0.getSchema() : null;
			jaxbTarget = JaxbHibernateMapping.class;
		}

		final Object target;
		try {
			JAXBContext jaxbContext = JAXBContext.newInstance( jaxbTarget );
			Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
			unmarshaller.setSchema( validationSchema );
			target = unmarshaller.unmarshal( new DOMSource( document ) );
		}
		catch ( JAXBException e ) {
			throw new MappingException( "Unable to perform unmarshalling", e, origin );
		}

		return new JaxbRoot( target, origin );
	}

	private Schema resolveSupportedOrmXsd(String explicitVersion, Origin origin) {

		if ( StringHelper.isEmpty( explicitVersion ) ) {
			return MappingReader.SupportedOrmXsdVersion.ORM_2_1.getSchema();
		}
		
		// Here we always use JPA 2.1 schema to do the validation, since the {@link LegacyJPAEventReader} already
		// transforms the legacy orm.xml to JPA 2.1 namespace and version.
		//
		// This may cause some problems, like a jpa 1.0 orm.xml having some element which is only available in the later
		// version. It is "invalid" but due to the fact we're using the latest schema to do the validation, then
		// it is "valid". Don't know if this will cause any problems, but let's do it for now.
		//
		// However, still check for the validity of the version by calling #parse.  If someone explicitly uses a value
		// that doesn't exist, we still need to throw the exception.
		@SuppressWarnings("unused")
		MappingReader.SupportedOrmXsdVersion version =
				MappingReader.SupportedOrmXsdVersion.parse(
						explicitVersion,
						new OriginImpl( origin.getType().name(), origin.getName() )
				);
		// return version.getSchema();
		return MappingReader.SupportedOrmXsdVersion.ORM_2_1.getSchema();
		
	}
}
