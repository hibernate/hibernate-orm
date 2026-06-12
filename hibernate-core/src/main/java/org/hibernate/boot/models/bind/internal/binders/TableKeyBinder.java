/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.bind.internal.binders;

import org.hibernate.boot.models.bind.internal.sources.ColumnSource;
import org.hibernate.boot.models.bind.spi.BindingState;
import org.hibernate.boot.models.categorize.spi.EntityTypeMetadata;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.DependantValue;
import org.hibernate.mapping.Index;
import org.hibernate.mapping.Join;
import org.hibernate.mapping.JoinedSubclass;
import org.hibernate.mapping.KeyValue;
import org.hibernate.mapping.PrimaryKey;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.UniqueKey;
import org.hibernate.models.ModelsException;

/// Binds table keys that depend on an already-bound entity hierarchy identifier.
///
/// This phase completes mapping tables whose key columns are derived from an
/// entity identifier rather than declared independently:
///
/// - joined-subclass tables
/// - secondary tables
/// - association join tables represented as entity joins
/// - collection tables
///
/// The phase creates dependent key values and primary keys, then records
/// [TableForeignKeyBinding] work for the later foreign-key phase.  It also
/// applies collection-table indexes and unique constraints once the table's key
/// and value columns are available.
///
/// @since 9.0
/// @author Steve Ebersole
public class TableKeyBinder {
	private final EntityTypeBinder entityBinder;
	private final BindingState bindingState;

	public TableKeyBinder(EntityTypeBinder entityBinder) {
		this.entityBinder = entityBinder;
		this.bindingState = entityBinder.getBindingState();
	}

	public void bindTableKeys() {
		if ( entityBinder.getTypeBinding() instanceof JoinedSubclass joinedSubclass ) {
			bindJoinedSubclassKey( joinedSubclass );
		}

		entityBinder.getTypeBinding().getJoins().forEach( this::bindSecondaryTableKey );
		bindingState.forEachCollectionTableBinding( (collectionTableBinding) -> {
			if ( collectionTableBinding.collection().getOwner() == entityBinder.getTypeBinding() ) {
				bindCollectionTableKey( collectionTableBinding );
			}
		} );
	}

	private void bindJoinedSubclassKey(JoinedSubclass joinedSubclass) {
		final IdentifierBinding rootIdentifierBinding = resolveIdentifierBinding();
		final DependantValue key = createDependentKeyValue( joinedSubclass.getTable(), rootIdentifierBinding );
		joinedSubclass.setKey( key );
		createPrimaryKey( joinedSubclass.getTable(), key );
		bindingState.addTableForeignKeyBinding( new TableForeignKeyBinding(
				entityBinder.getTypeBinding(),
				key,
				entityBinder.getSuperEntityBinder().getTypeBinding().getEntityName(),
				null
		) );
	}

	private void bindSecondaryTableKey(Join join) {
		final IdentifierBinding rootIdentifierBinding = resolveIdentifierBinding();
		final AssociationTableBinding associationTableBinding = bindingState.getAssociationTableBinding( join );
		final DependantValue key = associationTableBinding == null
				? createDependentKeyValue( join.getTable(), rootIdentifierBinding )
				: createDependentKeyValue( join.getTable(), rootIdentifierBinding, associationTableBinding );
		join.setKey( key );
		join.createPrimaryKey();
		if ( !join.isInverse() ) {
			bindingState.addTableForeignKeyBinding( new TableForeignKeyBinding(
					entityBinder.getTypeBinding(),
					key,
					entityBinder.getTypeBinding().getEntityName(),
					associationTableBinding == null
							? findSecondaryTableForeignKeySource( join )
							: associationTableBinding.foreignKeySource()
			) );
		}
	}

	private void bindCollectionTableKey(CollectionTableBinding collectionTableBinding) {
		final IdentifierBinding rootIdentifierBinding = resolveIdentifierBinding();
		final DependantValue key = createDependentKeyValue(
				collectionTableBinding.collection().getCollectionTable(),
				rootIdentifierBinding,
				collectionTableBinding
		);
		key.setOnDeleteAction( collectionTableBinding.onDeleteAction() );
		collectionTableBinding.collection().setKey( key );
		collectionTableBinding.collection().createPrimaryKeyIfNeeded();
		bindingState.addTableForeignKeyBinding( new TableForeignKeyBinding(
				entityBinder.getTypeBinding(),
				key,
				entityBinder.getTypeBinding().getEntityName(),
				collectionTableBinding.foreignKeySource()
		) );
		applyUniqueConstraints( collectionTableBinding );
		applyIndexes( collectionTableBinding );
	}

	private void applyUniqueConstraints(CollectionTableBinding collectionTableBinding) {
		for ( jakarta.persistence.UniqueConstraint uniqueConstraint : collectionTableBinding.uniqueConstraints() ) {
			if ( uniqueConstraint.columnNames().length == 0 ) {
				continue;
			}

			final Table table = collectionTableBinding.collection().getCollectionTable();
			final UniqueKey uniqueKey = StringHelper.isEmpty( uniqueConstraint.name() )
					? table.addUniqueKey( new UniqueKey( table ) )
					: table.getOrCreateUniqueKey( uniqueConstraint.name() );
			if ( StringHelper.isNotEmpty( uniqueConstraint.name() ) ) {
				uniqueKey.setName( uniqueConstraint.name() );
				uniqueKey.setNameExplicit( true );
			}
			uniqueKey.setExplicit( true );
			if ( StringHelper.isNotEmpty( uniqueConstraint.options() ) ) {
				uniqueKey.setOptions( uniqueConstraint.options() );
			}
			for ( String columnName : uniqueConstraint.columnNames() ) {
				uniqueKey.addColumn( resolveColumn( table, columnName ) );
			}
			if ( StringHelper.isEmpty( uniqueConstraint.name() ) ) {
				table.addUniqueKey( uniqueKey );
			}
		}
	}

