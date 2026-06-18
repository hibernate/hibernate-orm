/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.spi;

import jakarta.annotation.Nonnull;
import org.hibernate.query.criteria.JpaManipulationCriteria;
import org.hibernate.query.criteria.JpaRoot;
import org.hibernate.query.sqm.tree.spi.cte.SqmCteContainer;
import org.hibernate.query.sqm.tree.spi.delete.SqmDeleteStatement;
import org.hibernate.query.sqm.tree.spi.from.SqmRoot;
import org.hibernate.query.sqm.tree.spi.insert.SqmInsertSelectStatement;
import org.hibernate.query.sqm.tree.spi.update.SqmUpdateStatement;

/**
 * Common extension of SqmStatement for DML (delete, update and insert-select)
 * statements.  See {@link SqmDeleteStatement},
 * {@link SqmUpdateStatement} and
 * {@link SqmInsertSelectStatement} respectively
 *
 * @author Steve Ebersole
 */
public interface SqmDmlStatement<E> extends SqmStatement<E>, SqmCteContainer, JpaManipulationCriteria<E> {
	/**
	 * Get the root path that is the target of the DML statement.
	 */
	@Nonnull
	@Override
	SqmRoot<E> getTarget();

	/**
	 * Set the root path
	 */
	@Override
	void setTarget(@Nonnull JpaRoot<E> root);
}
