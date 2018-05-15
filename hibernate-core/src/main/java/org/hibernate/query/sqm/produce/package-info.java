/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

/**
 * = Producing SQM
 *
 * This package defines support for producing SQM trees (see {@link org.hibernate.query.sqm.tree}).
 * The main entry point into producing an SQM tree is
 * {@link org.hibernate.query.sqm.produce.spi.SemanticQueryProducer}, which
 * can be obtained via
 * {@link org.hibernate.query.spi.QueryEngine} which in turn is obtained via
 * {@link org.hibernate.engine.spi.SessionFactoryImplementor#getQueryEngine()}.
 *
 * == From HQL/JPQL
 *
 * `SemanticQueryProducer` defines just a single method for producing SQM based on HQL:
 * {@link org.hibernate.query.sqm.produce.spi.SemanticQueryProducer#interpret(java.lang.String)}.
 * See {@link org.hibernate.query.sqm.produce.internal.hql} for details
 *
 * == From Criteria
 *
 * TDB
 *
 * [NOTE]
 * ====
 * Would be nice later to be able to define our JPA Criteria impls based
 * on the SQM tree itself, meaning no walking to convert.  The conceptual
 * challenge here is that SQM is inherently "scoped" to a context
 * ({@link org.hibernate.query.sqm.produce.spi.SqmCreationContext}, which is
 * generally the Query instance). However, Criteria is inherently un-scoped -
 * it is created from the {@link javax.persistence.criteria.CriteriaBuilder}
 * which is "global" to the SessionFactory/EntityManagerFactory; it must first
 * be turned into a Query through a Session.
 * ====
 *
 * == Exceptions
 *
 * Generally, the interpretation will throw exceptions as one of 3 types:
 *  * {@link org.hibernate.query.sqm.QueryException} and derivatives represent problems with the
 *  		query itself.
 *  * {@link org.hibernate.query.sqm.ParsingException} and derivatives represent errors (potential
 *  		bugs) during parsing.
 *  * {@link org.hibernate.query.sqm.InterpretationException} represents an unexpected problem
 */
package org.hibernate.query.sqm.produce;
