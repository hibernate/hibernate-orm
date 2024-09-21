/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast.tree.predicate;

/**
 * Something that can contain predicates
 *
 * @author Steve Ebersole
 */
public interface PredicateContainer {
	/**
	 * Apply a predicate to this container
	 */
	void applyPredicate(Predicate predicate);
}
