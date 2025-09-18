/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.source.internal.hbm;

import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmTypeDefinitionType;
import org.hibernate.boot.model.TypeDefinition;


import static org.hibernate.boot.BootLogging.BOOT_LOGGER;
import static org.hibernate.boot.model.source.internal.hbm.ConfigParameterHelper.extractConfigParameters;

/**
 * @author Steve Ebersole
 */
public class TypeDefinitionBinder {

	/**
	 * Handling for a {@code <typedef/>} declaration
	 *
	 * @param context Access to information relative to the mapping document containing this binding
	 * @param typeDefinitionBinding The {@code <typedef/>} binding
	 */
	public static void processTypeDefinition(
			HbmLocalMetadataBuildingContext context,
			JaxbHbmTypeDefinitionType typeDefinitionBinding) {

		final var definition = new TypeDefinition(
				typeDefinitionBinding.getName(),
				context.getBootstrapContext().getClassLoaderService()
						.classForName( typeDefinitionBinding.getClazz() ),
				null,
				extractConfigParameters( typeDefinitionBinding )
		);

		if ( BOOT_LOGGER.isTraceEnabled() ) {
			BOOT_LOGGER.processedTypeDefinition( definition.getName(),
					definition.getTypeImplementorClass().getName() );
		}

		context.getMetadataCollector().getTypeDefinitionRegistry().register( definition );
	}

}
