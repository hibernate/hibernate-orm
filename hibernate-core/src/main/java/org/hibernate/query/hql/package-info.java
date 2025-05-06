/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */

/**
 * Package defining support for HQL, including JPQL as a subset of HQL.
 * <p>
 * Concerns handled by subpackages include:
 * <ul>
 * <li>query language transformation via ANTLR, and
 * <li>query execution via an instance of {@link org.hibernate.query.Query}.
 * </ul>
 * <p>
 * Translation of HQL to SQL involves the following steps:
 * <ul>
 * <li>First, the ANTLR-generated
 *     {@link org.hibernate.grammars.hql.HqlLexer tokenizer} and
 *     {@link org.hibernate.grammars.hql.HqlParser parser} work
 *     in series to parse the text of the query and produce an AST.
 * <li>Next, {@link org.hibernate.query.hql.internal.SemanticQueryBuilder}
 *     translates the AST into an instance of the SQM (Semantic Query
 *     Model) defined in {@link org.hibernate.query.sqm}.
 * <li>Next, {@link org.hibernate.query.sqm.sql.internal.StandardSqmTranslator}
 *     transforms the SQM tree and produces a SQL AST, an instance of
 *     the syntax tree defined by {@link org.hibernate.sql.ast.tree}.
 * <li>Finally, a SQL dialect-specific implementation of
 *     {@link org.hibernate.sql.ast.SqlAstTranslator} produces an
 *     executable SQL statement.
 * </ul>
 *
 * @see org.hibernate.query.hql.HqlTranslator
 * @see org.hibernate.query.hql.spi.SqmQueryImplementor
 * @see org.hibernate.query.sqm.spi.NamedSqmQueryMemento
 */
package org.hibernate.query.hql;
