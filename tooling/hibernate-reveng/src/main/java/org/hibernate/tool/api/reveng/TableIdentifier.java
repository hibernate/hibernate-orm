/*
 * Hibernate Tools, Tooling for your Hibernate Projects
 *
 * Copyright 2010-2025 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hibernate.tool.api.reveng;

import org.hibernate.mapping.Table;

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
		return obj instanceof TableIdentifier && equals( (TableIdentifier)obj);
	}
	
	public boolean equals(TableIdentifier otherIdentifier) {
		if (otherIdentifier==null) return false;
		if (this==otherIdentifier) return true;
		
		if (equals(this.name, otherIdentifier.name) ) {
			if(equals(this.schema, otherIdentifier.schema) ) {
				return equals(this.catalog, otherIdentifier.catalog);
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
	
	private boolean equals(String left, String right) {
		if (left==right) return true;
		if (left==null || right==null) return false;
		return left.equals(right);
	}
	
	public String toString() {
		StringBuffer buf = new StringBuffer()
					.append( "TableIdentifier" )
					.append('(');
		if ( getCatalog()!=null ) buf.append( getCatalog() + "." );
		if ( getSchema()!=null ) buf.append( getSchema()+ ".");
		buf.append( getName() ).append(')');
		return buf.toString();
	}
	
}
