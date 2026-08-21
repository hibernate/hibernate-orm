/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.core.strategy;

import org.hibernate.tool.reveng.api.core.RevengStrategy;

import java.util.ArrayList;
import java.util.List;

public class TableSelectorStrategy extends DelegatingStrategy {

	List<SchemaSelection> selections = new ArrayList<SchemaSelection>();

	public TableSelectorStrategy(RevengStrategy res) {
		super(res);
	}

	public List<SchemaSelection> getSchemaSelections() {
		return selections;
	}


	public void clearSchemaSelections() {
		selections.clear();
	}

	public void addSchemaSelection(SchemaSelection selection) {
		selections.add(selection);
	}
}
