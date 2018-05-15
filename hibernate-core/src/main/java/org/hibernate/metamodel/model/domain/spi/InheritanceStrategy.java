/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.spi;

/**
 * @author Steve Ebersole
 */
public enum InheritanceStrategy {
	/**
	 * No inheritance.  Similar to {@link #DISCRIMINATOR} in that there is just a
	 * single table, but with no discriminator column/formula.
	 */
	NONE,
	/**
	 * Uses a dedicated column to store a value indicating the row's concrete type.
	 * Inherently a single-table strategy.
	 */
	DISCRIMINATOR,
	/**
	 * Strategy using a complete table per each concrete subclass - the superclass columns
	 * are defined on each subclass table.  Querying is done via a SQL UNION over all the
	 * hierarchy tables.
	 */
	UNION,
	/**
	 * Strategy using a table per class, joined together.  Highly normalized.
	 */
	JOINED
}
