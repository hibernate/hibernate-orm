/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2015, Red Hat Inc. or third-party contributors as
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

import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.dialect.Dialect;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Table;

/**
 * @author Strong Liu <stliu@hibernate.org>
 */
public class TemporaryTableExporter extends StandardTableExporter {
	public TemporaryTableExporter(Dialect dialect) {
		super(dialect);
	}

	@Override
	public String[] getSqlCreateStrings(Table exportable, Metadata metadata) {
		if ( dialect.supportsTemporaryTables() ) {
			final String temporaryTableName = generateTableName( dialect, exportable );
			Table temporaryTable = new Table(
					Identifier.toIdentifier( exportable.getQuotedCatalog() ),
					Identifier.toIdentifier( exportable.getQuotedSchema() ),
					Identifier.toIdentifier( temporaryTableName ),
					false
			);
			//noinspection unchecked
			for ( Column column : ( (List<Column>) exportable.getPrimaryKey().getColumns() ) ) {
				final Column clone = column.clone();
				temporaryTable.addColumn( clone );
			}
			return super.getSqlCreateStrings( temporaryTable, metadata );
		}
		return null;
	}

	public static String generateTableName(final Dialect dialect, final Table primaryTable) {
		return dialect.generateTemporaryTableName( primaryTable.getName() );
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
	public String[] getSqlDropStrings(Table exportable, Metadata metadata) {
		return null;
	}
}
