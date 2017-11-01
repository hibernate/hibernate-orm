/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.relational.spi;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.hibernate.metamodel.model.relational.internal.InflightTable;
import org.hibernate.naming.Identifier;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractTable implements InflightTable {
	private PrimaryKey primaryKey = null;
	private List<ForeignKey> foreignKeys = new ArrayList<>();
	private List<UniqueKey> uniqueKeys = new ArrayList<>();
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
	public void addColumn(Column column) {
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

	@Override
	public Collection<ForeignKey> getForeignKeys() {
		return foreignKeys;
	}

	@Override
	public Collection<UniqueKey> getUniqueKeys() {
		return uniqueKeys;
	}

	@Override
	public ForeignKey createForeignKey(
			String name,
			boolean export,
			String keyDefinition,
			boolean cascadeDeleteEnabled,
			Table targetTable,
			ForeignKey.ColumnMappings columnMappings) {
		final ForeignKey foreignKey = new ForeignKey(
				name,
				export,
				keyDefinition,
				cascadeDeleteEnabled,
				this,
				targetTable,
				columnMappings
		);
		foreignKeys.add( foreignKey );
		return foreignKey;
	}

	@Override
	public UniqueKey createUniqueKey(String name) {
		final UniqueKey uniqueKey = new UniqueKey( Identifier.toIdentifier( name ), this );
		uniqueKeys.add( uniqueKey );
		return uniqueKey;
	}

	@Override
	public void addPrimaryKey(PrimaryKey primaryKey) {
		this.primaryKey = primaryKey;
	}

	@Override
	public boolean hasPrimaryKey() {
		return primaryKey != null;
	}
}
