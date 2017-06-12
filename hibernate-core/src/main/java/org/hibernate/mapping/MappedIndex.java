/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.mapping;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.metamodel.model.relational.spi.ExportableTable;
import org.hibernate.metamodel.model.relational.spi.Index;
import org.hibernate.metamodel.model.relational.spi.PhysicalNamingStrategy;
import org.hibernate.naming.Identifier;

/**
 * A relational table index
 *
 * @author Gavin King
 */
public class MappedIndex implements Serializable {
	private Table table;
	private java.util.List<Column> columns = new ArrayList<>();
	private java.util.Map<Column, String> columnOrderMap = new HashMap<>();
	private Identifier name;

	public Table getTable() {
		return table;
	}

	public void setTable(Table table) {
		this.table = table;
	}

	public int getColumnSpan() {
		return columns.size();
	}

	public Iterator<Column> getColumnIterator() {
		return columns.iterator();
	}

	public java.util.Map<Column, String> getColumnOrderMap() {
		return Collections.unmodifiableMap( columnOrderMap );
	}

	public void addColumn(Column column) {
		if ( !columns.contains( column ) ) {
			columns.add( column );
		}
	}

	public void addColumn(Column column, String order) {
		addColumn( column );
		if ( StringHelper.isNotEmpty( order ) ) {
			columnOrderMap.put( column, order );
		}
	}

	public void addColumns(Iterator extraColumns) {
		while ( extraColumns.hasNext() ) {
			addColumn( (Column) extraColumns.next() );
		}
	}

	public boolean containsColumn(Column column) {
		return columns.contains( column );
	}

	public String getName() {
		return name == null ? null : name.getText();
	}

	public void setName(String name) {
		this.name = Identifier.toIdentifier( name );
	}

	public String getQuotedName(Dialect dialect) {
		return name == null ? null : name.render( dialect );
	}

	@Override
	public String toString() {
		return getClass().getName() + "(" + getName() + ")";
	}

	public Index generateRuntimeIndex(
			ExportableTable runtimeTable,
			PhysicalNamingStrategy namingStrategy,
			JdbcEnvironment jdbcEnvironment) {
		Index index = new Index( name, runtimeTable );
		for ( Column column : columns ) {
			index.addColumn( column.generateRuntimeColumn( runtimeTable, namingStrategy, jdbcEnvironment ) );
		}
		return index;
	}
}
