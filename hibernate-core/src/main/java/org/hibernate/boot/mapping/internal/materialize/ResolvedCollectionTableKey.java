/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.materialize;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Table;

/// Resolved key materialization input for a collection table.
///
/// Collection table key materialization still consumes the compatibility
/// collection mapping, but this product makes the table role and ordered owner
/// key columns explicit for callers and future table-key product work.
///
/// @since 9.0
/// @author Steve Ebersole
public class ResolvedCollectionTableKey {
	private final String collectionRole;
	private final Collection collection;
	private final Table table;
	private final List<Column> keyColumns;

	public ResolvedCollectionTableKey(Collection collection) {
		this(
				collection.getRole(),
				collection,
				collection.getCollectionTable(),
				collection.getKey() == null ? List.of() : collection.getKey().getColumns()
		);
	}

	public ResolvedCollectionTableKey(
			String collectionRole,
			Collection collection,
			Table table,
			List<Column> keyColumns) {
		this.collectionRole = collectionRole;
		this.collection = collection;
		this.table = table;
		this.keyColumns = new ArrayList<>( keyColumns );
	}

	public String collectionRole() {
		return collectionRole;
	}

	public Collection collection() {
		return collection;
	}

	public Table table() {
		return table;
	}

	public List<Column> keyColumns() {
		return keyColumns;
	}
}
