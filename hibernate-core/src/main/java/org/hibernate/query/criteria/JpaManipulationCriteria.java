/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.criteria;

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
	void setTarget(JpaRoot<E> root);
}
