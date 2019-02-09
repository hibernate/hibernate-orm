
package org.hibernate.tool.internal.dialect;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.hibernate.tool.internal.util.TableNameQualifier;

/**
 * Oracle Specialised MetaData dialect that uses standard JDBC and querys on the
 * Data Dictionary for reading metadata.
 * 
 * @author David Channon
 * @author Eric Kershner (added preparedstatements HBX-817)
 * @author Jacques Stadler (added HBX-1027)
 *  
 */

public class OracleMetaDataDialect extends AbstractMetaDataDialect {

	
	
	public OracleMetaDataDialect() {
		super();
	}
	
	/* ******* TABLE QUERIES ******* */
	private static final String SQL_TABLE_BASE = 
		    "select a.table_name, a.owner, "
						  + "(SELECT b.comments\n"
              + "   FROM all_tab_comments b\n"
              + "  WHERE a.owner = b.owner\n"
              + "        AND a.table_name = b.table_name) AS comments, "
              + "'TABLE' "
			+ "from all_tables a ";

	private static final String SQL_TABLE_VIEW = 
		" union all select view_name, owner, NULL, 'VIEW' from all_views ";

	private static final String SQL_TABLE_NONE = SQL_TABLE_BASE	+ SQL_TABLE_VIEW;

	private static final String SQL_TABLE_SCHEMA = SQL_TABLE_BASE
			+ "where a.owner like ? " + SQL_TABLE_VIEW + " where owner like ?";

	private static final String SQL_TABLE_TABLE = SQL_TABLE_BASE
			+ "where a.table_name like ?" + SQL_TABLE_VIEW + "where view_name like ?";

	private static final String SQL_TABLE_SCHEMA_AND_TABLE =
        SQL_TABLE_BASE
			+ "where a.owner like ? and a.table_name like ?"
      + SQL_TABLE_VIEW
      + "where owner like ? and view_name like ?";

	private PreparedStatement prepTableNone;

	private PreparedStatement prepTableSchema;

	private PreparedStatement prepTableTable;

	private PreparedStatement prepTableSchemaAndTable;

	/* ***************************** */
	/* ******* INDEX QUERIES ******* */
	/* ***************************** */
	private static final String SQL_INDEX_BASE =
            "SELECT a.column_name\n" +
						"      ,decode((SELECT b.uniqueness\n" +
						"                FROM all_indexes b\n" +
						"               WHERE a.table_name = b.table_name\n" +
						"                     AND a.table_owner = b.table_owner\n" +
						"                     AND a.index_name = b.index_name\n" +
						"                     AND b.index_type NOT LIKE 'FUNCTION-BASED%'), 'UNIQUE', 'false', 'true') AS uniqueness\n" +
						"      ,a.index_owner\n" +
						"      ,a.index_name\n" +
						"      ,a.table_name\n" +
						"  FROM all_ind_columns a\n " +
						" WHERE 1 = 1\n ";

	private static final String SQL_INDEX_ORDER = " order by a.table_name, a.column_position";

	private static final String SQL_INDEX_NONE = SQL_INDEX_BASE
			+ SQL_INDEX_ORDER;

	private static final String SQL_INDEX_SCHEMA = SQL_INDEX_BASE
			+ "and a.table_owner like ? " + SQL_INDEX_ORDER;

	private static final String SQL_INDEX_TABLE = SQL_INDEX_BASE
			+ "and a.table_name like ? " + SQL_INDEX_ORDER;

	private static final String SQL_INDEX_SCHEMA_AND_TABLE = SQL_INDEX_BASE
			+ "and a.table_owner like ? and a.table_name like ? " + SQL_INDEX_ORDER;

	private PreparedStatement prepIndexNone;

	private PreparedStatement prepIndexSchema;

	private PreparedStatement prepIndexTable;

	private PreparedStatement prepIndexSchemaAndTable;

