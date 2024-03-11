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

import org.hibernate.Internal;
import org.hibernate.boot.internal.ForeignKeyNameSource;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.Namespace;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.internal.util.collections.JoinedList;

/**
 * @author Gavin King
 */
public class DenormalizedTable extends Table {

	private final Table includedTable;
	private List<Column> reorderedColumns;

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
	public void createForeignKeys(MetadataBuildingContext context) {
		includedTable.createForeignKeys( context );
		for ( ForeignKey foreignKey : includedTable.getForeignKeys().values() ) {
			final PersistentClass referencedClass =
					foreignKey.resolveReferencedClass( context.getMetadataCollector() );
			// the ForeignKeys created in the first pass did not have their referenced table initialized
			if ( foreignKey.getReferencedTable() == null ) {
				foreignKey.setReferencedTable( referencedClass.getTable() );
			}
			createForeignKey(
					context.getBuildingOptions()
							.getImplicitNamingStrategy()
							.determineForeignKeyName( new ForeignKeyNameSource( foreignKey, this, context ) )
							.render( context.getMetadataCollector().getDatabase().getDialect() ),
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

	@Override
	public Collection<Column> getColumns() {
		if ( reorderedColumns != null ) {
			return reorderedColumns;
		}
		return new JoinedList<>( new ArrayList<>( includedTable.getColumns() ), new ArrayList<>( super.getColumns() ) );
	}

	@Override
	public boolean containsColumn(Column column) {
		return super.containsColumn( column ) || includedTable.containsColumn( column );
	}

	@Override
	public PrimaryKey getPrimaryKey() {
		return includedTable.getPrimaryKey();
	}

	@Override @Deprecated(forRemoval = true)
	public Iterator<UniqueKey> getUniqueKeyIterator() {
		if ( !includedTable.isPhysicalTable() ) {
			for ( UniqueKey uniqueKey : includedTable.getUniqueKeys().values() ) {
				createUniqueKey( uniqueKey.getColumns() );
			}
		}
		return getUniqueKeys().values().iterator();
	}

	public Table getIncludedTable() {
		return includedTable;
	}

	@Internal
	@Override
	public void reorderColumns(List<Column> columns) {
		assert includedTable.getColumns().size() + super.getColumns().size() == columns.size()
				&& columns.containsAll( includedTable.getColumns() )
				&& columns.containsAll( super.getColumns() )
				&& reorderedColumns == null;
		this.reorderedColumns = columns;
	}
}
