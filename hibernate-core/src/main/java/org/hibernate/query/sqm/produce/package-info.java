/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

/**
 * @asciidoc
 *
 * = Producing SQM
 *
 * This package defines support for producing SQM trees (see {@link org.hibernate.query.sqm.tree}).
 * The main entry point into producing an SQM tree is
 * {@link org.hibernate.query.hql.SemanticQueryProducer}, which
 * can be obtained via
 * {@link org.hibernate.query.spi.QueryEngine} which in turn is obtained via
 * {@link org.hibernate.engine.spi.SessionFactoryImplementor#getQueryEngine()}.
 *
 *
 * == From HQL/JPQL
 *
 * `SemanticQueryProducer` defines just a single method for producing SQM based on HQL:
 * {@link org.hibernate.query.hql.SemanticQueryProducer#interpret}.
 * See {@link org.hibernate.query.hql.internal} for details
 *
 *
 * == From Criteria
 *
 * Because criteria queries are already typed, `SemanticQueryProducer` offers 3 distinct methods for transforming
 * select, update and delete criteria trees.  Mainly this is done to take advantage of the distinct typing to
 * define better return types.  See
 *
 * 		* {@link org.hibernate.query.criteria.sqm.CriteriaQueryToSqmTransformer#transform}:: For select criteria
 * 			transformation
 * 	    * _update and delete criteria transformations not yet implemented_
 *
 *
 *
 * == Exceptions
 *
 * Generally, the interpretation will throw exceptions as one of 3 types:
 *
 *  * {@link org.hibernate.query.sqm.SemanticException} and derivatives represent problems with the
 *  		query itself.
 *  * {@link org.hibernate.query.sqm.ParsingException} and derivatives represent errors (potential
 *  		bugs) during parsing.
 *  * {@link org.hibernate.query.sqm.InterpretationException} represents an unexpected problem
 */
package org.hibernate.query.sqm.produce;
