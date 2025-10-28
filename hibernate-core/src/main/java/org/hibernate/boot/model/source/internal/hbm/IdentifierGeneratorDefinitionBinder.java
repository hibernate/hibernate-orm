/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.source.internal.hbm;

import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmIdentifierGeneratorDefinitionType;
import org.hibernate.boot.model.IdentifierGeneratorDefinition;

import static org.hibernate.boot.BootLogging.BOOT_LOGGER;


/**
 * @author Steve Ebersole
 */
public class IdentifierGeneratorDefinitionBinder {

	public static void processIdentifierGeneratorDefinition(
			HbmLocalMetadataBuildingContext context,
			JaxbHbmIdentifierGeneratorDefinitionType identifierGenerator) {
		BOOT_LOGGER.processingIdentifierGenerator( identifierGenerator.getName() );
		context.getMetadataCollector().addIdentifierGenerator(
				new IdentifierGeneratorDefinition(
						identifierGenerator.getName(),
						identifierGenerator.getClazz()
				)
		);
	}
}
