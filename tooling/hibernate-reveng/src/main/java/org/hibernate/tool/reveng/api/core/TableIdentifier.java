/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.api.core;

import org.hibernate.mapping.Table;

import java.util.Objects;

/**
 * Identifier used for the full name of tables.
 * @author max
 *
 */
public class TableIdentifier {

	public static TableIdentifier create(Table table) {
		return new TableIdentifier(table.getCatalog(), table.getSchema(), table.getName() );
	}

	public static TableIdentifier create(String catalog, String schema, String name) {
		return new TableIdentifier(catalog, schema, name);
	}

	private final String catalog;
	private final String schema;
	private final String name;

	private TableIdentifier(String catalog, String schema, String name) {
		this.catalog = (catalog==null?null:catalog.intern() );
		this.schema = (schema==null?null:schema.intern() );
		this.name = (name==null?null:name.intern() );
	}

	public String getCatalog() {
		return catalog;
	}
	public String getName() {
		return name;
	}
	public String getSchema() {
		return schema;
	}

	public boolean equals(Object obj) {
		return obj instanceof TableIdentifier
			&& isEqualIdentifier( (TableIdentifier)obj);
	}

	private boolean isEqualIdentifier(TableIdentifier otherIdentifier) {
		if (otherIdentifier==null) return false;
		if (this==otherIdentifier) return true;

		if (Objects.equals(this.name, otherIdentifier.name) ) {
			if(Objects.equals(this.schema, otherIdentifier.schema) ) {
				return Objects.equals(this.catalog, otherIdentifier.catalog);
			}
		}
		return false;
	}

	public int hashCode() {
		int result = 13;
		result = 37 * result + ( name==null ? 0 : name.hashCode() );
		result = 37 * result + ( schema==null ? 0 : schema.hashCode() );
		result = 37 * result + ( catalog==null ? 0 : catalog.hashCode() );

		return result;
	}

	public String toString() {
		StringBuilder buf = new StringBuilder()
					.append( "TableIdentifier" )
					.append('(');
		if ( getCatalog()!=null ) buf.append( getCatalog() ).append( "." );
		if ( getSchema()!=null ) buf.append( getSchema() ).append( "." );
		buf.append( getName() ).append(')');
		return buf.toString();
	}

}
