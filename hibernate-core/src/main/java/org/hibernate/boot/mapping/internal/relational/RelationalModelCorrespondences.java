/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.relational;

import org.hibernate.boot.model.relational.Database;

/// Binding-time relational correspondence registry.
///
/// Correspondences capture pairings that are produced while materializing the
/// compatibility mapping model and later need to be queried without relying on
/// legacy collector side channels.
///
/// @since 9.0
/// @author Steve Ebersole
public class RelationalModelCorrespondences {
	private final ColumnNameCorrespondence columnNames;

	public RelationalModelCorrespondences(Database database) {
		columnNames = new ColumnNameCorrespondence( database );
	}

	public ColumnNameCorrespondence columnNames() {
		return columnNames;
	}
}
