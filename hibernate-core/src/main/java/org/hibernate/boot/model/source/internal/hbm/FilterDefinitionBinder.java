/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.source.internal.hbm;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.boot.MappingException;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmFilterDefinitionType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmFilterParameterType;
import org.hibernate.engine.spi.FilterDefinition;
import org.hibernate.metamodel.mapping.JdbcMapping;

import org.jboss.logging.Logger;

import jakarta.xml.bind.JAXBElement;

import static org.hibernate.internal.util.StringHelper.isNotEmpty;

/**
 * @author Steve Ebersole
 */
class FilterDefinitionBinder {
	private static final Logger LOG = Logger.getLogger( FilterDefinitionBinder.class );

	/**
	 * Handling for a {@code <filter-def/>} declaration.
	 *
	 * @param context Access to information relative to the mapping document containing this binding
	 * @param jaxbFilterDefinitionMapping The {@code <filter-def/>} JAXB mapping
	 */
	@SuppressWarnings("unchecked")
	static void processFilterDefinition(
			HbmLocalMetadataBuildingContext context,
			JaxbHbmFilterDefinitionType jaxbFilterDefinitionMapping) {
		Map<String, JdbcMapping> parameterMap = null;
		String condition = jaxbFilterDefinitionMapping.getCondition();

		for ( Serializable content : jaxbFilterDefinitionMapping.getContent() ) {
			if ( content instanceof String string ) {
				final String contentString = string.trim();
				if ( isNotEmpty( contentString ) ) {
					if ( condition != null && LOG.isDebugEnabled() ) {
						LOG.debugf(
								"filter-def [name=%s, origin=%s] defined multiple conditions, accepting arbitrary one",
								jaxbFilterDefinitionMapping.getName(),
								context.getOrigin().toString()
						);
					}
				}
			}
			else {
				final JaxbHbmFilterParameterType jaxbParameterMapping;
				if ( content instanceof JaxbHbmFilterParameterType filterParameterType ) {
					jaxbParameterMapping = filterParameterType;
				}
				else if ( content instanceof JAXBElement ) {
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
					parameterMap = new HashMap<>();
				}

				parameterMap.put(
						jaxbParameterMapping.getParameterName(),
						context.getMetadataCollector().getTypeConfiguration().getBasicTypeRegistry().getRegisteredType( jaxbParameterMapping.getParameterValueTypeName() )
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

		LOG.tracef( "Processed filter definition: %s", jaxbFilterDefinitionMapping.getName() );
	}
}
