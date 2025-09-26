/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.internal;

import org.hibernate.MappingException;
import org.hibernate.boot.model.relational.AuxiliaryDatabaseObject;
import org.hibernate.boot.model.relational.SimpleAuxiliaryDatabaseObject;
import org.hibernate.boot.models.spi.DatabaseObjectRegistration;
import org.hibernate.boot.models.spi.DialectScopeRegistration;
import org.hibernate.boot.registry.classloading.spi.ClassLoadingException;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.models.internal.util.StringHelper;

public class AuxiliaryDatabaseObjectBinder {
	public static void processAuxiliaryDatabaseObject(
			MetadataBuildingContext context,
			DatabaseObjectRegistration databaseObjectRegistration) {
		final AuxiliaryDatabaseObject auxDbObject;

		if ( StringHelper.isNotEmpty( databaseObjectRegistration.definition()) ) {
			try {
				auxDbObject = (AuxiliaryDatabaseObject)
						context.getBootstrapContext().getClassLoaderService()
								.classForName( databaseObjectRegistration.definition() )
								.newInstance();
			}
			catch (ClassLoadingException cle) {
				throw cle;
			}
			catch (Exception e) {
				throw new MappingException(
						String.format(
								"Unable to instantiate custom AuxiliaryDatabaseObject class [%s]",
								databaseObjectRegistration.definition()
						)
				);
			}
		}
		else {
			auxDbObject = new SimpleAuxiliaryDatabaseObject(
					context.getMetadataCollector().getDatabase().getDefaultNamespace(),
					databaseObjectRegistration.create(),
					databaseObjectRegistration.drop(),
					null
			);
		}

		if ( !databaseObjectRegistration.dialectScopes().isEmpty() ) {
			if ( auxDbObject instanceof AuxiliaryDatabaseObject.Expandable expandable ) {
				for ( DialectScopeRegistration dialectScopeRegistration : databaseObjectRegistration.dialectScopes() ) {
					expandable.addDialectScope( dialectScopeRegistration.name());
				}
			}
			else {
				// error?  warn?
			}
		}

		context.getMetadataCollector().getDatabase().addAuxiliaryDatabaseObject( auxDbObject );
	}

}
