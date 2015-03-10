/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat Inc. or third-party contributors as
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
package org.hibernate.boot.model.source.internal.hbm;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.boot.model.source.spi.ConstraintSource;
import org.hibernate.internal.util.compare.EqualsHelper;

/**
 * Support for index and unique-key constraint sources.
 *
 * @author Hardy Ferentschik
 * @author Steve Ebersole
 */
class AbstractConstraintSource implements ConstraintSource {
	protected final String name;
	protected final String tableName;
	protected final ArrayList<String> columnNames = new ArrayList<String>();

	protected AbstractConstraintSource(String name, String tableName) {
		assert name != null : "Constraint name was null";
		this.name = name;
		this.tableName = tableName;
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
	
	public void addColumnName( String columnName ) {
		columnNames.add( columnName );
	}

	@Override
	@SuppressWarnings("RedundantIfStatement")
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}

		AbstractConstraintSource that = (AbstractConstraintSource) o;
		return EqualsHelper.equals( name, that.name )
				&& EqualsHelper.equals( tableName, that.tableName )
				&& columnNames.equals( that.columnNames );
	}

	@Override
	public int hashCode() {
		int result = name.hashCode();
		result = 31 * result + ( tableName == null ? 0 : tableName.hashCode() );
		result = 31 * result + ( columnNames.hashCode() );
		return result;
	}
}


