/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.materialize;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.MappingException;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.ImplicitUniqueKeyNameSource;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Constraint;
import org.hibernate.mapping.IdentifierCollection;
import org.hibernate.mapping.IndexedCollection;
import org.hibernate.mapping.PrimaryKey;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.UniqueKey;

/// Explicit materializer for collection-table primary-key and uniqueness
/// constraints.
///
/// This is the first key-creation slice from the ORM 9 mapping-key design.  New
/// boot-model binding paths should create collection table keys here instead of
/// relying on [Collection#createPrimaryKeyIfNeeded()], which hides key creation
/// as a side effect of the mutable mapping object.
///
/// @since 9.0
/// @author Steve Ebersole
public class CollectionKeyMappingMaterializer {
	public void materializePrimaryKeyIfNeeded(Collection collection) {
		if ( collection.isInverse() || collection.isPrimaryKeyDisabled() ) {
			return;
		}

		createPrimaryKey( collection );
		adjustTemporalPrimaryKey( collection );
	}

	private void createPrimaryKey(Collection collection) {
		if ( collection.isOneToMany() ) {
			return;
		}
		if ( collection instanceof IdentifierCollection identifierCollection ) {
			createIdentifierCollectionPrimaryKey( identifierCollection );
		}
		else if ( collection instanceof IndexedCollection indexedCollection ) {
			createIndexedCollectionPrimaryKey( indexedCollection );
		}
		else if ( collection.isSet() ) {
			createSetKey( collection );
		}
	}

	private void createIdentifierCollectionPrimaryKey(IdentifierCollection collection) {
		final PrimaryKey primaryKey = new PrimaryKey( collection.getCollectionTable() );
		primaryKey.addColumns( collection.getIdentifier() );
		collection.getCollectionTable().setPrimaryKey( primaryKey );
	}

	private void createIndexedCollectionPrimaryKey(IndexedCollection collection) {
		final PrimaryKey primaryKey = new PrimaryKey( collection.getCollectionTable() );
		primaryKey.addColumns( collection.getKey() );

		if ( indexIsPartOfElement( collection ) ) {
			primaryKey.addColumns( collection.getElement() );
		}
		else {
			primaryKey.addColumns( collection.getIndex() );
		}
		collection.getCollectionTable().setPrimaryKey( primaryKey );
	}

	private boolean indexIsPartOfElement(IndexedCollection collection) {
		for ( var selectable : collection.getIndex().getSelectables() ) {
			if ( selectable.isFormula() || !collection.getCollectionTable().containsColumn( (Column) selectable ) ) {
				return true;
			}
		}
		return false;
	}

	private void createSetKey(Collection collection) {
		final Table collectionTable = collection.getCollectionTable();
		if ( collectionTable.hasPrimaryKey() || !collectionTable.getUniqueKeys().isEmpty() ) {
			return;
		}

		boolean useUniqueKey = false;
		for ( var selectable : collection.getElement().getSelectables() ) {
			if ( selectable instanceof Column column ) {
				try {
					if ( column.isSqlTypeLob( collection.getMetadata() ) ) {
						return;
					}
				}
				catch (MappingException ignored) {
				}
				if ( column.isNullable() ) {
					useUniqueKey = true;
				}
			}
		}

		final Constraint key;
		if ( useUniqueKey ) {
			final UniqueKey uniqueKey = new UniqueKey( collectionTable );
			uniqueKey.setNullsNotDistinct( true );
			key = uniqueKey;
		}
		else {
			key = new PrimaryKey( collectionTable );
		}

		key.addColumns( collection.getKey() );
		for ( var selectable : collection.getElement().getSelectables() ) {
			if ( selectable instanceof Column column ) {
				key.addColumn( column );
			}
		}

		key.setName( implicitKeyName( collection, key ) );
		if ( key.getColumnSpan() > collection.getKey().getColumnSpan() ) {
			if ( useUniqueKey ) {
				collectionTable.addUniqueKey( (UniqueKey) key );
			}
			else {
				collectionTable.setPrimaryKey( (PrimaryKey) key );
			}
		}
	}

	private String implicitKeyName(Collection collection, Constraint key) {
		final MetadataBuildingContext buildingContext = collection.getBuildingContext();
		return buildingContext.getBuildingOptions()
				.getImplicitNamingStrategy()
				.determineUniqueKeyName( new ImplicitUniqueKeyNameSource() {
					@Override
					public Identifier getTableName() {
						return collection.getTable().getNameIdentifier();
					}

					@Override
					public List<Identifier> getColumnNames() {
						final List<Identifier> list = new ArrayList<>();
						for ( var column : key.getColumns() ) {
							list.add( column.getNameIdentifier( buildingContext ) );
						}
						return list;
					}

					@Override
					public Identifier getUserProvidedIdentifier() {
						return null;
					}

					@Override
					public MetadataBuildingContext getBuildingContext() {
						return buildingContext;
					}
				} )
				.render( collection.getMetadata().getDatabase().getDialect() );
	}

	private void adjustTemporalPrimaryKey(Collection collection) {
		if ( collection.isAuxiliaryColumnInPrimaryKey() ) {
			final var startingColumn = collection.getAuxiliaryColumn( collection.getAuxiliaryColumnInPrimaryKey() );
			if ( startingColumn != null ) {
				final var primaryKey = collection.getCollectionTable().getPrimaryKey();
				if ( primaryKey != null ) {
					if ( !primaryKey.containsColumn( startingColumn ) ) {
						primaryKey.addColumn( startingColumn );
					}
				}
				else if ( !collection.getCollectionTable().getUniqueKeys().isEmpty() ) {
					for ( var uniqueKey : collection.getCollectionTable().getUniqueKeys().values() ) {
						if ( !uniqueKey.containsColumn( startingColumn ) ) {
							uniqueKey.addColumn( startingColumn );
						}
					}
				}
			}
		}
	}
}
