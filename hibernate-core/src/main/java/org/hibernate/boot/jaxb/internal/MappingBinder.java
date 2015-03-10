/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat Inc. or third-party contributors as
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
package org.hibernate.boot.jaxb.internal;

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
import org.hibernate.boot.jaxb.internal.stax.LocalSchema;
import org.hibernate.boot.jaxb.spi.Binding;

import org.jboss.logging.Logger;

import org.dom4j.Document;
import org.dom4j.Node;
import org.dom4j.io.STAXEventReader;

/**
 * @author Steve Ebersole
 */
public class MappingBinder extends AbstractBinder {
	private static final Logger log = Logger.getLogger( MappingBinder.class );

	public MappingBinder() {
		super();
	}

	public MappingBinder(boolean validateXml) {
		super( validateXml );
	}

	@Override
	protected Binding doBind(
			XMLEventReader staxEventReader,
			StartElement rootElementStartEvent,
			Origin origin) {
		final String rootElementLocalName = rootElementStartEvent.getName().getLocalPart();
		if ( "hibernate-mapping".equals( rootElementLocalName ) ) {
			log.debugf( "Performing JAXB binding of hbm.xml document : %s", origin.toString() );

			XMLEventReader hbmReader = new HbmEventReader( staxEventReader );
			JaxbHbmHibernateMapping hbmBindings = jaxb( hbmReader, LocalSchema.HBM.getSchema(), JaxbHbmHibernateMapping.class, origin );
			return new Binding<JaxbHbmHibernateMapping>( hbmBindings, origin );
		}
		else {
//			final XMLEventReader reader = new JpaOrmXmlEventReader( staxEventReader );
//			return jaxb( reader, LocalSchema.MAPPING.getSchema(), JaxbEntityMappings.class, origin );

			try {
				final XMLEventReader reader = new JpaOrmXmlEventReader( staxEventReader );
				return new Binding<Document>( toDom4jDocument( reader, origin), origin );
			}
			catch (JpaOrmXmlEventReader.BadVersionException e) {
				throw new UnsupportedOrmXsdVersionException( e.getRequestedVersion(), origin );
			}
		}
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

	/**
	 * Models an {@code orm.xml} file, which is processed in a delayed manner.
	 */
	public static class DelayedOrmXmlData {
		private final XMLEventReader staxEventReader;
		private final StartElement rootElementStartEvent;
		private final Origin origin;

		public DelayedOrmXmlData(
				XMLEventReader staxEventReader,
				StartElement rootElementStartEvent,
				Origin origin) {
			this.staxEventReader = staxEventReader;
			this.rootElementStartEvent = rootElementStartEvent;
			this.origin = origin;
		}

		public XMLEventReader getStaxEventReader() {
			return staxEventReader;
		}

		public StartElement getRootElementStartEvent() {
			return rootElementStartEvent;
		}

		public Origin getOrigin() {
			return origin;
		}
	}
}
