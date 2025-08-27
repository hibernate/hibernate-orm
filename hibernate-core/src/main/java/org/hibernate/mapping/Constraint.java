/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.mapping;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.MappingException;
import org.hibernate.boot.model.relational.Exportable;

/**
 * A mapping model object representing a constraint on a relational database table.
 *
 * @author Gavin King
 * @author Brett Meyer
 */
public abstract class Constraint implements Exportable, Serializable {

	private String name;
	private final ArrayList<Column> columns = new ArrayList<>();
	private Table table;
	private String options = "";

	Constraint() {}

	Constraint(Table table) {
		this.table = table;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getOptions() {
		return options;
	}

	public void setOptions(String options) {
		this.options = options;
	}

	public void addColumn(Column column) {
		if ( !columns.contains( column ) ) {
			columns.add( column );
		}
	}

	public void addColumns(Value value) {
		for ( Selectable selectable : value.getSelectables() ) {
			if ( selectable.isFormula() ) {
				throw new MappingException( "constraint involves a formula: " + name );
			}
			else {
				addColumn( (Column) selectable );
			}
		}
	}

	/**
	 * @return true if this constraint already contains a column with same name.
	 */
	public boolean containsColumn(Column column) {
		return columns.contains( column );
	}

	public int getColumnSpan() {
		return columns.size();
	}

	public Column getColumn(int i) {
		return columns.get( i );
	}

	public Table getTable() {
		return table;
	}

	@Deprecated(since = "7")
	public void setTable(Table table) {
		this.table = table;
	}

	public List<Column> getColumns() {
		return columns;
	}

	public String toString() {
		return getClass().getSimpleName() + '(' + getTable().getName() + getColumns() + ") as " + name;
	}
}
