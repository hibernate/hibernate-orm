/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

/**
 * Package for producing SQM trees (see {@link org.hibernate.query.sqm.tree}).
 * The main entry point into producing an SQM tree is
 * {@link org.hibernate.query.sqm.produce.spi.SemanticQueryProducer}, which
 * can be obtained via
 * {@link org.hibernate.engine.spi.SessionFactoryImplementor#getSemanticQueryProducer()}
 * <p/>
 * For HQL/JPQL parsing, pass in the query string and get back the SQM tree as a
 * {@link org.hibernate.query.sqm.tree.SqmStatement}.
 * <p/>
 * For Criteria queries ...
 * <p/>
 * Generally, the interpretation will throw exceptions as one of 3 types:<ul>
 *     <li>
 *         {@link org.hibernate.query.sqm.QueryException} and derivatives represent problems with the
 *         query itself.
 *     </li>
 *     <li>
 *         {@link org.hibernate.query.sqm.ParsingException} and derivatives represent errors (potential
 *         bugs) during parsing.
 *     </li>
 *     <li>
 *         {@link org.hibernate.query.sqm.InterpretationException} represents an unexpected problem
 *         during interpretation; this may indicate a problem with the query or a bug in the parser,
 *         we just are not sure as it was unexpected.
 *     </li>
 * </ul>
 */
package org.hibernate.query.sqm.produce;
