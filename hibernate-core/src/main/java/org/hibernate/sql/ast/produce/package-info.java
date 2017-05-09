/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

/**
 * This package defines support for producing an SQL AST tree ({@link org.hibernate.sql.ast.tree}),
 * as well as 2 implementations of suh tree generation:<ul>
 *     <li>
 *         Query sources (HQL, Criteria, etc) which produce SQL AST trees based on an SQM tree.
 *         See {@link org.hibernate.sql.ast.produce.sqm}
 *     </li>
 *     <li>
 *         Persister-based load, remove, etc handling which directly produces
 *     </li>
 * </ul>
 * <p/>
 * In either case, persisters are responsible for generating the various SQL AST
 * sub-trees such as its contributions to a QuerySpec's FROM clause, SELECT
 * clause, etc.  It was decided to have persisters directly produce SQL AST trees
 * in handling _Persister-based load, remove, etc calls_ because:<ul>
 *     <li>It already knows how to generate the necessary sub-trees.</li>
 *     <li>Is more performant than generation the SQM view and then walking that SQM.</li>
 * </ul>
 */
package org.hibernate.sql.ast.produce;
