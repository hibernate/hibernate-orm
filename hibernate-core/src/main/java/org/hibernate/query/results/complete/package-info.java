/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

/**
 * Support for immutable result/fetch builder graph nodes built from static sources
 * such as JPA's {@link jakarta.persistence.SqlResultSetMapping} or `hbm.xml` mapping
 * `<resultset/>`.
 *
 * The differentiation from {@link org.hibernate.query.results.dynamic} is that here
 * we have up-front knowledge of the complete mapping graph and can perform optimized
 * resolution process
 */
package org.hibernate.query.results.complete;
