/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

/**
 * @asciidoc
 *
 * = Semantic Query Model (SQM)
 *
 * SQM is a tree (AST) based representation of the *semantic* interpretation of a query
 * (HQL or Criteria).  It is semantic in that it is more than a simple syntax tree.
 *
 * == HQL
 *
 * HQL is interpreted into an SQM with the help of Antlr parsing library.  The main
 * entry point into that transformation is {@link org.hibernate.query.hql.internal.SemanticQueryBuilder}
 *
 * == Criteria
 *
 * The SQM tree implements the JPA Criteria contracts.  Hibernate's implementation of the
 * JPA {@link javax.persistence.criteria.CriteriaBuilder} contract
 * ({@link org.hibernate.query.sqm.internal.SqmCriteriaNodeBuilder}) returns SQM nodes
 * that are arranged into an SQM tree via the normal JPA Criteria building approach
 *
 * Note that this entire package is considered incubating
 */
@org.hibernate.Incubating
package org.hibernate.query.sqm;
