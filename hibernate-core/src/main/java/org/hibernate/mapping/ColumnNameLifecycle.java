/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.mapping;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import org.hibernate.Internal;
import org.hibernate.MappingException;
import org.hibernate.relational.naming.spi.PhysicalName;

/// Collects column names by graph identity before attaching names and validating indexes.
///
/// @author Steve Ebersole
@Internal
public final class ColumnNameLifecycle {
	private final Set<Column> columns = identitySet();
	private final Set<ColumnContainer> containers = identitySet();
	private final Set<Value> values = identitySet();
	private final Set<MappedSuperclass> declarations = identitySet();
	private final Set<UserDefinedObjectType> udts = identitySet();

	private static <T> Set<T> identitySet() {
		return Collections.newSetFromMap( new IdentityHashMap<>() );
	}

	public void addEntity(PersistentClass entity) {
		addMappedSuperclass( entity.getSuperMappedSuperclass() );
		entity.getUnjoinedProperties().forEach( property -> addValue( property.getValue() ) );
		addContainer( entity.getTable() );
		addContainer( entity.getAuxiliaryTable() );
		if ( entity instanceof RootClass root ) { addColumn( root.getSoftDeleteColumn() ); }
		if ( entity.getVersion() != null ) { addValue( entity.getVersion().getValue() ); }
		addValue( entity.getIdentifier() );
		addValue( entity.getIdentifierMapper() );
		addValue( entity.getDiscriminator() );
		entity.getProperties().forEach( property -> addValue( property.getValue() ) );
		entity.getJoins().forEach( join -> {
			addContainer( join.getTable() );
			addValue( join.getKey() );
			join.getProperties().forEach( property -> addValue( property.getValue() ) );
		} );
	}

	public void addMappedSuperclass(MappedSuperclass mapped) {
		if ( mapped == null || !declarations.add( mapped ) ) { return; }
		addMappedSuperclass( mapped.getSuperMappedSuperclass() );
		mapped.getDeclaredProperties().forEach( property -> addValue( property.getValue() ) );
		addValue( mapped.getIdentifierMapper() );
		if ( mapped.getDeclaredIdentifierProperty() != null ) {
			addValue( mapped.getDeclaredIdentifierProperty().getValue() );
		}
		if ( mapped.getDeclaredVersion() != null ) { addValue( mapped.getDeclaredVersion().getValue() ); }
	}

	public void addValue(Value value) {
		if ( value == null || !values.add( value ) ) { return; }
		if ( value instanceof Collection collection ) {
			addContainer( collection.getCollectionColumnContainer() );
			addValue( collection.getKey() );
			addValue( collection.getElement() );
			if ( collection instanceof IndexedCollection indexed ) { addValue( indexed.getIndex() ); }
			if ( collection instanceof IdentifierCollection identified ) { addValue( identified.getIdentifier() ); }
			return;
		}
		addContainer( value.getColumnContainer() );
		if ( value instanceof OneToMany ) { return; }
		value.getSelectables().forEach( this::addSelectable );
		if ( value instanceof Component component ) {
			addColumn( component.getAggregateColumn() );
			addValue( component.getDiscriminator() );
			component.getProperties().forEach( property -> addValue( property.getValue() ) );
		}
	}

	public void addSelectable(Selectable selectable) {
		if ( selectable instanceof Column column ) { addColumn( column ); }
	}

	public void addColumn(Column column) {
		if ( column != null && columns.add( column ) ) {
			addValue( column.getValue() );
			if ( column instanceof AggregateColumn aggregate ) { addValue( aggregate.getComponent() ); }
		}
	}

	public void addContainer(ColumnContainer container) {
		if ( container == null || !containers.add( container ) ) { return; }
		container.getColumns().forEach( this::addColumn );
		if ( container instanceof Table table ) {
			if ( table.getPrimaryKey() != null ) { table.getPrimaryKey().getColumns().forEach( this::addColumn ); }
			table.uniqueKeysForNameAttachment().forEach( key -> {
				key.getColumns().forEach( this::addColumn );
				key.visitOrderingSelectables( this::addSelectable );
			} );
			table.getForeignKeys().forEach( key -> {
				key.getColumns().forEach( this::addColumn );
				key.getReferencedColumns().forEach( this::addColumn );
				addContainer( key.getReferencedTable() );
			} );
			table.visitForeignKeyColumns( this::addColumn );
			if ( table instanceof PhysicalTable physical ) {
				physical.getIndexes().values().forEach( index -> {
					index.getSelectables().forEach( this::addSelectable );
					index.visitOrderingSelectables( this::addSelectable );
				} );
			}
			if ( table instanceof DenormalizedTable denormalized ) { addContainer( denormalized.getIncludedTable() ); }
		}
	}

	public void addUserDefinedType(UserDefinedObjectType udt) {
		if ( udts.add( udt ) ) { udt.getColumns().forEach( this::addColumn ); }
	}

	public void validateRename(Column column, PhysicalName replacement) {
		for ( var container : containers ) {
			if ( container.getColumns().stream().anyMatch( member -> member == column ) ) {
				container.validateRename( column, replacement );
			}
			if ( container instanceof Table table ) {
				table.validateColumnRename( column, replacement );
				for ( var key : table.uniqueKeysForNameAttachment() ) {
					validateOrderingRename( key.getColumnOrderMap().keySet(), column, replacement );
				}
				if ( table instanceof PhysicalTable physical ) {
					for ( var index : physical.getIndexes().values() ) {
						validateOrderingRename( index.getSelectableOrderMap().keySet(), column, replacement );
					}
				}
			}
		}
	}

	private void validateOrderingRename(java.util.Collection<? extends Selectable> entries,
			Column column, PhysicalName replacement) {
		final Set<Object> names = new java.util.HashSet<>();
		for ( var selectable : entries ) {
			final Object name = selectable == column ? replacement
					: selectable instanceof Column other ? other.getPhysicalName() : selectable;
			if ( !names.add( name ) ) {
				throw new MappingException( "Column rename collides in constraint/index ordering: " + replacement );
			}
		}
	}

	public void restore(PhysicalName.Factory factory) {
		columns.forEach( column -> column.reattachPhysicalName( factory ) );
		validate();
	}

	public void validate() {
		containers.forEach( container -> {
			container.validateColumnIndex();
			final var members = new java.util.HashMap<PhysicalName, Column>();
			for ( var column : container.getColumns() ) {
				final var previous = members.putIfAbsent( column.getPhysicalName(), column );
				if ( previous != null && previous != column ) {
					throw new MappingException( "Physical column collision between '" + previous.getName()
							+ "' and '" + column.getName() + "' in " + container );
				}
			}
		} );
		for ( var container : containers ) {
			if ( container instanceof Table table ) {
				table.validateForeignKeyIndex();
				table.uniqueKeysForNameAttachment().forEach( UniqueKey::getColumnOrderMap );
				if ( table instanceof PhysicalTable physical ) {
					physical.getIndexes().values().forEach( Index::getSelectableOrderMap );
				}
			}
		}
		for ( var udt : udts ) {
			final Set<PhysicalName> names = new java.util.HashSet<>();
			for ( var column : udt.getColumns() ) {
				if ( !names.add( column.getPhysicalName() ) ) {
					throw new MappingException( "Physical column collision in UDT " + udt.getName() );
				}
			}
		}
	}
}
