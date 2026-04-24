/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.strategy;

import org.hibernate.tool.reveng.api.reveng.TableIdentifier;

class TableColumnKey {

	private final TableIdentifier query;
	private final String name;

	TableColumnKey(TableIdentifier query, String name) {
		this.query = query;
		this.name = name;
	}

	@Override
	public int hashCode() {
		final int prime = 29;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((query == null) ? 0 : query.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		TableColumnKey other = (TableColumnKey) obj;
		if (name == null) {
			if (other.name != null) return false;
		}
		else if (!name.equals(other.name)) {
			return false;
		}
		if (query == null) {
			return other.query == null;
		}
		else {
			return query.equals(other.query);
		}
	}
}
