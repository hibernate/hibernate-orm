/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.metamodel.source.annotations.entity;

import org.hibernate.metamodel.source.binder.TableSource;

class TableSourceImpl implements TableSource {
	private final String schema;
	private final String catalog;
	private final String tableName;
	private final String logicalName;

	TableSourceImpl(String schema, String catalog, String tableName, String logicalName) {
		this.schema = schema;
		this.catalog = catalog;
		this.tableName = tableName;
		this.logicalName = logicalName;
	}

	@Override
	public String getExplicitSchemaName() {
		return schema;
	}

	@Override
	public String getExplicitCatalogName() {
		return catalog;
	}

	@Override
	public String getExplicitTableName() {
		return tableName;
	}

	@Override
	public String getLogicalName() {
		return logicalName;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}

		TableSourceImpl that = (TableSourceImpl) o;

		if ( catalog != null ? !catalog.equals( that.catalog ) : that.catalog != null ) {
			return false;
		}
		if ( logicalName != null ? !logicalName.equals( that.logicalName ) : that.logicalName != null ) {
			return false;
		}
		if ( schema != null ? !schema.equals( that.schema ) : that.schema != null ) {
			return false;
		}
		if ( tableName != null ? !tableName.equals( that.tableName ) : that.tableName != null ) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		int result = schema != null ? schema.hashCode() : 0;
		result = 31 * result + ( catalog != null ? catalog.hashCode() : 0 );
		result = 31 * result + ( tableName != null ? tableName.hashCode() : 0 );
		result = 31 * result + ( logicalName != null ? logicalName.hashCode() : 0 );
		return result;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append( "TableSourceImpl" );
		sb.append( "{schema='" ).append( schema ).append( '\'' );
		sb.append( ", catalog='" ).append( catalog ).append( '\'' );
		sb.append( ", tableName='" ).append( tableName ).append( '\'' );
		sb.append( ", logicalName='" ).append( logicalName ).append( '\'' );
		sb.append( '}' );
		return sb.toString();
	}
}


