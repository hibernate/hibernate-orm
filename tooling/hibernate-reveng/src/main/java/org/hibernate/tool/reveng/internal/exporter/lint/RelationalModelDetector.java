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
