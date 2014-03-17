/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010 by Red Hat Inc and/or its affiliates or by
 * third-party contributors as indicated by either @author tags or express
 * copyright attribution statements applied by the authors.  All
 * third-party contributions are distributed under license by Red Hat Inc.
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
package org.hibernate.metamodel.spi.relational;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.internal.util.compare.EqualsHelper;
import org.hibernate.tool.schema.extract.spi.ColumnInformation;
import org.hibernate.tool.schema.extract.spi.TableInformation;

/**
 * Models the concept of a relational <tt>TABLE</tt> (or <tt>VIEW</tt>).
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class Table extends AbstractTableSpecification implements Exportable {
	private final Schema database;
	private Identifier physicalName;
	private Identifier logicalName;
	private ObjectName qualifiedName;
	private String exportIdentifier;
	private boolean isPhysicalTable = true;
	private boolean hasDenormalizedTables = false;

	private String rowId;

	private final Set<Index> indexes = new LinkedHashSet<Index>();
	private final Set<UniqueKey> uniqueKeys = new LinkedHashSet<UniqueKey>();
	private final List<CheckConstraint> checkConstraints = new ArrayList<CheckConstraint>();
	private final List<String> comments = new ArrayList<String>();

	/**
	 * Constructs a {@link Table} instance.
	 *
	 * @param database - the schema
	 * @param logicalName - The logical name
	 * @param physicalName - the physical table name.
	 */
	public Table(Schema database, Identifier logicalName, Identifier physicalName) {
		this.database = database;
		this.logicalName = logicalName;
		this.physicalName = physicalName;
		this.qualifiedName = new ObjectName( database, physicalName );
		this.exportIdentifier = qualifiedName.toText();
	}

	@Override
	public Schema getSchema() {
		return database;
	}

	/**
	 * Gets the logical table name.
	 *
	 * @return the logical table name.
	 */
	@Override
	public Identifier getLogicalName() {
		return logicalName;
	}

	/**
	 * Returns the simple physical name.
	 *
	 * @return The simple (non-qualfied) table name.  For the qualified name, see {@link #getTableName()}
	 *
	 * @see {@link #getTableName()}
	 */
	public Identifier getPhysicalName() {
		return physicalName;
	}

	/**
	 * Is this a physical table or should it be a virtual table representing as root entity in table-per-class hierarchy.
	 *
	 * It's {@code false} only when the entity is {@code abstract} and also having union sub-class
	 */
	public boolean isPhysicalTable() {
		return isPhysicalTable;
	}

	public void setPhysicalTable(boolean physicalTable) {
		isPhysicalTable = physicalTable;
	}

	public boolean hasDenormalizedTables() {
		return hasDenormalizedTables;
	}

	protected void setHasDenormalizedTables(boolean hasDenormalizedTables) {
		this.hasDenormalizedTables = hasDenormalizedTables;
	}

	/**
	 * Gets the qualified table name.
	 *
	 * @return the qualified table name.
	 */
	public ObjectName getTableName() {
		return qualifiedName;
	}

	@Override
	public String getLoggableValueQualifier() {
		return exportIdentifier;
	}

	@Override
	public String getExportIdentifier() {
		return exportIdentifier;
	}

	@Override
	public String toLoggableString() {
		return exportIdentifier;
	}

	@Override
	public Iterable<Index> getIndexes() {
		return Collections.unmodifiableSet( indexes );
	}

	@Override
	public void addIndex(Index idx) {
		indexes.add( idx );
	}

	@Override
	public Iterable<UniqueKey> getUniqueKeys() {
		return Collections.unmodifiableSet( uniqueKeys );
	}

	@Override
	public void addUniqueKey(UniqueKey uk) {
		uniqueKeys.add( uk );
	}
	
	@Override
	public boolean hasUniqueKey(Column column) {
		for ( UniqueKey uniqueKey : uniqueKeys ) {
			if ( uniqueKey.hasColumn( column ) ) {
				return true;
			}
		}
		return false;
	}

	@Override
	public Iterable<CheckConstraint> getCheckConstraints() {
		return checkConstraints;
	}

	@Override
	public void addCheckConstraint(String checkCondition) {
        //todo ? StringHelper.isEmpty( checkCondition );
        //todo default name?
		checkConstraints.add( new CheckConstraint( this, "", checkCondition ) );
	}

	@Override
	public Iterable<String> getComments() {
		return comments;
	}

	@Override
	public void addComment(String comment) {
		comments.add( comment );
	}

	public String getRowId() {
		return rowId;
	}

	public void setRowId(String rowId) {
		this.rowId = rowId;
	}

	@Override
	public String getQualifiedName(Dialect dialect) {
		return qualifiedName.toText( dialect );
	}

	@Override
	public String toString() {
		return "Table{name=" + exportIdentifier + '}';
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( ! ( o instanceof Table ) ) {
			return false;
		}

		final Table that = (Table) o;

		return EqualsHelper.equals( this.database, that.database )
				&& EqualsHelper.equals( this.logicalName, that.logicalName )
				&& EqualsHelper.equals( this.physicalName, that.physicalName );
	}

	@Override
	public int hashCode() {
		int result = database != null ? database.hashCode() : 0;
		result = 31 * result + ( physicalName != null ? physicalName.hashCode() : 0 );
		result = 31 * result + ( logicalName != null ? logicalName.hashCode() : 0 );
		return result;
	}

	public String[] sqlAlterStrings(TableInformation tableInformation, JdbcEnvironment jdbcEnvironment) {
		final Dialect dialect = jdbcEnvironment.getDialect();
		final String baseAlterCommand = "alter table "
				+ jdbcEnvironment.getQualifiedObjectNameSupport().formatName( getTableName() )
				+ ' ' + dialect.getAddColumnString();

		final List<String> commands = new ArrayList<String>();

		for ( Value value : values() ) {
			if ( ! Column.class.isInstance( value ) ) {
				continue;
			}

			final Column column = (Column) value;
			final ColumnInformation columnInformation = tableInformation.getColumn( column.getColumnName() );

			if ( columnInformation != null ) {
				continue;
			}

			StringBuilder alter = new StringBuilder( baseAlterCommand )
					.append( ' ' )
					.append( column.getColumnName().getText( dialect ) )
					.append( ' ' )
					.append( column.getSqlType() );


			final String defaultValue = column.getDefaultValue();
			if ( defaultValue != null ) {
				alter.append( " default " )
						.append( defaultValue );
			}
			String nullablePostfix = column.isNullable() ? dialect.getNullColumnString() : " not null";
			alter.append( nullablePostfix );

			final String checkCondition = column.getCheckCondition();
			if ( checkCondition != null && dialect.supportsColumnCheck() ) {
				alter.append( " check(" )
						.append( checkCondition )
						.append( ")" );
			}

			final String columnComment = column.getComment();
			if ( columnComment != null ) {
				alter.append( dialect.getColumnComment( columnComment ) );
			}

			commands.add( alter.toString() );
		}

		return commands.toArray( new String[ commands.size() ] );
	}

	/**
	 * @return Sorted column list so that primary key appears first, followed by foreign keys and other properties.
	 * Within each group columns are not sorted in any way.
	 */
	public Iterable<Column> sortedColumns() {
		final Set<Column> sortedColumns = new LinkedHashSet<Column>();

		// Adding primary key columns.
		sortedColumns.addAll( getPrimaryKey().getColumns() );
		// Adding foreign key columns.
		for ( ForeignKey fk : getForeignKeys() ) {
			sortedColumns.addAll( fk.getColumns() );
		}
		// Adding other columns.
		for ( Value value : values() ) {
			if ( value instanceof Column ) {
				final Column column = (Column) value;
				if ( ! sortedColumns.contains( column ) ) {
					sortedColumns.add( column );
				}
			}
		}
		return sortedColumns;
	}
}
