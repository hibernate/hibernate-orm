/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.source.internal.hbm;

import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmAuxiliaryDatabaseObjectType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmDialectScopeType;
import org.hibernate.boot.model.relational.AuxiliaryDatabaseObject;
import org.hibernate.boot.model.relational.SimpleAuxiliaryDatabaseObject;
import org.hibernate.boot.registry.classloading.spi.ClassLoadingException;

/**
 * @author Steve Ebersole
 */
public class AuxiliaryDatabaseObjectBinder {
	/**
	 * Handling for a {@code <database-object/>} declaration.
	 *
	 * @param context Access to information relative to the mapping document containing this binding
	 * @param auxDbObjectMapping The {@code <database-object/>} binding
	 */
	public static void processAuxiliaryDatabaseObject(
			HbmLocalMetadataBuildingContext context,
			JaxbHbmAuxiliaryDatabaseObjectType auxDbObjectMapping) {
		final AuxiliaryDatabaseObject auxDbObject;

		if ( auxDbObjectMapping.getDefinition() != null ) {
			final String auxDbObjectImplClass = auxDbObjectMapping.getDefinition().getClazz();
			try {
				auxDbObject = (AuxiliaryDatabaseObject)
						context.getBootstrapContext().getClassLoaderService()
								.classForName( auxDbObjectImplClass )
								.newInstance();
			}
			catch (ClassLoadingException cle) {
				throw cle;
			}
			catch (Exception e) {
				throw new org.hibernate.boot.MappingException(
						String.format(
								"Unable to instantiate custom AuxiliaryDatabaseObject class [%s]",
								auxDbObjectImplClass
						),
						context.getOrigin()
				);
			}
		}
		else {
			auxDbObject = new SimpleAuxiliaryDatabaseObject(
					context.getMetadataCollector().getDatabase().getDefaultNamespace(),
					auxDbObjectMapping.getCreate(),
					auxDbObjectMapping.getDrop(),
					null
			);
		}

		if ( !auxDbObjectMapping.getDialectScope().isEmpty() ) {
			if ( auxDbObject instanceof AuxiliaryDatabaseObject.Expandable expandable ) {
				for ( JaxbHbmDialectScopeType dialectScopeBinding : auxDbObjectMapping.getDialectScope() ) {
					expandable.addDialectScope( dialectScopeBinding.getName() );
				}
			}
			else {
				// error?  warn?
			}
		}

		context.getMetadataCollector().getDatabase().addAuxiliaryDatabaseObject( auxDbObject );
	}
}
