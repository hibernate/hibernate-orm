/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
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
