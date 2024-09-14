/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.sqm.function;

/**
 * The kind of function e.g. normal, aggregate etc.
 *
 * @author Christian Beikov
 */
public enum FunctionKind {
	NORMAL,
	AGGREGATE,
	ORDERED_SET_AGGREGATE,
	WINDOW
}
