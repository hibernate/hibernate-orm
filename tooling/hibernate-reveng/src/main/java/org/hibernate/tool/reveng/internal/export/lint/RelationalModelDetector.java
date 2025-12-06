/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.export.lint;

import org.hibernate.mapping.Column;
import org.hibernate.mapping.Table;

import java.util.Iterator;

public abstract class RelationalModelDetector extends Detector {

	public void visit(IssueCollector collector) {
		for (Iterator<Table> iter = getMetadata().collectTableMappings().iterator(); iter.hasNext();) {
			Table table = (Table) iter.next();
			this.visit(table, collector);
		}
	}

	abstract protected void visit(Table table, Column col, IssueCollector collector);

	protected void visitColumns(Table table, IssueCollector collector) {
		for (Column col : table.getColumns()) {
			this.visit(table, col, collector );
		}
	}

	/**
	 * @return true if visit should continue down through the columns
	 */
	protected void visit(Table table, IssueCollector collector) {
		visitColumns(table, collector);
	}

}
