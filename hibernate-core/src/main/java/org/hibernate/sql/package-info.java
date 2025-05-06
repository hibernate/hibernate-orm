/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */

/**
 * This package contains helper classes for rendering SQL fragments and SQL statements.
 * <p>
 * Subpackages under this namespace contain the following very important components of
 * Hibernate:
 * <ul>
 * <li>The {@linkplain org.hibernate.sql.ast SQL AST} used for generation of SQL queries.
 * <li>A {@linkplain org.hibernate.sql.model specialized model} for building and executing
 *     mutation statements related to {@linkplain org.hibernate.metamodel.mapping.ModelPart
 *     domain model parts}.
 * <li>An API for {@linkplain org.hibernate.sql.results processing JDBC results} and
 *     producing graphs of entity objects based around a
 *     {@linkplain org.hibernate.sql.results.graph.DomainResultGraphNode domain result graph}.
 * <li>Support for {@linkplain org.hibernate.sql.exec execution} of SQL via JDBC.
 * </ul>
 */
package org.hibernate.sql;
