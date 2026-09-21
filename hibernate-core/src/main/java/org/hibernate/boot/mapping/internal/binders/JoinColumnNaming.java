/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.binders;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.hibernate.MappingException;
import org.hibernate.boot.mapping.internal.context.BindingState;
import org.hibernate.boot.model.naming.internal.ImplicitNamingContextImpl;
import org.hibernate.boot.model.naming.internal.ImplicitNamingHelper;
import org.hibernate.boot.model.naming.spi.EntityNamingInput;
import org.hibernate.boot.model.naming.spi.ImplicitNamingContext;
import org.hibernate.boot.model.naming.spi.InlineViewNamingInput;
import org.hibernate.boot.model.naming.spi.JoinColumnNamingInput;
import org.hibernate.boot.model.naming.spi.NamedTableNamingInput;
import org.hibernate.boot.model.naming.spi.NamingNamePair;
import org.hibernate.boot.model.naming.spi.ReferencedColumnsNamingInput;
import org.hibernate.boot.model.naming.spi.TableNamingInput;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.InlineView;
import org.hibernate.mapping.NamedTable;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Table;
import org.hibernate.relational.naming.spi.LogicalName;

/// Creates settled join-column dependency snapshots at the binding boundary.
///
/// @author Steve Ebersole
final class JoinColumnNaming {
	private JoinColumnNaming() {}

	static boolean hasDerivedIdentifier(EntityTypeBinder binder) {
		final boolean[] found = { false };
		org.hibernate.boot.mapping.internal.categorize.AbstractIdentifiableTypeMetadata type = binder.getManagedType();
		while ( type != null && !found[0] ) {
			type.forEachAttribute( (index, attribute) -> {
				if ( attribute.getMember().hasDirectAnnotationUsage( jakarta.persistence.MapsId.class ) ) {
					found[0] = true;
				}
			} );
			type = type.getSuperType();
		}

		return found[0];
	}

	static boolean requiresDeferred(EntityTypeBinder target, List<String> localNames, List<String> referencedNames, BindingState state) {
		if ( !localNames.isEmpty() && localNames.stream().allMatch( name -> name != null && !name.isEmpty() ) ) {
			return false;
		}
		if ( hasDerivedIdentifier( target ) ) {
			return true;
		}
		for ( String name : referencedNames ) {
			if ( name != null && !name.isEmpty() && state.getRelationalModelCorrespondences().columnNames().findPhysicalColumn(
					target.getTypeBinding().getTable(), state.getDatabase().toLogicalName( name ) ) == null ) {
				return true;
			}
		}
		return false;
	}

	static EntityNamingInput entity(PersistentClass entity) {
		return new EntityNamingInput( entity.getClassName(), entity.getEntityName(), entity.getJpaEntityName() );
	}

	static ImplicitNamingContext context(BindingState state) {
		return ImplicitNamingContextImpl.forPhysicalNaming( state.getMetadataBuildingContext() );
	}

	static TableNamingInput table(Table table, PersistentClass entity, BindingState state) {
		if ( table instanceof InlineView view ) {
			return new InlineViewNamingInput( view.getLogicalName() );
		}
		final org.hibernate.boot.mapping.internal.relational.TableReference[] selected = { null };
		state.forEachType( (name, binder) -> {
			if ( binder instanceof EntityTypeBinder entityBinder && entityBinder.getTypeBinding() == entity ) {
				final var owned = state.getTableByOwner( entityBinder.getManagedType() );
				if ( owned != null && owned.binding() == table ) {
					selected[0] = owned;
				}
			}
		} );
		final var reference = selected[0] == null ? state.getTableByBinding( table ) : selected[0];
		if ( reference == null ) {
			throw new MappingException( "No logical table binding for join-column naming: " + table.getName() );
		}
		return new NamedTableNamingInput( new NamingNamePair( reference.logicalName(),
				((NamedTable) table).getPhysicalName().objectName() ) );
	}

	static ReferencedColumnsNamingInput reference(
			PersistentClass entity, Table fallbackTable, List<Column> columns, List<String> referencedNames, int position, BindingState state) {
		final int count = referencedNames.isEmpty() ? columns.size() : referencedNames.size();
		final var names = new ArrayList<NamingNamePair>( count );
		Table table = fallbackTable;
		final var correspondence = state.getRelationalModelCorrespondences().columnNames();
		for ( int i = 0; i < count; i++ ) {
			final String selected = referencedNames.isEmpty() ? null : referencedNames.get( i );
			Column column = i < columns.size() ? columns.get( i ) : null;
			if ( selected != null && !selected.isEmpty() ) {
				final var selectedName = state.getDatabase().toLogicalName( selected );
				final var selectedColumn = correspondence.findPhysicalColumn( fallbackTable, selectedName );
				if ( selectedColumn != null ) {
					column = selectedColumn;
				}
				else if ( column == null || !correspondence.matches( column, selectedName ) ) {
					column = null;
					if ( column == null ) {
						for ( var join : entity.getJoins() ) {
							column = correspondence.findPhysicalColumn( join.getTable(), selectedName );
							if ( column != null ) {
								break;
							}
						}
					}
				}
			}
			if ( column == null ) {
				throw new MappingException( "Unresolved referenced column for join-column naming: " + selected );
			}
			final var container = column.getValue() == null ? fallbackTable : column.getValue().getColumnContainer();
			if ( i == position && container instanceof Table referencedTable ) {
				table = referencedTable;
			}
			final LogicalName logical = selected != null && !selected.isEmpty()
					? correspondence.selectReferenceName( container, column, state.getDatabase().toLogicalName( selected ) )
					: correspondence.findDeclarationName( container, column );
			if ( logical == null ) {
				throw new MappingException( "No logical column declaration for join-column naming: " + column.getName() );
			}
			names.add( new NamingNamePair( logical, column.getPhysicalName() ) );
		}
		return new ReferencedColumnsNamingInput( table( table, entity, state ), names, position );
	}

	static Supplier<String> toOne(PersistentClass owner, PersistentClass target, String path,
			Table targetTable, List<Column> columns, List<String> referencedNames, int position, BindingState state) {
		return toOne( owner, target, path, targetTable, columns, referencedNames, position, state, false );
	}

	static Supplier<String> toOne(PersistentClass owner, PersistentClass target, String path,
			Table targetTable, List<Column> columns, List<String> referencedNames, int position, BindingState state, boolean associationTable) {
		return ImplicitNamingHelper.once( () -> {
			final var strategy = state.getMetadataBuildingContext().getBuildingPlan().getImplicitNamingStrategy();
			final var dependency = reference( target, targetTable, columns, referencedNames, position, state );
			return associationTable
					? strategy.determineAssociationKeyColumnName(
							new org.hibernate.boot.model.naming.spi.AssociationKeyNamingInput( entity( owner ), entity( target ), path, dependency ), context( state ) )
					: strategy.determineJoinColumnName(
							new JoinColumnNamingInput( entity( owner ), entity( target ), path, dependency ), context( state ) );
		}, "association join column" );
	}
}
