/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.criteria;

import org.hibernate.query.sqm.tree.from.SqmRoot;

/**
 * @author Steve Ebersole
 */
public interface JpaManipulationCriteria<E> extends JpaQueryableCriteria<E> {
	/**
	 * Get the root path that is the target of the DML statement.
	 */
	JpaRoot<E> getTarget();

	/**
	 * Set the root path
	 */
	void setTarget(SqmRoot<E> root);
}
