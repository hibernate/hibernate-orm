/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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


