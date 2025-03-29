/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.model.ast.builder;

import org.hibernate.sql.model.ast.MutatingTableReference;
import org.hibernate.sql.model.ast.TableMutation;

/**
 * Generalized contract for building {@link TableMutation} instances
 *
 * @author Steve Ebersole
 */
public interface TableMutationBuilder<M extends TableMutation<?>> {
	/**
	 * Constant for `null`
	 */
	String NULL = "null";

	/**
	 * Reference (in the SQL AST sense) to the mutating table
	 */
	MutatingTableReference getMutatingTable();

	/**
	 * Build the mutation descriptor
	 */
	M buildMutation();
}
