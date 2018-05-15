/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.relational.spi;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

import org.hibernate.metamodel.model.relational.internal.InflightTable;
import org.hibernate.naming.Identifier;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractTable implements InflightTable {
	private final UUID uuid;
	private final boolean isAbstract;

	private PrimaryKey primaryKey = null;
	private Set<ForeignKey> foreignKeys = new HashSet<>();
	private Set<UniqueKey> uniqueKeys = new HashSet<>();

	private final Map<String,Column> columnMap = new TreeMap<>( String.CASE_INSENSITIVE_ORDER );


	public AbstractTable(UUID uuid, boolean isAbstract) {
		this.uuid = uuid;
		this.isAbstract = isAbstract;
	}

	@Override
	public UUID getUid() {
		return uuid;
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
			boolean isReferenceToPrimaryKey,
			Table targetTable,
			ForeignKey.ColumnMappings columnMappings) {
		final ForeignKey foreignKey = new ForeignKey(
				name,
				export,
				keyDefinition,
				cascadeDeleteEnabled,
				isReferenceToPrimaryKey,
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
