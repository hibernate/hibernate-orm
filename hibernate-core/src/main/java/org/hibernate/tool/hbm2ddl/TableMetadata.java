/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
package org.hibernate.tool.hbm2ddl;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.jboss.logging.Logger;

import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.mapping.ForeignKey;

/**
 * JDBC table metadata
 *
 * @author Christoph Sturm
 * @author Max Rydahl Andersen
 */
public class TableMetadata {

    private static final CoreMessageLogger LOG = Logger.getMessageLogger(CoreMessageLogger.class, TableMetadata.class.getName());

	private final String catalog;
	private final String schema;
	private final String name;
	private final Map columns = new HashMap();
	private final Map foreignKeys = new HashMap();
	private final Map indexes = new HashMap();

	TableMetadata(ResultSet rs, DatabaseMetaData meta, boolean extras) throws SQLException {
		catalog = rs.getString("TABLE_CAT");
		schema = rs.getString("TABLE_SCHEM");
		name = rs.getString("TABLE_NAME");
		initColumns(meta);
		if (extras) {
			initForeignKeys(meta);
			initIndexes(meta);
		}
		String cat = catalog==null ? "" : catalog + '.';
		String schem = schema==null ? "" : schema + '.';
        LOG.tableFound( cat + schem + name );
        LOG.columns( columns.keySet() );
		if (extras) {
            LOG.foreignKeys( foreignKeys.keySet() );
            LOG.indexes( indexes.keySet() );
		}
	}

	public String getName() {
		return name;
	}

	public String getCatalog() {
		return catalog;
	}

	public String getSchema() {
		return schema;
	}

	@Override
    public String toString() {
		return "TableMetadata(" + name + ')';
	}

	public ColumnMetadata getColumnMetadata(String columnName) {
		return (ColumnMetadata) columns.get( columnName.toLowerCase() );
	}

	public ForeignKeyMetadata getForeignKeyMetadata(String keyName) {
		return (ForeignKeyMetadata) foreignKeys.get( keyName.toLowerCase() );
	}

	public ForeignKeyMetadata getForeignKeyMetadata(ForeignKey fk) {
		Iterator it = foreignKeys.values().iterator();
		while ( it.hasNext() ) {
			ForeignKeyMetadata existingFk = ( ForeignKeyMetadata ) it.next();
			if ( existingFk.matches( fk ) ) {
				return existingFk;
			}
		}
		return null;
	}

	public IndexMetadata getIndexMetadata(String indexName) {
		return (IndexMetadata) indexes.get( indexName.toLowerCase() );
	}

	private void addForeignKey(ResultSet rs) throws SQLException {
		String fk = rs.getString("FK_NAME");

		if (fk == null) {
			return;
		}

		ForeignKeyMetadata info = getForeignKeyMetadata(fk);
		if (info == null) {
			info = new ForeignKeyMetadata(rs);
			foreignKeys.put( info.getName().toLowerCase(), info );
		}

		info.addReference( rs );
	}

	private void addIndex(ResultSet rs) throws SQLException {
		String index = rs.getString("INDEX_NAME");

		if (index == null) {
			return;
		}

		IndexMetadata info = getIndexMetadata(index);
		if (info == null) {
			info = new IndexMetadata(rs);
			indexes.put( info.getName().toLowerCase(), info );
		}

		info.addColumn( getColumnMetadata( rs.getString("COLUMN_NAME") ) );
	}

	public void addColumn(ResultSet rs) throws SQLException {
		String column = rs.getString("COLUMN_NAME");

		if (column==null) {
			return;
		}

		if ( getColumnMetadata(column) == null ) {
			ColumnMetadata info = new ColumnMetadata(rs);
			columns.put( info.getName().toLowerCase(), info );
		}
	}

	private void initForeignKeys(DatabaseMetaData meta) throws SQLException {
		ResultSet rs = null;

		try {
			rs = meta.getImportedKeys(catalog, schema, name);
			while ( rs.next() ) {
				addForeignKey(rs);
			}
		}
		finally {
			if (rs != null) {
				rs.close();
			}
		}
	}

	private void initIndexes(DatabaseMetaData meta) throws SQLException {
		ResultSet rs = null;

		try {
			rs = meta.getIndexInfo(catalog, schema, name, false, true);

			while ( rs.next() ) {
				if ( rs.getShort("TYPE") == DatabaseMetaData.tableIndexStatistic ) {
					continue;
				}
				addIndex(rs);
			}
		}
		finally {
			if (rs != null) {
				rs.close();
			}
		}
	}

	private void initColumns(DatabaseMetaData meta) throws SQLException {
		ResultSet rs = null;

		try {
			rs = meta.getColumns(catalog, schema, name, "%");
			while ( rs.next() ) {
				addColumn(rs);
			}
		}
		finally  {
			if (rs != null) {
				rs.close();
			}
		}
	}
}
