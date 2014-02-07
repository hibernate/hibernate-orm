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
package org.hibernate.xml.internal.jaxb;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.events.StartElement;
import javax.xml.validation.Schema;

import org.hibernate.jaxb.spi.hbm.JaxbHibernateMapping;
import org.hibernate.metamodel.internal.source.hbm.transform.HbmXmlTransformer;
import org.hibernate.metamodel.spi.source.jaxb.JaxbEntityMappings;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.xml.internal.stax.LocalSchemaLocator;
import org.hibernate.xml.spi.BindResult;
import org.hibernate.xml.spi.Origin;

import org.jboss.logging.Logger;

/**
 * Binder for the unified orm.xml schema
 *
 * @author Steve Ebersole
 */
public class UnifiedMappingBinder extends AbstractXmlBinder2 {
	private static final Logger log = Logger.getLogger( MappingXmlBinder.class );
	private static final Schema XSD = LocalSchemaLocator.resolveLocalSchema( "org/hibernate/xsd/mapping/orm-2.1.0.xsd" );
	private static final Schema HBM_XSD =LocalSchemaLocator.resolveLocalSchema( "org/hibernate/hibernate-mapping-4.0.xsd" );

	public static final String HBM_URI = "http://www.hibernate.org/xsd/hibernate-mapping";


	public UnifiedMappingBinder() {
		super();
	}

	public UnifiedMappingBinder(boolean validateXml) {
		super( validateXml );
	}

	@Override
	protected BindResult<JaxbEntityMappings> doBind(
			XMLEventReader staxEventReader,
			StartElement rootElementStartEvent,
			Origin origin) {
		final String rootElementLocalName = rootElementStartEvent.getName().getLocalPart();
		if ( "hibernate-mappings".equals( rootElementLocalName ) ) {
			// todo: finalize message test here, and possibly use a message logger
			log.debug(
					"Found legacy Hibernate hbm.xml mapping; performing on-the-fly transformation.  " +
							"Consider using build-time transformation tool to speed up run-time parsing"
			);

			XMLEventReader hbmReader = staxEventReader;
			if ( !hasNamespace( rootElementStartEvent ) ) {
				// if the elements are not namespaced in the source document, which can cause problems with validation
				// and/or JAXB binding (since the xsd is namespaced).  So we wrap the reader in a version that will
				// return events that are namespaced versions of the original
				hbmReader = new NamespaceAddingEventReader( staxEventReader, HBM_URI );
			}

			JaxbHibernateMapping hbmBindings = jaxb( hbmReader, HBM_XSD, JaxbHibernateMapping.class, origin );
			return new BindResult<JaxbEntityMappings>( HbmXmlTransformer.INSTANCE.transform( hbmBindings ), origin );
		}
		else {
			final XMLEventReader reader = new UnifiedMappingEventReader( staxEventReader );
			final JaxbEntityMappings bindings = jaxb( reader, XSD, JaxbEntityMappings.class, origin );
			return new BindResult<JaxbEntityMappings>( bindings, origin );
		}
	}
}
