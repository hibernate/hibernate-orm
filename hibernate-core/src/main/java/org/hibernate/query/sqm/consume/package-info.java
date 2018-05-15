/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

/**
 *
 * <p/>
 * Once you have received a {@link org.hibernate.query.sqm.tree.SqmStatement} from
 * SemanticQueryInterpreter you can:<ul>
 *     <li>
 *         "Split" it (if it is a {@link org.hibernate.query.sqm.tree.SqmSelectStatement})
 *         using {@link org.hibernate.query.sqm.consume.spi.QuerySplitter}
 *     </li>
 *     <li>
 *         Create a walker/visitor for it using {@link org.hibernate.query.sqm.consume.spi.BaseSemanticQueryWalker}
 *     </li>
 * </ul>
 */
package org.hibernate.query.sqm.consume;