	/* ****** COLUMN QUERIES ******* */	
	private static final String SQL_COLUMN_BASE =
            "SELECT a.column_name AS COLUMN_NAME\n" +
						"      ,a.owner AS TABLE_SCHEM\n" +
						"      ,decode(a.nullable, 'N', 0, 1) AS NULLABLE\n" +
						"      ,decode(a.data_type, 'FLOAT', decode(a.data_precision, NULL, a.data_length, a.data_precision), 'NUMBER',\n" +
						"              decode(a.data_precision, NULL, a.data_length, a.data_precision), 'VARCHAR2', a.char_length, 'VARCHAR',\n" +
						"              a.char_length, 'NVARCHAR2', a.char_length, 'CHAR', a.char_length, 'NCHAR', a.char_length, a.data_length) AS COLUMN_SIZE\n" +
						"      ,CASE\n" +
						"         WHEN a.data_type LIKE 'TIMESTAMP%' THEN\n" +
						"          93\n" +
						"         ELSE\n" +
						"          decode(a.data_type, 'CHAR', 1, 'DATE', 91, 'FLOAT', 6, 'LONG', -1, 'NUMBER', 2, 'VARCHAR2', 12, 'BFILE', -13,\n" +
						"                 'BLOB', 2004, 'CLOB', 2005, 'MLSLABEL', 1111, 'NCHAR', 1, 'NCLOB', 2005, 'NVARCHAR2', 12, 'RAW', -3,\n" +
						"                 'ROWID', 1111, 'UROWID', 1111, 'LONG RAW', -4, 'XMLTYPE', 2005, 1111)\n" +
						"       END AS DATA_TYPE\n" +
						"      ,a.table_name AS TABLE_NAME\n" +
						"      ,a.data_type AS TYPE_NAME\n" +
						"      ,decode(a.data_scale, NULL, 0, a.data_scale) AS DECIMAL_DIGITS\n" +
						"      ,(SELECT b.comments\n" +
						"          FROM all_col_comments b\n" +
						"         WHERE a.owner = b.owner\n" +
						"               AND a.table_name = b.table_name\n" +
						"               AND a.column_name = b.column_name) AS COMMENTS\n" +
						"  FROM all_tab_columns a\n";

	private static final String SQL_COLUMN_ORDER = " order by column_id ";

	private static final String SQL_COLUMN_NONE = SQL_COLUMN_BASE
			+ SQL_COLUMN_ORDER;

	private static final String SQL_COLUMN_SCHEMA = SQL_COLUMN_BASE
			+ "where a.owner like ? " + SQL_COLUMN_ORDER;

	private static final String SQL_COLUMN_TABLE = SQL_COLUMN_BASE
			+ "where a.table_name like ? " + SQL_COLUMN_ORDER;

	private static final String SQL_COLUMN_COLUMN = SQL_COLUMN_BASE
			+ "where a.column_name like ? " + SQL_COLUMN_ORDER;

	private static final String SQL_COLUMN_SCHEMA_AND_TABLE = SQL_COLUMN_BASE
			+ "where a.owner like ? and a.table_name like ? " + SQL_COLUMN_ORDER;

	private static final String SQL_COLUMN_SCHEMA_AND_COLUMN = SQL_COLUMN_BASE
			+ "where a.owner like ? and a.column_name like ? " + SQL_COLUMN_ORDER;

	private static final String SQL_COLUMN_TABLE_AND_COLUMN = SQL_COLUMN_BASE
			+ "where a.table_name like ? and a.column_name like ? "
			+ SQL_COLUMN_ORDER;

	private static final String SQL_COLUMN_SCHEMA_AND_TABLE_AND_COLUMN = SQL_COLUMN_BASE
			+ "where a.owner like ? and a.table_name like ? and a.column_name like ? "
			+ SQL_COLUMN_ORDER;

	private PreparedStatement prepColumnNone;

	private PreparedStatement prepColumnSchema;

	private PreparedStatement prepColumnTable;

	private PreparedStatement prepColumnColumn;

	private PreparedStatement prepColumnSchemaAndTable;

	private PreparedStatement prepColumnSchemaAndColumn;

	private PreparedStatement prepColumnTableAndColumn;

	private PreparedStatement prepColumnSchemaAndTableAndColumn;

	/* ***************************** */
	/* ******** PK QUERIES ********* */
	/* ***************************** */
	private static final String SQL_PK_BASE =
            "select c.table_name, c.column_name, c.position,  c.constraint_name, "
			+ "c.owner from all_cons_columns c join all_constraints k on "
			+ "(k.owner = c.owner AND k.table_name = c.table_name AND k.constraint_name = c.constraint_name) "
			+ "where  k.constraint_type = 'P' ";

	private static final String SQL_PK_ORDER = " order by c.table_name, c.constraint_name, c.position desc ";

