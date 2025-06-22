/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast.tree;

import java.util.List;

import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.from.NamedTableReference;

/**
 * Specialization of Statement for mutation (DML) statements
 *
 * @author Steve Ebersole
 */
public interface MutationStatement extends Statement {
	NamedTableReference getTargetTable();
	List<ColumnReference> getReturningColumns();
}
