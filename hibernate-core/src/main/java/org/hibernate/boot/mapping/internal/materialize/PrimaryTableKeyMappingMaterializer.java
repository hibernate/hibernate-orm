/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.materialize;

import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.PrimaryKey;
import org.hibernate.mapping.RootClass;
import org.hibernate.mapping.Table;
import org.hibernate.sql.Alias;

/// Explicit materializer for entity primary-table primary keys.
///
/// This is the primary-table half of the ORM 9 key-creation design.  New
/// boot-model binding paths should initialize and finalize root table primary
/// keys through this materializer instead of relying on hidden mapping-object
/// side effects.
///
/// @since 9.0
/// @author Steve Ebersole
public class PrimaryTableKeyMappingMaterializer {
	private static final Alias PK_ALIAS = new Alias( 15, "PK" );

	private final MetadataBuildingContext buildingContext;

	public PrimaryTableKeyMappingMaterializer(MetadataBuildingContext buildingContext) {
		this.buildingContext = buildingContext;
	}

	public ResolvedPrimaryTableKey resolvePrimaryKey(PersistentClass entityBinding, Table table) {
		return new ResolvedPrimaryTableKey( entityBinding, table, buildingContext );
	}

	public PrimaryKey initializePrimaryKey(ResolvedPrimaryTableKey primaryTableKey) {
		final Table table = primaryTableKey.table();
		final PrimaryKey existingPrimaryKey = table.getPrimaryKey();
		if ( existingPrimaryKey != null ) {
			return existingPrimaryKey;
		}

		final PrimaryKey primaryKey = new PrimaryKey( table );
		primaryKey.setName( PK_ALIAS.toAliasString( table.getName() ) );
		table.setPrimaryKey( primaryKey );
		return primaryKey;
	}

	public void finalizeRootPrimaryKey(RootClass rootClass) {
		final Table table = rootClass.getRootTable();
		if ( rootClass.isPrimaryKeyDisabled() ) {
			table.setPrimaryKey( null );
			return;
		}

		final PrimaryKey primaryKey = table.getPrimaryKey();
		if ( primaryKey == null ) {
			return;
		}

		addAuxiliaryPrimaryKeyColumn( rootClass, primaryKey );
	}

	public void addIdentifierColumn(ResolvedPrimaryTableKey primaryTableKey, Column column) {
		column.setNullable( false );
		primaryTableKey.addIdentifierColumn( column );
		final PrimaryKey primaryKey = initializePrimaryKey( primaryTableKey );
		final Column canonicalColumn = primaryTableKey.table().getColumn( column );
		if ( canonicalColumn != null ) {
			canonicalColumn.setNullable( false );
		}
		primaryKey.addColumn( column );
	}

	public void finalizePrimaryKey(ResolvedPrimaryTableKey primaryTableKey) {
		final PrimaryKey primaryKey = initializePrimaryKey( primaryTableKey );
		for ( Column identifierColumn : primaryTableKey.identifierColumns() ) {
			primaryKey.addColumn( identifierColumn );
		}
		if ( addPartitionKeyToPrimaryKey() ) {
			for ( var property : primaryTableKey.entityBinding().getProperties() ) {
				if ( property.getValue().isPartitionKey() ) {
					primaryKey.addColumns( property.getValue() );
				}
			}
		}
		if ( primaryTableKey.entityBinding() instanceof RootClass rootClass ) {
			addAuxiliaryPrimaryKeyColumn( rootClass, primaryKey );
		}
	}

	private void addAuxiliaryPrimaryKeyColumn(RootClass rootClass, PrimaryKey primaryKey) {
		if ( !rootClass.isAuxiliaryColumnInPrimaryKey() ) {
			return;
		}

		if ( rootClass.isVersioned() ) {
			final var version = rootClass.getVersion();
			if ( version != null ) {
				primaryKey.addColumns( version.getValue() );
			}
		}
		else {
			final Column auxiliaryColumn = rootClass.getAuxiliaryColumn( rootClass.getAuxiliaryColumnInPrimaryKey() );
			if ( auxiliaryColumn != null ) {
				primaryKey.addColumn( auxiliaryColumn );
			}
		}
	}

	private boolean addPartitionKeyToPrimaryKey() {
		return buildingContext.getMetadataCollector().getDatabase().getDialect().addPartitionKeyToPrimaryKey();
	}
}
