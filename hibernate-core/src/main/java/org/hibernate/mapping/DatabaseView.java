/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.mapping;

import org.hibernate.relational.naming.spi.QualifiedPhysicalName;
import org.hibernate.relational.naming.spi.PhysicalName;
import org.hibernate.boot.model.relational.Namespace;

/// Mapping for a named database view with an immutable defining query.
///
/// @author Steve Ebersole
public final class DatabaseView extends NamedTable {
	private final String query;

	public DatabaseView(String contributor, Namespace namespace, PhysicalName name, String query) {
		super( contributor, namespace, name );
		if ( name == null || query == null || query.isBlank() ) {
			throw new IllegalArgumentException( "A database view requires a name and a nonblank query" );
		}
		this.query = query;
	}

	public DatabaseView(String contributor, QualifiedPhysicalName name, String query) {
		super( contributor, name );
		if ( query == null || query.isBlank() ) {
			throw new IllegalArgumentException( "A database view requires a nonblank query" );
		}
		this.query = query;
	}

	@Override
	public String getViewQuery() {
		return query;
	}
}
