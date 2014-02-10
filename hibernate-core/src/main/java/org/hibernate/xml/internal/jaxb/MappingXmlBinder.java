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
package org.hibernate.xml.internal.jaxb;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.events.XMLEvent;
import javax.xml.transform.dom.DOMSource;
import javax.xml.validation.Schema;

import org.hibernate.internal.util.StringHelper;
import org.hibernate.jaxb.spi.hbm.JaxbHibernateMapping;
import org.hibernate.metamodel.source.internal.jaxb.JaxbEntityMappings;
import org.hibernate.metamodel.spi.source.MappingException;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.xml.internal.stax.LocalSchema;
import org.hibernate.xml.internal.stax.SupportedOrmXsdVersion;
import org.hibernate.xml.spi.BindResult;
import org.hibernate.xml.spi.Origin;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Loads {@code hbm.xml} and {@code orm.xml} files and processes them using StAX and JAXB.
 *
 * @author Steve Ebersole
 * @author Hardy Ferentschik
 *
 * @deprecated see {@link org.hibernate.xml.internal.jaxb.UnifiedMappingBinder}
 */
@Deprecated
public class MappingXmlBinder extends AbstractXmlBinder {
	public MappingXmlBinder(ServiceRegistry serviceRegistry) {
		this( serviceRegistry, true );
	}

	public MappingXmlBinder(ServiceRegistry serviceRegistry, boolean validateXml) {
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
			return LocalSchema.MAPPING.getSchema();
		}
		else {
			validationSchema = validateXml ? SupportedOrmXsdVersion.HBM_4_0.getSchema() : null;
		}
		return validationSchema;
	}

	@Override
	protected XMLEventReader wrapReader(XMLEventReader staxEventReader, XMLEvent event) {
		final String elementName = event.asStartElement().getName().getLocalPart();
		if ( "entity-mappings".equals( elementName ) ) {
			return new UnifiedMappingEventReader( staxEventReader );
		}
		else {
			return new HbmEventReader( staxEventReader );
		}
	}


	@SuppressWarnings( { "unchecked" })
	public BindResult bind(Document document, Origin origin) {
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
			validationSchema = validateXml ? SupportedOrmXsdVersion.HBM_4_0.getSchema() : null;
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

		return new BindResult( target, origin );
	}

	private Schema resolveSupportedOrmXsd(String explicitVersion, Origin origin) {
		if ( StringHelper.isEmpty( explicitVersion ) ) {
			return SupportedOrmXsdVersion.ORM_2_1.getSchema();
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
		SupportedOrmXsdVersion.parse( explicitVersion, origin );

		return SupportedOrmXsdVersion.ORM_2_1.getSchema();
		
	}
}
