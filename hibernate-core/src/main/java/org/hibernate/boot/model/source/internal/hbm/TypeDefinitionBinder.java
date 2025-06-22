/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.source.internal.hbm;

import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmTypeDefinitionType;
import org.hibernate.boot.model.TypeDefinition;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public class TypeDefinitionBinder {
	private static final Logger log = Logger.getLogger( TypeDefinitionBinder.class );

	/**
	 * Handling for a {@code <typedef/>} declaration
	 *
	 * @param context Access to information relative to the mapping document containing this binding
	 * @param typeDefinitionBinding The {@code <typedef/>} binding
	 */
	public static void processTypeDefinition(
			HbmLocalMetadataBuildingContext context,
			JaxbHbmTypeDefinitionType typeDefinitionBinding) {

		final TypeDefinition definition = new TypeDefinition(
				typeDefinitionBinding.getName(),
				context.getBootstrapContext().getClassLoaderService()
						.classForName( typeDefinitionBinding.getClazz() ),
				null,
				ConfigParameterHelper.extractConfigParameters( typeDefinitionBinding )
		);

		if ( log.isDebugEnabled() ) {
			log.debugf(
					"Processed type-definition : %s -> %s",
					definition.getName(),
					definition.getTypeImplementorClass().getName()
			);
		}

		context.getMetadataCollector().getTypeDefinitionRegistry().register( definition );
	}

}
