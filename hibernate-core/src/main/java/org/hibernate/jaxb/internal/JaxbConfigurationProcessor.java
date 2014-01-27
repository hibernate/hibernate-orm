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
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.events.XMLEvent;
import javax.xml.validation.Schema;

import org.jboss.logging.Logger;

import org.hibernate.internal.util.xml.MappingReader;
import org.hibernate.jaxb.spi.Origin;
import org.hibernate.jaxb.spi.cfg.JaxbHibernateConfiguration;
import org.hibernate.service.ServiceRegistry;

/**
 * @author Steve Ebersole
 */
public class JaxbConfigurationProcessor extends AbstractJaxbProcessor {
	private static final Logger log = Logger.getLogger( JaxbConfigurationProcessor.class );

	public static final String HIBERNATE_CONFIGURATION_URI = "http://www.hibernate.org/xsd/hibernate-configuration";


	public JaxbConfigurationProcessor(ServiceRegistry serviceRegistry) {
		this( serviceRegistry, true );
	}

	public JaxbConfigurationProcessor(ServiceRegistry serviceRegistry, boolean validateXml) {
		super(serviceRegistry, validateXml);
	}

	@Override
	protected XMLEventReader wrapReader(XMLEventReader xmlEventReader, XMLEvent event) {
		if ( !isNamespaced( event.asStartElement() ) ) {
			// if the elements are not namespaced, wrap the reader in a reader which will namespace them as pulled.
			log.debug( "cfg.xml document did not define namespaces; wrapping in custom event reader to introduce namespace information" );
			return new NamespaceAddingEventReader( xmlEventReader, HIBERNATE_CONFIGURATION_URI );
		}
		return super.wrapReader( xmlEventReader, event );
	}

	@Override
	protected JAXBContext getJaxbContext(XMLEvent event) throws JAXBException{
		return JAXBContext.newInstance( JaxbHibernateConfiguration.class );
	}

	@Override
	protected Schema getSchema(XMLEvent event, Origin origin) throws JAXBException {
		if ( schema == null ) {
			schema = MappingReader.resolveLocalSchema( "org/hibernate/hibernate-configuration-4.0.xsd" );
		}
		return schema;
	}

	private Schema schema;





}
