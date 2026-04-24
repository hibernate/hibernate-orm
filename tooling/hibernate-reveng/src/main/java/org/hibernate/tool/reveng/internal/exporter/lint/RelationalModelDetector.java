/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.exporter.lint;

import java.util.List;
import java.util.Properties;

import jakarta.persistence.Column;
import jakarta.persistence.Table;

import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.FieldDetails;

/**
 * Base class for lint detectors that analyze the relational model
 * (table/column structure) derived from {@link ClassDetails} annotations.
 * Iterates over all entities and their columns, calling
 * {@link #visitTable} and {@link #visitColumn} for each.
 */
public abstract class RelationalModelDetector {

	private List<ClassDetails> entities;
	private Properties properties;

	public void initialize(List<ClassDetails> entities, Properties properties) {
		this.entities = entities;
		this.properties = properties;
	}

	protected List<ClassDetails> getEntities() {
		return entities;
	}

	protected Properties getProperties() {
		return properties;
	}

	public void visit(IssueCollector collector) {
		for (ClassDetails entity : entities) {
			visitTable(entity, collector);
		}
	}

	protected void visitTable(ClassDetails entity, IssueCollector collector) {
		for (FieldDetails field : entity.getFields()) {
			Column column = field.getDirectAnnotationUsage(Column.class);
			if (column != null) {
				visitColumn(entity, field, column, collector);
			}
		}
	}

	protected abstract void visitColumn(ClassDetails entity,
										FieldDetails field,
										Column column,
										IssueCollector collector);

	public void visitGenerators(IssueCollector collector) {
		// default no-op; subclasses override as needed
	}

	public abstract String getName();

	// ---- Helper methods for extracting table info ----

	protected static String getTableName(ClassDetails entity) {
		Table table = entity.getDirectAnnotationUsage(Table.class);
		if (table != null && !table.name().isEmpty()) {
			return table.name();
		}
		return entity.getName();
	}

	protected static String getTableSchema(ClassDetails entity) {
		Table table = entity.getDirectAnnotationUsage(Table.class);
		return table != null && !table.schema().isEmpty()
				? table.schema() : null;
	}

	protected static String getTableCatalog(ClassDetails entity) {
		Table table = entity.getDirectAnnotationUsage(Table.class);
		return table != null && !table.catalog().isEmpty()
				? table.catalog() : null;
	}
}
