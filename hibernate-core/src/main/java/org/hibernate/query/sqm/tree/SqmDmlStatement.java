/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree;

import org.hibernate.query.criteria.JpaManipulationCriteria;
import org.hibernate.query.criteria.JpaRoot;
import org.hibernate.query.sqm.tree.cte.SqmCteContainer;
import org.hibernate.query.sqm.tree.delete.SqmDeleteStatement;
import org.hibernate.query.sqm.tree.from.SqmRoot;

/**
 * Common extension of SqmStatement for DML (delete, update and insert-select)
 * statements.  See {@link SqmDeleteStatement},
 * {@link org.hibernate.query.sqm.tree.update.SqmUpdateStatement} and
 * {@link org.hibernate.query.sqm.tree.insert.SqmInsertSelectStatement} respectively
 *
 * @author Steve Ebersole
 */
public interface SqmDmlStatement<E> extends SqmStatement<E>, SqmCteContainer, JpaManipulationCriteria<E> {
	/**
	 * Get the root path that is the target of the DML statement.
	 */
	@Override
	SqmRoot<E> getTarget();

	/**
	 * Set the root path
	 */
	@Override
	void setTarget(JpaRoot<E> root);
}
