/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.materialize;

import org.hibernate.mapping.Column;
import org.hibernate.mapping.KeyValue;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.PrimaryKey;
import org.hibernate.mapping.Table;
import org.hibernate.sql.Alias;

/// Explicit materializer for dependent table primary keys.
///
/// Joined-subclass and secondary-table bindings derive their table key from an
/// already-bound entity identifier.  New boot-model paths should materialize the
/// table primary key here instead of routing through hidden mapping-object key
/// creation helpers such as `Join#createPrimaryKey()`.
///
/// @since 9.0
/// @author Steve Ebersole
public final class DependentTableKeyMappingMaterializer {
	private static final Alias PK_ALIAS = new Alias( 15, "PK" );

	private DependentTableKeyMappingMaterializer() {
	}

	public static ResolvedDependentTableKey resolvePrimaryKey(
			PersistentClass ownerBinding,
			String sourceRole,
			Table table,
			KeyValue key) {
		return new ResolvedDependentTableKey( ownerBinding, sourceRole, table, key );
	}

	public static PrimaryKey materializePrimaryKey(ResolvedDependentTableKey dependentTableKey) {
		final Table table = dependentTableKey.table();
		final PrimaryKey primaryKey = new PrimaryKey( table );
		primaryKey.setName( PK_ALIAS.toAliasString( table.getName() ) );
		table.setPrimaryKey( primaryKey );
		for ( Column column : dependentTableKey.keyColumns() ) {
			primaryKey.addColumn( column );
		}
		return primaryKey;
	}
}
