/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast.tree.from;

/**
 * Marker interface for TableGroup impls that are virtual - should not be rendered
 * into the SQL.
 *
 * @author Steve Ebersole
 */
public interface VirtualTableGroup extends TableGroup {
	TableGroup getUnderlyingTableGroup();

	@Override
	default boolean isVirtual() {
		return true;
	}
}
