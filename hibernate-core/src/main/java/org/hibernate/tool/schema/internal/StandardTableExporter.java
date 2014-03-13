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
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.metamodel.spi.relational.CheckConstraint;
import org.hibernate.metamodel.spi.relational.Column;
import org.hibernate.metamodel.spi.relational.Table;
import org.hibernate.tool.schema.spi.Exporter;

/**
 * @author Steve Ebersole
 */
public class StandardTableExporter implements Exporter<Table> {
	protected final Dialect dialect;

	public StandardTableExporter(Dialect dialect) {
		this.dialect = dialect;
	}

	@Override
	public String[] getSqlCreateStrings(Table table, JdbcEnvironment jdbcEnvironment) {
		boolean hasPrimaryKey = table.getPrimaryKey().getColumns().iterator().hasNext();
		StringBuilder buf =
				new StringBuilder(tableCreateString( hasPrimaryKey ))
						.append( ' ' )
						.append( jdbcEnvironment.getQualifiedObjectNameSupport().formatName( table.getTableName() ) )
						.append( " (" );


		boolean isPrimaryKeyIdentity =
				hasPrimaryKey &&
						table.getPrimaryKey().getColumnSpan() == 1 &&
						table.getPrimaryKey().getColumns().get( 0 ).isIdentity();

		// Try to find out the name of the primary key in case the dialect needs it to create an identity
		String pkColName = null;
		if ( hasPrimaryKey  ) {
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
					buf.append( col.getSqlTypeString( dialect ) );
				}
				buf.append( ' ' )
						.append( dialect.getIdentityColumnString( col.getJdbcDataType().getTypeCode() ) );
			}
			else {
				buf.append( col.getSqlTypeString( dialect ) );

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

		applyTableCheck( table, buf );

		buf.append( ')' );
		applyTableTypeString( buf );

		List<String> sqlStrings = new ArrayList<String>();
		sqlStrings.add( buf.toString() );

		applyComments( table, sqlStrings );

		return sqlStrings.toArray( new String[ sqlStrings.size() ] );
	}

	protected void applyComments(Table table, List<String> sqlStrings) {
		for ( String comment : table.getComments() ) {
			if ( comment != null ) {
				sqlStrings.add( dialect.getTableComment( comment ) );
			}
		}
	}

	protected void applyTableTypeString(StringBuilder buf) {
		buf.append( dialect.getTableTypeString() );
	}

	protected void applyTableCheck(Table table, StringBuilder buf) {
		if ( dialect.supportsTableCheck() ) {
			for ( CheckConstraint checkConstraint : table.getCheckConstraints() ) {
				buf.append( ", check (" )
						.append( checkConstraint )
						.append( ')' );
			}
		}
	}

	protected String tableCreateString(boolean hasPrimaryKey) {
		return hasPrimaryKey ? dialect.getCreateTableString() : dialect.getCreateMultisetTableString();

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
