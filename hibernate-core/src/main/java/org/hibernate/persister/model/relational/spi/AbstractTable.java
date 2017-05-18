/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.persister.model.relational.spi;

import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

import org.hibernate.persister.model.relational.internal.InflightTable;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractTable implements InflightTable {
	private final PrimaryKey primaryKey = new PrimaryKey( this );
	private final Map<String,Column> columnMap = new TreeMap<>( String.CASE_INSENSITIVE_ORDER );
	private final boolean isAbstract;

	public AbstractTable(boolean isAbstract) {
		this.isAbstract = isAbstract;
	}

	@Override
	public PrimaryKey getPrimaryKey() {
		return primaryKey;
	}

	@Override
	public boolean isAbstract() {
		return isAbstract;
	}

	@Override
	public void addColumn(Column column, InflightTable runtimeTable) {
		columnMap.put( column.getExpression(), column );
	}

	@Override
	public Column getColumn(String name) {
		return columnMap.get( name );
	}

	@Override
	public Collection<Column> getColumns() {
		return columnMap.values();
	}
}