	private static final String SQL_PK_NONE = SQL_PK_BASE + SQL_PK_ORDER;

	private static final String SQL_PK_SCHEMA = SQL_PK_BASE
			+ " and c.owner like ? escape '\\' " + SQL_PK_ORDER;

	private static final String SQL_PK_TABLE = SQL_PK_BASE
			+ " and c.table_name like ? escape '\\' " + SQL_PK_ORDER;

	private static final String SQL_PK_SCHEMA_AND_TABLE = SQL_PK_BASE
			+ " and c.owner like ? escape '\\' and c.table_name like ? escape '\\' " + SQL_PK_ORDER;

	private PreparedStatement prepPkNone;

	private PreparedStatement prepPkSchema;

	private PreparedStatement prepPkTable;

	private PreparedStatement prepPkSchemaAndTable;

	/* ***************************** */
	/* ******** FK QUERIES ********* */
	/* ***************************** */
	private static final String SQL_FK_BASE =
            "SELECT p.table_name as p_table_name\n" +
            "      ,p.owner as p_owner\n" +
            "      ,f.owner as f_owner\n" +
            "      ,f.table_name as f_table_name\n" +
            "      ,(SELECT fc.column_name\n" +
            "          FROM all_cons_columns fc\n" +
            "         WHERE fc.owner = f.owner\n" +
            "               AND fc.constraint_name = f.constraint_name\n" +
            "               AND fc.table_name = f.table_name\n" +
            "               AND fc.position = pc.position) AS fc_column_name\n" +
            "      ,pc.column_name as pc_column_name\n" +
            "      ,f.constraint_name\n" +
            "      ,(SELECT fc.position\n" +
            "          FROM all_cons_columns fc\n" +
            "         WHERE fc.owner = f.owner\n" +
            "               AND fc.constraint_name = f.constraint_name\n" +
            "               AND fc.table_name = f.table_name\n" +
            "               AND fc.position = pc.position) AS fc_position\n" +
            "  FROM all_constraints p\n" +
            "  JOIN all_cons_columns pc\n" +
            "    ON pc.owner = p.owner\n" +
            "       AND pc.constraint_name = p.constraint_name\n" +
            "       AND pc.table_name = p.table_name\n" +
            "  JOIN all_constraints f\n" +
            "    ON p.owner = f.r_owner\n" +
            "       AND p.constraint_name = f.r_constraint_name\n" +
            " WHERE f.constraint_type = 'R'\n" +
            "       AND p.constraint_type = 'P'\n";

	private static final String SQL_FK_ORDER = " order by f.table_name, f.constraint_name, position ";

	private static final String SQL_FK_NONE = SQL_FK_BASE + SQL_FK_ORDER;

	private static final String SQL_FK_SCHEMA = SQL_FK_BASE
			+ " and p.owner like ? " + SQL_FK_ORDER;

	private static final String SQL_FK_TABLE = SQL_FK_BASE
			+ " and p.table_name like ? " + SQL_FK_ORDER;

	private static final String SQL_FK_SCHEMA_AND_TABLE = SQL_FK_BASE
			+ " and p.owner like ? and p.table_name like ? " + SQL_FK_ORDER;

	private PreparedStatement prepFkNone;

	private PreparedStatement prepFkSchema;

	private PreparedStatement prepFkTable;

	private PreparedStatement prepFkSchemaAndTable;
	
	public Iterator<Map<String,Object>> getTables(final String catalog, final String schema,
			String table) {
		try {
			log.debug("getTables(" + catalog + "." + schema + "." + table + ")");
			
			ResultSet tableRs = getTableResultSet( schema, table );

			return new ResultSetIterator(null, tableRs,
					getSQLExceptionConverter()) {

				Map<String, Object> element = new HashMap<String, Object>();

				
				protected Map<String, Object> convertRow(ResultSet tableResultSet)
						throws SQLException {
					element.clear();
					element.put("TABLE_NAME", tableResultSet.getString(1));
					element.put("TABLE_SCHEM", tableResultSet.getString(2));
					element.put("TABLE_CAT", null);
					element.put("TABLE_TYPE", tableResultSet.getString(4));
          element.put("REMARKS", tableResultSet.getString(3));
					log.info( element.toString() );
					return element;
				}

				protected Throwable handleSQLException(SQLException e) {
					// schemaRs and catalogRs are only used for error reporting
					// if
					// we get an exception
					String databaseStructure = getDatabaseStructure(catalog,
							schema);
					throw getSQLExceptionConverter().convert(
							e,
							"Could not get list of tables from database. Probably a JDBC driver problem. "
									+ databaseStructure, null);
				}
			};
		} catch (SQLException e) {
			// schemaRs and catalogRs are only used for error reporting if we
			// get an exception
			String databaseStructure = getDatabaseStructure(catalog, schema);
			throw getSQLExceptionConverter().convert(
					e,
					"Could not get list of tables from database. Probably a JDBC driver problem. "
							+ databaseStructure, null);
		}
	}
	
