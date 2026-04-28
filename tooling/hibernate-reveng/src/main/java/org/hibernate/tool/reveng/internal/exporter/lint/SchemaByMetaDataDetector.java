/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.exporter.lint;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.TableGenerator;

import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.FieldDetails;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.tool.reveng.api.reveng.RevengDialect;
import org.hibernate.tool.reveng.api.reveng.RevengDialectFactory;
import org.hibernate.tool.reveng.internal.util.TypeHelper;
import org.hibernate.tool.reveng.internal.util.TableNameQualifier;

/**
 * Lint detector that compares the entity model (from {@link ClassDetails})
 * against the actual database schema. Reports missing tables, missing
 * columns, column type mismatches, and missing identifier generators
 * (sequences/tables).
 */
public class SchemaByMetaDataDetector extends RelationalModelDetector {

	private Dialect dialect;
	private RevengDialect revengDialect;
	private ConnectionProvider connectionProvider;
	private ServiceRegistry serviceRegistry;

	public String getName() {
		return "schema";
	}

	@Override
	public void initialize(List<ClassDetails> entities, Properties properties) {
		super.initialize(entities, properties);
		Properties envProperties = Environment.getProperties();
		StandardServiceRegistryBuilder builder =
				new StandardServiceRegistryBuilder();
		if (properties != null) {
			builder.applySettings(properties);
		}
		try {
			serviceRegistry = builder.build();
			JdbcServices jdbcServices =
					serviceRegistry.getService(JdbcServices.class);
			if (jdbcServices != null) {
				dialect = jdbcServices.getDialect();
			}
			revengDialect = RevengDialectFactory.createMetaDataDialect(
					dialect, envProperties);
			connectionProvider =
					serviceRegistry.getService(ConnectionProvider.class);
			revengDialect.configure(connectionProvider);
		}
		catch (Exception e) {
			// Database not available — schema checks will be skipped
			revengDialect = null;
		}
	}

	@Override
	public void visit(IssueCollector collector) {
		if (revengDialect == null) {
			return;
		}
		super.visit(collector);
		visitGenerators(collector);
	}

	@Override
	protected void visitTable(ClassDetails entity,
							IssueCollector collector) {
		String tableName = getTableName(entity);
		String schema = getTableSchema(entity);
		String catalog = getTableCatalog(entity);

		// Read the table from the database
		Map<String, Map<String, Object>> dbColumns =
				readDbColumns(catalog, schema, tableName);

		if (dbColumns == null) {
			collector.reportIssue(new Issue(
					"SCHEMA_TABLE_MISSING",
					Issue.HIGH_PRIORITY,
					"Missing table "
					+ TableNameQualifier.qualify(catalog, schema, tableName)));
			return;
		}

		// Check each column
		for (FieldDetails field : entity.getFields()) {
			Column column = field.getDirectAnnotationUsage(Column.class);
			if (column != null) {
				visitColumn(entity, field, column, dbColumns, collector);
			}
		}
	}

	private void visitColumn(ClassDetails entity,
							FieldDetails field,
							Column column,
							Map<String, Map<String, Object>> dbColumns,
							IssueCollector collector) {
		String tableName = getTableName(entity);
		String schema = getTableSchema(entity);
		String catalog = getTableCatalog(entity);
		String columnName = column.name();
		String qualifiedTable =
				TableNameQualifier.qualify(catalog, schema, tableName);

		Map<String, Object> dbColumn =
				dbColumns.get(columnName.toUpperCase());
		if (dbColumn == null) {
			collector.reportIssue(new Issue(
					"SCHEMA_COLUMN_MISSING",
					Issue.HIGH_PRIORITY,
					qualifiedTable
					+ " is missing column: " + columnName));
			return;
		}

		// Compare SQL type codes
		Object rawDataType = dbColumn.get("DATA_TYPE");
		if (rawDataType == null) {
			return;
		}
		int dbTypeCode = (Integer) rawDataType;
		int modelTypeCode = Integer.MIN_VALUE;
		if (field.getType() != null) {
			var rawClass = field.getType().determineRawClass();
			if (rawClass != null) {
				modelTypeCode = TypeHelper.getJdbcTypeCode(rawClass.getClassName());
			}
		}
		if (modelTypeCode != Integer.MIN_VALUE
				&& dbTypeCode != modelTypeCode) {
			collector.reportIssue(new Issue(
					"SCHEMA_COLUMN_TYPE_MISMATCH",
					Issue.NORMAL_PRIORITY,
					qualifiedTable
					+ " has a wrong column type for "
					+ columnName + ", expected: "
					+ TypeHelper
							.getJDBCTypeName(modelTypeCode)
					+ " but was "
					+ TypeHelper
							.getJDBCTypeName(dbTypeCode)
					+ " in db"));
		}
	}

	@Override
	protected void visitColumn(ClassDetails entity,
							FieldDetails field,
							Column column,
							IssueCollector collector) {
		// Not used — we override visitTable to pass dbColumns through
	}

