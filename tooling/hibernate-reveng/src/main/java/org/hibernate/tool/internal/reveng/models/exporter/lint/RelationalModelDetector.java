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
package org.hibernate.tool.internal.reveng.models.exporter.lint;

import org.hibernate.boot.Metadata;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Table;

/**
 * Base class for lint detectors that analyze the relational model
 * ({@link Table}/{@link Column}) obtained from {@link Metadata}.
 * Iterates over all mapped tables, calling {@link #visit(Table, IssueCollector)}
 * and {@link #visit(Table, Column, IssueCollector)} for each.
 */
public abstract class RelationalModelDetector {

	private Metadata metadata;

	public void initialize(Metadata metadata) {
		this.metadata = metadata;
	}

	protected Metadata getMetadata() {
		return metadata;
	}

	public void visit(IssueCollector collector) {
		for (Table table : getMetadata().collectTableMappings()) {
			this.visit(table, collector);
		}
	}

	protected void visit(Table table, IssueCollector collector) {
		visitColumns(table, collector);
	}

	protected void visitColumns(Table table, IssueCollector collector) {
		for (Column col : table.getColumns()) {
			this.visit(table, col, collector);
		}
	}

	abstract protected void visit(Table table, Column col, IssueCollector collector);

	public void visitGenerators(IssueCollector collector) {
		// default no-op; subclasses override as needed
	}

	abstract public String getName();
}
