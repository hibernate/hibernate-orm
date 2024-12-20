/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.mapping;

import java.util.List;

/**
 * Used as part of circularity detection
 * <p>
 * Uniquely distinguishes a side of the foreign-key, using
 * that side's table and column(s)
 *
 * @see Association#resolveCircularFetch
 *
 * @author Andrea Boriero
 */
public class AssociationKey {
	private final String table;
	private final List<String> columns;

	public AssociationKey(String table, List<String> columns) {
		this.table = table;
		this.columns = columns;
	}

	public String getTable() {
		return table;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}

		final AssociationKey that = (AssociationKey) o;
		return table.equals( that.table ) && columns.equals( that.columns );
	}

	@Override
	public int hashCode() {
		return table.hashCode();
	}

	private String str;

	@Override
	public String toString() {
		if ( str == null ) {
			str = "AssociationKey(table=" + table + ", columns={" + String.join( ",", columns ) + "})";
		}
		return str;
	}

}
