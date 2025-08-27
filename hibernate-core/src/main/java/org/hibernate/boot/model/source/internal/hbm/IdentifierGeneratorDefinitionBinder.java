/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.source.internal.hbm;

import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmIdentifierGeneratorDefinitionType;
import org.hibernate.boot.model.IdentifierGeneratorDefinition;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public class IdentifierGeneratorDefinitionBinder {
	private static final Logger log = Logger.getLogger( IdentifierGeneratorDefinitionBinder.class );

	public static void processIdentifierGeneratorDefinition(
			HbmLocalMetadataBuildingContext context,
			JaxbHbmIdentifierGeneratorDefinitionType identifierGenerator) {
		log.tracef( "Processing <identifier-generator/> : %s", identifierGenerator.getName() );

		context.getMetadataCollector().addIdentifierGenerator(
				new IdentifierGeneratorDefinition(
						identifierGenerator.getName(),
						identifierGenerator.getClazz()
				)
		);
	}
}
