/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.mapping;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.hibernate.boot.model.relational.DenormalizedMappedTable;
import org.hibernate.boot.model.relational.MappedForeignKey;
import org.hibernate.boot.model.relational.MappedIndex;
import org.hibernate.boot.model.relational.MappedNamespace;
import org.hibernate.boot.model.relational.MappedPrimaryKey;
import org.hibernate.boot.model.relational.MappedTable;
import org.hibernate.boot.model.relational.MappedUniqueKey;
import org.hibernate.internal.util.JavaTypeHelper;
import org.hibernate.naming.Identifier;

/**
 * @author Gavin King
 */
@SuppressWarnings("unchecked")
public class DenormalizedTable extends Table implements DenormalizedMappedTable<Column> {
	private final MappedTable<Column> includedTable;

	public DenormalizedTable(
			MappedNamespace namespace,
			Identifier tableName,
			boolean isAbstract,
			MappedTable includedTable) {
		super( namespace, tableName, isAbstract );
		this.includedTable = includedTable;
		includedTable.setHasDenormalizedTables();
	}

	public DenormalizedTable(
			MappedNamespace namespace,
			Identifier tableName,
			String subselectFragment,
			boolean isAbstract,
			MappedTable includedTable) {
		super( namespace, tableName, subselectFragment, isAbstract );
		this.includedTable = includedTable;
		includedTable.setHasDenormalizedTables();
	}

	public DenormalizedTable(
			MappedNamespace namespace,
			String subselect,
			boolean isAbstract,
			MappedTable includedTable) {
		super( namespace, subselect, isAbstract );
		this.includedTable = includedTable;
		includedTable.setHasDenormalizedTables();
	}

	@Override
	public void createForeignKeys() {
		includedTable.createForeignKeys();
		for ( MappedForeignKey fk : includedTable.getForeignKeys() ) {
			createForeignKey(
					Constraint.generateName(
							fk.generatedConstraintNamePrefix(),
							this,
							fk.getColumns()
					),
					fk.getColumns(),
					fk.getReferencedEntityName(),
					fk.getKeyDefinition(),
					fk.getReferencedColumns()
			);
		}
	}

	@Override
	public Column getColumn(Column column) {
		Column superColumn = super.getColumn( column );
		if ( superColumn != null ) {
			return superColumn;
		}
		else {
			return includedTable.getColumn( column );
		}
	}

	@Override
	public Column getColumn(Identifier name) {
		Column superColumn = super.getColumn( name );
		if ( superColumn != null ) {
			return superColumn;
		}
		else {
			return includedTable.getColumn( name );
		}
	}

	@Override
	public Iterator getColumnIterator() {
		return getMappedColumns().iterator();
	}

	@Override
	public Set<Column> getMappedColumns() {
		Set<Column> mappedColumns = new HashSet<>();
		mappedColumns.addAll( includedTable.getMappedColumns() );
		mappedColumns.addAll( super.getMappedColumns() );
		return Collections.unmodifiableSet( mappedColumns );
	}

	@Override
	public boolean containsColumn(Column column) {
		return super.containsColumn( column ) || includedTable.containsColumn( column );
	}

	@Override
	public MappedPrimaryKey getPrimaryKey() {
		return includedTable.getPrimaryKey();
	}

	@Override
	public Iterator getUniqueKeyIterator() {
		return getUniqueKeys().iterator();
	}

	@Override
	public Collection<MappedUniqueKey> getUniqueKeys() {
		includedTable.getUniqueKeys().forEach( uniqueKey -> createUniqueKey( JavaTypeHelper.cast( uniqueKey.getColumns() ) ) );
		return super.getUniqueKeys();
	}

	@Override
	public Iterator getIndexIterator() {
		return getIndexes().iterator();
	}

	@Override
	public Collection<MappedIndex> getIndexes() {
		final List<MappedIndex> indexes = new ArrayList<>(  );
		indexes.addAll(includedTable.getIndexes()  );
		indexes.addAll( super.getIndexes() );
		return indexes;
	}

	/**
	 * @deprecated since 6.0, use {@link #getIncludedMappedTable()}.
	 */
	@Deprecated
	public Table getIncludedTable() {
		return (Table) getIncludedMappedTable();
	}

	public MappedTable getIncludedMappedTable() {
		return includedTable;
	}
}
