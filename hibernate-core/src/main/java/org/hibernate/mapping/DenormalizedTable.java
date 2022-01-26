/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.mapping;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.Namespace;
import org.hibernate.internal.util.collections.JoinedIterator;
import org.hibernate.internal.util.collections.JoinedList;

/**
 * @author Gavin King
 */
public class DenormalizedTable extends Table {

	private final Table includedTable;

	public DenormalizedTable(
			String contributor,
			Namespace namespace,
			Identifier physicalTableName,
			boolean isAbstract,
			Table includedTable) {
		super( contributor, namespace, physicalTableName, isAbstract );
		this.includedTable = includedTable;
		includedTable.setHasDenormalizedTables();
	}

	public DenormalizedTable(
			String contributor,
			Namespace namespace,
			Identifier physicalTableName,
			String subselectFragment,
			boolean isAbstract,
			Table includedTable) {
		super( contributor, namespace, physicalTableName, subselectFragment, isAbstract );
		this.includedTable = includedTable;
		includedTable.setHasDenormalizedTables();
	}

	public DenormalizedTable(
			String contributor,
			Namespace namespace,
			String subselect,
			boolean isAbstract,
			Table includedTable) {
		super( contributor, namespace, subselect, isAbstract );
		this.includedTable = includedTable;
		includedTable.setHasDenormalizedTables();
	}

	@Override
	public void createForeignKeys() {
		includedTable.createForeignKeys();
		for ( ForeignKey foreignKey : includedTable.getForeignKeys().values() ) {
			createForeignKey(
					Constraint.generateName(
							foreignKey.generatedConstraintNamePrefix(),
							this,
							foreignKey.getColumns()
					),
					foreignKey.getColumns(),
					foreignKey.getReferencedEntityName(),
					foreignKey.getKeyDefinition(),
					foreignKey.getReferencedColumns()
			);
		}
	}

	@Override
	public Column getColumn(Column column) {
		Column superColumn = super.getColumn( column );
		return superColumn != null ? superColumn : includedTable.getColumn(column);
	}

	public Column getColumn(Identifier name) {
		Column superColumn = super.getColumn( name );
		return superColumn != null ? superColumn : includedTable.getColumn(name);
	}

	@Override @Deprecated
	public Iterator<Column> getColumnIterator() {
		return new JoinedIterator<>(
				includedTable.getColumnIterator(),
				super.getColumnIterator()
		);
	}

	@Override
	public Collection<Column> getColumns() {
		return new JoinedList<>(
				new ArrayList<>( includedTable.getColumns() ),
				new ArrayList<>( super.getColumns() )
		);
	}

	@Override
	public boolean containsColumn(Column column) {
		return super.containsColumn( column ) || includedTable.containsColumn( column );
	}

	@Override
	public PrimaryKey getPrimaryKey() {
		return includedTable.getPrimaryKey();
	}

	@Override
	public Iterator<UniqueKey> getUniqueKeyIterator() {
		if ( !includedTable.isPhysicalTable() ) {
			for ( UniqueKey uniqueKey : includedTable.getUniqueKeys().values() ) {
				createUniqueKey( uniqueKey.getColumns() );
			}
		}
		return getUniqueKeys().values().iterator();
	}

	@Override
	public Iterator<Index> getIndexIterator() {
		List<Index> indexes = new ArrayList<>();
		Iterator<Index> iter = includedTable.getIndexIterator();
		while ( iter.hasNext() ) {
			Index parentIndex = iter.next();
			Index index = new Index();
			index.setName( getName() + parentIndex.getName() );
			index.setTable( this );
			index.addColumns( parentIndex.getColumnIterator() );
			indexes.add( index );
		}
		return new JoinedIterator<>(
				indexes.iterator(),
				super.getIndexIterator()
		);
	}

	public Table getIncludedTable() {
		return includedTable;
	}
}
