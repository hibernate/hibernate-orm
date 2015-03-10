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