	@Override
	public void visitGenerators(IssueCollector collector) {
		Set<String> sequences = readSequences();

		// Collect generator keys in a TreeMap so they are reported
		// in alphabetical order, matching the legacy detector behaviour.
		TreeMap<String, GenerationType> generators = new TreeMap<>();
		for (ClassDetails entity : getEntities()) {
			for (FieldDetails field : entity.getFields()) {
				if (!field.hasDirectAnnotationUsage(Id.class)
						&& !field.hasDirectAnnotationUsage(
								EmbeddedId.class)) {
					continue;
				}
				GeneratedValue gv = field.getDirectAnnotationUsage(
						GeneratedValue.class);
				if (gv == null) {
					continue;
				}
				String key = resolveGeneratorKey(field, gv);
				if (key != null) {
					generators.put(key, gv.strategy());
				}
			}
		}

		for (Map.Entry<String, GenerationType> entry
				: generators.entrySet()) {
			String key = entry.getKey();
			GenerationType strategy = entry.getValue();
			if (!isSequence(key, sequences) && !isTable(key)) {
				if (strategy == GenerationType.TABLE) {
					collector.reportIssue(new Issue(
							"SCHEMA_TABLE_MISSING",
							Issue.HIGH_PRIORITY,
							"Missing table " + key));
				}
				collector.reportIssue(new Issue(
						"MISSING_ID_GENERATOR",
						Issue.HIGH_PRIORITY,
						"Missing sequence or table: " + key));
			}
		}
	}

	private String resolveGeneratorKey(FieldDetails field,
										GeneratedValue gv) {
		GenerationType strategy = gv.strategy();
		if (strategy == GenerationType.SEQUENCE) {
			return resolveSequenceName(field, gv);
		}
		else if (strategy == GenerationType.TABLE) {
			return resolveGeneratorTableName(field, gv);
		}
		return null;
	}

	private String resolveSequenceName(FieldDetails field,
									GeneratedValue gv) {
		String generatorName = gv.generator();
		if (generatorName != null && !generatorName.isEmpty()) {
			SequenceGenerator sg = field.getDirectAnnotationUsage(
					SequenceGenerator.class);
			if (sg != null && generatorName.equals(sg.name())) {
				return sg.sequenceName();
			}
			// Generator name without @SequenceGenerator — treat as
			// the sequence name itself
			return generatorName;
		}
		return null;
	}

	private String resolveGeneratorTableName(FieldDetails field,
											GeneratedValue gv) {
		String generatorName = gv.generator();
		if (generatorName != null && !generatorName.isEmpty()) {
			TableGenerator tg = field.getDirectAnnotationUsage(
					TableGenerator.class);
			if (tg != null && generatorName.equals(tg.name())) {
				return tg.table();
			}
			return generatorName;
		}
		return null;
	}

	// ---- Database access ----

	private Map<String, Map<String, Object>> readDbColumns(
			String catalog, String schema, String tableName) {
		try {
			Iterator<Map<String, Object>> tableIter =
					revengDialect.getTables(catalog, schema, tableName);
			try {
				if (!tableIter.hasNext()) {
					return null;
				}
				tableIter.next();
			}
		finally {
				closeQuietly(tableIter);
			}

			Map<String, Map<String, Object>> result = new HashMap<>();
			Iterator<Map<String, Object>> columnIter =
					revengDialect.getColumns(
							catalog, schema, tableName, null);
			try {
				while (columnIter.hasNext()) {
					Map<String, Object> col = columnIter.next();
					String colName = (String) col.get("COLUMN_NAME");
					if (colName != null) {
						result.put(colName.toUpperCase(), col);
					}
				}
			}
		finally {
				closeQuietly(columnIter);
			}
			return result;
		}
		catch (Exception e) {
			// Database access failed — treat as missing table
			return null;
		}
	}

	private void closeQuietly(Iterator<?> iter) {
		try {
			revengDialect.close(iter);
		}
		catch (Exception ignored) {
			// Iterator may not have been fully initialized
		}
	}

	private Set<String> readSequences() {
		if (dialect != null
				&& dialect.getSequenceSupport().supportsSequences()) {
			SequenceCollector seqCollector =
					SequenceCollector.create(connectionProvider);
			return seqCollector.readSequences(
					dialect.getQuerySequencesString());
		}
		return new HashSet<>();
	}

	private boolean isSequence(String key, Set<String> sequences) {
		if (sequences.contains(key.toLowerCase())) {
			return true;
		}
		String[] parts = StringHelper.split(".", key);
		if (parts.length == 3) {
			return sequences.contains(parts[2].toLowerCase());
		}
		else if (parts.length == 2) {
			return sequences.contains(parts[1].toLowerCase());
		}
		return false;
	}

	private boolean isTable(String tableName) {
		String[] parts = StringHelper.split(".", tableName);
		String catalog = null, schema = null, table;
		if (parts.length == 3) {
			catalog = parts[0];
			schema = parts[1];
			table = parts[2];
		}
		else if (parts.length == 2) {
			schema = parts[0];
			table = parts[1];
		}
		else {
			table = parts[0];
		}
		try {
			Iterator<Map<String, Object>> iter =
					revengDialect.getTables(catalog, schema, table);
			try {
				return iter.hasNext();
			}
		finally {
				closeQuietly(iter);
			}
		}
		catch (Exception e) {
			return false;
		}
	}

}
