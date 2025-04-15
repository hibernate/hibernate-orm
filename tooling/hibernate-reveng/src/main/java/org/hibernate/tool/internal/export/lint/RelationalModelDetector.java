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
package org.hibernate.tool.internal.export.lint;

import java.util.Iterator;

import org.hibernate.mapping.Column;
import org.hibernate.mapping.Table;

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

