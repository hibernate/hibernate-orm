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

import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmAuxiliaryDatabaseObjectType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmDialectScopeType;
import org.hibernate.boot.model.relational.AuxiliaryDatabaseObject;
import org.hibernate.boot.model.relational.SimpleAuxiliaryDatabaseObject;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
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
				auxDbObject = (AuxiliaryDatabaseObject) context.getBuildingOptions()
						.getServiceRegistry()
						.getService( ClassLoaderService.class )
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
					context.getMetadataCollector().getDatabase().getDefaultSchema(),
					auxDbObjectMapping.getCreate(),
					auxDbObjectMapping.getDrop(),
					null
			);
		}

		if ( !auxDbObjectMapping.getDialectScope().isEmpty() ) {
			if ( AuxiliaryDatabaseObject.Expandable.class.isInstance( auxDbObject ) ) {
				final AuxiliaryDatabaseObject.Expandable expandable
						= (AuxiliaryDatabaseObject.Expandable) auxDbObject;
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
