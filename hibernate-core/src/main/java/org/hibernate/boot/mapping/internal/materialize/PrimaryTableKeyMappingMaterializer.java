/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.materialize;

import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.PrimaryKey;
import org.hibernate.mapping.Table;
import org.hibernate.sql.Alias;

/// Explicit materializer for entity primary-table primary keys.
///
/// This is the primary-table half of the ORM 9 key-creation design.  New
/// boot-model binding paths should initialize and finalize root table primary
/// keys through this materializer instead of relying on
/// [PersistentClass#createPrimaryKey()] as a hidden mapping-object side effect.
///
/// @since 9.0
/// @author Steve Ebersole
public class PrimaryTableKeyMappingMaterializer {
	private static final Alias PK_ALIAS = new Alias( 15, "PK" );

	private final MetadataBuildingContext buildingContext;

	public PrimaryTableKeyMappingMaterializer(MetadataBuildingContext buildingContext) {
		this.buildingContext = buildingContext;
	}

	public PrimaryKey initializePrimaryKey(PersistentClass entityBinding, Table table) {
		final PrimaryKey existingPrimaryKey = table.getPrimaryKey();
		if ( existingPrimaryKey != null ) {
			return existingPrimaryKey;
		}

		final PrimaryKey primaryKey = new PrimaryKey( table );
		primaryKey.setName( PK_ALIAS.toAliasString( table.getName() ) );
		table.setPrimaryKey( primaryKey );
		return primaryKey;
	}

	public void addIdentifierColumn(Table table, Column column) {
		table.getPrimaryKey().addColumn( column );
	}

	public void finalizePrimaryKey(PersistentClass entityBinding, Table table) {
		final PrimaryKey primaryKey = initializePrimaryKey( entityBinding, table );
		if ( addPartitionKeyToPrimaryKey( entityBinding ) ) {
			for ( var property : entityBinding.getProperties() ) {
				if ( property.getValue().isPartitionKey() ) {
					primaryKey.addColumns( property.getValue() );
				}
			}
		}
	}

	private boolean addPartitionKeyToPrimaryKey(PersistentClass entityBinding) {
		return buildingContext.getMetadataCollector().getDatabase().getDialect().addPartitionKeyToPrimaryKey();
	}
}
