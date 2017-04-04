/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.source.internal.hbm;

import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmTypeDefinitionType;
import org.hibernate.boot.model.TypeDefinition;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;

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
		final ClassLoaderService cls = context.getBuildingOptions().getServiceRegistry().getService( ClassLoaderService.class );

		final TypeDefinition definition = new TypeDefinition(
				typeDefinitionBinding.getName(),
				cls.classForName( typeDefinitionBinding.getClazz() ),
				null,
				ConfigParameterHelper.extractConfigParameters( typeDefinitionBinding )
		);

		log.debugf(
				"Processed type-definition : %s -> %s",
				definition.getName(),
				definition.getTypeImplementorClass().getName()
		);

		context.getMetadataCollector().addTypeDefinition( definition );
	}

}
