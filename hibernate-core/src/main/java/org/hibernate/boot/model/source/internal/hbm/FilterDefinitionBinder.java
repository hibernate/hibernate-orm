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
package org.hibernate.boot.model.source.internal.hbm;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import javax.xml.bind.JAXBElement;

import org.hibernate.boot.MappingException;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmFilterDefinitionType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmFilterParameterType;
import org.hibernate.engine.spi.FilterDefinition;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.type.Type;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public class FilterDefinitionBinder {
	private static final Logger log = Logger.getLogger( FilterDefinitionBinder.class );

	/**
	 * Handling for a {@code <filter-def/>} declaration.
	 *
	 * @param context Access to information relative to the mapping document containing this binding
	 * @param jaxbFilterDefinitionMapping The {@code <filter-def/>} JAXB mapping
	 */
	@SuppressWarnings("unchecked")
	public static void processFilterDefinition(
			HbmLocalMetadataBuildingContext context,
			JaxbHbmFilterDefinitionType jaxbFilterDefinitionMapping) {
		Map<String,Type> parameterMap = null;
		String condition = jaxbFilterDefinitionMapping.getCondition();

		for ( Serializable content : jaxbFilterDefinitionMapping.getContent() ) {
			if ( String.class.isInstance( content ) ) {
				final String contentString = content.toString().trim();
				if ( StringHelper.isNotEmpty( contentString ) ) {
					if ( condition != null ) {
						log.debugf(
								"filter-def [name=%s, origin=%s] defined multiple conditions, accepting arbitrary one",
								jaxbFilterDefinitionMapping.getName(),
								context.getOrigin().toString()
						);
					}
				}
			}
			else {
				final JaxbHbmFilterParameterType jaxbParameterMapping;
				if ( JaxbHbmFilterParameterType.class.isInstance( content ) ) {
					jaxbParameterMapping = (JaxbHbmFilterParameterType) content;
				}
				else if ( JAXBElement.class.isInstance( content ) ) {
					final JAXBElement<JaxbHbmFilterParameterType> jaxbElement = (JAXBElement<JaxbHbmFilterParameterType>) content;
					jaxbParameterMapping = jaxbElement.getValue();
				}
				else {
					throw new MappingException(
							"Unable to decipher filter-def content type [" + content.getClass().getName() + "]",
							context.getOrigin()
					);
				}

				if ( parameterMap == null ) {
					parameterMap = new HashMap<String, Type>();
				}

				parameterMap.put(
						jaxbParameterMapping.getParameterName(),
						context.getMetadataCollector().getTypeResolver().heuristicType( jaxbParameterMapping.getParameterValueTypeName() )
				);
			}
		}

		context.getMetadataCollector().addFilterDefinition(
				new FilterDefinition(
						jaxbFilterDefinitionMapping.getName(),
						condition,
						parameterMap
				)
		);

		log.debugf( "Processed filter definition : %s", jaxbFilterDefinitionMapping.getName() );
	}
}
