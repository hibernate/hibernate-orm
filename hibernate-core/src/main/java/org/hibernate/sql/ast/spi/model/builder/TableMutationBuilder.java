/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast.spi.model.builder;

import org.hibernate.SPI;
import org.hibernate.sql.ast.spi.model.MutatingTableReference;
import org.hibernate.sql.ast.spi.model.TableMutation;

import static org.hibernate.SPI.Role.IMPLEMENT;

/**
 * Generalized contract for building {@link TableMutation} instances
 *
 * @author Steve Ebersole
 */
@SPI( IMPLEMENT )
public interface TableMutationBuilder<M extends TableMutation<?>> {
	/**
	 * Constant for `null`
	 */
	String NULL = "null";
	/**
	 * Constant for `not null`
	 */
	String NOT_NULL = "not null";

	/**
	 * Reference (in the SQL AST sense) to the mutating table
	 */
	MutatingTableReference getMutatingTable();

	/**
	 * Build the mutation descriptor
	 */
	M buildMutation();

	boolean hasValueBindings();
}
