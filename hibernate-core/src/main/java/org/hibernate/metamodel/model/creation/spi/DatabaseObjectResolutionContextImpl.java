/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.creation.spi;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.boot.model.relational.MappedColumn;
import org.hibernate.boot.model.relational.MappedTable;
import org.hibernate.cfg.NotYetImplementedException;
import org.hibernate.mapping.Selectable;
import org.hibernate.metamodel.model.relational.spi.Column;
import org.hibernate.metamodel.model.relational.spi.RuntimeDatabaseModelProducer;
import org.hibernate.metamodel.model.relational.spi.ForeignKey;
import org.hibernate.metamodel.model.relational.spi.Table;

/**
 * @author Steve Ebersole
 */
public class DatabaseObjectResolutionContextImpl
		implements DatabaseObjectResolver, RuntimeDatabaseModelProducer.Callback {

	private final Map<MappedTable, Table> tableMap = new HashMap<>();
	private final Map<MappedColumn, Column> columnMap = new HashMap<>();
	private final Map<ColumnMapping,ForeignKey> foreignKeyMap = new HashMap<>();

	@Override
	public void tableBuilt(MappedTable mappedTable, Table table) {
		tableMap.put( mappedTable, table );
	}

	@Override
	public void columnBuilt(MappedColumn mappedColumn, Column column) {
		columnMap.put( mappedColumn, column );
	}

	@Override
	public void foreignKeyBuilt(org.hibernate.mapping.ForeignKey mappedFk, ForeignKey runtimeFk) {

	}

	@Override
	public Table resolveTable(MappedTable mappedTable) {
		return tableMap.get( mappedTable );
	}

	@Override
	public Column resolveColumn(MappedColumn mappedColumn) {
		return columnMap.get( mappedColumn );
	}

	@Override
	public ForeignKey.ColumnMappings resolveColumnMappings(
			List<Selectable> columns, List<Selectable> otherColumns) {
		throw new NotYetImplementedException(  );
	}

	private static class ColumnMapping {
		List<MappedColumn> referringColumns;
		List<MappedColumn> referencedColumns;
	}
}