	public Iterator<Map<String, Object>> getIndexInfo(final String catalog, final String schema,
			final String table) {
		try {
			log.debug("getIndexInfo(" + catalog + "." + schema + "." + table + ")");

			ResultSet indexRs;
			indexRs = getIndexInfoResultSet( schema, table );

			return new ResultSetIterator(null, indexRs,
					getSQLExceptionConverter()) {

				Map<String, Object> element = new HashMap<String, Object>();

				
				protected Map<String, Object> convertRow(ResultSet rs) throws SQLException {
					element.clear();
					element.put("COLUMN_NAME", rs.getString(1));
					element.put("TYPE", Short.valueOf((short) 1)); // CLUSTERED
																// INDEX
					element.put("NON_UNIQUE", Boolean.valueOf(rs.getString(2)));
					element.put("TABLE_SCHEM", rs.getString(3));
					element.put("INDEX_NAME", rs.getString(4));
					element.put("TABLE_CAT", null);
					element.put("TABLE_NAME", rs.getString(5));

					return element;
				}

				protected Throwable handleSQLException(SQLException e) {
					throw getSQLExceptionConverter().convert(
							e,
							"Exception while getting index info for "
									+ TableNameQualifier.qualify(catalog, schema, table),
							null);
				}
			};
		} catch (SQLException e) {
			throw getSQLExceptionConverter().convert(
					e,
					"Exception while getting index info for "
							+ TableNameQualifier.qualify(catalog, schema, table) + ": " + e.getMessage(), null);
		}
	}

	public Iterator<Map<String, Object>> getColumns(final String catalog, final String schema,
			final String table, String column) {
		
		try {
			log.debug("getColumns(" + catalog + "." + schema + "." + table + "." + column + ")");

			ResultSet columnRs;
			columnRs = getColumnsResultSet( schema, table, column );

			return new ResultSetIterator(null, columnRs,
					getSQLExceptionConverter()) {

				Map<String, Object> element = new HashMap<String, Object>();

				
				protected Map<String, Object> convertRow(ResultSet rs) throws SQLException {
					element.clear();
					element.put("COLUMN_NAME", rs.getString(1));
					element.put("TABLE_SCHEM", rs.getString(2));
					element.put("NULLABLE", Integer.valueOf(rs.getInt(3)));
					element.put("COLUMN_SIZE", Integer.valueOf(rs.getInt(4)));
					element.put("DATA_TYPE", Integer.valueOf(rs.getInt(5)));
					element.put("TABLE_NAME", rs.getString(6));
					element.put("TYPE_NAME", rs.getString(7));
					element.put("DECIMAL_DIGITS", Integer.valueOf(rs.getInt(8)));
					element.put("TABLE_CAT", null);
					element.put("REMARKS", rs.getString(9));
					return element;
				}

				protected Throwable handleSQLException(SQLException e) {
					throw getSQLExceptionConverter().convert(
							e,
							"Error while reading column meta data for "
									+ TableNameQualifier.qualify(catalog, schema, table),
							null);
				}
			};
		} catch (SQLException e) {
			throw getSQLExceptionConverter().convert(
					e,
					"Error while reading column meta data for "
							+ TableNameQualifier.qualify(catalog, schema, table), null);
		}
	}

