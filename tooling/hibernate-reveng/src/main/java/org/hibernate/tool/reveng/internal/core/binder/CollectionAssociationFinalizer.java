/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.core.binder;

import org.hibernate.MappingException;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.DependantValue;
import org.hibernate.mapping.OneToMany;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Value;

/**
 * Finalizes collection associations after all reverse-engineered entity mappings
 * have been registered.
 *
 * @author Steve Ebersole
 */
public class CollectionAssociationFinalizer {
	private final MetadataBuildingContext mdbc;
	private final Collection collection;

	public CollectionAssociationFinalizer(MetadataBuildingContext mdbc, Collection coll) {
		this.mdbc = mdbc;
		this.collection = coll;
	}

	public void finalizeCollectionAssociation() throws MappingException {
		final Value element = collection.getElement();
		DependantValue elementDependantValue = null;
		String oldElementForeignKeyName = null;
		if ( element instanceof DependantValue ) {
			elementDependantValue = (DependantValue) element;
			oldElementForeignKeyName = elementDependantValue.getForeignKeyName();
			elementDependantValue.setForeignKeyName( "none" );
		}

		final Value key = collection.getKey();
		DependantValue keyDependantValue = null;
		String oldKeyForeignKeyName = null;
		if ( key instanceof DependantValue ) {
			keyDependantValue = (DependantValue) key;
			oldKeyForeignKeyName = keyDependantValue.getForeignKeyName();
			keyDependantValue.setForeignKeyName( "none" );
		}

		bindCollectionAssociation( collection, mdbc );

		if ( elementDependantValue != null ) {
			elementDependantValue.setForeignKeyName( oldElementForeignKeyName );
		}
		if ( keyDependantValue != null ) {
			keyDependantValue.setForeignKeyName( oldKeyForeignKeyName );
		}
	}

	private void bindCollectionAssociation(
			Collection collection,
			MetadataBuildingContext mdbc) throws MappingException {
		if ( collection.isOneToMany() ) {
			final OneToMany oneToMany = (OneToMany) collection.getElement();
			final PersistentClass persistentClass =
					mdbc.getMetadataCollector().getEntityBinding( oneToMany.getReferencedEntityName() );

			if ( persistentClass == null ) {
				throw new MappingException(
						"Association " + collection.getRole() + " references unmapped class: "
								+ oneToMany.getReferencedEntityName()
				);
			}

			oneToMany.setAssociatedClass( persistentClass );
		}
	}
}
