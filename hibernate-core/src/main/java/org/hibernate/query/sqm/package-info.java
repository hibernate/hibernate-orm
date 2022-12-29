/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

/**
 * This package defines a semantic model of HQL queries.
 *
 * <h3>Semantic Query Model (SQM)</h3>
 *
 * SQM is a tree (AST) based representation of the semantic interpretation of a query
 * (HQL or Criteria). It is "semantic" in the sense that it is more than a simple syntax
 * tree.
 *
 * <h3>HQL</h3>
 *
 * HQL is interpreted into an SQM with the help of ANTRL parsed generator. The entry point
 * into the transformation is {@link org.hibernate.query.hql.internal.SemanticQueryBuilder}.
 *
 * <h3>Criteria queries</h3>
 *
 * The SQM tree implements the JPA criteria query contracts. Our implementation of the JPA
 * {@link jakarta.persistence.criteria.CriteriaBuilder} interface is
 * {@link org.hibernate.query.sqm.internal.SqmCriteriaNodeBuilder}. It produced SQM nodes
 * that are arranged into an SQM tree via the standard JPA criteria building approach.
 *
 * @apiNote This entire package is in an incubating state.
 */
@Incubating
package org.hibernate.query.sqm;

import org.hibernate.Incubating;