	public Iterator<Map<String, Object>> getPrimaryKeys(final String catalog, final String schema,
			final String table) {
		
		try {
			log.debug("getPrimaryKeys(" + catalog + "." + schema + "." + table
					+ ")");

			ResultSet pkeyRs;
			pkeyRs = getPrimaryKeysResultSet( schema, table );

			return new ResultSetIterator(null, pkeyRs,
					getSQLExceptionConverter()) {

				Map<String, Object> element = new HashMap<String, Object>();

				
				protected Map<String, Object> convertRow(ResultSet rs) throws SQLException {
					element.clear();
					element.put("TABLE_NAME", rs.getString(1));
					element.put("COLUMN_NAME", rs.getString(2));
					element.put("KEY_SEQ", Short.valueOf(rs.getShort(3)));
					element.put("PK_NAME", rs.getString(4));
					element.put("TABLE_SCHEM", rs.getString(5));
					element.put("TABLE_CAT", null);
					return element;
				}

				protected Throwable handleSQLException(SQLException e) {
					throw getSQLExceptionConverter().convert(
							e,
							"Error while reading primary key meta data for "
									+ TableNameQualifier.qualify(catalog, schema, table),
							null);
				}
			};
		} catch (SQLException e) {
			throw getSQLExceptionConverter().convert(
					e,
					"Error while reading primary key meta data for "
							+ TableNameQualifier.qualify(catalog, schema, table), null);
		}
	}

	public Iterator<Map<String, Object>> getExportedKeys(final String catalog, final String schema,
			final String table) {
		
		try {
			log.debug("getExportedKeys(" + catalog + "." + schema + "." + table
					+ ")");

			ResultSet pExportRs = getExportedKeysResultSet( schema, table );

			return new ResultSetIterator(null, pExportRs,
					getSQLExceptionConverter()) {

				Map<String, Object> element = new HashMap<String, Object>();

				
				protected Map<String, Object> convertRow(ResultSet rs) throws SQLException {
					element.clear();
					element.put("PKTABLE_NAME", rs.getString(1));
					element.put("PKTABLE_SCHEM", rs.getString(2));
					element.put("PKTABLE_CAT", null);
					element.put("FKTABLE_CAT", null);
					element.put("FKTABLE_SCHEM", rs.getString(3));
					element.put("FKTABLE_NAME", rs.getString(4));
					element.put("FKCOLUMN_NAME", rs.getString(5));
					element.put("PKCOLUMN_NAME", rs.getString(6));
					element.put("FK_NAME", rs.getString(7));
					element.put("KEY_SEQ", Short.valueOf(rs.getShort(8)));
					return element;
				}

				protected Throwable handleSQLException(SQLException e) {
					throw getSQLExceptionConverter().convert(
							e,
							"Error while reading exported keys meta data for "
									+ TableNameQualifier.qualify(catalog, schema, table),
							null);
				}
			};
		} catch (SQLException e) {
			throw getSQLExceptionConverter().convert(
					e,
					"Error while reading exported keys meta data for "
							+ TableNameQualifier.qualify(catalog, schema, table), null);
		}
	}	
	
	public void close() {
		try {
			prepTableNone = close( prepTableNone );
			prepTableSchema = close( prepTableSchema );
			prepTableTable = close( prepTableTable );
			prepTableSchemaAndTable = close( prepTableSchemaAndTable );
			prepIndexNone = close( prepIndexNone );
			prepIndexSchema = close( prepIndexSchema );
			prepIndexTable = close( prepIndexTable );
			prepIndexSchemaAndTable = close( prepIndexSchemaAndTable );
			prepColumnNone = close( prepColumnNone );
			prepColumnSchema = close( prepColumnSchema );
			prepColumnTable = close( prepColumnTable );
			prepColumnColumn = close( prepColumnColumn );
			prepColumnSchemaAndTable = close( prepColumnSchemaAndTable );
			prepColumnSchemaAndColumn = close( prepColumnSchemaAndColumn );
			prepColumnTableAndColumn = close( prepColumnTableAndColumn );
			prepColumnSchemaAndTableAndColumn = close( prepColumnSchemaAndTableAndColumn );
			prepPkNone = close( prepPkNone );
			prepPkSchema = close( prepPkSchema );
			prepPkTable = close( prepPkTable );
			prepPkSchemaAndTable = close( prepPkSchemaAndTable );
			prepFkNone = close( prepFkNone );
			prepFkSchema = close( prepFkSchema );
			prepFkTable = close( prepFkTable );
			prepFkSchemaAndTable = close( prepFkSchemaAndTable );
		}
		finally {
			super.close();
		}
	}

