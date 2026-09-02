/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast.spi.query;

import java.util.List;

import org.hibernate.sql.ast.spi.Statement;
import org.hibernate.sql.ast.spi.query.expression.ColumnReference;
import org.hibernate.sql.ast.spi.query.from.NamedTableReference;
import org.hibernate.sql.spi.mutation.MutationTarget;

/**
 * Specialization of Statement for mutation (DML) statements
 *
 * @author Steve Ebersole
 */
public interface MutationStatement extends Statement {
	NamedTableReference getTargetTable();

	MutationTarget getMutationTarget();

	List<ColumnReference> getReturningColumns();
}
