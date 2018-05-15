/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.creation.spi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.boot.model.relational.MappedColumn;
import org.hibernate.boot.model.relational.MappedForeignKey;
import org.hibernate.boot.model.relational.MappedTable;
import org.hibernate.mapping.Selectable;
import org.hibernate.metamodel.model.relational.spi.Column;
import org.hibernate.metamodel.model.relational.spi.ForeignKey;
import org.hibernate.metamodel.model.relational.spi.RuntimeDatabaseModelProducer;
import org.hibernate.metamodel.model.relational.spi.Table;

/**
 * @author Steve Ebersole
 */
public class DatabaseObjectResolutionContextImpl
		implements DatabaseObjectResolver, RuntimeDatabaseModelProducer.Callback {

	private final Map<MappedTable, Table> runtimeTableByBootTable = new HashMap<>();
	private final Map<MappedColumn, Column> columnMap = new HashMap<>();

	private final Map<MappedForeignKey, ForeignKey> foreignKeyMap = new IdentityHashMap<>();
	// Product.vendor
	//		or T_PRODUCT->VENDOR
	// T_PRODUCT->PRODUCT_SUPP

	@Override
	public void tableBuilt(MappedTable mappedTable, Table table) {
		runtimeTableByBootTable.put( mappedTable, table );
	}

	@Override
	public void columnBuilt(MappedColumn mappedColumn, Column column) {
		columnMap.put( mappedColumn, column );
	}

	@Override
	public void foreignKeyBuilt(MappedForeignKey mappedFk, ForeignKey runtimeFk) {
		foreignKeyMap.put( mappedFk, runtimeFk );
	}

	@Override
	public Table resolveTable(MappedTable mappedTable) {
		return runtimeTableByBootTable.get( mappedTable );
	}

	@Override
	public Column resolveColumn(MappedColumn mappedColumn) {
		return columnMap.get( mappedColumn );
	}

	@Override
	public ForeignKey resolveForeignKey(MappedForeignKey bootForeignKey) {
		return foreignKeyMap.get( bootForeignKey );
	}

	@Override
	public ForeignKey.ColumnMappings resolveColumnMappings(
			List<Selectable> columns,
			List<Selectable> otherColumns) {
		if ( columns == null || columns.isEmpty() ) {
			throw new IllegalArgumentException( "`columns` was null or empty" );
		}

		if ( otherColumns == null || otherColumns.isEmpty() ) {
			throw new IllegalArgumentException( "`otherColumns` was null or empty" );
		}

		if ( columns.size() != otherColumns.size() ) {
			throw new IllegalArgumentException( "`columns` and `otherColumns` had different sizes" );
		}

		Table referencingTable = null;
		Table targetTable = null;

		final ArrayList<Column> referencingColumns = new ArrayList<>();
		final ArrayList<Column> targetColumns = new ArrayList<>();

		for ( int i = 0; i < columns.size(); i++ ) {
			final MappedColumn bootReferencingColumn = columns.get( i );
			final MappedColumn bootTargetColumn = otherColumns.get( i );

			final Column referencingColumn = resolveColumn( bootReferencingColumn );
			final Column targetColumn = resolveColumn( bootTargetColumn );

			if ( referencingTable == null ) {
				assert targetTable == null;

				referencingTable = referencingColumn.getSourceTable();
				targetTable = targetColumn.getSourceTable();
			}

			referencingColumns.add( referencingColumn );
			targetColumns.add( targetColumn );
		}

		ForeignKey matchedFk = null;
		fk_loop: for ( ForeignKey foreignKey : referencingTable.getForeignKeys() ) {
			final ForeignKey.ColumnMappings mappings = foreignKey.getColumnMappings();
			for ( ForeignKey.ColumnMappings.ColumnMapping columnMapping : mappings.getColumnMappings() ) {
				final int matchedPosition = referencingColumns.indexOf( columnMapping.getReferringColumn() );
				if ( matchedPosition == -1 ) {
					continue fk_loop;
				}

				final Column correspondingTargetColumn = targetColumns.get( matchedPosition );
				if ( !columnMapping.getTargetColumn().equals( correspondingTargetColumn ) ) {
					continue fk_loop;
				}
			}

			matchedFk = foreignKey;
			break;
		}

		if ( matchedFk == null ) {
			// todo (6.0) : how to best handle this situation
			//		for now, the very hacky way
//			matchedFk = ( (InflightTable) referencingTable ).createForeignKey(
//
//			)
		}

		return matchedFk.getColumnMappings();
	}

}
