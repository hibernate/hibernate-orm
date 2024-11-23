/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.jaxb.internal;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.events.StartElement;

import org.hibernate.boot.UnsupportedOrmXsdVersionException;
import org.hibernate.boot.jaxb.Origin;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmHibernateMapping;
import org.hibernate.boot.jaxb.internal.stax.HbmEventReader;
import org.hibernate.boot.jaxb.internal.stax.JpaOrmXmlEventReader;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityMappings;
import org.hibernate.boot.jaxb.spi.Binding;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.xsd.MappingXsdSupport;
import org.hibernate.internal.util.config.ConfigurationException;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public class MappingBinder extends AbstractBinder {
	private static final Logger log = Logger.getLogger( MappingBinder.class );

	private final XMLEventFactory xmlEventFactory = XMLEventFactory.newInstance();

	private JAXBContext hbmJaxbContext;
	private JAXBContext entityMappingsJaxbContext;

	public MappingBinder(ClassLoaderService classLoaderService, boolean validateXml) {
		super( classLoaderService, validateXml );
	}

	@Override
	protected Binding<?> doBind(
			XMLEventReader staxEventReader,
			StartElement rootElementStartEvent,
			Origin origin) {
		final String rootElementLocalName = rootElementStartEvent.getName().getLocalPart();
		if ( "hibernate-mapping".equals( rootElementLocalName ) ) {
			log.debugf( "Performing JAXB binding of hbm.xml document : %s", origin.toString() );

			XMLEventReader hbmReader = new HbmEventReader( staxEventReader, xmlEventFactory );
			JaxbHbmHibernateMapping hbmBindings = jaxb( hbmReader, MappingXsdSupport.INSTANCE.hbmXsd().getSchema(), hbmJaxbContext(), origin );
			return new Binding<>( hbmBindings, origin );
		}
		else {
			try {
				log.debugf( "Performing JAXB binding of orm.xml document : %s", origin.toString() );

				XMLEventReader reader = new JpaOrmXmlEventReader( staxEventReader, xmlEventFactory );
				JaxbEntityMappings bindingRoot = jaxb( reader, MappingXsdSupport.INSTANCE.latestJpaDescriptor().getSchema(), entityMappingsJaxbContext(), origin );
				return new Binding<>( bindingRoot, origin );
			}
			catch (JpaOrmXmlEventReader.BadVersionException e) {
				throw new UnsupportedOrmXsdVersionException( e.getRequestedVersion(), origin );
			}
		}
	}

	private JAXBContext hbmJaxbContext() {
		if ( hbmJaxbContext == null ) {
			try {
				hbmJaxbContext = JAXBContext.newInstance( JaxbHbmHibernateMapping.class );
			}
			catch ( JAXBException e ) {
				throw new ConfigurationException( "Unable to build hbm.xml JAXBContext", e );
			}
		}
		return hbmJaxbContext;
	}

	private JAXBContext entityMappingsJaxbContext() {
		if ( entityMappingsJaxbContext == null ) {
			try {
				entityMappingsJaxbContext = JAXBContext.newInstance( JaxbEntityMappings.class );
			}
			catch ( JAXBException e ) {
				throw new ConfigurationException( "Unable to build orm.xml JAXBContext", e );
			}
		}
		return entityMappingsJaxbContext;
	}
}
