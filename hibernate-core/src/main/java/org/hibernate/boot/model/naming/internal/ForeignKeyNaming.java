/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.naming.internal;

import java.util.ArrayList;
import org.hibernate.MappingException;
import org.hibernate.boot.mapping.internal.relational.RelationalModelCorrespondences;
import org.hibernate.boot.model.naming.spi.ForeignKeyColumnNamingInput;
import org.hibernate.boot.model.naming.spi.ForeignKeyNamingInput;
import org.hibernate.boot.model.naming.spi.InlineViewNamingInput;
import org.hibernate.boot.model.naming.spi.NamedTableNamingInput;
import org.hibernate.boot.model.naming.spi.NamingNamePair;
import org.hibernate.boot.model.naming.spi.TableNamingInput;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.mapping.InlineView;
import org.hibernate.mapping.NamedTable;
import org.hibernate.mapping.Table;
import org.hibernate.relational.naming.spi.LogicalName;

/// Finalizes foreign-key names from retained logical and physical dependencies.
///
/// @author Steve Ebersole
public final class ForeignKeyNaming {
	private ForeignKeyNaming() {}

	public static void finish(ForeignKey key, MetadataBuildingContext context) {
		final var names = context.getMetadataCollector().getRelationalModelCorrespondences();
		if ( names.isForeignKeyNamed( key ) ) {
			return;
		}
		final var explicitName = key.getName();
		final LogicalName logical;
		if ( explicitName != null && !explicitName.isEmpty() ) {
			logical = context.getMetadataCollector().getDatabase().toLogicalName( explicitName, true );
		}
		else {
			logical = context.getBuildingPlan().getImplicitNamingStrategy().determineForeignKeyName(
					input( key, names ), ImplicitNamingContextImpl.from( context ) );
		}
		key.setName( ConstraintNamingHelper.resolveLogical( logical, ConstraintNamingHelper.Kind.FOREIGN_KEY, context ) );
		names.markForeignKeyNamed( key );
	}

	private static ForeignKeyNamingInput input(ForeignKey key, RelationalModelCorrespondences names) {
		final var targetTable = key.getReferencedTable();
		final var primaryKey = targetTable.getPrimaryKey();
		final var targetColumns = key.isReferenceToPrimaryKey()
				? primaryKey == null ? java.util.List.<Column>of() : primaryKey.getColumns()
				: key.getReferencedColumns();
		if ( key.getColumns().size() != targetColumns.size() ) {
			throw new MappingException( "Unresolved foreign key column correspondence for " + key.getTable().getName() );
		}
		final var pairs = new ArrayList<ForeignKeyColumnNamingInput>( targetColumns.size() );
		for ( int i = 0; i < targetColumns.size(); i++ ) {
			final var target = targetColumns.get( i );
			final var local = key.getColumn( i );
			final var selected = names.referenceName( key, target );
			final var referenceName = selected == null ? names.columnNames().findReferenceName( local, target ) : selected;
			pairs.add( new ForeignKeyColumnNamingInput(
					column( key.getTable(), local, null, names ),
					column( targetTable, target, referenceName, names ) ) );
		}
		final boolean referencesPrimaryKey = primaryKey != null
				&& targetColumns.size() == primaryKey.getColumns().size()
				&& targetColumns.containsAll( primaryKey.getColumns() );
		return new ForeignKeyNamingInput( table( key.getTable(), names.foreignKeyTableName( key, false ), names ),
				table( targetTable, names.foreignKeyTableName( key, true ), names ), referencesPrimaryKey, pairs );
	}

	private static TableNamingInput table(Table table, LogicalName selected, RelationalModelCorrespondences names) {
		if ( table instanceof InlineView view ) {
			return new InlineViewNamingInput( view.getLogicalName() );
		}
		final var logical = selected == null ? names.tableName( table ) : selected;
		if ( logical == null ) {
			throw new MappingException( "No logical table dependency for foreign key naming: " + table.getName() );
		}
		return new NamedTableNamingInput( new NamingNamePair( logical, ((NamedTable) table).getPhysicalName().objectName() ) );
	}

	private static NamingNamePair column(Table table, Column column, LogicalName selected, RelationalModelCorrespondences names) {
		final var logical = selected == null ? names.columnNames().findDeclarationName( table, column ) : selected;
		if ( logical == null ) {
			throw new MappingException( "No logical column dependency for foreign key naming: " + table.getName() + "." + column.getName() );
		}
		return new NamingNamePair( logical, column.getPhysicalName() );
	}
}
