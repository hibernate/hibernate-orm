/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.dialect;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.hibernate.tool.reveng.internal.util.TableNameQualifier;

/**
 * Oracle specialised MetaData dialect that uses standard JDBC
 * and queries on the Data Dictionary for reading metadata.
 *
 * @author David Channon
 * @author Eric Kershner
 * @author Jacques Stadler
 */
public class OracleRevengDialect extends AbstractRevengDialect {

	private final OracleQueryExecutor queryExecutor =
			new OracleQueryExecutor(this::getConnection);

	public OracleRevengDialect() {
		super();
	}

	public Iterator<Map<String, Object>> getTables(
			String catalog, String schema, String table) {
		try {
			log.debug("getTables(" + catalog + "." + schema
					+ "." + table + ")");
			ResultSet tableRs =
					queryExecutor.getTableResultSet(
							schema, table);
			return new ResultSetIterator(null, tableRs) {
				final Map<String, Object> element =
						new HashMap<>();

				protected Map<String, Object> convertRow(
						ResultSet rs) throws SQLException {
					element.clear();
					element.put("TABLE_NAME", rs.getString(1));
					element.put("TABLE_SCHEM", rs.getString(2));
					element.put("TABLE_CAT", null);
					element.put("TABLE_TYPE", rs.getString(4));
					element.put("REMARKS", rs.getString(3));
					log.info(element.toString());
					return element;
				}

				protected Throwable handleSQLException(
						SQLException e) {
					throw new RuntimeException(
							"Could not get list of tables from"
							+ " database. Probably a JDBC driver"
							+ " problem. "
							+ getDatabaseStructure(
									catalog, schema), e);
				}
			};
		}
		catch (SQLException e) {
			throw new RuntimeException(
					"Could not get list of tables from database."
					+ " Probably a JDBC driver problem. "
					+ getDatabaseStructure(catalog, schema), e);
		}
	}

	public Iterator<Map<String, Object>> getIndexInfo(
			String catalog, String schema, String table) {
		try {
			log.debug("getIndexInfo(" + catalog + "." + schema
					+ "." + table + ")");
			ResultSet indexRs =
					queryExecutor.getIndexInfoResultSet(
							schema, table);
			return new ResultSetIterator(null, indexRs) {
				final Map<String, Object> element =
						new HashMap<>();

				protected Map<String, Object> convertRow(
						ResultSet rs) throws SQLException {
					element.clear();
					element.put("COLUMN_NAME", rs.getString(1));
					element.put("TYPE", (short) 1);
					element.put("NON_UNIQUE",
							Boolean.valueOf(rs.getString(2)));
					element.put("TABLE_SCHEM", rs.getString(3));
					element.put("INDEX_NAME", rs.getString(4));
					element.put("TABLE_CAT", null);
					element.put("TABLE_NAME", rs.getString(5));
					return element;
				}

				protected Throwable handleSQLException(
						SQLException e) {
					throw new RuntimeException(
							"Exception while getting index info"
							+ " for " + TableNameQualifier
									.qualify(catalog, schema,
											table), e);
				}
			};
		}
		catch (SQLException e) {
			throw new RuntimeException(
					"Exception while getting index info for "
					+ TableNameQualifier.qualify(
							catalog, schema, table)
					+ ": " + e.getMessage(), e);
		}
	}

