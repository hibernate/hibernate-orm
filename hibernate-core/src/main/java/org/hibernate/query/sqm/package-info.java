/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

/**
 * Semantic Query Model (SQM) defines:<ul>
 *     <li>
 *         An actual query tree modeling the "meaning" (semantic) of a user supplied query.  See
 *         the {@link org.hibernate.query.sqm.tree} package.
 *     </li>
 *     <li>
 *         Support for producing an SQM tree based on HQL/JPQL via Antlr or
 *         based on JPA-based (although eventually extended) Criteria tree via
 *         walking.
 *         <p/>
 *         <i>
 *             Would be nice later to be able to define our JPA Criteria impls based
 *                 on the SQM tree itself, meaning no walking to convert - we'd kind of
 *                 build the SQM as the user defines the Criteria, rather than a separate
 *                 "conversion walk".  However, this absolutely requires folding the SQM
 *                 project into ORM.
 *         </i>
 *         See {@link org.hibernate.query.sqm.produce}.
 *     </li>
 *     <li>
 *         Walking (or generally consuming) SQM trees.  See {@link org.hibernate.query.sqm.consume}.
 *     </li>
 * </ul>
 */
package org.hibernate.query.sqm;
