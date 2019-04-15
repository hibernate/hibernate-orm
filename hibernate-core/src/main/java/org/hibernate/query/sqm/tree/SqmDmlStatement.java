/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree;

import org.hibernate.query.criteria.JpaManipulationCriteria;
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
public interface SqmDmlStatement<E> extends SqmStatement<E>, JpaManipulationCriteria<E> {
	/**
	 * Get the root path that is the target of the DML statement.
	 */
	SqmRoot<E> getTarget();

	/**
	 * Set the root path
	 */
	void setTarget(SqmRoot<E> root);
}