	public Iterator<Map<String, Object>> getColumns(
			String catalog, String schema,
			String table, String column) {
		try {
			log.debug("getColumns(" + catalog + "." + schema
					+ "." + table + "." + column + ")");
			ResultSet columnRs =
					queryExecutor.getColumnsResultSet(
							schema, table, column);
			return new ResultSetIterator(null, columnRs) {
				final Map<String, Object> element =
						new HashMap<>();

				protected Map<String, Object> convertRow(
						ResultSet rs) throws SQLException {
					element.clear();
					element.put("COLUMN_NAME", rs.getString(1));
					element.put("TABLE_SCHEM", rs.getString(2));
					element.put("NULLABLE", rs.getInt(3));
					element.put("COLUMN_SIZE", rs.getInt(4));
					element.put("DATA_TYPE", rs.getInt(5));
					element.put("TABLE_NAME", rs.getString(6));
					element.put("TYPE_NAME", rs.getString(7));
					element.put("DECIMAL_DIGITS", rs.getInt(8));
					element.put("TABLE_CAT", null);
					element.put("REMARKS", rs.getString(9));
					return element;
				}

				protected Throwable handleSQLException(
						SQLException e) {
					throw new RuntimeException(
							"Error while reading column meta"
							+ " data for "
							+ TableNameQualifier.qualify(
									catalog, schema, table), e);
				}
			};
		}
		catch (SQLException e) {
			throw new RuntimeException(
					"Error while reading column meta data for "
					+ TableNameQualifier.qualify(
							catalog, schema, table), e);
		}
	}

	public Iterator<Map<String, Object>> getPrimaryKeys(
			String catalog, String schema, String table) {
		try {
			log.debug("getPrimaryKeys(" + catalog + "." + schema
					+ "." + table + ")");
			ResultSet pkeyRs =
					queryExecutor.getPrimaryKeysResultSet(
							schema, table);
			return new ResultSetIterator(null, pkeyRs) {
				final Map<String, Object> element =
						new HashMap<>();

				protected Map<String, Object> convertRow(
						ResultSet rs) throws SQLException {
					element.clear();
					element.put("TABLE_NAME", rs.getString(1));
					element.put("COLUMN_NAME", rs.getString(2));
					element.put("KEY_SEQ", rs.getShort(3));
					element.put("PK_NAME", rs.getString(4));
					element.put("TABLE_SCHEM", rs.getString(5));
					element.put("TABLE_CAT", null);
					return element;
				}

				protected Throwable handleSQLException(
						SQLException e) {
					throw new RuntimeException(
							"Error while reading primary key"
							+ " meta data for "
							+ TableNameQualifier.qualify(
									catalog, schema, table), e);
				}
			};
		}
		catch (SQLException e) {
			throw new RuntimeException(
					"Error while reading primary key meta data"
					+ " for " + TableNameQualifier.qualify(
							catalog, schema, table), e);
		}
	}

	public Iterator<Map<String, Object>> getExportedKeys(
			String catalog, String schema, String table) {
		try {
			log.debug("getExportedKeys(" + catalog + "."
					+ schema + "." + table + ")");
			ResultSet pExportRs =
					queryExecutor.getExportedKeysResultSet(
							schema, table);
			return new ResultSetIterator(null, pExportRs) {
				final Map<String, Object> element =
						new HashMap<>();

				protected Map<String, Object> convertRow(
						ResultSet rs) throws SQLException {
					element.clear();
					element.put("PKTABLE_NAME", rs.getString(1));
					element.put("PKTABLE_SCHEM",
							rs.getString(2));
					element.put("PKTABLE_CAT", null);
					element.put("FKTABLE_CAT", null);
					element.put("FKTABLE_SCHEM",
							rs.getString(3));
					element.put("FKTABLE_NAME", rs.getString(4));
					element.put("FKCOLUMN_NAME",
							rs.getString(5));
					element.put("PKCOLUMN_NAME",
							rs.getString(6));
					element.put("FK_NAME", rs.getString(7));
					element.put("KEY_SEQ", rs.getShort(8));
					return element;
				}

				protected Throwable handleSQLException(
						SQLException e) {
					throw new RuntimeException(
							"Error while reading exported keys"
							+ " meta data for "
							+ TableNameQualifier.qualify(
									catalog, schema, table), e);
				}
			};
		}
		catch (SQLException e) {
			throw new RuntimeException(
					"Error while reading exported keys meta data"
					+ " for " + TableNameQualifier.qualify(
							catalog, schema, table), e);
		}
	}

	public void close() {
		try {
			queryExecutor.closeAll();
		}
		finally {
			super.close();
		}
	}
}
