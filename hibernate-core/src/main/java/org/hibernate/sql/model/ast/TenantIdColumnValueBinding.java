/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.model.ast;

/**
 * A tenant restriction, whose value comes from the session. A null binding
 * denotes a root tenant. Unlike a version restriction, tenant identifiers
 * must always be compared for equality, including in a merge statement.
 */
@org.hibernate.Internal
public final class TenantIdColumnValueBinding extends ColumnValueBinding {

	public TenantIdColumnValueBinding(ColumnValueBinding binding) {
		super( binding.getColumnReference(), binding.getValueExpression() );
	}
}
