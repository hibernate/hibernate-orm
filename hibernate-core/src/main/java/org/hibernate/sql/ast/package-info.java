/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

/**
 * Contains the approach to SQL generation, execution and result processing that was introduced in 6.0:<ul>
 *     <li>
 *         A tree structure representing a SQL query and an Executor for the various types of SQL
 *         statements (SELECT, DELETE, etc).  See {@link org.hibernate.sql.ast.tree}
 *     </li>
 *     <li>
 *         Support for producing such an SQL AST tree ({@link org.hibernate.sql.ast.produce}).
 *     </li>
 *     <li>
 *         Support for consuming such an SQL AST tree ({@link org.hibernate.sql.ast.consume}), e.g. for transformation
 *         into a JDBC call.
 *     </li>
 * </ul>
 * <p/>
 * Be sure to read the <i>sql-engine-design.adoc</i> in root directory of the Hibernate ORM repo.
 */
package org.hibernate.sql.ast;
