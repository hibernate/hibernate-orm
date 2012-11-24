/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
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
package org.hibernate.tool.schema.internal;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.dialect.Dialect;
import org.hibernate.metamodel.spi.relational.CheckConstraint;
import org.hibernate.metamodel.spi.relational.Column;
import org.hibernate.metamodel.spi.relational.Size;
import org.hibernate.metamodel.spi.relational.Table;
import org.hibernate.metamodel.spi.relational.UniqueKey;
import org.hibernate.metamodel.spi.relational.Value;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.tool.schema.spi.Exporter;

/**
 * @author Steve Ebersole
 */
public class StandardTableExporter implements Exporter<Table> {
	private final Dialect dialect;

	public StandardTableExporter(Dialect dialect) {
		this.dialect = dialect;
	}

	@Override
	public String[] getSqlCreateStrings(Table table, JdbcEnvironment jdbcEnvironment) {
		boolean hasPrimaryKey = table.getPrimaryKey().getColumns().iterator().hasNext();
		StringBuilder buf =
				new StringBuilder(
						hasPrimaryKey ? dialect.getCreateTableString() : dialect.getCreateMultisetTableString() )
						.append( ' ' )
						.append( jdbcEnvironment.getQualifiedObjectNameSupport().formatName( table.getTableName() ) )
						.append( " (" );


		boolean isPrimaryKeyIdentity =
				hasPrimaryKey &&
						table.getPrimaryKey().getColumnSpan() == 1 &&
						table.getPrimaryKey().getColumns().get( 0 ).isIdentity();

		// Try to find out the name of the primary key in case the dialect needs it to create an identity
		String pkColName = null;
		if ( hasPrimaryKey && isPrimaryKeyIdentity ) {
			Column pkColumn = table.getPrimaryKey().getColumns().iterator().next();
			pkColName = pkColumn.getColumnName().getText( dialect );
		}

		boolean isFirst = true;
		for ( Column col : table.sortedColumns() ) {
			if ( isFirst ) {
				isFirst = false;
			}
			else {
				buf.append( ", " );
			}
			String colName = col.getColumnName().getText( dialect );

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
					UniqueKey uk = table.getOrCreateUniqueKey( col.getColumnName().getText( dialect ) + '_' );
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
					.append( table.getPrimaryKey().sqlConstraintStringInCreateTable( dialect ) );
		}

		if ( dialect.supportsUniqueConstraintInCreateAlterTable() ) {
			for ( UniqueKey uk : table.getUniqueKeys() ) {
				String constraint = uk.sqlConstraintStringInCreateTable( dialect );
				if ( constraint != null ) {
					buf.append( ", " ).append( constraint );
				}
			}
		}

		if ( dialect.supportsTableCheck() ) {
			for ( CheckConstraint checkConstraint : table.getCheckConstraints() ) {
				buf.append( ", check (" )
						.append( checkConstraint )
						.append( ')' );
			}
		}

		buf.append( ')' );
		buf.append( dialect.getTableTypeString() );

		List<String> sqlStrings = new ArrayList<String>();
		sqlStrings.add( buf.toString() );

		for ( String comment : table.getComments() ) {
			sqlStrings.add( dialect.getTableComment( comment ) );
		}

		return sqlStrings.toArray( new String[ sqlStrings.size() ] );
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
	public String[] getSqlDropStrings(Table table, JdbcEnvironment jdbcEnvironment) {
		StringBuilder buf = new StringBuilder( "drop table " );
		if ( dialect.supportsIfExistsBeforeTableName() ) {
			buf.append( "if exists " );
		}

		buf.append( jdbcEnvironment.getQualifiedObjectNameSupport().formatName( table.getTableName() ) )
				.append( dialect.getCascadeConstraintsString() );

		if ( dialect.supportsIfExistsAfterTableName() ) {
			buf.append( " if exists" );
		}

		return new String[] { buf.toString() };
	}
}
