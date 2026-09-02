/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast.spi.query.insert;

import java.util.List;
import java.util.function.BiConsumer;

import org.hibernate.sql.ast.spi.query.MutationStatement;
import org.hibernate.sql.ast.spi.query.expression.ColumnReference;

/**
 * Specialization of MutationStatement for inserts
 *
 * @author Steve Ebersole
 */
public interface InsertStatement extends MutationStatement {
	/**
	 * Get all target columns
	 */
	List<ColumnReference> getTargetColumns();

	/**
	 * The number of target columns associated with this insert.
	 *
	 * @implNote By default, returns the size of {@link #getTargetColumns()}
	 * which may be appropriate or not
	 */
	default int getNumberOfTargetColumns() {
		return getTargetColumns().size();
	}

	/**
	 * Iterates each target column
	 */
	void forEachTargetColumn(BiConsumer<Integer, ColumnReference> consumer);
}
