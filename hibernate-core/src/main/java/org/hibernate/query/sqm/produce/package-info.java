/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */

/**
 * @asciidoc
 *
 * = Producing SQM
 *
 * This package defines support for producing SQM trees (see {@link org.hibernate.query.sqm.tree}).
 * The main entry point into producing an SQM tree is
 * {@link org.hibernate.query.hql.HqlTranslator}, which
 * can be obtained via
 * {@link org.hibernate.query.spi.QueryEngine} which in turn is obtained via
 * {@link org.hibernate.engine.spi.SessionFactoryImplementor#getQueryEngine()}.
 *
 *
 * == From HQL/JPQL
 *
 * `SemanticQueryProducer` defines just a single method for producing SQM based on HQL:
 * {@link org.hibernate.query.hql.HqlTranslator#translate}.
 * See {@link org.hibernate.query.hql.internal} for details
 *
 *
 * == From Criteria
 *
 * `SemanticQueryProducer` builds SQM directly.
 * See {@link org.hibernate.query.sqm.internal.SqmCriteriaNodeBuilder} for details
 *
 *
 * == Exceptions
 *
 * Generally, the interpretation will throw exceptions as one of 3 types:
 *
 *  * {@link org.hibernate.query.SemanticException} and derivatives represent problems with the
 *  		query itself.
 *  * {@link org.hibernate.query.sqm.ParsingException} and derivatives represent errors (potential
 *  		bugs) during parsing.
 *  * {@link org.hibernate.query.sqm.InterpretationException} represents an unexpected problem
 */
package org.hibernate.query.sqm.produce;
