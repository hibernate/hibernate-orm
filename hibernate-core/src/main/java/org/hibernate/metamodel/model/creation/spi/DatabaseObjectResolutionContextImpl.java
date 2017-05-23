/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.creation.spi;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.boot.model.relational.MappedColumn;
import org.hibernate.boot.model.relational.MappedTable;
import org.hibernate.metamodel.model.relational.spi.Column;
import org.hibernate.metamodel.model.relational.spi.DatabaseModelProducer;
import org.hibernate.metamodel.model.relational.spi.Table;

/**
 * @author Steve Ebersole
 */
public class DatabaseObjectResolutionContextImpl
		implements DatabaseObjectResolver, DatabaseModelProducer.Callback {
	final Map<MappedTable, Table> tableMap = new HashMap<>();
	final Map<MappedColumn, Column> columnMap = new HashMap<>();

	@Override
	public void tableBuilt(MappedTable mappedTable, Table table) {
		tableMap.put( mappedTable, table );
	}

	@Override
	public void columnBuilt(MappedColumn mappedColumn, Column column) {
		columnMap.put( mappedColumn, column );
	}

	@Override
	public Table resolveTable(MappedTable mappedTable) {
		return tableMap.get( mappedTable );
	}

	@Override
	public Column resolveColumn(MappedColumn mappedColumn) {
		return columnMap.get( mappedColumn );
	}
}