	private PreparedStatement close(PreparedStatement ps) {
		if(ps==null) {
			return null;
		} else {
			try {
				ps.close();
			}
			catch (SQLException e) {
				throw getSQLExceptionConverter().convert(e,
						"Problem while closing prepared statement", null);				
			}
			return null;
		}
		
	}
	
	private String escape(String str) {
		return str.replace("_", "\\_");
	}

	private ResultSet getPrimaryKeysResultSet(final String schem, final String tab) throws SQLException {
		String schema = escape(schem);
		String table = escape(tab);
		if(prepPkNone==null) {
			// Prepare primary key queries
			log.debug("Preparing primary key queries...");
			Connection con = getConnection();
			prepPkNone = con .prepareStatement(SQL_PK_NONE);
			prepPkSchema = con.prepareStatement(SQL_PK_SCHEMA);
			prepPkTable = con.prepareStatement(SQL_PK_TABLE);
			prepPkSchemaAndTable = con
			.prepareStatement(SQL_PK_SCHEMA_AND_TABLE);
			log.debug("  primary key queries prepared!");
		}
		
		ResultSet pkeyRs;
		if (schema == null && table == null) {
			pkeyRs = prepPkNone.executeQuery();
		} else if (schema != null) {
			if (table == null) {
				prepPkSchema.setString(1, schema);
				pkeyRs = prepPkSchema.executeQuery();
			} else {
				prepPkSchemaAndTable.setString(1, schema);
				prepPkSchemaAndTable.setString(2, table);
				pkeyRs = prepPkSchemaAndTable.executeQuery();
			}
		} else {
			prepPkTable.setString(1, table);
			pkeyRs = prepPkTable.executeQuery();
		}
		return pkeyRs;
	}

	private ResultSet getIndexInfoResultSet(final String schema, final String table) throws SQLException {
		if(prepIndexNone==null) {
			//	Prepare index queries
			log.debug("Preparing index queries...");
			Connection con = getConnection();
			prepIndexNone = con.prepareStatement(SQL_INDEX_NONE);
			prepIndexSchema = con.prepareStatement(SQL_INDEX_SCHEMA);
			prepIndexTable = con.prepareStatement(SQL_INDEX_TABLE);
			prepIndexSchemaAndTable = con.prepareStatement(SQL_INDEX_SCHEMA_AND_TABLE);
			log.debug("  ...index queries prepared!");			
		}
		ResultSet indexRs;
		if (schema == null && table == null) {
			indexRs = prepIndexNone.executeQuery();
		} else if (schema != null) {
			if (table == null) {
				prepIndexSchema.setString(1, schema);
				indexRs = prepIndexSchema.executeQuery();
			} else {
				prepIndexSchemaAndTable.setString(1, schema);
				prepIndexSchemaAndTable.setString(2, table);
				indexRs = prepIndexSchemaAndTable.executeQuery();
			}
		} else {
			prepIndexTable.setString(1, table);
			indexRs = prepIndexTable.executeQuery();
		}
		return indexRs;
	}

