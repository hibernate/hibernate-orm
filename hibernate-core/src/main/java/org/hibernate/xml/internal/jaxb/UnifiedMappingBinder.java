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

import org.hibernate.metamodel.source.internal.jaxb.hbm.JaxbHibernateMapping;
import org.hibernate.metamodel.source.internal.jaxb.hbm.HbmXmlTransformer;
import org.hibernate.metamodel.source.internal.jaxb.JaxbEntityMappings;
import org.hibernate.metamodel.spi.ClassLoaderAccess;
import org.hibernate.xml.internal.stax.LocalSchema;
import org.hibernate.xml.spi.Origin;

import org.jboss.logging.Logger;

/**
 * Binder for the unified orm.xml schema
 *
 * @author Steve Ebersole
 */
public class UnifiedMappingBinder extends AbstractUnifiedBinder<JaxbEntityMappings> {
	private static final Logger log = Logger.getLogger( UnifiedMappingBinder.class );
	
	private final ClassLoaderAccess classLoaderAccess;

	public UnifiedMappingBinder(ClassLoaderAccess classLoaderAccess) {
		super();
		this.classLoaderAccess = classLoaderAccess;
	}

	public UnifiedMappingBinder(boolean validateXml, ClassLoaderAccess classLoaderAccess) {
		super( validateXml );
		this.classLoaderAccess = classLoaderAccess;
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
					"Found legacy Hibernate hbm.xml mapping; performing on-the-fly transformation.  "
							+ "Consider using build-time transformation tool to speed up run-time parsing"
			);

			XMLEventReader hbmReader = new HbmEventReader( staxEventReader );
			JaxbHibernateMapping hbmBindings = jaxb( hbmReader, LocalSchema.HBM.getSchema(), JaxbHibernateMapping.class, origin );
			return HbmXmlTransformer.INSTANCE.transform( hbmBindings, origin, classLoaderAccess );
		}
		else {
			final XMLEventReader reader = new UnifiedMappingEventReader( staxEventReader );
			return jaxb( reader, LocalSchema.MAPPING.getSchema(), JaxbEntityMappings.class, origin );
		}
	}
}
