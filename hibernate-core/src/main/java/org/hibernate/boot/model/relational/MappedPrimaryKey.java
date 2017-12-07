/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.relational;

/**
 * Contract for a relational primary key constraint.
 *
 * @author Chris Cranford
 */
public interface MappedPrimaryKey extends MappedConstraint {
	/**
	 * Returns the prefix to append to a primary key constraint.
	 */
	String generatedConstraintNamePrefix();
}