	private ResultSet getExportedKeysResultSet(final String schema, final String table) throws SQLException {
		if(prepFkNone==null) {
			//	Prepare foreign key queries
			log.debug("Preparing foreign key queries...");
			Connection con = getConnection();
			prepFkNone = con .prepareStatement(SQL_FK_NONE);
			prepFkSchema = con.prepareStatement(SQL_FK_SCHEMA);
			prepFkTable = con.prepareStatement(SQL_FK_TABLE);
			prepFkSchemaAndTable = con.prepareStatement(SQL_FK_SCHEMA_AND_TABLE);
			log.debug("  foreign key queries prepared!");
		}
		
		ResultSet pExportRs;
		if (schema == null && table == null) {
			pExportRs = prepFkNone.executeQuery();
		} else if (schema != null) {
			if (table == null) {
				prepFkSchema.setString(1, schema);
				pExportRs = prepFkSchema.executeQuery();
			} else {
				prepFkSchemaAndTable.setString(1, schema);
				prepFkSchemaAndTable.setString(2, table);
				pExportRs = prepFkSchemaAndTable.executeQuery();
			}
		} else {
			prepFkTable.setString(1, table);
			pExportRs = prepFkTable.executeQuery();
		}
		return pExportRs;
	}
	private ResultSet getColumnsResultSet(final String schema, final String table, String column) throws SQLException {
		
		if(prepColumnNone==null) {			
			// Prepare column queries
			log.debug("Preparing column queries...");
			Connection con = getConnection();
			prepColumnNone = con.prepareStatement(SQL_COLUMN_NONE);
			prepColumnSchema = con.prepareStatement(SQL_COLUMN_SCHEMA);
			prepColumnTable = con.prepareStatement(SQL_COLUMN_TABLE);
			prepColumnColumn = con.prepareStatement(SQL_COLUMN_COLUMN);
			prepColumnSchemaAndTable = con.prepareStatement(SQL_COLUMN_SCHEMA_AND_TABLE);
			prepColumnSchemaAndColumn = con.prepareStatement(SQL_COLUMN_SCHEMA_AND_COLUMN);
			prepColumnTableAndColumn = con.prepareStatement(SQL_COLUMN_TABLE_AND_COLUMN);
			prepColumnSchemaAndTableAndColumn = con.prepareStatement(SQL_COLUMN_SCHEMA_AND_TABLE_AND_COLUMN);
			log.debug("  ...column queries prepared!");
		}
		
		ResultSet columnRs;
		// No parameters specified
		if (schema == null && table == null && column == null) {
			columnRs = prepColumnNone.executeQuery();
		} else if (schema != null) {
			if (table == null) {
				if (column == null) {
					// Schema specified
					prepColumnSchema.setString(1, schema);
					columnRs = prepColumnSchema.executeQuery();
				} else {
					// Schema and column specified
					prepColumnSchemaAndColumn.setString(1, schema);
					prepColumnSchemaAndColumn.setString(2, column);
					columnRs = prepColumnSchemaAndColumn.executeQuery();
				}
			} else {
				if (column == null) {
					// Schema and table specified
					prepColumnSchemaAndTable.setString(1, schema);
					prepColumnSchemaAndTable.setString(2, table);
					columnRs = prepColumnSchemaAndTable.executeQuery();
				} else {
					// Schema, table and column specified
					prepColumnSchemaAndTableAndColumn.setString(1, schema);
					prepColumnSchemaAndTableAndColumn.setString(2, table);
					prepColumnSchemaAndTableAndColumn.setString(3, column);
					columnRs = prepColumnSchemaAndTableAndColumn.executeQuery();
				}
			}
		} else {
			if (table == null) {
				// Column specified
				prepColumnColumn.setString(1, column);
				columnRs = prepColumnColumn.executeQuery();
			} else {
				if (column == null) {
					// Table specified
					prepColumnTable.setString(1, table);
					columnRs = prepColumnTable.executeQuery();
				} else {
					// Table and column specified
					prepColumnTableAndColumn.setString(1, table);
					prepColumnTableAndColumn.setString(2, column);
					columnRs = prepColumnTableAndColumn.executeQuery();
	
				}
			}
		}
		return columnRs;
	}

	private ResultSet getTableResultSet(final String schema, String table) throws SQLException {
		ResultSet tableRs;
		if(prepTableNone==null) {
			// Prepare table queries
			log.debug("Preparing table queries...");
			Connection connection2 = getConnection();
			prepTableNone = connection2.prepareStatement(SQL_TABLE_NONE);
			prepTableSchema = connection2.prepareStatement(SQL_TABLE_SCHEMA);
			prepTableTable = connection2.prepareStatement(SQL_TABLE_TABLE);
			prepTableSchemaAndTable = connection2.prepareStatement(SQL_TABLE_SCHEMA_AND_TABLE);
			log.debug("  ...table queries prepared!");
		}
		if (schema == null && table == null) {
			tableRs = prepTableNone.executeQuery();
		} else if (schema != null) {
			if (table == null) {
				prepTableSchema.setString(1, schema);
				prepTableSchema.setString(2, schema);
				tableRs = prepTableSchema.executeQuery();
			} else {
				prepTableSchemaAndTable.setString(1, schema);
				prepTableSchemaAndTable.setString(2, table);
				prepTableSchemaAndTable.setString(3, schema);
				prepTableSchemaAndTable.setString(4, table);
				tableRs = prepTableSchemaAndTable.executeQuery();
			}
		} else {
			prepTableTable.setString(1, table);
			prepTableTable.setString(2, table);
			tableRs = prepTableTable.executeQuery();
		}
		return tableRs;
	}

}
