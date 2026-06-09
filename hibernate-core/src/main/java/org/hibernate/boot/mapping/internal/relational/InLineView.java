/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.relational;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.mapping.Table;

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
public record InLineView(Identifier logicalName, Table binding) implements TableReference {
	@Override
	public Identifier logicalName() {
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
	public Table binding() {
		return binding;
	}
}
