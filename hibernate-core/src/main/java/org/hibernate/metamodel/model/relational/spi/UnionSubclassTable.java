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

import org.hibernate.MappingException;
import org.hibernate.boot.model.relational.InitCommand;
import org.hibernate.naming.Identifier;
import org.hibernate.naming.QualifiedTableName;

/**
 * @author Steve Ebersole
 */
public class UnionSubclassTable extends AbstractTable implements ExportableTable {
	private final String unionQuery;
	private final PhysicalTable physicalTable;
	private final UnionSubclassTable superTable;

	public UnionSubclassTable(
			String unionQuery,
			PhysicalTable physicalTable,
			UnionSubclassTable superTable,
			boolean isAbstract) {
		super( isAbstract );
		this.unionQuery = unionQuery;
		this.physicalTable = physicalTable;
		this.superTable = superTable;
	}

	public String getUnionQuery() {
		return unionQuery;
	}

	public PhysicalTable getPhysicalTable() {
		return physicalTable;
	}

	public UnionSubclassTable getSuperTable() {
		return superTable;
	}

	@Override
	public String getTableExpression() {
		return getUnionQuery();
	}

	@Override
	public boolean isExportable() {
		return true;
	}

	@Override
	public Column getColumn(String columnName) {
		final Column column = getPhysicalTable().getColumn( columnName );
		if ( column != null ) {
			return column;
		}
		throw new MappingException( "Could not locate column : " + columnName );
	}

	@Override
	public Collection<Column> getColumns() {
		final List<Column> columns = new ArrayList<>();
		columns.addAll( getPhysicalTable().getColumns() );
		if ( getSuperTable() != null ) {
			columns.addAll( getSuperTable().getColumns() );
		}
		return columns;
	}

	public boolean includes(Table table) {
		return includes( table.getTableExpression() );
	}

	public boolean includes(String tableExpression) {
		if ( tableExpression == null ) {
			throw new IllegalArgumentException( "Passed tableExpression cannot be null" );
		}

		if ( tableExpression.equals( getUnionQuery() ) ) {
			return true;
		}

		if ( tableExpression.equals( getPhysicalTable().getTableExpression() ) ) {
			return true;
		}

		if ( getSuperTable() != null ) {
			return getSuperTable().includes( tableExpression );
		}

		return false;
	}

	@Override
	public String getExportIdentifier() {
		return physicalTable.getExportIdentifier();
	}

	@Override
	public Identifier getCatalogName() {
		return physicalTable.getCatalogName();
	}

	@Override
	public Identifier getSchemaName() {
		return physicalTable.getSchemaName();
	}

	@Override
	public Identifier getTableName() {
		return physicalTable.getTableName();
	}

	@Override
	public QualifiedTableName getQualifiedTableName() {
		return physicalTable.getQualifiedTableName();
	}

	@Override
	public Collection<PhysicalColumn> getPhysicalColumns() {
		return physicalTable.getPhysicalColumns();
	}

	@Override
	public boolean hasPrimaryKey() {
		return physicalTable.hasPrimaryKey();
	}

	@Override
	public String getComment() {
		return physicalTable.getComment();
	}

	@Override
	public Collection<UniqueKey> getUniqueKeys() {
		return physicalTable.getUniqueKeys();
	}

	@Override
	public List<String> getCheckConstraints() {
		return physicalTable.getCheckConstraints();
	}

	@Override
	public Collection<Index> getIndexes() {
		return physicalTable.getIndexes();
	}

	@Override
	public boolean isPrimaryKeyIdentity() {
		return physicalTable.isPrimaryKeyIdentity();
	}

	@Override
	public Collection<InitCommand> getInitCommands() {
		return physicalTable.getInitCommands();
	}
}
