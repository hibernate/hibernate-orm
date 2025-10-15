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

import jakarta.xml.bind.JAXBElement;

import static org.hibernate.boot.BootLogging.BOOT_LOGGER;
import static org.hibernate.internal.util.StringHelper.isNotBlank;

/**
 * @author Steve Ebersole
 */
class FilterDefinitionBinder {

	/**
	 * Handling for a {@code <filter-def/>} declaration.
	 *
	 * @param context Access to information relative to the mapping document containing this binding
	 * @param filterDefinitionMapping The {@code <filter-def/>} JAXB mapping
	 */
	static void processFilterDefinition(
			HbmLocalMetadataBuildingContext context,
			JaxbHbmFilterDefinitionType filterDefinitionMapping) {
		Map<String, JdbcMapping> parameterMap = null;
		final String condition = filterDefinitionMapping.getCondition();

		final var collector = context.getMetadataCollector();
		final var basicTypeRegistry = collector.getTypeConfiguration().getBasicTypeRegistry();

		for ( var content : filterDefinitionMapping.getContent() ) {
			if ( content instanceof String string ) {
				if ( isNotBlank( string ) ) {
					if ( condition != null && BOOT_LOGGER.isDebugEnabled() ) {
						BOOT_LOGGER.filterDefDefinedMultipleConditions(
								filterDefinitionMapping.getName(),
								context.getOrigin().toString()
						);
					}
				}
			}
			else {
				final var parameterMapping = filterParameterType( context, content );
				if ( parameterMap == null ) {
					parameterMap = new HashMap<>();
				}
				parameterMap.put( parameterMapping.getParameterName(),
						basicTypeRegistry.getRegisteredType( parameterMapping.getParameterValueTypeName() ) );
			}
		}

		collector.addFilterDefinition( new FilterDefinition( filterDefinitionMapping.getName(), condition, parameterMap ) );

		BOOT_LOGGER.processedFilterDefinition( filterDefinitionMapping.getName() );
	}

	private static JaxbHbmFilterParameterType filterParameterType(
			HbmLocalMetadataBuildingContext context, Serializable content) {
		if ( content instanceof JaxbHbmFilterParameterType filterParameterType ) {
			return filterParameterType;
		}
		else if ( content instanceof JAXBElement ) {
			final var jaxbElement = (JAXBElement<JaxbHbmFilterParameterType>) content;
			return jaxbElement.getValue();
		}
		else {
			throw new MappingException(
					"Unable to decipher filter-def content type [" + content.getClass().getName() + "]",
					context.getOrigin()
			);
		}
	}
}
