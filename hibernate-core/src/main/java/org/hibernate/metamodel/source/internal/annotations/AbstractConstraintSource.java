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
package org.hibernate.metamodel.source.internal.annotations;

import java.util.List;

import org.hibernate.metamodel.source.spi.ConstraintSource;

/**
 * @author Hardy Ferentschik
 */
class AbstractConstraintSource implements ConstraintSource {
	protected final String name;
	protected final String tableName;
	protected final List<String> columnNames;
	protected final List<String> orderings;

	protected AbstractConstraintSource(String name, String tableName, List<String> columnNames, List<String> orderings) {
		this.name = name;
		this.tableName = tableName;
		this.columnNames = columnNames;
		this.orderings = orderings;
	}

	@Override
	public String name() {
		return name;
	}

	@Override
	public String getTableName() {
		return tableName;
	}

	@Override
	public List<String> columnNames() {
		return columnNames;
	}
	
	public List<String> orderings() {
		return orderings;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}

		AbstractConstraintSource that = (AbstractConstraintSource) o;

		if ( columnNames != null ? !columnNames.equals( that.columnNames ) : that.columnNames != null ) {
			return false;
		}
		if ( orderings != null ? !orderings.equals( that.orderings ) : that.orderings != null ) {
			return false;
		}
		if ( name != null ? !name.equals( that.name ) : that.name != null ) {
			return false;
		}
		if ( tableName != null ? !tableName.equals( that.tableName ) : that.tableName != null ) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		int result = name != null ? name.hashCode() : 0;
		result = 31 * result + ( tableName != null ? tableName.hashCode() : 0 );
		result = 31 * result + ( columnNames != null ? columnNames.hashCode() : 0 );
		result = 31 * result + ( orderings != null ? orderings.hashCode() : 0 );
		return result;
	}
}


