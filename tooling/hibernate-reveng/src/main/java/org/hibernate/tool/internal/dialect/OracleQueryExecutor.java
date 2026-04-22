/*
 * Hibernate Tools, Tooling for your Hibernate Projects
 *
 * Copyright 2010-2025 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hibernate.tool.internal.dialect;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Logger;

class OracleQueryExecutor {

	private static final Logger log =
			Logger.getLogger(OracleQueryExecutor.class.getName());

	@FunctionalInterface
	interface ConnectionSupplier {
		Connection get() throws SQLException;
	}

	private final ConnectionSupplier connectionSupplier;

	// ---- Table SQL ----

	private static final String SQL_TABLE_BASE =
			"""
					select a.table_name, a.owner, \
					(SELECT b.comments
					   FROM all_tab_comments b
					  WHERE a.owner = b.owner
					        AND a.table_name = b.table_name) AS comments, \
					'TABLE' \
					from all_tables a\s""";

	private static final String SQL_TABLE_VIEW =
			" union all select view_name, owner, NULL, 'VIEW'"
			+ " from all_views ";

	private static final String SQL_TABLE_NONE =
			SQL_TABLE_BASE + SQL_TABLE_VIEW;

	private static final String SQL_TABLE_SCHEMA = SQL_TABLE_BASE
			+ "where a.owner like ? " + SQL_TABLE_VIEW
			+ " where owner like ?";

	private static final String SQL_TABLE_TABLE = SQL_TABLE_BASE
			+ "where a.table_name like ?" + SQL_TABLE_VIEW
			+ "where view_name like ?";

	private static final String SQL_TABLE_SCHEMA_AND_TABLE =
			SQL_TABLE_BASE
					+ "where a.owner like ? and a.table_name like ?"
					+ SQL_TABLE_VIEW
					+ "where owner like ? and view_name like ?";

	private PreparedStatement prepTableNone;
	private PreparedStatement prepTableSchema;
	private PreparedStatement prepTableTable;
	private PreparedStatement prepTableSchemaAndTable;

	// ---- Index SQL ----

	private static final String SQL_INDEX_BASE =
			"""
					SELECT a.column_name
					      ,decode((SELECT b.uniqueness
					                FROM all_indexes b
					               WHERE a.table_name = b.table_name
					                     AND a.table_owner = b.table_owner
					                     AND a.index_name = b.index_name
					                     AND b.index_type NOT LIKE\
					 'FUNCTION-BASED%'), 'UNIQUE', 'false', 'true')\
					 AS uniqueness
					      ,a.index_owner
					      ,a.index_name
					      ,a.table_name
					  FROM all_ind_columns a
					 \
					 WHERE 1 = 1
					\s""";

	private static final String SQL_INDEX_ORDER =
			" order by a.table_name, a.column_position";

	private static final String SQL_INDEX_NONE =
			SQL_INDEX_BASE + SQL_INDEX_ORDER;

	private static final String SQL_INDEX_SCHEMA = SQL_INDEX_BASE
			+ "and a.table_owner like ? " + SQL_INDEX_ORDER;

	private static final String SQL_INDEX_TABLE = SQL_INDEX_BASE
			+ "and a.table_name like ? " + SQL_INDEX_ORDER;

	private static final String SQL_INDEX_SCHEMA_AND_TABLE =
			SQL_INDEX_BASE
					+ "and a.table_owner like ?"
					+ " and a.table_name like ? "
					+ SQL_INDEX_ORDER;

	private PreparedStatement prepIndexNone;
	private PreparedStatement prepIndexSchema;
	private PreparedStatement prepIndexTable;
	private PreparedStatement prepIndexSchemaAndTable;

	// ---- Column SQL ----

	private static final String SQL_COLUMN_BASE =
			"""
					SELECT a.column_name AS COLUMN_NAME
					      ,a.owner AS TABLE_SCHEM
					      ,decode(a.nullable, 'N', 0, 1) AS NULLABLE
					      ,decode(a.data_type, 'FLOAT',\
					 decode(a.data_precision, NULL, a.data_length,\
					 a.data_precision), 'NUMBER',
					              decode(a.data_precision, NULL,\
					 a.data_length, a.data_precision), 'VARCHAR2',\
					 a.char_length, 'VARCHAR',
					              a.char_length, 'NVARCHAR2',\
					 a.char_length, 'CHAR', a.char_length, 'NCHAR',\
					 a.char_length, a.data_length) AS COLUMN_SIZE
					      ,CASE
					         WHEN a.data_type LIKE 'TIMESTAMP%' THEN
					          93
					         ELSE
					          decode(a.data_type, 'CHAR', 1, 'DATE',\
					 91, 'FLOAT', 6, 'LONG', -1, 'NUMBER', 2,\
					 'VARCHAR2', 12, 'BFILE', -13,
					                 'BLOB', 2004, 'CLOB', 2005,\
					 'MLSLABEL', 1111, 'NCHAR', 1, 'NCLOB', 2005,\
					 'NVARCHAR2', 12, 'RAW', -3,
					                 'ROWID', 1111, 'UROWID', 1111,\
					 'LONG RAW', -4, 'XMLTYPE', 2005, 1111)
					       END AS DATA_TYPE
					      ,a.table_name AS TABLE_NAME
					      ,a.data_type AS TYPE_NAME
					      ,decode(a.data_scale, NULL, 0,\
					 a.data_scale) AS DECIMAL_DIGITS
					      ,(SELECT b.comments
					          FROM all_col_comments b
					         WHERE a.owner = b.owner
					               AND a.table_name = b.table_name
					               AND a.column_name = b.column_name)\
					 AS COMMENTS
					  FROM all_tab_columns a
					""";

	private static final String SQL_COLUMN_ORDER =
			" order by column_id ";

	private static final String SQL_COLUMN_NONE =
			SQL_COLUMN_BASE + SQL_COLUMN_ORDER;

	private static final String SQL_COLUMN_SCHEMA = SQL_COLUMN_BASE
			+ "where a.owner like ? " + SQL_COLUMN_ORDER;

	private static final String SQL_COLUMN_TABLE = SQL_COLUMN_BASE
			+ "where a.table_name like ? " + SQL_COLUMN_ORDER;

	private static final String SQL_COLUMN_COLUMN = SQL_COLUMN_BASE
			+ "where a.column_name like ? " + SQL_COLUMN_ORDER;

	private static final String SQL_COLUMN_SCHEMA_AND_TABLE =
			SQL_COLUMN_BASE
					+ "where a.owner like ? and a.table_name like ? "
					+ SQL_COLUMN_ORDER;

	private static final String SQL_COLUMN_SCHEMA_AND_COLUMN =
			SQL_COLUMN_BASE
					+ "where a.owner like ? and a.column_name like ? "
					+ SQL_COLUMN_ORDER;

	private static final String SQL_COLUMN_TABLE_AND_COLUMN =
			SQL_COLUMN_BASE
					+ "where a.table_name like ?"
					+ " and a.column_name like ? "
					+ SQL_COLUMN_ORDER;

	private static final String SQL_COLUMN_SCHEMA_AND_TABLE_AND_COLUMN =
			SQL_COLUMN_BASE
					+ "where a.owner like ? and a.table_name like ?"
					+ " and a.column_name like ? "
					+ SQL_COLUMN_ORDER;

	private PreparedStatement prepColumnNone;
	private PreparedStatement prepColumnSchema;
	private PreparedStatement prepColumnTable;
	private PreparedStatement prepColumnColumn;
	private PreparedStatement prepColumnSchemaAndTable;
	private PreparedStatement prepColumnSchemaAndColumn;
	private PreparedStatement prepColumnTableAndColumn;
	private PreparedStatement prepColumnSchemaAndTableAndColumn;

	// ---- Primary Key SQL ----

	private static final String SQL_PK_BASE =
			"select c.table_name, c.column_name, c.position,"
			+ "  c.constraint_name, c.owner"
			+ " from all_cons_columns c"
			+ " join all_constraints k on"
			+ " (k.owner = c.owner"
			+ " AND k.table_name = c.table_name"
			+ " AND k.constraint_name = c.constraint_name)"
			+ " where  k.constraint_type = 'P' ";

	private static final String SQL_PK_ORDER =
			" order by c.table_name, c.constraint_name,"
			+ " c.position desc ";

	private static final String SQL_PK_NONE =
			SQL_PK_BASE + SQL_PK_ORDER;

	private static final String SQL_PK_SCHEMA = SQL_PK_BASE
			+ " and c.owner like ? escape '\\' " + SQL_PK_ORDER;

	private static final String SQL_PK_TABLE = SQL_PK_BASE
			+ " and c.table_name like ? escape '\\' "
			+ SQL_PK_ORDER;

	private static final String SQL_PK_SCHEMA_AND_TABLE =
			SQL_PK_BASE
					+ " and c.owner like ? escape '\\'"
					+ " and c.table_name like ? escape '\\' "
					+ SQL_PK_ORDER;

	private PreparedStatement prepPkNone;
	private PreparedStatement prepPkSchema;
	private PreparedStatement prepPkTable;
	private PreparedStatement prepPkSchemaAndTable;

	// ---- Foreign Key SQL ----

	private static final String SQL_FK_BASE =
			"""
					SELECT p.table_name as p_table_name
					      ,p.owner as p_owner
					      ,f.owner as f_owner
					      ,f.table_name as f_table_name
					      ,(SELECT fc.column_name
					          FROM all_cons_columns fc
					         WHERE fc.owner = f.owner
					               AND fc.constraint_name =\
					 f.constraint_name
					               AND fc.table_name = f.table_name
					               AND fc.position = pc.position)\
					 AS fc_column_name
					      ,pc.column_name as pc_column_name
					      ,f.constraint_name
					      ,(SELECT fc.position
					          FROM all_cons_columns fc
					         WHERE fc.owner = f.owner
					               AND fc.constraint_name =\
					 f.constraint_name
					               AND fc.table_name = f.table_name
					               AND fc.position = pc.position)\
					 AS fc_position
					  FROM all_constraints p
					  JOIN all_cons_columns pc
					    ON pc.owner = p.owner
					       AND pc.constraint_name =\
					 p.constraint_name
					       AND pc.table_name = p.table_name
					  JOIN all_constraints f
					    ON p.owner = f.r_owner
					       AND p.constraint_name =\
					 f.r_constraint_name
					 WHERE f.constraint_type = 'R'
					       AND p.constraint_type = 'P'
					""";

	private static final String SQL_FK_ORDER =
			" order by f.table_name, f.constraint_name, position ";

	private static final String SQL_FK_NONE =
			SQL_FK_BASE + SQL_FK_ORDER;

	private static final String SQL_FK_SCHEMA = SQL_FK_BASE
			+ " and p.owner like ? " + SQL_FK_ORDER;

	private static final String SQL_FK_TABLE = SQL_FK_BASE
			+ " and p.table_name like ? " + SQL_FK_ORDER;

	private static final String SQL_FK_SCHEMA_AND_TABLE =
			SQL_FK_BASE
					+ " and p.owner like ?"
					+ " and p.table_name like ? "
					+ SQL_FK_ORDER;

	private PreparedStatement prepFkNone;
	private PreparedStatement prepFkSchema;
	private PreparedStatement prepFkTable;
	private PreparedStatement prepFkSchemaAndTable;

	// ---- Constructor ----

	OracleQueryExecutor(ConnectionSupplier connectionSupplier) {
		this.connectionSupplier = connectionSupplier;
	}

	// ---- ResultSet methods ----

	ResultSet getTableResultSet(String schema, String table)
			throws SQLException {
		if (prepTableNone == null) {
			Connection con = connectionSupplier.get();
			prepTableNone =
					con.prepareStatement(SQL_TABLE_NONE);
			prepTableSchema =
					con.prepareStatement(SQL_TABLE_SCHEMA);
			prepTableTable =
					con.prepareStatement(SQL_TABLE_TABLE);
			prepTableSchemaAndTable =
					con.prepareStatement(
							SQL_TABLE_SCHEMA_AND_TABLE);
		}
		if (schema == null && table == null) {
			return prepTableNone.executeQuery();
		} else if (schema != null) {
			if (table == null) {
				prepTableSchema.setString(1, schema);
				prepTableSchema.setString(2, schema);
				return prepTableSchema.executeQuery();
			} else {
				prepTableSchemaAndTable.setString(1, schema);
				prepTableSchemaAndTable.setString(2, table);
				prepTableSchemaAndTable.setString(3, schema);
				prepTableSchemaAndTable.setString(4, table);
				return prepTableSchemaAndTable.executeQuery();
			}
		} else {
			prepTableTable.setString(1, table);
			prepTableTable.setString(2, table);
			return prepTableTable.executeQuery();
		}
	}

	ResultSet getIndexInfoResultSet(String schema, String table)
			throws SQLException {
		if (prepIndexNone == null) {
			Connection con = connectionSupplier.get();
			prepIndexNone =
					con.prepareStatement(SQL_INDEX_NONE);
			prepIndexSchema =
					con.prepareStatement(SQL_INDEX_SCHEMA);
			prepIndexTable =
					con.prepareStatement(SQL_INDEX_TABLE);
			prepIndexSchemaAndTable =
					con.prepareStatement(
							SQL_INDEX_SCHEMA_AND_TABLE);
		}
		if (schema == null && table == null) {
			return prepIndexNone.executeQuery();
		} else if (schema != null) {
			if (table == null) {
				prepIndexSchema.setString(1, schema);
				return prepIndexSchema.executeQuery();
			} else {
				prepIndexSchemaAndTable.setString(1, schema);
				prepIndexSchemaAndTable.setString(2, table);
				return prepIndexSchemaAndTable.executeQuery();
			}
		} else {
			prepIndexTable.setString(1, table);
			return prepIndexTable.executeQuery();
		}
	}

	ResultSet getColumnsResultSet(
			String schema, String table, String column)
			throws SQLException {
		if (prepColumnNone == null) {
			Connection con = connectionSupplier.get();
			prepColumnNone =
					con.prepareStatement(SQL_COLUMN_NONE);
			prepColumnSchema =
					con.prepareStatement(SQL_COLUMN_SCHEMA);
			prepColumnTable =
					con.prepareStatement(SQL_COLUMN_TABLE);
			prepColumnColumn =
					con.prepareStatement(SQL_COLUMN_COLUMN);
			prepColumnSchemaAndTable =
					con.prepareStatement(
							SQL_COLUMN_SCHEMA_AND_TABLE);
			prepColumnSchemaAndColumn =
					con.prepareStatement(
							SQL_COLUMN_SCHEMA_AND_COLUMN);
			prepColumnTableAndColumn =
					con.prepareStatement(
							SQL_COLUMN_TABLE_AND_COLUMN);
			prepColumnSchemaAndTableAndColumn =
					con.prepareStatement(
							SQL_COLUMN_SCHEMA_AND_TABLE_AND_COLUMN);
		}
		if (schema == null && table == null && column == null) {
			return prepColumnNone.executeQuery();
		} else if (schema != null) {
			if (table == null) {
				if (column == null) {
					prepColumnSchema.setString(1, schema);
					return prepColumnSchema.executeQuery();
				} else {
					prepColumnSchemaAndColumn.setString(
							1, schema);
					prepColumnSchemaAndColumn.setString(
							2, column);
					return prepColumnSchemaAndColumn
							.executeQuery();
				}
			} else {
				if (column == null) {
					prepColumnSchemaAndTable.setString(
							1, schema);
					prepColumnSchemaAndTable.setString(
							2, table);
					return prepColumnSchemaAndTable
							.executeQuery();
				} else {
					prepColumnSchemaAndTableAndColumn
							.setString(1, schema);
					prepColumnSchemaAndTableAndColumn
							.setString(2, table);
					prepColumnSchemaAndTableAndColumn
							.setString(3, column);
					return prepColumnSchemaAndTableAndColumn
							.executeQuery();
				}
			}
		} else {
			if (table == null) {
				prepColumnColumn.setString(1, column);
				return prepColumnColumn.executeQuery();
			} else {
				if (column == null) {
					prepColumnTable.setString(1, table);
					return prepColumnTable.executeQuery();
				} else {
					prepColumnTableAndColumn.setString(
							1, table);
					prepColumnTableAndColumn.setString(
							2, column);
					return prepColumnTableAndColumn
							.executeQuery();
				}
			}
		}
	}

	ResultSet getPrimaryKeysResultSet(String schema, String table)
			throws SQLException {
		String escapedSchema = escape(schema);
		String escapedTable = escape(table);
		if (prepPkNone == null) {
			Connection con = connectionSupplier.get();
			prepPkNone = con.prepareStatement(SQL_PK_NONE);
			prepPkSchema =
					con.prepareStatement(SQL_PK_SCHEMA);
			prepPkTable = con.prepareStatement(SQL_PK_TABLE);
			prepPkSchemaAndTable =
					con.prepareStatement(
							SQL_PK_SCHEMA_AND_TABLE);
		}
		prepPkSchemaAndTable.setString(1, escapedSchema);
		prepPkSchemaAndTable.setString(2, escapedTable);
		return prepPkSchemaAndTable.executeQuery();
	}

	ResultSet getExportedKeysResultSet(
			String schema, String table) throws SQLException {
		if (prepFkNone == null) {
			Connection con = connectionSupplier.get();
			prepFkNone = con.prepareStatement(SQL_FK_NONE);
			prepFkSchema =
					con.prepareStatement(SQL_FK_SCHEMA);
			prepFkTable = con.prepareStatement(SQL_FK_TABLE);
			prepFkSchemaAndTable =
					con.prepareStatement(
							SQL_FK_SCHEMA_AND_TABLE);
		}
		if (schema == null && table == null) {
			return prepFkNone.executeQuery();
		} else if (schema != null) {
			if (table == null) {
				prepFkSchema.setString(1, schema);
				return prepFkSchema.executeQuery();
			} else {
				prepFkSchemaAndTable.setString(1, schema);
				prepFkSchemaAndTable.setString(2, table);
				return prepFkSchemaAndTable.executeQuery();
			}
		} else {
			prepFkTable.setString(1, table);
			return prepFkTable.executeQuery();
		}
	}

	// ---- Cleanup ----

	void closeAll() {
		prepTableNone = closeSilently(prepTableNone);
		prepTableSchema = closeSilently(prepTableSchema);
		prepTableTable = closeSilently(prepTableTable);
		prepTableSchemaAndTable =
				closeSilently(prepTableSchemaAndTable);
		prepIndexNone = closeSilently(prepIndexNone);
		prepIndexSchema = closeSilently(prepIndexSchema);
		prepIndexTable = closeSilently(prepIndexTable);
		prepIndexSchemaAndTable =
				closeSilently(prepIndexSchemaAndTable);
		prepColumnNone = closeSilently(prepColumnNone);
		prepColumnSchema = closeSilently(prepColumnSchema);
		prepColumnTable = closeSilently(prepColumnTable);
		prepColumnColumn = closeSilently(prepColumnColumn);
		prepColumnSchemaAndTable =
				closeSilently(prepColumnSchemaAndTable);
		prepColumnSchemaAndColumn =
				closeSilently(prepColumnSchemaAndColumn);
		prepColumnTableAndColumn =
				closeSilently(prepColumnTableAndColumn);
		prepColumnSchemaAndTableAndColumn =
				closeSilently(prepColumnSchemaAndTableAndColumn);
		prepPkNone = closeSilently(prepPkNone);
		prepPkSchema = closeSilently(prepPkSchema);
		prepPkTable = closeSilently(prepPkTable);
		prepPkSchemaAndTable =
				closeSilently(prepPkSchemaAndTable);
		prepFkNone = closeSilently(prepFkNone);
		prepFkSchema = closeSilently(prepFkSchema);
		prepFkTable = closeSilently(prepFkTable);
		prepFkSchemaAndTable =
				closeSilently(prepFkSchemaAndTable);
	}

	private PreparedStatement closeSilently(PreparedStatement ps) {
		if (ps != null) {
			try {
				ps.close();
			} catch (SQLException e) {
				throw new RuntimeException(
						"Problem while closing prepared statement",
						e);
			}
		}
		return null;
	}

	private String escape(String str) {
		return str == null ? null : str.replace("_", "\\_");
	}
}
