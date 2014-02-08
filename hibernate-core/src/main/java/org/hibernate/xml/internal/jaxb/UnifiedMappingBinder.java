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

import org.hibernate.jaxb.spi.hbm.JaxbHibernateMapping;
import org.hibernate.metamodel.internal.source.hbm.transform.HbmXmlTransformer;
import org.hibernate.metamodel.spi.source.jaxb.JaxbEntityMappings;
import org.hibernate.xml.internal.stax.LocalSchema;
import org.hibernate.xml.spi.Origin;

import org.jboss.logging.Logger;

/**
 * Binder for the unified orm.xml schema
 *
 * @author Steve Ebersole
 */
public class UnifiedMappingBinder extends AbstractUnifiedBinder<JaxbEntityMappings> {
	private static final Logger log = Logger.getLogger( MappingXmlBinder.class );

	public UnifiedMappingBinder() {
		super();
	}

	public UnifiedMappingBinder(boolean validateXml) {
		super( validateXml );
	}

	@Override
	protected JaxbEntityMappings doBind(
			XMLEventReader staxEventReader,
			StartElement rootElementStartEvent,
			Origin origin) {
		final String rootElementLocalName = rootElementStartEvent.getName().getLocalPart();
		if ( "hibernate-mapping".equals( rootElementLocalName ) ) {
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
				hbmReader = new NamespaceAddingEventReader( staxEventReader, LocalSchema.LEGACY_HBM.getNamespaceUri() );
			}

			JaxbHibernateMapping hbmBindings = jaxb( hbmReader, LocalSchema.LEGACY_HBM.getSchema(), JaxbHibernateMapping.class, origin );
			return HbmXmlTransformer.INSTANCE.transform( hbmBindings );
		}
		else {
			final XMLEventReader reader = new UnifiedMappingEventReader( staxEventReader );
			return jaxb( reader, LocalSchema.MAPPING.getSchema(), JaxbEntityMappings.class, origin );
		}
	}
}
