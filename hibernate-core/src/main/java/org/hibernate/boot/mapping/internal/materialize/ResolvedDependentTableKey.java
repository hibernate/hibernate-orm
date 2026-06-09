/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.materialize;

import java.util.ArrayList;
import java.util.List;

import jakarta.annotation.Nullable;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.KeyValue;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Table;

/// Resolved primary-key materialization input for a dependent table.
///
/// Dependent tables derive their key columns from an already-bound entity
/// identifier.  This product captures the table role, local key value, and
/// ordered key columns before the mapping [org.hibernate.mapping.PrimaryKey] is
/// materialized.
///
/// @since 9.0
/// @author Steve Ebersole
public class ResolvedDependentTableKey {
	private final PersistentClass ownerBinding;
	private final String sourceRole;
	private final Table table;
	private final KeyValue key;
	private final List<Column> keyColumns;

	public ResolvedDependentTableKey(
			@Nullable PersistentClass ownerBinding,
			String sourceRole,
			Table table,
			KeyValue key) {
		this( ownerBinding, sourceRole, table, key, key.getColumns() );
	}

	public ResolvedDependentTableKey(
			@Nullable PersistentClass ownerBinding,
			String sourceRole,
			Table table,
			KeyValue key,
			List<Column> keyColumns) {
		this.ownerBinding = ownerBinding;
		this.sourceRole = sourceRole;
		this.table = table;
		this.key = key;
		this.keyColumns = new ArrayList<>( keyColumns );
	}

	public @Nullable PersistentClass ownerBinding() {
		return ownerBinding;
	}

	public String sourceRole() {
		return sourceRole;
	}

	public Table table() {
		return table;
	}

	public KeyValue key() {
		return key;
	}

	public List<Column> keyColumns() {
		return keyColumns;
	}
}
