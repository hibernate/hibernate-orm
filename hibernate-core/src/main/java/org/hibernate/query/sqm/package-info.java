/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */

/**
 * This package defines a semantic model of HQL queries.
 *
 * <h3>Semantic Query Model (SQM)</h3>
 *
 * An SQM is a tree representing the semantic interpretation of a query.
 * It's "semantic" in the sense that it contains more information than a
 * simple syntax tree.
 *
 * <h3>HQL</h3>
 *
 * HQL is interpreted as an SQM with the help of an ANTRL-generated parser.
 * The class {@link org.hibernate.query.hql.internal.SemanticQueryBuilder}
 * is responsible for visiting the syntax tree produced by the parser and
 * building an SQM.
 *
 * <h3>Criteria queries</h3>
 *
 * The SQM tree nodes directly implement the JPA criteria query contracts.
 * For example, {@link org.hibernate.query.sqm.tree.from.SqmFrom} implements
 * {@link jakarta.persistence.criteria.From}.
 * <p>
 * Our implementation of the JPA
 * {@link jakarta.persistence.criteria.CriteriaBuilder} interface is
 * {@link org.hibernate.query.sqm.internal.SqmCriteriaNodeBuilder}.
 * It instantiates SQM nodes and arranges them into SQM tree using the
 * standard operations for building a JPA criteria query.
 *
 * <h3>Transforming SQM to SQL</h3>
 *
 * The package {@link org.hibernate.sql.ast} defines an AST representing
 * SQL. To generate SQL from SQM, we must transform the SQM tree to a
 * SQL AST tree. This process is described
 * {@linkplain org.hibernate.query.hql here}, and is handled by a
 * {@link org.hibernate.query.sqm.sql.internal.StandardSqmTranslator}
 * and a {@link org.hibernate.sql.ast.SqlAstTranslator}.
 *
 * @apiNote This entire package is in an incubating state.
 */
@Incubating
package org.hibernate.query.sqm;

import org.hibernate.Incubating;
