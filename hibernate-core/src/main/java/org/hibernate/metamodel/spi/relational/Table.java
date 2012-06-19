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
import java.util.LinkedHashMap;
import java.util.List;

import org.hibernate.dialect.Dialect;

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
	private ObjectName objectName;
	private String exportIdentifier;

	private final LinkedHashMap<String,Index> indexes = new LinkedHashMap<String,Index>();
	private final LinkedHashMap<String,UniqueKey> uniqueKeys = new LinkedHashMap<String,UniqueKey>();
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
		this.objectName = new ObjectName( database, physicalName );
		this.exportIdentifier = objectName.toText();
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
	 * Gets the table name.
	 * @return the table name.
	 */
	public Identifier getTableName() {
		return physicalName;
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
		return indexes.values();
	}

	@Override
	public Index getOrCreateIndex(String name) {
		if( indexes.containsKey( name ) ){
			return indexes.get( name );
		}
		Index index = new Index( this, name );
		indexes.put(name, index );
		return index;
	}

	@Override
	public Iterable<UniqueKey> getUniqueKeys() {
		return uniqueKeys.values();
	}

	@Override
	public UniqueKey getOrCreateUniqueKey(String name) {
		if( uniqueKeys.containsKey( name ) ){
			return uniqueKeys.get( name );
		}
		UniqueKey uniqueKey = new UniqueKey( this, name );
		uniqueKeys.put(name, uniqueKey );
		return uniqueKey;
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

	@Override
	public String getQualifiedName(Dialect dialect) {
		return objectName.toText( dialect );
	}

	public String[] sqlCreateStrings(Dialect dialect) {
		boolean hasPrimaryKey = getPrimaryKey().getColumns().iterator().hasNext();
		StringBuilder buf =
				new StringBuilder(
						hasPrimaryKey ? dialect.getCreateTableString() : dialect.getCreateMultisetTableString() )
				.append( ' ' )
				.append( objectName.toText( dialect ) )
				.append( " (" );


		boolean isPrimaryKeyIdentity =
				hasPrimaryKey &&
				getPrimaryKey().getColumnSpan() == 1 &&
				getPrimaryKey().getColumns().get( 0 ).isIdentity();

		// Try to find out the name of the primary key in case the dialect needs it to create an identity
		String pkColName = null;
		if ( hasPrimaryKey && isPrimaryKeyIdentity ) {
			Column pkColumn = getPrimaryKey().getColumns().iterator().next();
			pkColName = pkColumn.getColumnName().encloseInQuotesIfQuoted( dialect );
		}

		boolean isFirst = true;
		for ( Value simpleValue : values() ) {
			if ( ! Column.class.isInstance( simpleValue ) ) {
				continue;
			}
			if ( isFirst ) {
				isFirst = false;
			}
			else {
				buf.append( ", " );
			}
			Column col = ( Column ) simpleValue;
			String colName = col.getColumnName().encloseInQuotesIfQuoted( dialect );

			buf.append( colName ).append( ' ' );

			if ( isPrimaryKeyIdentity && colName.equals( pkColName ) ) {
				// to support dialects that have their own identity data type
				if ( dialect.hasDataTypeInIdentityColumn() ) {
					buf.append( getTypeString( col, dialect ) );
				}
				buf.append( ' ' )
						.append( dialect.getIdentityColumnString( col.getJdbcDataType().getTypeCode() ) );
			}
			else {
				buf.append( getTypeString( col, dialect ) );

				String defaultValue = col.getDefaultValue();
				if ( defaultValue != null ) {
					buf.append( " default " ).append( defaultValue );
				}

				if ( col.isNullable() ) {
					buf.append( dialect.getNullColumnString() );
				}
				else {
					buf.append( " not null" );
				}

			}

			boolean useUniqueConstraint = col.isUnique() &&
					( !col.isNullable() || dialect.supportsNotNullUnique() );
			if ( useUniqueConstraint ) {
				if ( dialect.supportsUnique() ) {
					buf.append( " unique" );
				}
				else {
					UniqueKey uk = getOrCreateUniqueKey( col.getColumnName().encloseInQuotesIfQuoted( dialect ) + '_' );
					uk.addColumn( col );
				}
			}

			if ( col.getCheckCondition() != null && dialect.supportsColumnCheck() ) {
				buf.append( " check (" )
						.append( col.getCheckCondition() )
						.append( ")" );
			}

			String columnComment = col.getComment();
			if ( columnComment != null ) {
				buf.append( dialect.getColumnComment( columnComment ) );
			}
		}
		if ( hasPrimaryKey ) {
			buf.append( ", " )
					.append( getPrimaryKey().sqlConstraintStringInCreateTable( dialect ) );
		}

		if ( dialect.supportsUniqueConstraintInCreateAlterTable() ) {
			for ( UniqueKey uk : uniqueKeys.values() ) {
				String constraint = uk.sqlConstraintStringInCreateTable( dialect );
				if ( constraint != null ) {
					buf.append( ", " ).append( constraint );
				}
			}
		}

		if ( dialect.supportsTableCheck() ) {
			for ( CheckConstraint checkConstraint : checkConstraints ) {
				buf.append( ", check (" )
						.append( checkConstraint )
						.append( ')' );
			}
		}

		buf.append( ')' );
		buf.append( dialect.getTableTypeString() );

		String[] sqlStrings = new String[ comments.size() + 1 ];
		sqlStrings[ 0 ] = buf.toString();

		for ( int i = 0 ; i < comments.size(); i++ ) {
			sqlStrings[ i + 1 ] = dialect.getTableComment( comments.get( i ) );
		}

		return sqlStrings;
	}

	private static String getTypeString(Column col, Dialect dialect) {
		String typeString;
		if ( col.getSqlType() != null ) {
			typeString = col.getSqlType();
		}
		else {
			Size size = col.getSize() == null ?
					new Size( ) :
					col.getSize();

			typeString = dialect.getTypeName(
						col.getJdbcDataType().getTypeCode(),
						size.getLength(),
						size.getPrecision(),
						size.getScale()
			);
		}
		return typeString;
	}

	@Override
	public String[] sqlDropStrings(Dialect dialect) {
		StringBuilder buf = new StringBuilder( "drop table " );
		if ( dialect.supportsIfExistsBeforeTableName() ) {
			buf.append( "if exists " );
		}
		buf.append( getQualifiedName( dialect ) )
				.append( dialect.getCascadeConstraintsString() );
		if ( dialect.supportsIfExistsAfterTableName() ) {
			buf.append( " if exists" );
		}
		return new String[] { buf.toString() };
	}

	@Override
	public String toString() {
		return "Table{name=" + exportIdentifier + '}';
	}
}