	private void applyIndexes(CollectionTableBinding collectionTableBinding) {
		for ( jakarta.persistence.Index indexAnn : collectionTableBinding.indexes() ) {
			if ( StringHelper.isEmpty( indexAnn.columnList() ) ) {
				continue;
			}

			final Table table = collectionTableBinding.collection().getCollectionTable();
			final Index index = table.getOrCreateIndex( indexAnn.name() );
			index.setTable( table );
			index.setName( indexAnn.name() );
			index.setUnique( indexAnn.unique() );
			if ( StringHelper.isNotEmpty( indexAnn.options() ) ) {
				index.setOptions( indexAnn.options() );
			}
			for ( String columnName : indexAnn.columnList().split( "," ) ) {
				index.addColumn( resolveColumn( table, columnName.trim() ) );
			}
		}
	}

	private org.hibernate.boot.models.bind.internal.sources.ForeignKeySource findSecondaryTableForeignKeySource(Join join) {
		final org.hibernate.boot.models.bind.internal.SecondaryTable secondaryTable = bindingState.getSecondaryTable( join.getTable() );
		return secondaryTable == null ? null : secondaryTable.foreignKeySource();
	}

	private Column resolveColumn(Table table, String columnName) {
		final Column column = table.getColumn( new Column( columnName ) );
		if ( column == null ) {
			throw new ModelsException( "Could not resolve collection table column `" + columnName + "` on " + table.getName() );
		}
		return column;
	}

	private IdentifierBinding resolveIdentifierBinding() {
		final EntityTypeMetadata rootType = entityBinder.getManagedType().getHierarchy().getRoot();
		final IdentifierBinding identifierBinding = bindingState.getIdentifierBinding( rootType );
		if ( identifierBinding == null ) {
			throw new ModelsException( "Identifier binding not available for " + rootType.getEntityName() );
		}
		return identifierBinding;
	}

	private DependantValue createDependentKeyValue(Table table, IdentifierBinding identifierBinding) {
		final DependantValue key = new DependantValue(
				bindingState.getMetadataBuildingContext(),
				table,
				identifierBinding.value()
		);
		key.setNullable( false );
		key.setUpdateable( false );
		for ( Column identifierColumn : identifierBinding.columns() ) {
			key.addColumn( copyKeyColumn( identifierColumn ), true, false );
		}
		return key;
	}

	private DependantValue createDependentKeyValue(
			Table table,
			IdentifierBinding identifierBinding,
			AssociationTableBinding associationTableBinding) {
		final DependantValue key = new DependantValue(
				bindingState.getMetadataBuildingContext(),
				table,
				identifierBinding.value()
		);
		key.setNullable( false );
		key.setUpdateable( false );

		final var orderedJoinColumns = ToOneAttributeBinder.orderJoinColumns(
				associationTableBinding.joinColumns(),
				identifierBinding.columns(),
				entityBinder.getManagedType().getClassDetails().getClassName(),
				associationTableBinding.join().getTable().getName()
		);
		for ( int i = 0; i < identifierBinding.columns().size(); i++ ) {
			final Column identifierColumn = identifierBinding.columns().get( i );
			key.addColumn(
					bindKeyColumn( table, identifierColumn, orderedJoinColumns.isEmpty() ? null : orderedJoinColumns.get( i ) ),
					true,
					false
			);
		}
		return key;
	}

	private DependantValue createDependentKeyValue(
			Table table,
			IdentifierBinding identifierBinding,
			CollectionTableBinding collectionTableBinding) {
		final DependantValue key = new DependantValue(
				bindingState.getMetadataBuildingContext(),
				table,
				identifierBinding.value()
		);
		key.setNullable( false );
		key.setUpdateable( false );

		final var orderedJoinColumns = ToOneAttributeBinder.orderJoinColumns(
				collectionTableBinding.joinColumns(),
				identifierBinding.columns(),
				entityBinder.getManagedType().getClassDetails().getClassName(),
				collectionTableBinding.collection().getRole()
		);
		for ( int i = 0; i < identifierBinding.columns().size(); i++ ) {
			final Column identifierColumn = identifierBinding.columns().get( i );
			key.addColumn(
					bindKeyColumn( table, identifierColumn, orderedJoinColumns.isEmpty() ? null : orderedJoinColumns.get( i ) ),
					true,
					false
			);
		}
		return key;
	}

	private Column copyKeyColumn(Column source) {
		// todo : is this enough detail?
		final Column result = new Column( source.getName() );
		result.setLength( source.getLength() );
		result.setPrecision( source.getPrecision() );
		result.setScale( source.getScale() );
		result.setSqlType( source.getSqlType() );
		result.setNullable( false );
		result.setUnique( source.isUnique() );
		return result;
	}

	private Column bindKeyColumn(Table table, Column identifierColumn, jakarta.persistence.JoinColumn joinColumn) {
		final Column result = ColumnBinder.bindColumn(
				ColumnSource.from( joinColumn ),
				identifierColumn::getName,
				false,
				false
		);
		result.setNullable( false );
		table.addColumn( result );
		return result;
	}

	private void createPrimaryKey(Table table, KeyValue key) {
		final PrimaryKey primaryKey = new PrimaryKey( table );
		table.setPrimaryKey( primaryKey );
		key.getColumns().forEach( primaryKey::addColumn );
	}
}
