/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
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


import java.util.List;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.metamodel.spi.relational.Column;
import org.hibernate.metamodel.spi.relational.Identifier;
import org.hibernate.metamodel.spi.relational.Table;
import org.hibernate.metamodel.spi.relational.TableSpecification;

/**
 * @author Strong Liu <stliu@hibernate.org>
 */
public class TemporaryTableExporter extends StandardTableExporter {

	public TemporaryTableExporter(Dialect dialect) {
		super(dialect);
	}

	@Override
	public String[] getSqlCreateStrings(Table exportable, JdbcEnvironment jdbcEnvironment) {
		if ( dialect.supportsTemporaryTables() ) {
			String temperaryTableName = generateTableName( dialect, exportable );
			Table table = new Table(
					exportable.getSchema(),
					Identifier.toIdentifier( temperaryTableName ),
					Identifier.toIdentifier( temperaryTableName )
			);
			for ( final Column column : exportable.getPrimaryKey().getColumns() ) {
				Column clone = table.createColumn( column.getColumnName() );
				clone.setCheckCondition( column.getCheckCondition() );
				clone.setIdentity( column.isIdentity() );
				clone.setSize( column.getSize() );
				clone.setSqlType( column.getSqlType() );
				clone.setJdbcDataType( column.getJdbcDataType() );
				clone.setNullable( column.isNullable() );
				clone.setComment( column.getComment() );
				clone.setDefaultValue( column.getDefaultValue() );
				clone.setReadFragment( column.getReadFragment() );
				clone.setWriteFragment( column.getWriteFragment() );
			}
			return super.getSqlCreateStrings( table, jdbcEnvironment );
		}
		return null;
	}

	@Override
	protected String tableCreateString(boolean hasPrimaryKey) {
		return dialect.getCreateTemporaryTableString();
	}

	@Override
	protected void applyTableCheck(Table table, StringBuilder buf) {
		// N/A
	}

	@Override
	protected void applyComments(Table table, List<String> sqlStrings) {
		// N/A
	}

	@Override
	protected void applyTableTypeString(StringBuilder buf) {
		buf.append( " " ).append( dialect.getCreateTemporaryTablePostfix() );
	}

	@Override
	public String[] getSqlDropStrings(Table exportable, JdbcEnvironment jdbcEnvironment) {
		return null;
	}

	public static String generateTableName(final Dialect dialect, final TableSpecification primaryTable) {
		return dialect.generateTemporaryTableName( primaryTable.getLogicalName().getText() );
	}
}
