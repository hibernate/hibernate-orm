/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

/**
 * Package defining Hibernate's SQL generation and execution based on SQM and its own "SQL AST" (converted from SQM)<ul>
 *     <li>{@link org.hibernate.sql.sqm.ast} represents the actual "SQL AST" definitions</li>
 *     <li>{@link org.hibernate.sql.sqm.convert} represents the conversion from an SQM into a "SQL AST" plus other needed goodies</li>
 *     <li>{@link org.hibernate.sql.sqm.exec} contains the actual JDBC/SQL execution</li>
 * </ul>
 */
package org.hibernate.sql.sqm;
