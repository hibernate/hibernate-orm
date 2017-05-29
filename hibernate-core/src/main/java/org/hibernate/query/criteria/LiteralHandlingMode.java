/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.criteria;

/**
 * This enum defines how literals are handled by JPA Criteria.
 *
 * By default ({@code AUTO}), Criteria queries uses bind parameters for any literal that is not a numeric value.
 *
 * However, to increase the likelihood of JDBC statement caching,
 * you might want to use bind parameters for numeric values too.
 * The {@code BIND} mode will use bind variables for any literal value.
 *
 * The {@code INLINE} mode will inline literal values as-is.
 * To prevent SQL injection, never use {@code INLINE} with String variables.
 * Always use constants with the {@code INLINE} mode.
 *
 * @author Vlad Mihalcea
 */
public enum LiteralHandlingMode {

	AUTO,
	BIND,
	INLINE
}
