/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.descriptor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Manages an ordered list of {@link JoinColumnPair} entries.
 * Used by {@link ForeignKeyDescriptor} and {@link OneToOneDescriptor}
 * to avoid duplicating join column handling logic.
 *
 * @author Koen Aers
 */
class JoinColumnList {

	private final List<JoinColumnPair> columns = new ArrayList<>();

	void add(String fkColumnName, String referencedColumnName) {
		columns.add(new JoinColumnPair(fkColumnName, referencedColumnName));
	}

	void updateFirstReferencedColumn(String referencedColumnName) {
		if (!columns.isEmpty()) {
			JoinColumnPair first = columns.get(0);
			columns.set(0, new JoinColumnPair(first.fkColumnName(), referencedColumnName));
		}
	}

	String firstForeignKeyColumnName() {
		return columns.isEmpty() ? null : columns.get(0).fkColumnName();
	}

	String firstReferencedColumnName() {
		return columns.isEmpty() ? null : columns.get(0).referencedColumnName();
	}

	List<JoinColumnPair> asList() {
		return Collections.unmodifiableList(columns);
	}

	List<String> foreignKeyColumnNames() {
		return columns.stream().map(JoinColumnPair::fkColumnName).toList();
	}

	boolean isEmpty() {
		return columns.isEmpty();
	}
}
