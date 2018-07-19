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
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import org.hibernate.boot.MappingException;
import org.hibernate.boot.UnsupportedOrmXsdVersionException;
import org.hibernate.boot.jaxb.Origin;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmHibernateMapping;
import org.hibernate.boot.jaxb.internal.stax.HbmEventReader;
import org.hibernate.boot.jaxb.internal.stax.JpaOrmXmlEventReader;
import org.hibernate.boot.jaxb.spi.Binding;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.xsd.MappingXsdSupport;
import org.hibernate.internal.util.config.ConfigurationException;

import org.jboss.logging.Logger;

import org.dom4j.Document;
import org.dom4j.Node;
import org.dom4j.io.STAXEventReader;

/**
 * @author Steve Ebersole
 */
public class MappingBinder extends AbstractBinder {
	private static final Logger log = Logger.getLogger( MappingBinder.class );

	private final XMLEventFactory xmlEventFactory = XMLEventFactory.newInstance();

	private JAXBContext hbmJaxbContext;

	public MappingBinder(ClassLoaderService classLoaderService) {
		this( classLoaderService, true );
	}

	public MappingBinder(ClassLoaderService classLoaderService, boolean validateXml) {
		super( classLoaderService, validateXml );
	}

	@Override
	protected Binding doBind(
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
//			final XMLEventReader reader = new JpaOrmXmlEventReader( staxEventReader );
//			return jaxb( reader, LocalSchema.MAPPING.getSchema(), JaxbEntityMappings.class, origin );

			try {
				final XMLEventReader reader = new JpaOrmXmlEventReader( staxEventReader, xmlEventFactory );
				return new Binding<>( toDom4jDocument( reader, origin ), origin );
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

	private Document toDom4jDocument(XMLEventReader jpaOrmXmlEventReader, Origin origin) {
		// todo : do we need to build a DocumentFactory instance for use here?
		//		historically we did that to set TCCL since, iirc, dom4j uses TCCL
		org.dom4j.io.STAXEventReader staxToDom4jReader = new STAXEventReader() {
			@Override
			public Node readNode(XMLEventReader reader) throws XMLStreamException {
				// dom4j's reader misses handling of XML comments.  So if the document we
				// are trying to read has comments this process will blow up.  So we
				// override that to add that support as best we can
				XMLEvent event = reader.peek();
				if ( javax.xml.stream.events.Comment.class.isInstance( event ) ) {
					return super.readComment( reader );
				}
				return super.readNode( reader );
			}
		};
		try {
			return staxToDom4jReader.readDocument( jpaOrmXmlEventReader );
		}
		catch (XMLStreamException e) {
			throw new MappingException(
					"An error occurred transforming orm.xml document from StAX to dom4j representation ",
					e,
					origin
			);
		}
	}
}
