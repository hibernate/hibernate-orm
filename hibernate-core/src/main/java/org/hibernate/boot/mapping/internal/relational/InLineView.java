/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.relational;

import org.hibernate.relational.naming.spi.LogicalName;

/// Table reference for a from-clause subquery.
///
/// `@Subselect` entities use an inline SQL query as their table expression.  The
/// mapping table is registered so associations and entity metadata can refer to
/// it, but it is not exportable schema state.
///
/// @see org.hibernate.annotations.Subselect
///
/// @since 9.0
/// @author Steve Ebersole
public record InLineView(LogicalName logicalName, org.hibernate.mapping.InlineView binding) implements TableReference {
	@Override
	public LogicalName logicalName() {
		return logicalName;
	}

	public String getQuery() {
		return binding.getSubselect();
	}

	@Override
	public boolean exportable() {
		return false;
	}

	@Override
	public org.hibernate.mapping.InlineView binding() {
		return binding;
	}
}
