/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.naming.internal;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.mapping.InlineView;
import org.hibernate.mapping.NamedTable;
import org.hibernate.mapping.Table;

/// Adapts table names to the existing Identifier-based implicit naming callbacks.
/// This is a callback boundary, not relational name storage.
///
/// @author Steve Ebersole
public final class ImplicitNamingSourceHelper {
	private ImplicitNamingSourceHelper() {}

	public static Identifier tableName(Table table) {
		return table instanceof InlineView view
				? PhysicalNamingStrategyHelper.identifier( view.getLogicalName() )
				: PhysicalNamingStrategyHelper.physicalIdentifier( ((NamedTable) table).getPhysicalName().objectName() );
	}
}
