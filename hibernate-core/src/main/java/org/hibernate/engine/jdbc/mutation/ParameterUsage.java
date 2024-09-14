/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.jdbc.mutation;

/**
 * Describes how a parameter is used in a mutation statement
 *
 * @author Steve Ebersole
 */
public enum ParameterUsage {
	/**
	 * The parameter is used in the update set clause or insert values clause
	 */
	SET,

	/**
	 * The parameter is used in the where clause
	 */
	RESTRICT
}
